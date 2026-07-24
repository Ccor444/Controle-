package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.TvBrand
import com.example.model.TvDevice

@Entity(tableName = "saved_tvs")
data class TvEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ipAddress: String,
    val macAddress: String,
    val brand: String,
    val port: Int,
    val token: String,
    val modelName: String,
    val lastConnected: Long
) {
    fun toDomain(): TvDevice {
        return TvDevice(
            id = id,
            name = name,
            ipAddress = ipAddress,
            macAddress = macAddress,
            brand = TvBrand.fromString(brand),
            port = port,
            token = token,
            modelName = modelName,
            lastConnected = lastConnected
        )
    }

    companion object {
        fun fromDomain(device: TvDevice): TvEntity {
            return TvEntity(
                id = device.id,
                name = device.name,
                ipAddress = device.ipAddress,
                macAddress = device.macAddress,
                brand = device.brand.name,
                port = device.port,
                token = device.token,
                modelName = device.modelName,
                lastConnected = device.lastConnected
            )
        }
    }
}
