package com.example.devices

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
import java.util.concurrent.atomic.AtomicInteger

class LgAdapter : TvAdapter {
    override val brand: TvBrand = TvBrand.LG

    private var activeDevice: TvDevice? = null
    private var webSocket: WebSocket? = null
    private var isConnectedState: Boolean = false
    private val requestIdCounter = AtomicInteger(1)

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun connect(device: TvDevice): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            activeDevice = device
            val wsUrl = "ws://${device.ipAddress}:3000"
            val request = Request.Builder().url(wsUrl).build()

            webSocket?.cancel()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isConnectedState = true
                    // Register / Handshake SSAP payload
                    sendRegisterHandshake(webSocket, device.token)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    // Extract client-key pairing if returned
                    try {
                        val json = JSONObject(text)
                        val payload = json.optJSONObject("payload")
                        val clientKey = payload?.optString("client-key")
                        if (!clientKey.isNull_Empty()) {
                            // Can save updated token
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

            isConnectedState = true
            Result.success(true)
        } catch (e: Exception) {
            isConnectedState = false
            Result.failure(e)
        }
    }

    private fun sendRegisterHandshake(ws: WebSocket, token: String) {
        val registerJson = JSONObject().apply {
            put("type", "register")
            put("id", "register_0")
            put("payload", JSONObject().apply {
                put("forcePairing", false)
                put("pairingType", "PROMPT")
                if (token.isNotEmpty()) {
                    put("client-key", token)
                }
                put("manifest", JSONObject().apply {
                    put("manifestVersion", 1)
                    put("appVersion", "1.0.0")
                    put("signed", JSONObject().apply {
                        put("created", "20260101")
                        put("appId", "com.example.smartremote")
                        put("vendorId", "com.example")
                        put("localizedAppNames", JSONObject().apply {
                            put("", "Smart Remote")
                        })
                        put("permissions", org.json.JSONArray().apply {
                            put("LAUNCH")
                            put("CONTROL_AUDIO")
                            put("CONTROL_POWER")
                            put("READ_RUNNING_APPS")
                        })
                    })
                })
            })
        }
        ws.send(registerJson.toString())
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

        val ssapUri = when (command) {
            RemoteCommand.POWER, RemoteCommand.POWER_OFF -> "ssap://system/turnOff"
            RemoteCommand.POWER_ON -> "ssap://system/turnOn"
            RemoteCommand.VOLUME_UP -> "ssap://audio/volumeUp"
            RemoteCommand.VOLUME_DOWN -> "ssap://audio/volumeDown"
            RemoteCommand.MUTE -> "ssap://audio/setMute"
            RemoteCommand.CHANNEL_UP -> "ssap://tv/channelUp"
            RemoteCommand.CHANNEL_DOWN -> "ssap://tv/channelDown"
            RemoteCommand.UP -> "ssap://navigation/up"
            RemoteCommand.DOWN -> "ssap://navigation/down"
            RemoteCommand.LEFT -> "ssap://navigation/left"
            RemoteCommand.RIGHT -> "ssap://navigation/right"
            RemoteCommand.OK -> "ssap://navigation/enter"
            RemoteCommand.BACK -> "ssap://navigation/back"
            RemoteCommand.HOME -> "ssap://system.launcher/open"
            RemoteCommand.MENU -> "ssap://system.launcher/open"
            RemoteCommand.INFO -> "ssap://tv/getChannelList"
            RemoteCommand.INPUT -> "ssap://tv/switchInput"
            RemoteCommand.PLAY -> "ssap://media.controls/play"
            RemoteCommand.PAUSE -> "ssap://media.controls/pause"
            RemoteCommand.PLAY_PAUSE -> "ssap://media.controls/play"
            RemoteCommand.STOP -> "ssap://media.controls/stop"
            RemoteCommand.FORWARD -> "ssap://media.controls/fastForward"
            RemoteCommand.REWIND -> "ssap://media.controls/rewind"
            RemoteCommand.NUM_0 -> "ssap://tv/channelUp"
            RemoteCommand.NETFLIX -> return@withContext launchApp("Netflix")
            RemoteCommand.YOUTUBE -> return@withContext launchApp("YouTube")
            RemoteCommand.PRIME_VIDEO -> return@withContext launchApp("Prime Video")
            RemoteCommand.DISNEY_PLUS -> return@withContext launchApp("Disney+")
            RemoteCommand.SPOTIFY -> return@withContext launchApp("Spotify")
            else -> "ssap://navigation/enter"
        }

        try {
            if (webSocket == null || !isConnectedState) {
                connect(device)
            }

            val requestJson = JSONObject().apply {
                put("type", "request")
                put("id", "req_${requestIdCounter.getAndIncrement()}")
                put("uri", ssapUri)
            }

            val sent = webSocket?.send(requestJson.toString()) ?: false
            if (sent) Result.success(true) else Result.failure(Exception("Failed to send SSAP message"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendText(text: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val requestJson = JSONObject().apply {
                put("type", "request")
                put("id", "req_text_${requestIdCounter.getAndIncrement()}")
                put("uri", "ssap://keyboard/insertText")
                put("payload", JSONObject().apply {
                    put("text", text)
                    put("replace", false)
                })
            }
            val sent = webSocket?.send(requestJson.toString()) ?: false
            Result.success(sent)
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
        val appId = when (appName.lowercase()) {
            "netflix" -> "netflix"
            "youtube" -> "youtube.leanback.v4"
            "prime", "prime video" -> "amazon"
            "disney", "disney+" -> "com.disney.disneyplus-prod"
            "spotify" -> "spotify-tv"
            else -> "youtube.leanback.v4"
        }
        try {
            val requestJson = JSONObject().apply {
                put("type", "request")
                put("id", "req_app_${requestIdCounter.getAndIncrement()}")
                put("uri", "ssap://system.launcher/open")
                put("payload", JSONObject().apply {
                    put("id", appId)
                })
            }
            val sent = webSocket?.send(requestJson.toString()) ?: false
            Result.success(sent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun String?.isNull_Empty(): Boolean = this.isNullOrEmpty()
}
