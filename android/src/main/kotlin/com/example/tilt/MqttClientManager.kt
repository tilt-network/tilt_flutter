import android.content.Context
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.example.tilt.getDeviceMetadata
import java.nio.charset.StandardCharsets
import java.util.UUID

class MqttClientManager(
    private val context: Context,
    private var device: DeviceStatus,
    private val serverHost: String,
    private val username: String,
    private val password: String,
    private val log: (String) -> Unit = { Log.d("MqttClient", it) }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _tasks = MutableSharedFlow<Task>(extraBufferCapacity = 64)
    val tasks: SharedFlow<Task> = _tasks
    private val environment = "development"
    // private val environment = "staging"

    private val statusTopic = "${environment}/${device.deviceToken}/status"
    private val taskTopic = "${environment}/${device.deviceToken}/task"
    private val client = MqttClient.builder()
        .useMqttVersion5()
        .identifier("android_${device.deviceToken}")
        .serverHost(serverHost)
        .serverPort(8883)
        .sslWithDefaultConfig()
        .simpleAuth()
        .username(username)
        .password(password.toByteArray())
        .applySimpleAuth()
        .buildAsync()

    fun connectAndSubscribe() {
        Log.i("topic", taskTopic)
        client.connect().whenComplete { _, throwable ->
            if (throwable == null) {
                client.subscribeWith()
                    .topicFilter(taskTopic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .callback { publish: Mqtt5Publish ->
                        val buffer = publish.payload.orElse(null) ?: return@callback

                        val data = ByteArray(buffer.remaining())
                        buffer.duplicate().get(data)
                        val messageStr = String(data, StandardCharsets.UTF_8)
                        scope.launch {
                            try {
                                val task = Task.fromJson(messageStr)
                                    ?: throw IllegalArgumentException("Invalid task format")
                                scope.launch {
                                    _tasks.emit(task)
                                }
                            } catch (e: Exception) {
                                Log.e("MqttClientManager", "Error processing task: ${e.message}")
                            }
                        }
                    }
                    .send()

                scope.launch {
                    while (isActive) {
                        sendStatus()
                        delay(20000)
                    }
                }
            } else {
                println("Connection failed: ${throwable.message}")
            }
        }
    }

    fun sendStatus() {
        val deviceData = getDeviceMetadata(context)
        device.deviceToken         = deviceData.deviceToken ?: UUID.randomUUID().toString()
        device.model               = deviceData.model
        device.brand               = deviceData.brand
        device.year                = deviceData.year
        device.batteryLevel        = deviceData.batteryLevel
        device.batteryState        = deviceData.batteryStatus
        device.isInBatterySaveMode = deviceData.isInBatterySaveMode
        device.available           = true
        device.scoreCpu            = 0
        device.scoreMem            = 0

        val msg = device.toStatusString()

        val response = client.publishWith()
            .topic(statusTopic)
            .payload(msg.toByteArray(StandardCharsets.UTF_8))
            .qos(MqttQos.AT_LEAST_ONCE)
            .send()
        log("Status sent: $msg | Response: $response")
    }
}
