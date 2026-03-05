package com.u1.slicer.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * HTTP client for Moonraker API (Klipper web interface).
 * Used to communicate with Snapmaker U1 printer.
 *
 * Ported from u1-slicer-bridge: moonraker.py
 */
class MoonrakerClient {
    private val TAG = "MoonrakerClient"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    var baseUrl: String = ""
        set(value) {
            field = value.trimEnd('/')
        }

    private fun url(path: String): String = "$baseUrl$path"

    /**
     * Test connection to the printer.
     * @return null on success, or a human-readable error string on failure.
     */
    suspend fun testConnection(): String? = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext "No URL configured"
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            return@withContext "URL must start with http:// or https://"
        }
        try {
            val request = Request.Builder().url(url("/server/info")).get().build()
            val response = client.newCall(request).execute()
            val ok = response.isSuccessful
            val code = response.code
            response.close()
            if (ok) null else "HTTP $code — is Moonraker running on this address?"
        } catch (e: java.net.ConnectException) {
            "Connection refused — check IP and port (Moonraker default: 7125)"
        } catch (e: java.net.SocketTimeoutException) {
            "Timed out — check printer is on and reachable"
        } catch (e: java.net.UnknownHostException) {
            "Unknown host — check URL"
        } catch (e: Exception) {
            Log.w(TAG, "Connection test failed: ${e.message}")
            e.message?.take(100) ?: "Connection failed"
        }
    }

    /**
     * Get printer status: temps, state, progress.
     */
    suspend fun getStatus(): PrinterStatus = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext PrinterStatus(state = "disconnected", progress = 0f)
        try {
            val queryParams = "print_stats&heater_bed&extruder&extruder1&extruder2&extruder3"
            val request = Request.Builder()
                .url(url("/printer/objects/query?$queryParams"))
                .get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext PrinterStatus(state = "error", progress = 0f)
            }

            val json = JSONObject(response.body?.string() ?: "{}")
            response.close()
            val result = json.optJSONObject("result")?.optJSONObject("status")
                ?: return@withContext PrinterStatus(state = "standby", progress = 0f)

            val printStats = result.optJSONObject("print_stats")
            val heaterBed = result.optJSONObject("heater_bed")
            val extruder = result.optJSONObject("extruder")

            // Parse extruders 1-3
            val extruders = mutableListOf<ExtruderStatus>()
            extruder?.let {
                extruders.add(ExtruderStatus(0,
                    it.optDouble("temperature", 0.0).toFloat(),
                    it.optDouble("target", 0.0).toFloat(),
                    it.optDouble("target", 0.0) > 0
                ))
            }
            for (i in 1..3) {
                result.optJSONObject("extruder$i")?.let {
                    extruders.add(ExtruderStatus(i,
                        it.optDouble("temperature", 0.0).toFloat(),
                        it.optDouble("target", 0.0).toFloat(),
                        it.optDouble("target", 0.0) > 0
                    ))
                }
            }

            PrinterStatus(
                state = printStats?.optString("state", "standby") ?: "standby",
                progress = printStats?.optDouble("progress", 0.0)?.toFloat() ?: 0f,
                filename = printStats?.optString("filename", "") ?: "",
                printDuration = printStats?.optDouble("print_duration", 0.0)?.toFloat() ?: 0f,
                filamentUsed = printStats?.optDouble("filament_used", 0.0)?.toFloat() ?: 0f,
                nozzleTemp = extruder?.optDouble("temperature", 0.0)?.toFloat() ?: 0f,
                nozzleTarget = extruder?.optDouble("target", 0.0)?.toFloat() ?: 0f,
                bedTemp = heaterBed?.optDouble("temperature", 0.0)?.toFloat() ?: 0f,
                bedTarget = heaterBed?.optDouble("target", 0.0)?.toFloat() ?: 0f,
                extruders = extruders
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get status: ${e.message}")
            PrinterStatus(state = "disconnected", progress = 0f)
        }
    }

    /**
     * Upload a G-code file to the printer.
     */
    suspend fun uploadGcode(file: File, filename: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileSizeMb = file.length() / (1024 * 1024)
            val timeout = maxOf(30L, minOf(300L, fileSizeMb))

            val uploadClient = client.newBuilder()
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build()

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename,
                    file.asRequestBody("application/octet-stream".toMediaType()))
                .build()

            val request = Request.Builder()
                .url(url("/server/files/upload"))
                .post(body)
                .build()

            val response = uploadClient.newCall(request).execute()
            val ok = response.isSuccessful
            response.close()
            Log.i(TAG, "Upload ${if (ok) "success" else "failed"}: $filename")
            ok
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed: ${e.message}")
            false
        }
    }

    /**
     * Start printing a previously uploaded file.
     */
    suspend fun startPrint(filename: String): Boolean = withContext(Dispatchers.IO) {
        postCommand("/printer/print/start?filename=$filename")
    }

    suspend fun pausePrint(): Boolean = withContext(Dispatchers.IO) {
        postCommand("/printer/print/pause")
    }

    suspend fun resumePrint(): Boolean = withContext(Dispatchers.IO) {
        postCommand("/printer/print/resume")
    }

    suspend fun cancelPrint(): Boolean = withContext(Dispatchers.IO) {
        postCommand("/printer/print/cancel")
    }

    private fun postCommand(path: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url(path))
                .post(ByteArray(0).toRequestBody(null))
                .build()
            val response = client.newCall(request).execute()
            val ok = response.isSuccessful
            response.close()
            ok
        } catch (e: Exception) {
            Log.w(TAG, "Command failed ($path): ${e.message}")
            false
        }
    }
}
