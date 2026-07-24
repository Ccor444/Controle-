package com.example.model

data class TvDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val macAddress: String = "",
    val brand: TvBrand,
    val port: Int = brand.defaultPort,
    val isOnline: Boolean = true,
    val token: String = "",
    val modelName: String = "",
    val lastConnected: Long = System.currentTimeMillis()
)
