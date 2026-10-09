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
                title = "AAPT2 ARM64 (Bundled)",
                description = "Pre-compiled AAPT2 arm64 binary extracted from APK to override Maven's x86 artifact.",
                sizeText = "6 MB",
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
                description = "Adoptium Temurin JVM runtime required to execute Gradle Wrapper tasks.",
                sizeText = "320 MB",
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
                description = "Full native arm64 tool suite (d8, zipalign, aidl, aapt) extracted from ZeroStudio.",
                sizeText = "80 MB",
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
                title = "Android SDK Platform 35",
                description = "Provides android.jar API 35 for projects targeting Android 15.",
                sizeText = "150 MB",
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
                title = "Android SDK Platform 34",
                description = "Provides android.jar API 34 for projects targeting Android 14.",
                sizeText = "145 MB",
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
                description = "Custom Clang/Sysroot toolchain for apps containing native C/C++ code.",
                sizeText = "1.8 GB",
                downloadUrl = "https://github.com/ZeroAicy/AIDE-Ndk-Install/releases/download/v2.7.8/android-ndk-r29-beta1-aarch64.zip",
                archiveType = ArchiveType.ZIP,
                targetDirName = "android-sdk/ndk/29.0.13113456",
                verifyRelativePath = "android-sdk/ndk/29.0.13113456/source.properties",
                category = ToolCategory.NATIVE
            )
        )
        tools.add(
            ToolItem(
                id = "cmake_326",
                title = "CMake 3.26.0 (arm64)",
                description = "Build system binary for NDK-based C/C++ compilation.",
                sizeText = "60 MB",
                downloadUrl = "https://github.com/AndroidIDEOfficial/androidide-tools/releases/download/v3.26.0/cmake-3.26.0-aarch64.tar.xz",
                archiveType = ArchiveType.TAR_XZ,
                targetDirName = "android-sdk/cmake/3.26.0",
                verifyRelativePath = "android-sdk/cmake/3.26.0/bin/cmake",
                category = ToolCategory.NATIVE
            )
        )
    }
}
