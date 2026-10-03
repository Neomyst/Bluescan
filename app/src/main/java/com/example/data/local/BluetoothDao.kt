package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothDao {
    @Query("SELECT * FROM bluetooth_devices ORDER BY updatedAtMs DESC")
    fun getAllSavedDevices(): Flow<List<BluetoothDeviceEntity>>

    @Query("SELECT * FROM bluetooth_devices WHERE isFavorite = 1 ORDER BY updatedAtMs DESC")
    fun getFavoriteDevices(): Flow<List<BluetoothDeviceEntity>>

    @Query("SELECT * FROM bluetooth_devices WHERE macAddress = :macAddress LIMIT 1")
    suspend fun getDeviceByMac(macAddress: String): BluetoothDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: BluetoothDeviceEntity)

    @Query("UPDATE bluetooth_devices SET customName = :customName, updatedAtMs = :updatedAt WHERE macAddress = :macAddress")
    suspend fun renameDevice(macAddress: String, customName: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE bluetooth_devices SET isFavorite = :isFavorite, updatedAtMs = :updatedAt WHERE macAddress = :macAddress")
    suspend fun setFavorite(macAddress: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE bluetooth_devices SET notes = :notes, customCategory = :category, iconType = :iconType, updatedAtMs = :updatedAt WHERE macAddress = :macAddress")
    suspend fun updateDetails(
        macAddress: String,
        notes: String,
        category: String?,
        iconType: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM bluetooth_devices WHERE macAddress = :macAddress")
    suspend fun deleteDevice(macAddress: String)

    @Query("DELETE FROM bluetooth_devices WHERE isFavorite = 0")
    suspend fun clearNonFavorites()
}
