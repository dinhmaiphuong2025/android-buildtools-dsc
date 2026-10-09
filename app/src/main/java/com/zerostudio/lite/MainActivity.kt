package com.zerostudio.lite

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvLogs: TextView
    private lateinit var scrollLogs: ScrollView
    private lateinit var chipDaemonStatus: Chip
    private lateinit var rvTools: RecyclerView
    private lateinit var adapter: ToolAdapter

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val toolList = mutableListOf<ToolItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLogs = findViewById(R.id.tvLogs)
        scrollLogs = findViewById(R.id.scrollLogs)
        chipDaemonStatus = findViewById(R.id.chipDaemonStatus)
        rvTools = findViewById(R.id.rvTools)

        initToolList()
        setupRecyclerView()
        startDaemonService()

        // Auto-extract bundled AAPT2 if needed
        val aapt2Tool = toolList.firstOrNull { it.id == "aapt2_bundled" }
        if (aapt2Tool != null && !aapt2Tool.isInstalled(filesDir)) {
            performInstall(aapt2Tool)
        }
    }

    private fun initToolList() {
        toolList.clear()
        toolList.add(
            ToolItem(
                id = "aapt2_bundled",
                title = "AAPT2 ARM64 (Tích hợp)",
                description = "Binary aapt2 arm64 được đóng gói sẵn trong APK, dùng để ghi đè AAPT2 của Maven.",
                sizeText = "Dung lượng: ~6 MB (có sẵn)",
                downloadUrl = "aapt2-arm64-v8a",
                archiveType = ArchiveType.ASSET_COPY,
                targetDirName = "android-sdk/build-tools/36.0.0",
                verifyRelativePath = "android-sdk/build-tools/36.0.0/aapt2"
            )
        )
        toolList.add(
            ToolItem(
                id = "jdk17",
                title = "OpenJDK 17 (arm64)",
                description = "Môi trường JVM bắt buộc để thực thi Gradle Wrapper và tác vụ biên dịch mã nguồn.",
                sizeText = "Dung lượng: ~190 MB (nén) -> ~320 MB (cài đặt)",
                downloadUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.14%2B7/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.14_7.tar.gz",
                archiveType = ArchiveType.TAR_GZ,
                targetDirName = "usr/opt/openjdk-17",
                verifyRelativePath = "usr/opt/openjdk-17/bin/java"
            )
        )
        toolList.add(
            ToolItem(
                id = "platform_35",
                title = "Android SDK Platform 35 (Android 15)",
                description = "Cung cấp android.jar API 35 cho các dự án thiết lập compileSdk 35.",
                sizeText = "Dung lượng: ~64 MB (nén) -> ~150 MB (cài đặt)",
                downloadUrl = "https://dl.google.com/android/repository/platform-35_r02.zip",
                archiveType = ArchiveType.ZIP,
                targetDirName = "android-sdk/platforms",
                verifyRelativePath = "android-sdk/platforms/android-35/android.jar"
            )
        )
        toolList.add(
            ToolItem(
                id = "platform_34",
                title = "Android SDK Platform 34 (Android 14)",
                description = "Cung cấp android.jar API 34 cho các dự án compileSdk 34.",
                sizeText = "Dung lượng: ~63 MB (nén) -> ~145 MB (cài đặt)",
                downloadUrl = "https://dl.google.com/android/repository/platform-34-ext7_r03.zip",
                archiveType = ArchiveType.ZIP,
                targetDirName = "android-sdk/platforms",
                verifyRelativePath = "android-sdk/platforms/android-34/android.jar"
            )
        )
        toolList.add(
            ToolItem(
                id = "build_tools_36",
                title = "Build-Tools 36.0.0 (arm64)",
                description = "Trọn bộ d8, zipalign, aidl native arm64 trích xuất từ ZeroStudio toolchain.",
                sizeText = "Dung lượng: ~16 MB (nén) -> ~80 MB (cài đặt)",
                downloadUrl = "https://github.com/msmt2018/SDK-tool-for-Android-platform/releases/download/36.0.0/build-tools_36.0.0_aarch64.tar.xz",
                archiveType = ArchiveType.TAR_XZ,
                targetDirName = "android-sdk/build-tools/36.0.0",
                verifyRelativePath = "android-sdk/build-tools/36.0.0/zipalign"
            )
        )
        toolList.add(
            ToolItem(
                id = "ndk_29",
                title = "Android NDK r29 (arm64)",
                description = "Bộ công cụ Clang/Sysroot arm64 dùng cho các dự án có mã nguồn C/C++ native (Tùy chọn).",
                sizeText = "Dung lượng: ~170 MB (nén) -> ~1.8 GB (cài đặt)",
                downloadUrl = "https://github.com/HomuHomu833/android-ndk-custom/releases/download/r29/android-ndk-r29-aarch64-linux-android.tar.xz",
                archiveType = ArchiveType.TAR_XZ,
                targetDirName = "android-sdk/ndk/29.0.14033849",
                verifyRelativePath = "android-sdk/ndk/29.0.14033849/source.properties"
            )
        )
        toolList.add(
            ToolItem(
                id = "cmake_326",
                title = "CMake 3.26.0 (arm64)",
                description = "Công cụ dựng dự án Native CMake phối hợp cùng NDK (Tùy chọn).",
                sizeText = "Dung lượng: ~15 MB (nén) -> ~60 MB (cài đặt)",
                downloadUrl = "https://github.com/AndroidIDEOfficial/androidide-tools/releases/download/v3.26.0/cmake-3.26.0-aarch64.tar.xz",
                archiveType = ArchiveType.TAR_XZ,
                targetDirName = "android-sdk/cmake/3.26.0",
                verifyRelativePath = "android-sdk/cmake/3.26.0/bin/cmake"
            )
        )
    }

    private fun setupRecyclerView() {
        rvTools.layoutManager = LinearLayoutManager(this)
        adapter = ToolAdapter(
            tools = toolList,
            filesDir = filesDir,
            onActionClick = { tool -> performInstall(tool) },
            onDeleteClick = { tool -> performDelete(tool) }
        )
        rvTools.adapter = adapter
    }

    private fun startDaemonService() {
        val intent = Intent(this, BuilderDaemonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        chipDaemonStatus.text = "Đang chạy"
    }

    private fun log(message: String) {
        scope.launch {
            tvLogs.append("$message\n")
            scrollLogs.post { scrollLogs.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun performInstall(tool: ToolItem) {
        tool.status = ToolStatus.DOWNLOADING
        tool.progress = 0
        adapter.notifyDataSetChanged()

        scope.launch(Dispatchers.IO) {
            val success = InstallerEngine.installTool(
                context = applicationContext,
                tool = tool,
                onProgress = { p ->
                    tool.progress = p
                    scope.launch { adapter.notifyDataSetChanged() }
                },
                onLog = { msg -> log(msg) }
            )

            tool.status = if (success) ToolStatus.INSTALLED else ToolStatus.NOT_INSTALLED
            scope.launch {
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun performDelete(tool: ToolItem) {
        scope.launch(Dispatchers.IO) {
            val ok = InstallerEngine.deleteTool(filesDir, tool)
            log(if (ok) ">> Đã xóa ${tool.title}" else "!! Không thể xóa ${tool.title}")
            scope.launch {
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
