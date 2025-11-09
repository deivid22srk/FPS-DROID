package com.fpsdroid.monitor.util

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootUtils {
    private const val TAG = "RootUtils"
    
    private var rootAvailable: Boolean? = null
    private var suProcess: Process? = null

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (rootAvailable != null) return@withContext rootAvailable!!
        
        try {
            val process = Runtime.getRuntime().exec("su -c exit")
            val exitCode = process.waitFor()
            rootAvailable = (exitCode == 0)
            process.destroy()
            
            Log.d(TAG, "Root available: $rootAvailable")
            rootAvailable!!
        } catch (e: Exception) {
            Log.e(TAG, "Root check failed", e)
            rootAvailable = false
            false
        }
    }

    suspend fun executeRootCommand(command: String): String? = withContext(Dispatchers.IO) {
        if (isRootAvailable().not()) {
            Log.w(TAG, "Root not available, skipping command: $command")
            return@withContext null
        }

        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            
            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
            reader.close()
            
            val result = output.toString().trim()
            Log.d(TAG, "Command executed: $command, result length: ${result.length}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute root command: $command", e)
            null
        } finally {
            try {
                process?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to destroy process", e)
            }
        }
    }

    suspend fun readFile(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val normalRead = java.io.File(path).readText()
            Log.d(TAG, "Read file without root: $path")
            normalRead
        } catch (e: Exception) {
            Log.d(TAG, "Normal read failed, trying with root: $path")
            executeRootCommand("cat $path")
        }
    }

    fun cleanup() {
        try {
            suProcess?.destroy()
            suProcess = null
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
        }
    }
}
