package com.fpsdroid.monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpsdroid.monitor.core.PerformanceStats
import kotlin.math.roundToInt

@Composable
fun OverlayContent(
    stats: PerformanceStats,
    onClose: () -> Unit,
    onPositionChange: (IntOffset) -> Unit = {}
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Surface(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    onPositionChange(IntOffset(offsetX.roundToInt(), offsetY.roundToInt()))
                }
            }
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FPS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            stats.fps >= 60 -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                            stats.fps >= 30 -> Color(0xFFFF9800).copy(alpha = 0.15f)
                            else -> Color(0xFFF44336).copy(alpha = 0.15f)
                        }
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${stats.fps}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            stats.fps >= 60 -> Color(0xFF4CAF50)
                            stats.fps >= 30 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            CompactStat("CPU", "${String.format("%.0f", stats.cpuUsage)}%")
            
            if (stats.gpuUsage > 0) {
                Spacer(modifier = Modifier.height(3.dp))
                CompactStat("GPU", "${String.format("%.0f", stats.gpuUsage)}%")
            }
            
            if (stats.cpuTemp > 0) {
                Spacer(modifier = Modifier.height(3.dp))
                CompactStat(
                    "TEMP", 
                    "${String.format("%.0f", stats.cpuTemp)}°C",
                    color = when {
                        stats.cpuTemp < 60 -> Color(0xFF4CAF50)
                        stats.cpuTemp < 80 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
        }
    }
}

@Composable
fun CompactStat(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 10.sp,
            color = color.copy(alpha = 0.9f),
            fontWeight = FontWeight.Bold
        )
    }
}
