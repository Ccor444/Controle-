package com.example.devices

import com.example.model.RemoteCommand
import com.example.model.TvBrand
import com.example.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class RokuAdapter : TvAdapter {
    override val brand: TvBrand = TvBrand.ROKU

    private var activeDevice: TvDevice? = null
    private var connected: Boolean = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()

    override suspend fun connect(device: TvDevice): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            activeDevice = device
            // Ping Roku query/device-info
            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/query/device-info")
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    connected = true
                    Result.success(true)
                } else {
                    connected = false
                    Result.failure(Exception("HTTP error ${response.code} connecting to Roku TV"))
                }
            }
        } catch (e: Exception) {
            connected = false
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        connected = false
        activeDevice = null
    }

    override fun isConnected(): Boolean = connected && activeDevice != null

    override suspend fun sendCommand(command: RemoteCommand): Result<Boolean> = withContext(Dispatchers.IO) {
        val device = activeDevice ?: return@withContext Result.failure(Exception("No TV connected"))
        
        val ecpKey = when (command) {
            RemoteCommand.POWER -> "Power"
            RemoteCommand.POWER_OFF -> "PowerOff"
            RemoteCommand.POWER_ON -> "PowerOn"
            RemoteCommand.VOLUME_UP -> "VolumeUp"
            RemoteCommand.VOLUME_DOWN -> "VolumeDown"
            RemoteCommand.MUTE -> "VolumeMute"
            RemoteCommand.CHANNEL_UP -> "ChannelUp"
            RemoteCommand.CHANNEL_DOWN -> "ChannelDown"
            RemoteCommand.UP -> "Up"
            RemoteCommand.DOWN -> "Down"
            RemoteCommand.LEFT -> "Left"
            RemoteCommand.RIGHT -> "Right"
            RemoteCommand.OK -> "Select"
            RemoteCommand.BACK -> "Back"
            RemoteCommand.HOME -> "Home"
            RemoteCommand.MENU -> "Info"
            RemoteCommand.INFO -> "Info"
            RemoteCommand.INPUT -> "InputTuner"
            RemoteCommand.PLAY, RemoteCommand.PLAY_PAUSE -> "Play"
            RemoteCommand.PAUSE -> "Play"
            RemoteCommand.STOP -> "Play"
            RemoteCommand.FORWARD -> "Fwd"
            RemoteCommand.REWIND -> "Rev"
            RemoteCommand.NUM_0 -> "Lit_0"
            RemoteCommand.NUM_1 -> "Lit_1"
            RemoteCommand.NUM_2 -> "Lit_2"
            RemoteCommand.NUM_3 -> "Lit_3"
            RemoteCommand.NUM_4 -> "Lit_4"
            RemoteCommand.NUM_5 -> "Lit_5"
            RemoteCommand.NUM_6 -> "Lit_6"
            RemoteCommand.NUM_7 -> "Lit_7"
            RemoteCommand.NUM_8 -> "Lit_8"
            RemoteCommand.NUM_9 -> "Lit_9"
            RemoteCommand.NETFLIX -> return@withContext launchApp("Netflix")
            RemoteCommand.YOUTUBE -> return@withContext launchApp("YouTube")
            RemoteCommand.PRIME_VIDEO -> return@withContext launchApp("Prime Video")
            RemoteCommand.DISNEY_PLUS -> return@withContext launchApp("Disney+")
            RemoteCommand.SPOTIFY -> return@withContext launchApp("Spotify")
            RemoteCommand.BROWSER -> "Home"
        }

        try {
            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/keypress/$ecpKey")
                .post("".toRequestBody(null))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Failed to send keypress $ecpKey: code ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendText(text: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val device = activeDevice ?: return@withContext Result.failure(Exception("No TV connected"))
        try {
            var allSuccess = true
            for (char in text) {
                val encodedChar = if (char == ' ') "%20" else char.toString()
                val request = Request.Builder()
                    .url("http://${device.ipAddress}:${device.port}/keypress/Lit_$encodedChar")
                    .post("".toRequestBody(null))
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) allSuccess = false
                }
            }
            Result.success(allSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMouseClick(x: Float, y: Float): Result<Boolean> {
        return sendCommand(RemoteCommand.OK)
    }

    override suspend fun sendMouseMove(dx: Float, dy: Float): Result<Boolean> {
        return if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
            if (dx > 0) sendCommand(RemoteCommand.RIGHT) else sendCommand(RemoteCommand.LEFT)
        } else {
            if (dy > 0) sendCommand(RemoteCommand.DOWN) else sendCommand(RemoteCommand.UP)
        }
    }

    override suspend fun launchApp(appName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val device = activeDevice ?: return@withContext Result.failure(Exception("No TV connected"))
        val appId = when (appName.lowercase()) {
            "netflix" -> "12"
            "youtube" -> "837"
            "prime", "prime video", "amazon prime" -> "13"
            "disney", "disney+" -> "291097"
            "spotify" -> "22271"
            "hbo", "max" -> "61322"
            else -> "12"
        }
        try {
            val request = Request.Builder()
                .url("http://${device.ipAddress}:${device.port}/launch/$appId")
                .post("".toRequestBody(null))
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
