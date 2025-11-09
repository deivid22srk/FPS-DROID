package com.fpsdroid.monitor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpsdroid.monitor.core.PerformanceStats

@Composable
fun OverlayContent(
    stats: PerformanceStats,
    onClose: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .width(IntrinsicSize.Max)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FPS DROID",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            stats.fps >= 60 -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            stats.fps >= 30 -> Color(0xFFFF9800).copy(alpha = 0.2f)
                            else -> Color(0xFFF44336).copy(alpha = 0.2f)
                        }
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${stats.fps}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            stats.fps >= 60 -> Color(0xFF4CAF50)
                            stats.fps >= 30 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FPS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PerformanceRow(
                label = "CPU",
                value = "${String.format("%.1f", stats.cpuUsage)}%",
                progress = stats.cpuUsage / 100f,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (stats.cpuTemp > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                PerformanceRow(
                    label = "Temp",
                    value = "${String.format("%.0f", stats.cpuTemp)}°C",
                    progress = (stats.cpuTemp / 100f).coerceIn(0f, 1f),
                    color = when {
                        stats.cpuTemp < 60 -> Color(0xFF4CAF50)
                        stats.cpuTemp < 80 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            
            if (stats.gpuUsage > 0 || stats.gpuFreq > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                PerformanceRow(
                    label = "GPU",
                    value = if (stats.gpuUsage > 0) "${String.format("%.1f", stats.gpuUsage)}%" 
                           else "${stats.gpuFreq} MHz",
                    progress = if (stats.gpuUsage > 0) stats.gpuUsage / 100f 
                              else (stats.gpuFreq / 1000f).coerceIn(0f, 1f),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            if (stats.ramTotal > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                PerformanceRow(
                    label = "RAM",
                    value = "${stats.ramUsed} / ${stats.ramTotal} MB",
                    progress = stats.ramUsed.toFloat() / stats.ramTotal.toFloat(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun PerformanceRow(
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}
