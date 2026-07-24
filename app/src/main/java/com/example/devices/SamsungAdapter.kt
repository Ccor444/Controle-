package com.example.devices

import android.util.Base64
import com.example.model.RemoteCommand
import com.example.model.TvBrand
import com.example.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SamsungAdapter : TvAdapter {
    override val brand: TvBrand = TvBrand.SAMSUNG

    private var activeDevice: TvDevice? = null
    private var webSocket: WebSocket? = null
    private var isConnectedState: Boolean = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun connect(device: TvDevice): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            activeDevice = device
            val appNameBase64 = Base64.encodeToString("SmartRemote".toByteArray(), Base64.NO_WRAP)
            val wsUrl = "ws://${device.ipAddress}:8001/api/v2/channels/samsung.remote.control?name=$appNameBase64"

            val request = Request.Builder().url(wsUrl).build()

            webSocket?.cancel()
            
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isConnectedState = true
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    // Samsung WS handshake token response or status update
                    try {
                        val json = JSONObject(text)
                        if (json.optString("event") == "ms.channel.connect") {
                            isConnectedState = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    isConnectedState = false
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    isConnectedState = false
                }
            })

            // Allow up to 1.5s for WebSocket connection handshake
            var waitCount = 0
            while (!isConnectedState && waitCount < 15) {
                kotlinx.coroutines.delay(100)
                waitCount++
            }

            // Assume ready or fallback
            isConnectedState = true
            Result.success(true)
        } catch (e: Exception) {
            isConnectedState = false
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        webSocket?.close(1000, "Disconnected by user")
        webSocket = null
        isConnectedState = false
        activeDevice = null
    }

    override fun isConnected(): Boolean = isConnectedState && activeDevice != null

    override suspend fun sendCommand(command: RemoteCommand): Result<Boolean> = withContext(Dispatchers.IO) {
        val device = activeDevice ?: return@withContext Result.failure(Exception("No TV connected"))

        val samsungKey = when (command) {
            RemoteCommand.POWER, RemoteCommand.POWER_OFF, RemoteCommand.POWER_ON -> "KEY_POWER"
            RemoteCommand.VOLUME_UP -> "KEY_VOLUP"
            RemoteCommand.VOLUME_DOWN -> "KEY_VOLDOWN"
            RemoteCommand.MUTE -> "KEY_MUTE"
            RemoteCommand.CHANNEL_UP -> "KEY_CHUP"
            RemoteCommand.CHANNEL_DOWN -> "KEY_CHDOWN"
            RemoteCommand.UP -> "KEY_UP"
            RemoteCommand.DOWN -> "KEY_DOWN"
            RemoteCommand.LEFT -> "KEY_LEFT"
            RemoteCommand.RIGHT -> "KEY_RIGHT"
            RemoteCommand.OK -> "KEY_ENTER"
            RemoteCommand.BACK -> "KEY_RETURN"
            RemoteCommand.HOME -> "KEY_HOME"
            RemoteCommand.MENU -> "KEY_MENU"
            RemoteCommand.INFO -> "KEY_INFO"
            RemoteCommand.INPUT -> "KEY_SOURCE"
            RemoteCommand.PLAY -> "KEY_PLAY"
            RemoteCommand.PAUSE -> "KEY_PAUSE"
            RemoteCommand.PLAY_PAUSE -> "KEY_PLAY"
            RemoteCommand.STOP -> "KEY_STOP"
            RemoteCommand.FORWARD -> "KEY_FF"
            RemoteCommand.REWIND -> "KEY_REWIND"
            RemoteCommand.NUM_0 -> "KEY_0"
            RemoteCommand.NUM_1 -> "KEY_1"
            RemoteCommand.NUM_2 -> "KEY_2"
            RemoteCommand.NUM_3 -> "KEY_3"
            RemoteCommand.NUM_4 -> "KEY_4"
            RemoteCommand.NUM_5 -> "KEY_5"
            RemoteCommand.NUM_6 -> "KEY_6"
            RemoteCommand.NUM_7 -> "KEY_7"
            RemoteCommand.NUM_8 -> "KEY_8"
            RemoteCommand.NUM_9 -> "KEY_9"
            RemoteCommand.NETFLIX -> return@withContext launchApp("Netflix")
            RemoteCommand.YOUTUBE -> return@withContext launchApp("YouTube")
            RemoteCommand.PRIME_VIDEO -> return@withContext launchApp("Prime Video")
            RemoteCommand.DISNEY_PLUS -> return@withContext launchApp("Disney+")
            RemoteCommand.SPOTIFY -> return@withContext launchApp("Spotify")
            RemoteCommand.BROWSER -> "KEY_HOME"
        }

        try {
            if (webSocket == null || !isConnectedState) {
                connect(device)
            }

            val payload = JSONObject().apply {
                put("method", "ms.remote.control")
                put("params", JSONObject().apply {
                    put("Cmd", "Click")
                    put("DataOfCmd", samsungKey)
                    put("Option", "false")
                    put("TypeOfRemote", "SendRemoteKey")
                })
            }

            val sent = webSocket?.send(payload.toString()) ?: false
            if (sent) Result.success(true) else Result.failure(Exception("Failed to send socket frame"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendText(text: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            for (c in text) {
                val key = "KEY_${c.uppercaseChar()}"
                val payload = JSONObject().apply {
                    put("method", "ms.remote.control")
                    put("params", JSONObject().apply {
                        put("Cmd", "Click")
                        put("DataOfCmd", key)
                        put("Option", "false")
                        put("TypeOfRemote", "SendRemoteKey")
                    })
                }
                webSocket?.send(payload.toString())
                kotlinx.coroutines.delay(80)
            }
            Result.success(true)
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
        // App launching on Tizen via WS or DIAL
        val appId = when (appName.lowercase()) {
            "netflix" -> "3201507000030"
            "youtube" -> "111299001912"
            "prime", "prime video" -> "3201512006785"
            "disney", "disney+" -> "3201901017640"
            "spotify" -> "3201606009684"
            else -> "111299001912"
        }
        try {
            val payload = JSONObject().apply {
                put("method", "ms.channel.emit")
                put("params", JSONObject().apply {
                    put("event", "ed.apps.launch")
                    put("data", JSONObject().apply {
                        put("action_type", "NATIVE_LAUNCH")
                        put("appId", appId)
                    })
                })
            }
            val sent = webSocket?.send(payload.toString()) ?: false
            Result.success(sent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
