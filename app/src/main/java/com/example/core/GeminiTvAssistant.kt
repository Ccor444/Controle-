package com.example.core

import com.example.BuildConfig
import com.example.model.RemoteCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AiTvIntent {
    data class SingleCommand(val command: RemoteCommand, val explanation: String) : AiTvIntent()
    data class RepeatCommand(val command: RemoteCommand, val times: Int, val explanation: String) : AiTvIntent()
    data class LaunchApp(val appName: String, val explanation: String) : AiTvIntent()
    data class SetSleepTimer(val minutes: Int, val explanation: String) : AiTvIntent()
    data class TypeText(val text: String, val explanation: String) : AiTvIntent()
    data class Unknown(val explanation: String) : AiTvIntent()
}

class GeminiTvAssistant {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun processUserVoiceCommand(userText: String): AiTvIntent = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext parseRuleBasedFallback(userText)
        }

        val systemPrompt = """
            Você é o assistente inteligente de um controle remoto universal para Smart TVs.
            Analise o comando do usuário (em Português ou Inglês) e converta rigorosamente em um JSON estruturado com os seguintes campos:
            - intent: "COMMAND" | "REPEAT_COMMAND" | "LAUNCH_APP" | "SET_TIMER" | "TYPE_TEXT" | "UNKNOWN"
            - commandName: (Apenas se intent for COMMAND ou REPEAT_COMMAND) Opções válidas: "POWER", "POWER_OFF", "POWER_ON", "VOLUME_UP", "VOLUME_DOWN", "MUTE", "CHANNEL_UP", "CHANNEL_DOWN", "HOME", "BACK", "MENU", "OK", "UP", "DOWN", "LEFT", "RIGHT", "PLAY", "PAUSE", "INPUT"
            - times: (Int, quantidade de repetições se o usuário disse ex: "aumentar volume 5 vezes")
            - appName: (String ex: "Netflix", "YouTube", "Prime Video", "Disney+", "Spotify")
            - minutes: (Int, minutos para timer ex: 30 para "desligar em 30 min")
            - text: (String para digitar no teclado)
            - explanation: (Frase curta e amigável em português confirmando a ação)

            Exemplos de Saída JSON:
            {"intent": "LAUNCH_APP", "appName": "Netflix", "explanation": "Abrindo a Netflix na sua TV."}
            {"intent": "REPEAT_COMMAND", "commandName": "VOLUME_UP", "times": 5, "explanation": "Aumentando o volume em 5 níveis."}
            {"intent": "SET_TIMER", "minutes": 30, "explanation": "Timer de desligamento definido para 30 minutos."}
            {"intent": "COMMAND", "commandName": "POWER_OFF", "explanation": "Desligando a TV."}

            Retorne APENAS o objeto JSON puro sem marcações markdown.
        """.trimIndent()

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userText) })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: ""
                    val rootJson = JSONObject(respStr)
                    val candidates = rootJson.optJSONArray("candidates")
                    val firstPart = candidates?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                    val text = firstPart?.optString("text", "") ?: ""
                    parseGeminiJsonResponse(text, userText)
                } else {
                    parseRuleBasedFallback(userText)
                }
            }
        } catch (e: Exception) {
            parseRuleBasedFallback(userText)
        }
    }

    private fun parseGeminiJsonResponse(jsonString: String, rawUserText: String): AiTvIntent {
        try {
            val cleanJson = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonObject = JSONObject(cleanJson)
            val intent = jsonObject.optString("intent", "UNKNOWN")
            val explanation = jsonObject.optString("explanation", "Comando processado.")

            return when (intent) {
                "COMMAND" -> {
                    val cmdStr = jsonObject.optString("commandName", "OK")
                    val cmd = mapStringToCommand(cmdStr)
                    AiTvIntent.SingleCommand(cmd, explanation)
                }
                "REPEAT_COMMAND" -> {
                    val cmdStr = jsonObject.optString("commandName", "VOLUME_UP")
                    val times = jsonObject.optInt("times", 1)
                    val cmd = mapStringToCommand(cmdStr)
                    AiTvIntent.RepeatCommand(cmd, times, explanation)
                }
                "LAUNCH_APP" -> {
                    val app = jsonObject.optString("appName", "Netflix")
                    AiTvIntent.LaunchApp(app, explanation)
                }
                "SET_TIMER" -> {
                    val mins = jsonObject.optInt("minutes", 30)
                    AiTvIntent.SetSleepTimer(mins, explanation)
                }
                "TYPE_TEXT" -> {
                    val textToType = jsonObject.optString("text", "")
                    AiTvIntent.TypeText(textToType, explanation)
                }
                else -> parseRuleBasedFallback(rawUserText)
            }
        } catch (e: Exception) {
            return parseRuleBasedFallback(rawUserText)
        }
    }

    private fun parseRuleBasedFallback(text: String): AiTvIntent {
        val lower = text.lowercase()
        return when {
            lower.contains("netflix") -> AiTvIntent.LaunchApp("Netflix", "Abrindo Netflix...")
            lower.contains("youtube") -> AiTvIntent.LaunchApp("YouTube", "Abrindo YouTube...")
            lower.contains("prime") || lower.contains("amazon") -> AiTvIntent.LaunchApp("Prime Video", "Abrindo Prime Video...")
            lower.contains("disney") -> AiTvIntent.LaunchApp("Disney+", "Abrindo Disney+...")
            lower.contains("spotify") -> AiTvIntent.LaunchApp("Spotify", "Abrindo Spotify...")

            lower.contains("desligar") || lower.contains("desliga") || lower.contains("turn off") -> {
                if (lower.contains("minuto") || lower.contains("min")) {
                    val minutes = lower.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 30
                    AiTvIntent.SetSleepTimer(minutes, "Timer de desligamento definido para $minutes minutos.")
                } else {
                    AiTvIntent.SingleCommand(RemoteCommand.POWER_OFF, "Desligando a TV.")
                }
            }
            lower.contains("ligar") || lower.contains("liga") -> AiTvIntent.SingleCommand(RemoteCommand.POWER_ON, "Ligando a TV.")

            lower.contains("aumenta") || lower.contains("aumentar") || lower.contains("mais alto") || lower.contains("vol+") -> {
                val times = lower.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
                if (times > 1) {
                    AiTvIntent.RepeatCommand(RemoteCommand.VOLUME_UP, times, "Aumentando o volume em $times níveis.")
                } else {
                    AiTvIntent.SingleCommand(RemoteCommand.VOLUME_UP, "Aumentando o volume.")
                }
            }
            lower.contains("diminui") || lower.contains("diminuir") || lower.contains("mais baixo") || lower.contains("vol-") -> {
                val times = lower.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
                if (times > 1) {
                    AiTvIntent.RepeatCommand(RemoteCommand.VOLUME_DOWN, times, "Diminuindo o volume em $times níveis.")
                } else {
                    AiTvIntent.SingleCommand(RemoteCommand.VOLUME_DOWN, "Diminuindo o volume.")
                }
            }
            lower.contains("mudo") || lower.contains("silenciar") || lower.contains("mute") -> AiTvIntent.SingleCommand(RemoteCommand.MUTE, "Alternando Mute.")
            lower.contains("canal +") || lower.contains("proximo canal") -> AiTvIntent.SingleCommand(RemoteCommand.CHANNEL_UP, "Mudando para o próximo canal.")
            lower.contains("canal -") || lower.contains("canal anterior") -> AiTvIntent.SingleCommand(RemoteCommand.CHANNEL_DOWN, "Voltando o canal.")
            lower.contains("inicio") || lower.contains("home") -> AiTvIntent.SingleCommand(RemoteCommand.HOME, "Indo para Tela Inicial.")
            lower.contains("voltar") || lower.contains("back") -> AiTvIntent.SingleCommand(RemoteCommand.BACK, "Voltando.")
            lower.contains("pause") || lower.contains("pausar") -> AiTvIntent.SingleCommand(RemoteCommand.PAUSE, "Pausando.")
            lower.contains("play") || lower.contains("tocar") -> AiTvIntent.SingleCommand(RemoteCommand.PLAY, "Reproduzindo.")

            else -> AiTvIntent.Unknown("Não entendi completamente o comando '$text'. Tente dizer 'Coloque na Netflix' ou 'Aumente o volume'.")
        }
    }

    private fun mapStringToCommand(cmdStr: String): RemoteCommand {
        return when (cmdStr.uppercase()) {
            "POWER", "POWER_OFF" -> RemoteCommand.POWER_OFF
            "POWER_ON" -> RemoteCommand.POWER_ON
            "VOLUME_UP" -> RemoteCommand.VOLUME_UP
            "VOLUME_DOWN" -> RemoteCommand.VOLUME_DOWN
            "MUTE" -> RemoteCommand.MUTE
            "CHANNEL_UP" -> RemoteCommand.CHANNEL_UP
            "CHANNEL_DOWN" -> RemoteCommand.CHANNEL_DOWN
            "HOME" -> RemoteCommand.HOME
            "BACK" -> RemoteCommand.BACK
            "MENU" -> RemoteCommand.MENU
            "OK" -> RemoteCommand.OK
            "UP" -> RemoteCommand.UP
            "DOWN" -> RemoteCommand.DOWN
            "LEFT" -> RemoteCommand.LEFT
            "RIGHT" -> RemoteCommand.RIGHT
            "PLAY" -> RemoteCommand.PLAY
            "PAUSE" -> RemoteCommand.PAUSE
            "INPUT" -> RemoteCommand.INPUT
            else -> RemoteCommand.OK
        }
    }
}
