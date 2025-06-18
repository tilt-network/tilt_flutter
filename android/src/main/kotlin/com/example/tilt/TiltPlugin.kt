package com.example.tilt

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.os.Build
import io.flutter.plugin.common.EventChannel
import kotlinx.coroutines.*
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import com.example.tilt.Tilt

/** TiltPlugin */
class TiltPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  private lateinit var eventSink: EventChannel.EventSink

  private lateinit var tiltSdk: Tilt

  private val logLines = mutableListOf<String>()
  private val job = SupervisorJob()
  private val scope = CoroutineScope(Dispatchers.Default + job)

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "tilt")
    channel.setMethodCallHandler(this)

    // EventChannel(flutterPluginBinding.binaryMessenger, "tilt/logs")
    //   .setStreamHandler(object : EventChannel.StreamHandler {
    //     override fun onListen(arguments: Any?, sink: EventChannel.EventSink) {
    //       eventSink = sink
    //       startFakeLogging()
    //     }
    //     override fun onCancel(arguments: Any?) {
    //       job.cancel()
    //     }
    //   })
    EventChannel(flutterPluginBinding.binaryMessenger, "tilt/logs")
      .setStreamHandler(object: EventChannel.StreamHandler {
        override fun onListen(args: Any?, sink: EventChannel.EventSink) {
          eventSink = sink

          // inicializa e starta o SDK

          tiltSdk = Tilt(flutterPluginBinding.applicationContext, "pk_3NqPrvpe6nDkdtyS1gJt4kX_4MQ").apply {
            onLogMessage = { line ->
              scope.launch {
                // adiciona à lista
                logLines.add(line)
                // envia direto pro Flutter
                withContext(Dispatchers.Main) {
                  eventSink.success(ArrayList(logLines))
                }
              }
              // scope.launch {
              //   logLines.add(line)
              //   Log.d("TiltPlugin", line)
              //   withContext(Dispatchers.Main) {
              //     eventSink.success(ArrayList(logLines))
              //   }
              // }
            }
          }
          tiltSdk.start()
        }

        override fun onCancel(args: Any?) {
          tiltSdk.stop()
          job.cancel()
        }
      })
  }

  private fun startFakeLogging() {
    scope.launch {
      while (isActive) {
        val fake = "Fake log @ ${System.currentTimeMillis() % 100000}"
        logLines.add(fake)
        Log.d("TiltPlugin", fake)
        // enviar na Main
        withContext(Dispatchers.Main) {
          eventSink.success(ArrayList(logLines))
        }
        delay(2_000)
      }
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "getLogLines" -> {
        // logLines é MutableList<String>, converta pra ArrayList (serializável)
        result.success(ArrayList(logLines))
      }
      "getPlatformVersion" -> {
        result.success("Android ${Build.VERSION.RELEASE}")
      }
      else -> result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    scope.cancel()
    channel.setMethodCallHandler(null)
  }
}
