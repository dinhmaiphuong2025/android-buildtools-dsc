package com.zerostudio.lite

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ToolRepository {

    val tools = mutableListOf<ToolItem>()
    private val _logFlow = MutableSharedFlow<String>(replay = 50)
    val logFlow = _logFlow.asSharedFlow()

    init {
        initTools()
    }

    suspend fun emitLog(msg: String) {
        _logFlow.emit(msg)
    }

    private fun initTools() {
        tools.clear()
        // Core Compiler
        tools.add(
            ToolItem(
                id = "aapt2_bundled",
                title = "AAPT2 ARM64 (Tích hợp)",
                description = "Binary aapt2 arm64 được trích xuất sẵn từ APK, dùng ghi đè AAPT2 của Maven.",
                sizeText = "Dung lượng: ~6 MB (có sẵn)",
                downloadUrl = "aapt2-arm64-v8a",
                archiveType = ArchiveType.ASSET_COPY,
                targetDirName = "android-sdk/build-tools/36.0.0",
                verifyRelativePath = "android-sdk/build-tools/36.0.0/aapt2",
                category = ToolCategory.COMPILER
            )
        )
        tools.add(
            ToolItem(
                id = "jdk17",
                title = "OpenJDK 17 (arm64)",
                description = "Môi trường JVM Java runtime bắt buộc để thực thi tiến trình Gradle Wrapper.",
                sizeText = "Dung lượng: ~190 MB nén -> ~320 MB",
                downloadUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.14%2B7/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.14_7.tar.gz",
                archiveType = ArchiveType.TAR_GZ,
                targetDirName = "usr/opt/openjdk-17",
                verifyRelativePath = "usr/opt/openjdk-17/bin/java",
                category = ToolCategory.COMPILER
            )
        )
        tools.add(
            ToolItem(
                id = "build_tools_36",
                title = "Build-Tools 36.0.0 (arm64)",
                description = "Trọn bộ d8, zipalign, aidl, aapt native arm64 trích xuất từ ZeroStudio.",
                sizeText = "Dung lượng: ~16 MB nén -> ~80 MB",
                downloadUrl = "https://github.com/msmt2018/SDK-tool-for-Android-platform/releases/download/36.0.0/build-tools_36.0.0_aarch64.tar.xz",
                archiveType = ArchiveType.TAR_XZ,
                targetDirName = "android-sdk/build-tools/36.0.0",
                verifyRelativePath = "android-sdk/build-tools/36.0.0/zipalign",
                category = ToolCategory.COMPILER
            )
        )

        // SDK Platforms
        tools.add(
            ToolItem(
                id = "platform_35",
                title = "Android SDK Platform 35 (Android 15)",
                description = "Cung cấp android.jar API 35 cho các dự án thiết lập compileSdk 35.",
                sizeText = "Dung lượng: ~64 MB nén -> ~150 MB",
                downloadUrl = "https://dl.google.com/android/repository/platform-35_r02.zip",
                archiveType = ArchiveType.ZIP,
                targetDirName = "android-sdk/platforms",
                verifyRelativePath = "android-sdk/platforms/android-35/android.jar",
                category = ToolCategory.PLATFORMS
            )
        )
        tools.add(
            ToolItem(
                id = "platform_34",
                title = "Android SDK Platform 34 (Android 14)",
                description = "Cung cấp android.jar API 34 cho các dự án thiết lập compileSdk 34.",
                sizeText = "Dung lượng: ~63 MB nén -> ~145 MB",
                downloadUrl = "https://dl.google.com/android/repository/platform-34-ext7_r03.zip",
                archiveType = ArchiveType.ZIP,
                targetDirName = "android-sdk/platforms",
                verifyRelativePath = "android-sdk/platforms/android-34/android.jar",
                category = ToolCategory.PLATFORMS
            )
        )

        // Native C/C++
        tools.add(
            ToolItem(
                id = "ndk_29",
                title = "Android NDK r29 (arm64)",
                description = "Bộ công cụ Clang/Sysroot arm64 dùng cho các dự án chứa C/C++ native.",
                sizeText = "Dung lượng: ~170 MB nén -> ~1.8 GB",
                downloadUrl = "https://github.com/HomuHomu833/android-ndk-custom/releases/download/r29/android-ndk-r29-aarch64-linux-android.tar.xz",
                archiveType = ArchiveType.TAR_XZ,
                targetDirName = "android-sdk/ndk/29.0.14033849",
                verifyRelativePath = "android-sdk/ndk/29.0.14033849/source.properties",
                category = ToolCategory.NATIVE
            )
        )
        tools.add(
            ToolItem(
                id = "cmake_326",
                title = "CMake 3.26.0 (arm64)",
                description = "Công cụ dựng native CMake phối hợp cùng NDK khi compile C/C++.",
                sizeText = "Dung lượng: ~15 MB nén -> ~60 MB",
                downloadUrl = "https://github.com/AndroidIDEOfficial/androidide-tools/releases/download/v3.26.0/cmake-3.26.0-aarch64.tar.xz",
                archiveType = ArchiveType.TAR_XZ,
                targetDirName = "android-sdk/cmake/3.26.0",
                verifyRelativePath = "android-sdk/cmake/3.26.0/bin/cmake",
                category = ToolCategory.NATIVE
            )
        )
    }
}
