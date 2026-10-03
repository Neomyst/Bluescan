package com.example.data.repository

import com.example.data.bluetooth.BluetoothScannerManager
import com.example.data.local.BluetoothDao
import com.example.data.local.BluetoothDeviceEntity
import com.example.data.model.BluetoothDeviceItem
import com.example.data.model.DeviceCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BluetoothRepository(
    private val dao: BluetoothDao,
    private val scannerManager: BluetoothScannerManager
) {

    val scannedMap = scannerManager.scannedDevices
    val isScanning = scannerManager.isScanning
    val simulationMode = scannerManager.simulationMode
    val isBluetoothEnabled = scannerManager.isBluetoothEnabled

    val combinedDevices: Flow<List<BluetoothDeviceItem>> = combine(
        scannerManager.scannedDevices,
        dao.getAllSavedDevices()
    ) { scannedMap, savedEntities ->
        val savedMap = savedEntities.associateBy { it.macAddress }

        // All MAC addresses from scanned or saved
        val allMacs = scannedMap.keys + savedMap.keys

        allMacs.map { mac ->
            val scannedItem = scannedMap[mac]
            val savedEntity = savedMap[mac]

            if (scannedItem != null && savedEntity != null) {
                // Scanned and saved
                scannedItem.copy(
                    customName = savedEntity.customName,
                    isFavorite = savedEntity.isFavorite,
                    notes = savedEntity.notes,
                    category = DeviceCategory.fromString(savedEntity.customCategory ?: scannedItem.category.name),
                    iconType = savedEntity.iconType
                )
            } else if (scannedItem != null) {
                // Scanned only
                scannedItem
            } else {
                // Saved only (currently offline / out of range)
                val entity = savedEntity!!
                BluetoothDeviceItem(
                    macAddress = entity.macAddress,
                    originalName = entity.originalName ?: "Appareil Sauvegardé",
                    customName = entity.customName,
                    rssi = entity.lastRssi,
                    isFavorite = entity.isFavorite,
                    notes = entity.notes,
                    category = DeviceCategory.fromString(entity.customCategory),
                    iconType = entity.iconType,
                    isSimulated = false,
                    lastSeenMs = entity.updatedAtMs
                )
            }
        }.sortedWith(
            compareByDescending<BluetoothDeviceItem> { it.isFavorite }
                .thenByDescending { it.rssi }
        )
    }

    val favoriteDevices: Flow<List<BluetoothDeviceItem>> = combinedDevices.map { list ->
        list.filter { it.isFavorite }
    }

    suspend fun renameDevice(mac: String, originalName: String, customName: String) {
        val trimmedCustom = customName.trim()
        val existing = dao.getDeviceByMac(mac)
        if (existing == null) {
            dao.insertOrUpdateDevice(
                BluetoothDeviceEntity(
                    macAddress = mac,
                    originalName = originalName,
                    customName = if (trimmedCustom.isBlank()) null else trimmedCustom,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        } else {
            dao.renameDevice(mac, if (trimmedCustom.isBlank()) null else trimmedCustom)
        }
    }

    suspend fun toggleFavorite(device: BluetoothDeviceItem) {
        val newFavState = !device.isFavorite
        val existing = dao.getDeviceByMac(device.macAddress)
        if (existing == null) {
            dao.insertOrUpdateDevice(
                BluetoothDeviceEntity(
                    macAddress = device.macAddress,
                    originalName = device.originalName,
                    customName = device.customName,
                    isFavorite = newFavState,
                    notes = device.notes,
                    customCategory = device.category.name,
                    iconType = device.iconType,
                    lastRssi = device.rssi,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        } else {
            dao.setFavorite(device.macAddress, newFavState)
        }
    }

    suspend fun updateDeviceDetails(
        mac: String,
        originalName: String,
        customName: String?,
        notes: String,
        category: DeviceCategory,
        iconType: String
    ) {
        val existing = dao.getDeviceByMac(mac)
        val trimmedCustom = customName?.trim()
        if (existing == null) {
            dao.insertOrUpdateDevice(
                BluetoothDeviceEntity(
                    macAddress = mac,
                    originalName = originalName,
                    customName = if (trimmedCustom.isNullOrBlank()) null else trimmedCustom,
                    notes = notes,
                    customCategory = category.name,
                    iconType = iconType,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        } else {
            dao.renameDevice(mac, if (trimmedCustom.isNullOrBlank()) null else trimmedCustom)
            dao.updateDetails(mac, notes, category.name, iconType)
        }
    }

    suspend fun deleteSavedDevice(mac: String) {
        dao.deleteDevice(mac)
    }

    fun startScan() = scannerManager.startScan()
    fun stopScan() = scannerManager.stopScan()
    fun toggleSimulationMode(enabled: Boolean) = scannerManager.toggleSimulationMode(enabled)
    fun addSimulatedDevice(name: String, category: DeviceCategory) = scannerManager.addSimulatedDevice(name, category)
    fun hasPermissions(): Boolean = scannerManager.hasPermissions()
}
