// package android.src.main.kotlin.com.example.tilt
// package com.example.tilt

import Device
import DeviceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Callback
import java.io.IOException
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.serialization.serializer
import kotlinx.serialization.builtins.serializer
import com.example.tilt.DeviceCreateRequest
import kotlinx.serialization.serializer
import kotlinx.serialization.builtins.serializer

object TiltApiClient {
    private val client = OkHttpClient()
    // private val BASE_URL: String = "http://localhost:3000"
    private const val BASE_URL = "http://10.0.2.2:3000"
    // private const val BASE_URL = "https://staging.tilt.rest"

    suspend fun fetchProgram(endpoint: String): ByteArray = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$BASE_URL/programs/files/$endpoint").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            resp.body?.bytes() ?: error("Empty response body")
        }
    }
    suspend fun fetchData(endpoint: String): ByteArray = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$BASE_URL/tasks/data/$endpoint").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            resp.body?.bytes() ?: error("Empty response body")
        }
    }

    fun fetchCredentials(publicKey: String): MqttCredentials {
        val request = Request.Builder()
            .url("$BASE_URL/mqtt/credentials?public_key=$publicKey")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("HTTP ${response.code}")
        val body = response.body?.string() ?: error("Empty response body")

        return Json.decodeFromString(MqttCredentials.serializer(), body)
    }

    fun createDevice(payload: DeviceCreateRequest): Device? {
        val json = Json.encodeToString(DeviceCreateRequest.serializer(), payload)
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/devices")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("HTTP ${response.code}")
        val responseBody = response.body?.string() ?: error("Empty response body")

        return Device.fromJson(responseBody)
        // return Json.decodeFromString(DeviceStatus.serializer(), responseBody)
    }

    fun sendDataProcessed(
        taskId: String,
        unprocessedPath: String,
        data: ByteArray
    ) {
        // replace wasm for .dat on path
        val processedPath = unprocessedPath
            .replace("/unprocessed/", "/processed/")
            .replace(Regex("\\.wasm$"), ".dat")
        val url = "$BASE_URL/tasks/processed_data"
        val mediaTypeBinary = "application/octet-stream".toMediaTypeOrNull()

        // Create the multipart body
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("task_id", taskId)
            .addFormDataPart("path", processedPath)
            .addFormDataPart(
                name = "data",
                filename = "data.bin",
                body = data.toRequestBody(mediaTypeBinary)
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        println("Upload failed: ${it.code}")
                    } else {
                        println("Upload successful: ${it.body?.string()}")
                    }
                }
            }
        })
    }
}

@Serializable
data class MqttCredentials(
    val username: String,
    val password: String,
    @SerialName("server_host") val serverHost: String
)