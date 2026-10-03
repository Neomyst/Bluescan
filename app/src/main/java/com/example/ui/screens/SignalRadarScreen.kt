package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BluetoothDeviceItem
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.SignalMeter
import com.example.ui.theme.AccentGold
import com.example.ui.viewmodel.BluetoothUiState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SignalRadarScreen(
    uiState: BluetoothUiState,
    onSelectDevice: (BluetoothDeviceItem) -> Unit,
    onEditDevice: (BluetoothDeviceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = uiState.selectedDeviceForRadar ?: uiState.devices.firstOrNull()

    val infiniteTransition = rememberInfiniteTransition(label = "radarRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val accentGold = AccentGold

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Radar Proximité Signal",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Repérage visuel selon la puissance du signal (RSSI)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Device Selector Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(uiState.devices, key = { it.macAddress }) { dev ->
                val isSel = dev.macAddress == selected?.macAddress
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectDevice(dev) },
                    label = { Text(dev.displayName, fontSize = 12.sp) },
                    leadingIcon = {
                        if (dev.isFavorite) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(14.dp))
                        }
                    },
                    modifier = Modifier.testTag("radar_chip_${dev.macAddress}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Radar Scope View
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2

                // Concentric circles
                listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { fraction ->
                    drawCircle(
                        color = gridColor,
                        radius = maxRadius * fraction,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Crosshair lines
                drawLine(
                    color = gridColor,
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 1.dp.toPx()
                )

                // Rotating Scanner Sweep Line
                val rad = Math.toRadians(rotationAngle.toDouble())
                val endX = center.x + (maxRadius * cos(rad)).toFloat()
                val endY = center.y + (maxRadius * sin(rad)).toFloat()

                drawLine(
                    color = primaryColor,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Plot all devices on radar
                uiState.devices.forEachIndexed { idx, dev ->
                    val normalizedRssi = ((dev.rssi + 100).coerceIn(0, 60)) / 60f
                    val distanceFraction = 1f - (normalizedRssi * 0.85f) // Closer = near center

                    // Angle distributed around radar circle
                    val angleDeg = (idx * (360f / uiState.devices.size.coerceAtLeast(1))) + 45f
                    val devRad = Math.toRadians(angleDeg.toDouble())
                    val devX = center.x + (maxRadius * distanceFraction * cos(devRad)).toFloat()
                    val devY = center.y + (maxRadius * distanceFraction * sin(devRad)).toFloat()

                    val isSelectedDev = dev.macAddress == selected?.macAddress
                    val dotColor = if (isSelectedDev) primaryColor else if (dev.isFavorite) accentGold else Color.Gray
                    val dotRadius = if (isSelectedDev) 8.dp.toPx() else 5.dp.toPx()

                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(devX, devY)
                    )

                    if (isSelectedDev) {
                        drawCircle(
                            color = dotColor.copy(alpha = 0.3f),
                            radius = dotRadius * 2.2f,
                            center = Offset(devX, devY)
                        )
                    }
                }
            }

            // Center radar icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Device Info Details Card
        if (selected != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CategoryAvatar(category = selected.category, iconType = selected.iconType)

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = selected.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (selected.isFavorite) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = AccentGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = selected.macAddress,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { onEditDevice(selected) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Puissance du Signal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            SignalMeter(rssi = selected.rssi)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Distance Estimée",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selected.distanceEstimate,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (selected.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Note : ${selected.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
