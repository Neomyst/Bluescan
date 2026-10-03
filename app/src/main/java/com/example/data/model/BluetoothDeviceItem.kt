package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DeviceCategory(val labelFr: String, val defaultIconName: String) {
    AUDIO("Casque / Écouteurs", "HEADPHONES"),
    SPEAKER("Enceinte Bluetooth", "SPEAKER"),
    PHONE("Smartphone / Tablette", "PHONE"),
    WATCH("Montre / Bracelet", "WATCH"),
    LAPTOP("Ordinateur / PC", "LAPTOP"),
    TV("Téléviseur / Media", "TV"),
    SMART_HOME("Maison Connectée", "SMART_HOME"),
    CAR("Système Auto", "CAR"),
    HEALTH("Santé & Capteur", "HEALTH"),
    GENERIC("Autre appareil", "GENERIC");

    companion object {
        fun fromString(value: String?): DeviceCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.defaultIconName.equals(value, ignoreCase = true) }
                ?: GENERIC
        }
    }
}

enum class SignalLevel(val labelFr: String) {
    EXCELLENT("Excellent (> -60 dBm)"),
    GOOD("Bon (-60 à -72 dBm)"),
    MEDIUM("Moyen (-72 à -85 dBm)"),
    WEAK("Faible (< -85 dBm)")
}

data class BluetoothDeviceItem(
    val macAddress: String,
    val originalName: String,
    val customName: String? = null,
    val rssi: Int = -70,
    val isFavorite: Boolean = false,
    val notes: String = "",
    val category: DeviceCategory = DeviceCategory.GENERIC,
    val iconType: String = category.defaultIconName,
    val isSimulated: Boolean = false,
    val isConnected: Boolean = false,
    val lastSeenMs: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = if (!customName.isNullOrBlank()) customName else if (originalName.isNotBlank()) originalName else "Appareil Inconnu ($macAddress)"

    val signalLevel: SignalLevel
        get() = when {
            rssi >= -60 -> SignalLevel.EXCELLENT
            rssi >= -72 -> SignalLevel.GOOD
            rssi >= -85 -> SignalLevel.MEDIUM
            else -> SignalLevel.WEAK
        }

    val distanceEstimate: String
        get() = when {
            rssi >= -55 -> "< 1 mètre"
            rssi >= -68 -> "1 à 3 mètres"
            rssi >= -82 -> "3 à 7 mètres"
            else -> "> 7 mètres"
        }

    val formattedLastSeen: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.FRENCH)
            return sdf.format(Date(lastSeenMs))
        }
}
