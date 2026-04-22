package technology.tilt.network

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.*
import technology.tilt.sdk.Tilt
import technology.tilt.sdk.TiltLogBus

/** TiltPlugin */
class TiltPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var binding: FlutterPluginBinding

    private val logLines = mutableListOf<String>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var logCollectionJob: Job? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
        binding = flutterPluginBinding
        Tilt.initialize(flutterPluginBinding.applicationContext)

        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "tilt")
        channel.setMethodCallHandler(this)

        EventChannel(flutterPluginBinding.binaryMessenger, "tilt/logs")
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(args: Any?, sink: EventChannel.EventSink) {
                    // replay already-buffered log lines
                    scope.launch(Dispatchers.Main) {
                        sink.success(ArrayList(logLines))
                    }
                    // collect new events from TiltLogBus
                    logCollectionJob = scope.launch {
                        TiltLogBus.events.collect { event ->
                            val line = "[${event.level}] ${event.message}"
                            logLines.add(line)
                            withContext(Dispatchers.Main) {
                                sink.success(ArrayList(logLines))
                            }
                        }
                    }
                }

                override fun onCancel(args: Any?) {
                    logCollectionJob?.cancel()
                    logCollectionJob = null
                }
            })
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "initialize" -> {
                val key = call.argument<String>("publicKey")
                    ?: return result.error("MISSING_ARG", "publicKey is required", null)
                val env = call.argument<String>("environment") ?: "production"
                Tilt.start(binding.applicationContext, key, env)
                result.success(null)
            }
            "getLogLines" -> {
                result.success(ArrayList(logLines))
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        logCollectionJob?.cancel()
        scope.cancel()
        Tilt.stop(binding.applicationContext)
        channel.setMethodCallHandler(null)
    }
}
