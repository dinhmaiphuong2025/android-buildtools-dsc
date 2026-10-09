package com.zerostudio.lite

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLogs: TextView
    private lateinit var btnInstall: Button
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvLogs = findViewById(R.id.tvLogs)
        btnInstall = findViewById(R.id.btnInstallTools)

        extractBundledAapt2()

        val serviceIntent = Intent(this, BuilderDaemonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        tvStatus.text = "Daemon sẵn sàng tại http://127.0.0.1:8088"

        btnInstall.setOnClickListener {
            startBuildToolsDownload()
        }
    }

    private fun log(msg: String) {
        scope.launch {
            tvLogs.append("$msg\n")
        }
    }

    private fun extractBundledAapt2() {
        scope.launch(Dispatchers.IO) {
            try {
                val sdkDir = File(filesDir, "android-sdk")
                val buildToolsDir = File(sdkDir, "build-tools/36.0.0")
                buildToolsDir.mkdirs()

                val aapt2File = File(buildToolsDir, "aapt2")
                if (!aapt2File.exists() || aapt2File.length() == 0L) {
                    log(">> Đang trích xuất AAPT2 arm64 tích hợp sẵn...")
                    assets.open("aapt2-arm64-v8a").use { input ->
                        FileOutputStream(aapt2File).use { output ->
                            input.copyTo(output)
                        }
                    }
                    aapt2File.setExecutable(true, false)
                    log(">> AAPT2 arm64 đã sẵn sàng tại: ${aapt2File.absolutePath}")
                } else {
                    log(">> AAPT2 arm64 đã tồn tại: ${aapt2File.absolutePath}")
                }
            } catch (e: Exception) {
                log("!! Không thể giải nén AAPT2: ${e.message}")
            }
        }
    }

    private fun startBuildToolsDownload() {
        btnInstall.isEnabled = false
        log(">> Đang truy vấn manifest...")

        scope.launch(Dispatchers.IO) {
            try {
                val manifestUrl = "https://github.com/msmt2018/SDK-tool-for-Android-platform/releases/download/IDESdkDownJson2.3/manifest.json"
                val conn = URL(manifestUrl).openConnection() as HttpURLConnection
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)

                val buildToolsUrl = json.getJSONObject("build_tools").getJSONObject("aarch64").getString("_36_0_0")
                log(">> URL: $buildToolsUrl")

                val sdkDir = File(filesDir, "android-sdk")
                val buildToolsDir = File(sdkDir, "build-tools/36.0.0")
                buildToolsDir.mkdirs()

                val dlFile = File(cacheDir, "build-tools.tar.xz")
                log(">> Đang tải build-tools (~16MB)...")
                val dlConn = URL(buildToolsUrl).openConnection() as HttpURLConnection
                dlConn.inputStream.use { input ->
                    FileOutputStream(dlFile).use { output ->
                        input.copyTo(output)
                    }
                }

                log(">> Đang giải nén...")
                val process = Runtime.getRuntime().exec(arrayOf("tar", "-xf", dlFile.absolutePath, "--strip-components=2", "-C", buildToolsDir.absolutePath))
                process.waitFor()
                dlFile.delete()

                File(buildToolsDir, "aapt2").setExecutable(true, false)
                File(buildToolsDir, "zipalign").setExecutable(true, false)

                log(">> Cập nhật Build-Tools 36.0.0 aarch64 hoàn tất!")
                withContext(Dispatchers.Main) {
                    btnInstall.isEnabled = true
                }
            } catch (e: Exception) {
                log("!! Lỗi tải build-tools: ${e.message}")
                withContext(Dispatchers.Main) {
                    btnInstall.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
