package com.example.ui.viewmodel

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AiTvIntent
import com.example.core.ConnectionState
import com.example.core.GeminiTvAssistant
import com.example.core.NetworkScanner
import com.example.core.RemoteCore
import com.example.core.VoiceRecognitionHelper
import com.example.data.AppDatabase
import com.example.data.TvEntity
import com.example.model.RemoteCommand
import com.example.model.TvBrand
import com.example.model.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RemoteTab {
    REMOTE,
    TOUCHPAD,
    KEYBOARD,
    DEVICES,
    AI_VOICE
}

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val tvDao = db.tvDao()

    val remoteCore = RemoteCore(viewModelScope)
    val networkScanner = NetworkScanner(application)
    val aiAssistant = GeminiTvAssistant()
    val voiceHelper = VoiceRecognitionHelper(application)

    // Saved TVs Flow from Room
    val savedTvs: StateFlow<List<TvEntity>> = tvDao.getAllSavedTvs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedTab = MutableStateFlow(RemoteTab.REMOTE)
    val selectedTab: StateFlow<RemoteTab> = _selectedTab.asStateFlow()

    private val _manualIpInput = MutableStateFlow("")
    val manualIpInput: StateFlow<String> = _manualIpInput.asStateFlow()

    private val _selectedBrandInput = MutableStateFlow(TvBrand.ROKU)
    val selectedBrandInput: StateFlow<TvBrand> = _selectedBrandInput.asStateFlow()

    private val _aiChatLog = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("Olá! Sou o seu assistente de TV inteligente. Fale ou digite comandos como 'Coloque na Netflix' ou 'Aumente o volume 5 vezes'." to false)
    )
    val aiChatLog: StateFlow<List<Pair<String, Boolean>>> = _aiChatLog.asStateFlow()

    private val _isProcessingAi = MutableStateFlow(false)
    val isProcessingAi: StateFlow<Boolean> = _isProcessingAi.asStateFlow()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(VibrationEffect::class.java) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        // Automatically trigger initial scan on startup
        scanNetwork()
    }

    fun selectTab(tab: RemoteTab) {
        _selectedTab.value = tab
    }

    fun updateManualIp(ip: String) {
        _manualIpInput.value = ip
    }

    fun updateSelectedBrand(brand: TvBrand) {
        _selectedBrandInput.value = brand
    }

    fun triggerHapticFeedback() {
        try {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(25)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibration disallowed
        }
    }

    fun scanNetwork() {
        viewModelScope.launch {
            networkScanner.startScan()
        }
    }

    fun connectToDevice(device: TvDevice) {
        viewModelScope.launch {
            triggerHapticFeedback()
            val result = remoteCore.connectToTv(device)
            if (result.isSuccess) {
                // Save or update in Room DB
                tvDao.insertOrUpdate(TvEntity.fromDomain(device.copy(lastConnected = System.currentTimeMillis())))
            }
        }
    }

    fun connectManualIp() {
        val ip = manualIpInput.value.trim()
        if (ip.isEmpty()) return
        val brand = selectedBrandInput.value
        val manualDevice = TvDevice(
            id = "manual_$ip",
            name = "${brand.displayName} ($ip)",
            ipAddress = ip,
            brand = brand,
            port = brand.defaultPort,
            isOnline = true
        )
        connectToDevice(manualDevice)
    }

    fun disconnectCurrent() {
        viewModelScope.launch {
            triggerHapticFeedback()
            remoteCore.disconnect()
        }
    }

    fun sendCommand(command: RemoteCommand) {
        triggerHapticFeedback()
        viewModelScope.launch {
            remoteCore.sendCommand(command)
        }
    }

    fun sendText(text: String) {
        triggerHapticFeedback()
        viewModelScope.launch {
            remoteCore.sendText(text)
        }
    }

    fun sendMouseMove(dx: Float, dy: Float) {
        viewModelScope.launch {
            remoteCore.sendMouseMove(dx, dy)
        }
    }

    fun sendMouseClick(x: Float, y: Float) {
        triggerHapticFeedback()
        viewModelScope.launch {
            remoteCore.sendMouseClick(x, y)
        }
    }

    fun launchApp(appName: String) {
        triggerHapticFeedback()
        viewModelScope.launch {
            remoteCore.launchApp(appName)
        }
    }

    fun processAiCommand(userPrompt: String) {
        if (userPrompt.isBlank()) return
        triggerHapticFeedback()

        _aiChatLog.value = _aiChatLog.value + (userPrompt to true)
        _isProcessingAi.value = true

        viewModelScope.launch {
            val resultIntent = aiAssistant.processUserVoiceCommand(userPrompt)
            _isProcessingAi.value = false

            when (resultIntent) {
                is AiTvIntent.SingleCommand -> {
                    _aiChatLog.value = _aiChatLog.value + (resultIntent.explanation to false)
                    remoteCore.sendCommand(resultIntent.command)
                }
                is AiTvIntent.RepeatCommand -> {
                    _aiChatLog.value = _aiChatLog.value + (resultIntent.explanation to false)
                    for (i in 1..resultIntent.times) {
                        remoteCore.sendCommand(resultIntent.command)
                        kotlinx.coroutines.delay(120)
                    }
                }
                is AiTvIntent.LaunchApp -> {
                    _aiChatLog.value = _aiChatLog.value + (resultIntent.explanation to false)
                    remoteCore.launchApp(resultIntent.appName)
                }
                is AiTvIntent.SetSleepTimer -> {
                    _aiChatLog.value = _aiChatLog.value + (resultIntent.explanation to false)
                    remoteCore.startSleepTimer(resultIntent.minutes)
                }
                is AiTvIntent.TypeText -> {
                    _aiChatLog.value = _aiChatLog.value + (resultIntent.explanation to false)
                    remoteCore.sendText(resultIntent.text)
                }
                is AiTvIntent.Unknown -> {
                    _aiChatLog.value = _aiChatLog.value + (resultIntent.explanation to false)
                }
            }
        }
    }

    fun startVoiceListening() {
        triggerHapticFeedback()
        voiceHelper.startListening { spokenText ->
            processAiCommand(spokenText)
        }
    }

    fun deleteSavedTv(tvId: String) {
        viewModelScope.launch {
            tvDao.deleteById(tvId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceHelper.destroy()
    }
}
