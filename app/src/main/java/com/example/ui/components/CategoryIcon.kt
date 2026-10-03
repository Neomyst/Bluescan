package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.DeviceCategory

@Composable
fun getCategoryIcon(category: DeviceCategory, iconType: String? = null): ImageVector {
    val key = (iconType ?: category.name).uppercase()
    return when {
        key.contains("HEADPHONES") || key.contains("AUDIO") -> Icons.Default.Headphones
        key.contains("SPEAKER") -> Icons.Default.Speaker
        key.contains("PHONE") -> Icons.Default.PhoneAndroid
        key.contains("WATCH") -> Icons.Default.Watch
        key.contains("LAPTOP") -> Icons.Default.Computer
        key.contains("TV") -> Icons.Default.Tv
        key.contains("SMART_HOME") -> Icons.Default.Home
        key.contains("CAR") -> Icons.Default.DirectionsCar
        key.contains("HEALTH") -> Icons.Default.MonitorWeight
        else -> Icons.Default.Bluetooth
    }
}

@Composable
fun CategoryAvatar(
    category: DeviceCategory,
    iconType: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIcon(category, iconType),
            contentDescription = category.labelFr,
            tint = tint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
