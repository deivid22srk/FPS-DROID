package com.fpsdroid.monitor.core

import android.util.Log
import com.fpsdroid.monitor.util.RootUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class PerformanceStats(
    val fps: Int = 0,
    val cpuUsage: Float = 0f,
    val cpuTemp: Float = 0f,
    val gpuUsage: Float = 0f,
    val gpuFreq: Int = 0,
    val ramUsed: Int = 0,
    val ramTotal: Int = 0
)

class PerformanceMonitor {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null
    
    private val _stats = MutableStateFlow(PerformanceStats())
    val stats: StateFlow<PerformanceStats> = _stats.asStateFlow()
    
    private var lastFrameCount = 0L
    private var lastFrameTime = 0L
    
    private val TAG = "PerformanceMonitor"

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        
        Log.d(TAG, "Starting performance monitoring")
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    val fps = measureFps()
                    val cpuUsage = measureCpuUsage()
                    val cpuTemp = measureCpuTemp()
                    val gpuUsage = measureGpuUsage()
                    val gpuFreq = measureGpuFreq()
                    val (ramUsed, ramTotal) = measureRamUsage()
                    
                    _stats.value = PerformanceStats(
                        fps = fps,
                        cpuUsage = cpuUsage,
                        cpuTemp = cpuTemp,
                        gpuUsage = gpuUsage,
                        gpuFreq = gpuFreq,
                        ramUsed = ramUsed,
                        ramTotal = ramTotal
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error measuring performance", e)
                }
                
