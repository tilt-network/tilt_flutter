package com.example.tilt

import DeviceStatus
import DeviceData
import MqttClientManager
import java.util.Calendar
import Task
import WasmProcessor
import android.R
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable

class TiltSdkService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var mqttManager: MqttClientManager
    private lateinit var wasmProcessor: WasmProcessor
    private var currentTask: Task? = null

    override fun onCreate() {
        Log.i("init", "test")
        log("TiltSdkService created")
        super.onCreate()

        startForeground(NOTIFICATION_ID, buildNotification())

        val prefs = getSharedPreferences("tilt_prefs", Context.MODE_PRIVATE)
        val publicKey = prefs.getString("public_key", null) ?: run {
            log("publicKey not found in prefs")
            return
        }
        var deviceId = prefs.getString("deviceId", null)

        scope.launch {
            val deviceData = getDeviceMetadata(applicationContext)
            if (deviceId.isNullOrEmpty()) {
                val payload = DeviceCreateRequest(publicKey, "android", deviceData.osVersion, deviceData.brand, deviceData.model, deviceData.batteryLevel)
                val device = TiltApiClient.createDevice(payload)
                if (device == null) {
                    log("Failed to create device on server")
                    return@launch
                }
                applicationContext.getSharedPreferences("tilt_prefs", Context.MODE_PRIVATE)
                    .edit {
                        putString("deviceId", device.id)
                        putString("model", device.model)
                        putString("brand", device.brand)
                        putString("year", deviceData.year.toString())
                        // putString("scoreCpu", deviceData.scoreCpu.toString())
                        // putString("scoreMem", deviceData.scoreMem.toString())
                        putString("publicKey", publicKey)
                    }
                deviceId = device.id
            }
            log("TiltSdkService started with publicKey: $publicKey, deviceId: $deviceId, deviceToken: ${deviceData.deviceToken}")
            val status = DeviceStatus(
                deviceId            = deviceId,
                deviceToken         = deviceData.deviceToken ?: UUID.randomUUID().toString(),
                model               = deviceData.model,
                brand               = deviceData.brand,
                year                = 0,
                batteryLevel        = deviceData.batteryLevel,
                batteryState        = deviceData.batteryStatus,
                isInBatterySaveMode = deviceData.isInBatterySaveMode,
                available           = true,
                scoreCpu            = 0,
                publicKey           = publicKey,
                scoreMem            = 0
            )
            var mqttCredentials = TiltApiClient.fetchCredentials(publicKey = publicKey)
            log("Fetched MQTT credentials: $mqttCredentials")
            mqttManager = MqttClientManager(applicationContext,
                device=status, mqttCredentials.serverHost,
                mqttCredentials.username,
                mqttCredentials.password,
                ::log)
            wasmProcessor = WasmProcessor(applicationContext)

            // wasmProcessor.init()
            mqttManager.connectAndSubscribe()


            mqttManager.tasks.collectLatest { task ->
                // Log.i("TiltSdkService", "Received message: $task")
                // log("TiltSdkService: Received message: $task")
                log("TiltSdkService: task received")
                try {
                    handleTask(task.taskId, task.programPath, task.dataPath)
                    log("TiltSdkService: Task ${task.taskId} processed successfully")
                } catch (e: Exception) {
                    e.printStackTrace()
                    log("TiltSdkService: Error processing task ${task.taskId}: ${e.message}")
                }
            }
        }
    }

    private suspend fun handleTask(taskId: String, programPath: String, dataPath: String) {
        val programBytes = TiltApiClient.fetchProgram(programPath)
        val dataBytes = TiltApiClient.fetchData(dataPath)
        val result = wasmProcessor.execute(programBytes, dataBytes)
        val text = String(result, Charsets.UTF_8)
        log("Task $taskId processed with result: $text | size: ${result.size} bytes")
        TiltApiClient.sendDataProcessed(
            data = result,
            taskId = taskId,
            unprocessedPath = dataPath
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        wasmProcessor.close()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // android.os.Debug.waitForDebugger()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        val restart = Intent(applicationContext, TiltSdkService::class.java)
        applicationContext.startForegroundService(restart)
    }

    private fun buildNotification(): Notification {
        val channel =
                NotificationChannel(
                        CHANNEL_ID,
                        "Tilt SDK Service",
                        NotificationManager.IMPORTANCE_LOW
                )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tilt SDK Running")
                .setContentText("Processing tasks in background...")
                .setSmallIcon(R.drawable.ic_media_play)
                .build()
    }

    companion object {
        private const val CHANNEL_ID = "tilt_sdk_channel"
        private const val NOTIFICATION_ID = 1

        var logCallback: ((String) -> Unit)? = null

        fun start(context: Context) {
            Intent(context, TiltSdkService::class.java).also {
                context.startService(it)
            }
        }

        fun stop(context: Context) {
            Intent(context, TiltSdkService::class.java).also {
                context.stopService(it)
            }
        }

        // fun start(context: Context) {
        //     val intent = Intent(context, TiltSdkService::class.java)
        //     context.startForegroundService(intent)
        // }
        //
        // fun stop(context: Context) {
        //     context.stopService(Intent(context, TiltSdkService::class.java))
        // }
    }

    private fun log(text: String) {
        Log.i("TiltSdkService", text)
        logCallback?.invoke(text)
    }

    override fun onBind(intent: Intent?): IBinder? = null

}

data class DeviceMetadata(
        val brand: String,
        val model: String,
        val manufacturer: String,
        val osVersion: String,
        val sdkInt: Int,
        var year: Int = 0,
        val batteryLevel: Int,
        val deviceToken: String?,
        val hardware: String,
        val batteryStatus: String,
        val isInBatterySaveMode: Boolean
)

@SuppressLint("HardwareIds")
fun getDeviceMetadata(context: Context): DeviceMetadata {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    val buildYear = Calendar.getInstance().apply {
        timeInMillis = Build.TIME
    }.get(Calendar.YEAR)

    return DeviceMetadata(
            brand = Build.BRAND,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            osVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            year = buildYear,
            batteryLevel = batteryLevel,
            deviceToken = androidId,
            hardware = Build.HARDWARE,
            batteryStatus = if (isDeviceCharging(context)) "charging" else "discharging",
            isInBatterySaveMode = isBatterySaverOn(context)
    )
}

fun isDeviceCharging(context: Context): Boolean {
    val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus = context.registerReceiver(null, intentFilter)

    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return false

    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
}

fun isBatterySaverOn(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        powerManager?.isPowerSaveMode ?: false
    } else {
        false
    }
}

@Serializable
data class DeviceCreateRequest(
    var public_key: String,
    val platform: String,
    val version: String,
    val brand: String,
    val model: String,
    val battery: Int
)