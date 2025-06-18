package com.example.tilt

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit

class Tilt(private val context: Context, private val publicKey: String) {
    var onLogMessage: ((String) -> Unit)? = null

    fun start() {
        context.getSharedPreferences("tilt_prefs", Context.MODE_PRIVATE)
            .edit {
                putString("public_key", publicKey)
            }

        log("TiltSdk initialized")
        TiltSdkService.logCallback = onLogMessage

        // TiltSdkService.start(context)
        Intent(context, TiltSdkService::class.java).also { intent ->
            intent.putExtra("public_key", publicKey)
            context.startForegroundService(intent)
        }
        // Intent(context, TiltSdkService::class.java).also {
        //     context.startService(it)
        // }
    }

    fun stop() {
        log("TiltSdk stopped")
        Intent(context, TiltSdkService::class.java).also {
            context.stopService(it)
        }
        TiltSdkService.logCallback = null
        // TiltSdkService.stop(context)
    }

    private fun log(msg: String) {
        Log.i("TiltSdk", msg)
        onLogMessage?.invoke(msg)
    }
}
