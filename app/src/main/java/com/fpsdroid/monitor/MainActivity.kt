package com.fpsdroid.monitor

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpsdroid.monitor.core.PerformanceMonitor
import com.fpsdroid.monitor.service.OverlayService
import com.fpsdroid.monitor.ui.theme.FpsDroidTheme
import com.fpsdroid.monitor.util.RootUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkOverlayPermission() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FpsDroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onStartOverlay = { startOverlayService() },
                        onStopOverlay = { stopOverlayService() },
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        hasOverlayPermission = { checkOverlayPermission() }
                    )
                }
            }
        }
    }
    
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }
    
    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun stopOverlayService() {
        stopService(Intent(this, OverlayService::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    hasOverlayPermission: () -> Boolean
) {
    var isOverlayActive by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(hasOverlayPermission()) }
    var isRootAvailable by remember { mutableStateOf<Boolean?>(null) }
    
    val scope = rememberCoroutineScope()
    val performanceMonitor = remember { PerformanceMonitor() }
    val stats by performanceMonitor.stats.collectAsState()
    
    LaunchedEffect(Unit) {
        scope.launch {
            isRootAvailable = RootUtils.isRootAvailable()
        }
    }
    
    DisposableEffect(Unit) {
        performanceMonitor.startMonitoring()
        onDispose {
            performanceMonitor.cleanup()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "FPS DROID",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            HeroCard(
                fps = stats.fps,
                cpuUsage = stats.cpuUsage,
                gpuUsage = stats.gpuUsage
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ControlCard(
                isOverlayActive = isOverlayActive,
                hasPermission = hasPermission,
                onToggleOverlay = {
                    if (hasPermission) {
                        if (isOverlayActive) {
                            onStopOverlay()
                            isOverlayActive = false
                        } else {
                            onStartOverlay()
                            isOverlayActive = true
                        }
                    } else {
                        onRequestOverlayPermission()
                        hasPermission = hasOverlayPermission()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            StatsGrid(stats = stats)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            SystemInfoCard(
                isRootAvailable = isRootAvailable,
                ramUsed = stats.ramUsed,
                ramTotal = stats.ramTotal,
                cpuTemp = stats.cpuTemp,
                gpuFreq = stats.gpuFreq
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HeroCard(
    fps: Int,
    cpuUsage: Float,
    gpuUsage: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.primaryContainer
                        ),
                        startX = offset,
                        endX = offset + 500f
                    )
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current FPS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$fps",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            fps >= 60 -> Color(0xFF4CAF50)
                            fps >= 30 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FPS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickStat("CPU", "${String.format("%.0f", cpuUsage)}%")
                    QuickStat("GPU", "${String.format("%.0f", gpuUsage)}%")
                }
            }
        }
    }
}

@Composable
fun QuickStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ControlCard(
    isOverlayActive: Boolean,
    hasPermission: Boolean,
    onToggleOverlay: () -> Unit
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isOverlayActive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "buttonColor"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isOverlayActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = buttonColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (!hasPermission) {
                    "Grant Overlay Permission"
                } else if (isOverlayActive) {
                    "Overlay is Active"
                } else {
                    "Start Monitoring"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (!hasPermission) {
                    "Required to show FPS overlay"
                } else if (isOverlayActive) {
                    "Overlay is displayed on your screen"
                } else {
                    "Launch floating FPS monitor"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onToggleOverlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (isOverlayActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (!hasPermission) "Grant Permission" 
                          else if (isOverlayActive) "Stop Overlay" 
                          else "Start Overlay",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatsGrid(stats: com.fpsdroid.monitor.core.PerformanceStats) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Performance Stats",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Memory,
                label = "CPU Usage",
                value = "${String.format("%.1f", stats.cpuUsage)}%",
                color = MaterialTheme.colorScheme.primary
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Videocam,
                label = "GPU Usage",
                value = "${String.format("%.1f", stats.gpuUsage)}%",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SystemInfoCard(
    isRootAvailable: Boolean?,
    ramUsed: Int,
    ramTotal: Int,
    cpuTemp: Float,
    gpuFreq: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "System Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            InfoRow(
                icon = Icons.Default.Security,
                label = "Root Access",
                value = when (isRootAvailable) {
                    true -> "Available"
                    false -> "Not Available"
                    null -> "Checking..."
                },
                color = when (isRootAvailable) {
                    true -> Color(0xFF4CAF50)
                    false -> Color(0xFFF44336)
                    null -> MaterialTheme.colorScheme.outline
                }
            )
            
            if (ramTotal > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(
                    icon = Icons.Default.Storage,
                    label = "RAM Usage",
                    value = "$ramUsed / $ramTotal MB",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            if (cpuTemp > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(
                    icon = Icons.Default.Thermostat,
                    label = "CPU Temperature",
                    value = "${String.format("%.1f", cpuTemp)}°C",
                    color = when {
                        cpuTemp < 60 -> Color(0xFF4CAF50)
                        cpuTemp < 80 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            
            if (gpuFreq > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(
                    icon = Icons.Default.Speed,
                    label = "GPU Frequency",
                    value = "$gpuFreq MHz",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
