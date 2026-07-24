package com.example.core

import com.example.devices.TvAdapter
import com.example.devices.TvAdapterFactory
import com.example.model.RemoteCommand
import com.example.model.TvDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val device: TvDevice) : ConnectionState()
    data class Connected(val device: TvDevice) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class RemoteCore(private val scope: CoroutineScope) {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _lastCommandLog = MutableStateFlow<String>("Aguardando comando...")
    val lastCommandLog: StateFlow<String> = _lastCommandLog.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private var activeAdapter: TvAdapter? = null
    private var activeDevice: TvDevice? = null
    private var timerJob: Job? = null

    suspend fun connectToTv(device: TvDevice): Result<Boolean> = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.Connecting(device)
        _lastCommandLog.value = "Conectando a ${device.name} (${device.ipAddress})..."

        val adapter = TvAdapterFactory.createAdapter(device.brand)
        val result = adapter.connect(device)

        if (result.isSuccess) {
            activeAdapter = adapter
            activeDevice = device
            _connectionState.value = ConnectionState.Connected(device)
            _lastCommandLog.value = "Conectado com sucesso a ${device.name}"
            Result.success(true)
        } else {
            val err = result.exceptionOrNull()?.message ?: "Erro ao conectar"
            _connectionState.value = ConnectionState.Error(err)
            _lastCommandLog.value = "Falha de conexão: $err"
            Result.failure(Exception(err))
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        activeAdapter?.disconnect()
        activeAdapter = null
        activeDevice = null
        _connectionState.value = ConnectionState.Disconnected
        _lastCommandLog.value = "Desconectado"
    }

    suspend fun sendCommand(command: RemoteCommand): Result<Boolean> = withContext(Dispatchers.IO) {
        val adapter = activeAdapter
        if (adapter == null || !adapter.isConnected()) {
            val currentDev = activeDevice
            if (currentDev != null) {
                // Auto reconnect attempt
                connectToTv(currentDev)
            } else {
                _lastCommandLog.value = "Nenhuma TV conectada. Escolha um dispositivo."
                return@withContext Result.failure(Exception("Nenhuma TV conectada"))
            }
        }

        val active = activeAdapter ?: return@withContext Result.failure(Exception("Nenhuma TV conectada"))
        _lastCommandLog.value = "Enviando: ${command.label}"

        val result = active.sendCommand(command)
        if (result.isSuccess) {
            _lastCommandLog.value = "Executado: ${command.label}"
        } else {
            _lastCommandLog.value = "Erro no comando ${command.label}: ${result.exceptionOrNull()?.message}"
        }
        result
    }

    suspend fun sendText(text: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val adapter = activeAdapter ?: return@withContext Result.failure(Exception("Nenhuma TV conectada"))
        _lastCommandLog.value = "Enviando texto: $text"
        val res = adapter.sendText(text)
        if (res.isSuccess) {
            _lastCommandLog.value = "Texto enviado: $text"
        }
        res
    }

    suspend fun sendMouseMove(dx: Float, dy: Float): Result<Boolean> = withContext(Dispatchers.IO) {
        val adapter = activeAdapter ?: return@withContext Result.failure(Exception("Nenhuma TV conectada"))
        adapter.sendMouseMove(dx, dy)
    }

    suspend fun sendMouseClick(x: Float, y: Float): Result<Boolean> = withContext(Dispatchers.IO) {
        val adapter = activeAdapter ?: return@withContext Result.failure(Exception("Nenhuma TV conectada"))
        _lastCommandLog.value = "Clique no Touchpad"
        adapter.sendMouseClick(x, y)
    }

    suspend fun launchApp(appName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val adapter = activeAdapter ?: return@withContext Result.failure(Exception("Nenhuma TV conectada"))
        _lastCommandLog.value = "Abrindo $appName..."
        val res = adapter.launchApp(appName)
        if (res.isSuccess) {
            _lastCommandLog.value = "$appName aberto na TV"
        }
        res
    }

    fun startSleepTimer(minutes: Int) {
        timerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        _lastCommandLog.value = "Timer de desligamento definido para $minutes minutos."

        timerJob = scope.launch(Dispatchers.IO) {
            var remaining = minutes
            while (remaining > 0) {
                delay(60000L)
                remaining--
                _sleepTimerMinutes.value = remaining
                _lastCommandLog.value = "Desligando a TV em $remaining minuto(s)..."
            }
            // Timer expired -> Turn off TV
            _sleepTimerMinutes.value = null
            _lastCommandLog.value = "Timer finalizado. Desligando TV agora!"
            sendCommand(RemoteCommand.POWER_OFF)
        }
    }

    fun cancelSleepTimer() {
        timerJob?.cancel()
        timerJob = null
        _sleepTimerMinutes.value = null
        _lastCommandLog.value = "Timer de desligamento cancelado."
    }

    fun getActiveDevice(): TvDevice? = activeDevice
}
