package com.example.devices

import com.example.model.RemoteCommand
import com.example.model.TvBrand
import com.example.model.TvDevice

interface TvAdapter {
    val brand: TvBrand
    
    suspend fun connect(device: TvDevice): Result<Boolean>
    suspend fun disconnect()
    suspend fun sendCommand(command: RemoteCommand): Result<Boolean>
    suspend fun sendText(text: String): Result<Boolean>
    suspend fun sendMouseClick(x: Float, y: Float): Result<Boolean>
    suspend fun sendMouseMove(dx: Float, dy: Float): Result<Boolean>
    suspend fun launchApp(appName: String): Result<Boolean>
    fun isConnected(): Boolean
}