                delay(500)
            }
        }
    }

    fun stopMonitoring() {
        Log.d(TAG, "Stopping performance monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private suspend fun measureFps(): Int = withContext(Dispatchers.IO) {
        try {
            val surfaceFlingerDump = RootUtils.executeRootCommand(
                "dumpsys SurfaceFlinger --latency"
            )
            
            if (surfaceFlingerDump != null) {
                val lines = surfaceFlingerDump.split("\n").filter { it.isNotBlank() }
                if (lines.size > 2) {
                    val frames = lines.drop(1).mapNotNull { line ->
                        val parts = line.split("\t")
                        if (parts.size >= 3) parts[0].toLongOrNull() else null
                    }
                    
                    if (frames.size >= 2) {
                        val timeSpan = (frames.last() - frames.first()) / 1_000_000_000.0
                        if (timeSpan > 0) {
                            val fps = (frames.size / timeSpan).toInt()
                            return@withContext fps.coerceIn(0, 240)
                        }
                    }
                }
            }
            
            val gfxInfoOutput = RootUtils.executeRootCommand(
                "dumpsys gfxinfo | grep 'Total frames rendered' -A 1"
            )
            
            if (gfxInfoOutput != null) {
                val frameMatch = Regex("(\\d+)").find(gfxInfoOutput)
                if (frameMatch != null) {
                    val currentFrameCount = frameMatch.value.toLong()
                    val currentTime = System.currentTimeMillis()
                    
                    if (lastFrameTime > 0) {
                        val timeDiff = (currentTime - lastFrameTime) / 1000.0
                        val frameDiff = currentFrameCount - lastFrameCount
                        val fps = (frameDiff / timeDiff).toInt()
                        
                        lastFrameCount = currentFrameCount
                        lastFrameTime = currentTime
                        
                        return@withContext fps.coerceIn(0, 240)
                    }
                    
                    lastFrameCount = currentFrameCount
                    lastFrameTime = currentTime
                }
            }
            
            60
        } catch (e: Exception) {
            Log.e(TAG, "FPS measurement failed", e)
            60
        }
    }

    private suspend fun measureCpuUsage(): Float = withContext(Dispatchers.IO) {
        try {
            val stat1 = readCpuStats()
            delay(100)
            val stat2 = readCpuStats()
            
            if (stat1 != null && stat2 != null) {
                val idle1 = stat1[3] + stat1[4]
                val idle2 = stat2[3] + stat2[4]
                
                val total1 = stat1.sum()
                val total2 = stat2.sum()
                
                val totalDiff = total2 - total1
                val idleDiff = idle2 - idle1
                
                if (totalDiff > 0) {
                    val usage = ((totalDiff - idleDiff).toFloat() / totalDiff) * 100
                    return@withContext usage.coerceIn(0f, 100f)
                }
            }
            
            0f
        } catch (e: Exception) {
            Log.e(TAG, "CPU usage measurement failed", e)
            0f
        }
    }

    private suspend fun readCpuStats(): LongArray? = withContext(Dispatchers.IO) {
        try {
            val content = RootUtils.readFile("/proc/stat")
            content?.lines()?.firstOrNull()?.let { line ->
                val parts = line.split("\\s+".toRegex()).drop(1)
                parts.mapNotNull { it.toLongOrNull() }.toLongArray()
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun measureCpuTemp(): Float = withContext(Dispatchers.IO) {
        try {
            val thermalZones = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp"
            )
            
            for (zone in thermalZones) {
                val temp = RootUtils.readFile(zone)
                temp?.toFloatOrNull()?.let {
                    return@withContext (it / 1000f).coerceIn(0f, 150f)
                }
            }
            
            0f
        } catch (e: Exception) {
            Log.e(TAG, "CPU temp measurement failed", e)
            0f
        }
    }

    private suspend fun measureGpuUsage(): Float = withContext(Dispatchers.IO) {
        try {
            val gpuLoadPaths = listOf(
                "/sys/class/kgsl/kgsl-3d0/gpubusy",
                "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
                "/sys/devices/platform/mali.0/utilization",
                "/sys/devices/platform/soc/soc:qcom,kgsl-3d0/kgsl/kgsl-3d0/gpubusy"
            )
            
            for (path in gpuLoadPaths) {
                val content = RootUtils.readFile(path)
                if (content != null) {
                    val parts = content.split(" ")
                    if (parts.size >= 2) {
                        val busy = parts[0].toLongOrNull() ?: continue
                        val total = parts[1].toLongOrNull() ?: continue
                        if (total > 0) {
                            val usage = (busy.toFloat() / total) * 100
                            return@withContext usage.coerceIn(0f, 100f)
                        }
                    } else {
                        content.toFloatOrNull()?.let {
                            return@withContext it.coerceIn(0f, 100f)
                        }
                    }
                }
            }
            
            0f
        } catch (e: Exception) {
            Log.e(TAG, "GPU usage measurement failed", e)
            0f
        }
    }

    private suspend fun measureGpuFreq(): Int = withContext(Dispatchers.IO) {
        try {
            val freqPaths = listOf(
                "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
                "/sys/class/kgsl/kgsl-3d0/clock_mhz",
                "/sys/devices/platform/mali.0/clock"
            )
            
            for (path in freqPaths) {
                val freq = RootUtils.readFile(path)
                freq?.toLongOrNull()?.let {
                    val mhz = if (it > 1_000_000) (it / 1_000_000).toInt() else it.toInt()
                    return@withContext mhz.coerceIn(0, 5000)
                }
            }
            
            0
        } catch (e: Exception) {
            Log.e(TAG, "GPU freq measurement failed", e)
            0
        }
    }

    private suspend fun measureRamUsage(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        try {
            val memInfo = RootUtils.readFile("/proc/meminfo")
            if (memInfo != null) {
                val lines = memInfo.lines()
                var memTotal = 0
                var memAvailable = 0
                
                for (line in lines) {
                    when {
                        line.startsWith("MemTotal:") -> {
                            memTotal = line.split("\\s+".toRegex())[1].toIntOrNull() ?: 0
                        }
                        line.startsWith("MemAvailable:") -> {
                            memAvailable = line.split("\\s+".toRegex())[1].toIntOrNull() ?: 0
                        }
                    }
                }
                
                val memUsed = (memTotal - memAvailable) / 1024
                val memTotalMB = memTotal / 1024
                
                return@withContext Pair(memUsed, memTotalMB)
            }
            
            Pair(0, 0)
        } catch (e: Exception) {
            Log.e(TAG, "RAM measurement failed", e)
            Pair(0, 0)
        }
    }

    fun cleanup() {
        stopMonitoring()
        scope.cancel()
    }
}
