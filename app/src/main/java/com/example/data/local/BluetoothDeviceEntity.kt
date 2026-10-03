package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bluetooth_devices")
data class BluetoothDeviceEntity(
    @PrimaryKey val macAddress: String,
    val customName: String? = null,
    val originalName: String? = null,
    val isFavorite: Boolean = false,
    val notes: String = "",
    val customCategory: String? = null,
    val iconType: String = "GENERIC",
    val lastRssi: Int = -100,
    val dateAddedMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)
