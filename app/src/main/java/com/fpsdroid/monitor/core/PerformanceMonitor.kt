package com.fpsdroid.monitor.core

import android.util.Log
import android.view.Choreographer
import com.fpsdroid.monitor.util.RootUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0
    
    private val TAG = "PerformanceMonitor"

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - lastFpsTime
            
            if (timeDiff >= 1000) {
                currentFps = ((frameCount * 1000f) / timeDiff).toInt()
                frameCount = 0
                lastFpsTime = currentTime
            }
            
            if (monitoringJob?.isActive == true) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        
        Log.d(TAG, "Starting performance monitoring")
        
        Choreographer.getInstance().postFrameCallback(frameCallback)
        
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
                
                delay(1000)
            }
        }
    }

    fun stopMonitoring() {
        Log.d(TAG, "Stopping performance monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
        frameCount = 0
        currentFps = 0
    }

    private suspend fun measureFps(): Int = withContext(Dispatchers.IO) {
        try {
            if (currentFps > 0) {
                return@withContext currentFps.coerceIn(0, 240)
            }

            val refreshRate = try {
                val output = RootUtils.executeRootCommand(
                    "dumpsys display | grep 'mRefreshRate'"
                )
                output?.let {
                    val match = Regex("(\\d+\\.\\d+)").find(it)
                    match?.value?.toFloatOrNull()?.toInt() ?: 60
                } ?: 60
            } catch (e: Exception) {
                60
            }

            val gfxInfoOutput = RootUtils.executeRootCommand(
                "dumpsys gfxinfo | grep -A 1 'Total frames rendered'"
            )
            
            if (gfxInfoOutput != null && gfxInfoOutput.isNotBlank()) {
                val lines = gfxInfoOutput.trim().split("\n")
                for (line in lines) {
                    val frameMatch = Regex("(\\d+)").find(line)
                    if (frameMatch != null) {
                        val fps = frameMatch.value.toIntOrNull()
                        if (fps != null && fps in 1..240) {
                            return@withContext fps
                        }
                    }
                }
            }

            val sfOutput = RootUtils.executeRootCommand(
                "dumpsys SurfaceFlinger --list"
            )
            
            if (sfOutput != null && sfOutput.contains("SurfaceView")) {
                return@withContext refreshRate
            }

            refreshRate.coerceIn(0, 240)
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
                "/sys/class/thermal/thermal_zone2/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp"
            )
            
            for (zone in thermalZones) {
                val temp = RootUtils.readFile(zone)
                temp?.trim()?.toFloatOrNull()?.let {
                    val celsius = if (it > 1000) it / 1000f else it
                    if (celsius in 0f..150f) {
                        return@withContext celsius
                    }
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
                "/sys/devices/platform/soc/soc:qcom,kgsl-3d0/kgsl/kgsl-3d0/gpubusy",
                "/sys/kernel/gpu/gpu_busy"
            )
            
            for (path in gpuLoadPaths) {
                val content = RootUtils.readFile(path)
                if (content != null && content.isNotBlank()) {
                    val parts = content.trim().split(" ")
                    if (parts.size >= 2) {
                        val busy = parts[0].toLongOrNull() ?: continue
                        val total = parts[1].toLongOrNull() ?: continue
                        if (total > 0) {
                            val usage = (busy.toFloat() / total) * 100
                            if (usage in 0f..100f) {
                                return@withContext usage
                            }
                        }
                    } else {
                        content.trim().toFloatOrNull()?.let {
                            if (it in 0f..100f) {
                                return@withContext it
                            }
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
                "/sys/devices/platform/mali.0/clock",
                "/sys/kernel/gpu/gpu_clock"
            )
            
            for (path in freqPaths) {
                val freq = RootUtils.readFile(path)
                freq?.trim()?.toLongOrNull()?.let {
                    val mhz = if (it > 1_000_000) (it / 1_000_000).toInt() else it.toInt()
                    if (mhz in 1..5000) {
                        return@withContext mhz
                    }
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
                
                if (memTotal > 0) {
                    val memUsed = (memTotal - memAvailable) / 1024
                    val memTotalMB = memTotal / 1024
                    return@withContext Pair(memUsed, memTotalMB)
                }
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
