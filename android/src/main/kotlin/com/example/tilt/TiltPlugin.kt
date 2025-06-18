package com.example.tilt

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.*
import technology.tilt.sdk.Tilt

/** TiltPlugin */
class TiltPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var eventSink: EventChannel.EventSink
    private lateinit var tiltSdk: Tilt
    private lateinit var binding: FlutterPluginBinding

    private var publicKey: String? = null
    private var environment: String? = "production"

    private val logLines = mutableListOf<String>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        binding = flutterPluginBinding
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "tilt")
        channel.setMethodCallHandler(this)

        EventChannel(flutterPluginBinding.binaryMessenger, "tilt/logs")
                .setStreamHandler(
                        object : EventChannel.StreamHandler {
                            override fun onListen(args: Any?, sink: EventChannel.EventSink) {
                                eventSink = sink

                                scope.launch(Dispatchers.Main) {
                                    eventSink.success(ArrayList(logLines))
                                }

                                // val key = publicKey ?: error("plugin not initialized")
                                // val env = environment ?: error("plugin not initialized")
                                // tiltSdk = Tilt(flutterPluginBinding.applicationContext, key,
                                // env).apply {
                                //     onLogMessage = { line ->
                                //         scope.launch {
                                //             logLines.add(line)
                                //             withContext(Dispatchers.Main) {
                                //                 eventSink.success(ArrayList(logLines))
                                //             }
                                //         }
                                //     }
                                // }
                                // tiltSdk.start()
                            }

                            override fun onCancel(args: Any?) {
                                // tiltSdk.stop()
                                job.cancel()
                            }
                        }
                )
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "initialize" -> {
                publicKey = call.argument("publicKey")
                environment = call.argument("environment")
                result.success(null)

                publicKey = call.argument<String>("publicKey")
                environment = call.argument<String>("environment") ?: "production"
                val key = publicKey ?: error("plugin not initialized: publicKey is null")
                tiltSdk =
                        Tilt(binding.applicationContext, key, environment ?: "production").apply {
                            onLogMessage = { line ->
                                scope.launch {
                                    logLines.add(line)
                                    withContext(Dispatchers.Main) {
                                        if (::eventSink.isInitialized) {
                                            eventSink.success(ArrayList(logLines))
                                        }
                                    }
                                }
                            }
                        }
                // tiltSdk = Tilt(flutterPluginBinding.applicationContext, key, env).apply {
                //     onLogMessage = { line ->
                //         scope.launch {
                //             logLines.add(line)
                //             withContext(Dispatchers.Main) {
                //                 eventSink.success(ArrayList(logLines))
                //             }
                //         }
                //     }
                // }
                // tiltSdk.start()
                tiltSdk.start()
                result.success(null)
            }
            "getLogLines" -> {
                // logLines é MutableList<String>, converta pra ArrayList (serializável)
                result.success(ArrayList(logLines))
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        scope.cancel()
        channel.setMethodCallHandler(null)
    }
}
