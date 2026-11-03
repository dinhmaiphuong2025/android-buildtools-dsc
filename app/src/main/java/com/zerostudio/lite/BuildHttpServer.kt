package com.zerostudio.lite

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class BuildHttpServer(port: Int, private val context: Context) : NanoHTTPD(port) {

    @Volatile
    var lastLogs: String = ""

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        if (method == Method.GET && uri == "/status") {
            val json = JSONObject()
            json.put("status", "running")
            val filesDir = context.filesDir
            val javaHome = resolveJavaHome(filesDir)
            val javaBin = File(javaHome, "bin/java")
            ensureExecutable(javaBin)

            val sdkDir = File(filesDir, "android-sdk")
            val aapt2Bin = resolveAapt2Bin(sdkDir)
            if (aapt2Bin != null) ensureExecutable(aapt2Bin)
            val ndkDir = resolveNdkDir(sdkDir)

            json.put("java_ready", javaBin.exists() && javaBin.canExecute())
            json.put("java_home", javaHome.absolutePath)
            json.put("aapt2_ready", aapt2Bin != null && aapt2Bin.exists() && aapt2Bin.canExecute())
            json.put("sdk_ready", sdkDir.exists())
            json.put("ndk_ready", ndkDir != null && ndkDir.exists())

            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
        }

        if (method == Method.GET && uri == "/build/log") {
            return newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", lastLogs)
        }

        if (method == Method.POST && uri == "/build") {
            val map = HashMap<String, String>()
            try {
                session.parseBody(map)
            } catch (e: Exception) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Parse error")
            }

            val postData = map["postData"] ?: ""
            val json = if (postData.isNotEmpty()) JSONObject(postData) else JSONObject()
            val projectPath = json.optString("projectPath", "")
            val tasks = json.optString("tasks", "assembleDebug")

            if (projectPath.isEmpty() || !File(projectPath).exists()) {
                val res = JSONObject().apply {
                    put("success", false)
                    put("error", "Project path does not exist: $projectPath")
                }
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", res.toString())
            }

            val result = executeBuild(File(projectPath), tasks.split(" "))
            return newFixedLengthResponse(Response.Status.OK, "application/json", result.toString())
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
    }

    private fun logBoth(msg: String, buffer: StringBuilder) {
        buffer.append(msg).append("\n")
        CoroutineScope(Dispatchers.Main).launch {
            ToolRepository.emitLog(msg)
        }
    }

    private fun executeBuild(projectDir: File, tasks: List<String>): JSONObject {
        val logBuffer = StringBuilder()
        var exitCode = -1

        try {
            val gradlew = projectDir.resolve("gradlew")
            if (!gradlew.exists()) {
                return JSONObject().apply {
                    put("success", false)
                    put("error", "gradlew not found in $projectDir")
                }
            }

            val filesDir = context.filesDir
            val javaHome = resolveJavaHome(filesDir)
            val javaBin = File(javaHome, "bin/java")
            ensureBinDirectoryExecutables(File(javaHome, "bin"))
            ensureExecutable(javaBin)

            val sdkDir = File(filesDir, "android-sdk")
            val aapt2Bin = resolveAapt2Bin(sdkDir)
            if (aapt2Bin != null) ensureExecutable(aapt2Bin)
            val cmakeDir = File(sdkDir, "cmake/3.26.0/bin")
            val ndkDir = resolveNdkDir(sdkDir)

            // Dùng binary sh của Android để thông dịch script gradlew nhằm bypass kernel noexec trên /storage/emulated/0
            val shBinary = if (File("/system/bin/sh").exists()) "/system/bin/sh" else "sh"
            val cmd = mutableListOf(shBinary, gradlew.absolutePath)
            cmd.addAll(tasks.filter { it.isNotBlank() })
            if (aapt2Bin != null && aapt2Bin.exists()) {
                cmd.add("-Pandroid.aapt2FromMavenOverride=${aapt2Bin.absolutePath}")
            }

            logBoth(">> [Daemon] Received build request: ${projectDir.absolutePath}", logBuffer)
            logBoth(">> Tasks: ${tasks.joinToString(" ")}", logBuffer)

            val pb = ProcessBuilder(cmd)
            pb.directory(projectDir)
            val env = pb.environment()

            var pathEnv = env["PATH"] ?: ""
            if (!pathEnv.contains("/system/bin")) {
                pathEnv = if (pathEnv.isEmpty()) "/system/bin" else "$pathEnv:/system/bin"
            }

            if (javaHome.exists() && javaBin.exists()) {
                env["JAVA_HOME"] = javaHome.absolutePath
                pathEnv = "${javaHome.absolutePath}/bin:$pathEnv"
                logBoth(">> [Daemon] JAVA_HOME set to: ${javaHome.absolutePath} (canExecute=${javaBin.canExecute()})", logBuffer)
            } else {
                logBoth("!! [Daemon] WARNING: bin/java not found in ${javaHome.absolutePath}", logBuffer)
            }

            if (sdkDir.exists()) {
                env["ANDROID_HOME"] = sdkDir.absolutePath
                env["ANDROID_SDK_ROOT"] = sdkDir.absolutePath
            }
            if (ndkDir != null && ndkDir.exists()) {
                env["ANDROID_NDK_ROOT"] = ndkDir.absolutePath
                env["ANDROID_NDK_HOME"] = ndkDir.absolutePath
            }
            if (cmakeDir.exists()) {
                pathEnv = "${cmakeDir.absolutePath}:$pathEnv"
            }

            env["PATH"] = pathEnv

            pb.redirectErrorStream(true)
            val process = pb.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { logBoth(it, logBuffer) }
            }
            exitCode = process.waitFor()
            lastLogs = logBuffer.toString()

            logBoth(">> [Daemon] Build exited with code: $exitCode", logBuffer)

        } catch (e: Exception) {
            logBoth("!! [Daemon] Exception during build: ${e.message}", logBuffer)
            lastLogs = logBuffer.toString()
            return JSONObject().apply {
                put("success", false)
                put("error", e.message)
                put("exitCode", -1)
            }
        }

        return JSONObject().apply {
            put("success", exitCode == 0)
            put("exitCode", exitCode)
            put("summary", if (exitCode == 0) "Build successful" else "Build failed")
        }
    }

    companion object {
        fun resolveJavaHome(filesDir: File): File {
            val defaultHome = File(filesDir, "usr/opt/openjdk-17")
            if (File(defaultHome, "bin/java").exists()) return defaultHome
            val subdirs = defaultHome.listFiles { f -> f.isDirectory } ?: emptyArray()
            for (sub in subdirs) {
                if (File(sub, "bin/java").exists()) return sub
            }
            return defaultHome
        }

        fun ensureExecutable(file: File) {
            if (file.exists()) {
                file.setExecutable(true, false)
                try {
                    Runtime.getRuntime().exec(arrayOf("chmod", "+x", file.absolutePath)).waitFor()
                } catch (_: Exception) {}
            }
        }

        fun ensureBinDirectoryExecutables(binDir: File) {
            if (!binDir.exists() || !binDir.isDirectory) return
            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", binDir.absolutePath)).waitFor()
            } catch (_: Exception) {}
            val files = binDir.listFiles() ?: return
            for (f in files) {
                if (f.isFile) {
                    f.setExecutable(true, false)
                }
            }
        }

        fun resolveAapt2Bin(sdkDir: File): File? {
            val candidates = listOf(
                File(sdkDir, "build-tools/36.0.0/aapt2"),
                File(sdkDir, "build-tools/36.0.0/build-tools/36.0.0/aapt2")
            )
            return candidates.firstOrNull { it.exists() } ?: File(sdkDir, "build-tools/36.0.0/aapt2")
        }

        fun resolveNdkDir(sdkDir: File): File? {
            val candidates = listOf(
                File(sdkDir, "ndk/29.0.13113456/android-ndk-r29-beta1"),
                File(sdkDir, "ndk/29.0.13113456"),
                File(sdkDir, "ndk/29.0.14033849")
            )
            for (cand in candidates) {
                if (File(cand, "source.properties").exists()) return cand
            }
            val ndkParent = File(sdkDir, "ndk")
            val subdirs = ndkParent.listFiles { f -> f.isDirectory } ?: emptyArray()
            for (sub in subdirs) {
                if (File(sub, "source.properties").exists()) return sub
                val nested = sub.listFiles { f -> f.isDirectory } ?: emptyArray()
                for (n in nested) {
                    if (File(n, "source.properties").exists()) return n
                }
            }
            return candidates.firstOrNull { it.exists() }
        }
    }
}
