package com.zerostudio.lite

import android.content.Context
import fi.iki.elonen.NanoHTTPD
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
            val sdkDir = File(context.filesDir, "android-sdk")
            val aapt2 = File(sdkDir, "build-tools/36.0.0/aapt2")
            json.put("aapt2_ready", aapt2.exists() && aapt2.canExecute())
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

    private fun executeBuild(projectDir: File, tasks: List<String>): JSONObject {
        val logBuffer = StringBuilder()
        var exitCode = -1

        try {
            val gradlew = File(projectDir, "gradlew")
            if (!gradlew.exists()) {
                return JSONObject().apply {
                    put("success", false)
                    put("error", "gradlew not found in $projectDir")
                }
            }

            val filesDir = context.filesDir
            val sdkDir = File(filesDir, "android-sdk")
            val aapt2Bin = File(sdkDir, "build-tools/36.0.0/aapt2")

            val cmd = mutableListOf("sh", gradlew.absolutePath)
            cmd.addAll(tasks.filter { it.isNotBlank() })
            if (aapt2Bin.exists()) {
                cmd.add("-Pandroid.aapt2FromMavenOverride=${aapt2Bin.absolutePath}")
            }

            val pb = ProcessBuilder(cmd)
            pb.directory(projectDir)
            val env = pb.environment()

            if (sdkDir.exists()) {
                env["ANDROID_HOME"] = sdkDir.absolutePath
                env["ANDROID_SDK_ROOT"] = sdkDir.absolutePath
            }

            pb.redirectErrorStream(true)
            val process = pb.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                logBuffer.append(line).append("\n")
            }
            exitCode = process.waitFor()
            lastLogs = logBuffer.toString()

        } catch (e: Exception) {
            logBuffer.append("Exception during build: ${e.message}\n")
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
}
