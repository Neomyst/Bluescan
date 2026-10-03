package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SignalLevel
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalOrange
import com.example.ui.theme.SignalRed

@Composable
fun SignalMeter(
    rssi: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val level = when {
        rssi >= -60 -> 4
        rssi >= -72 -> 3
        rssi >= -85 -> 2
        rssi > -100 -> 1
        else -> 0
    }

    val color = when (level) {
        4 -> SignalGreen
        3 -> Color(0xFF10B981)
        2 -> SignalOrange
        1 -> SignalRed
        else -> Color.Gray
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.height(16.dp)
        ) {
            for (bar in 1..4) {
                val barHeight = (4 + bar * 3).dp
                val active = bar <= level
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (active) color else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "$rssi dBm",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = color
            )
        }
    }
}
