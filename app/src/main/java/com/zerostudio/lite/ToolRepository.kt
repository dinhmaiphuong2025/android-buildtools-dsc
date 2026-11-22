package com.zerostudio.lite

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

data class BuildSession(
    val isBuilding: Boolean = false,
    val projectPath: String = "",
    val tasks: String = "",
    val startTime: Long = 0L,
    val lastDurationMs: Long = 0L,
    val lastExitCode: Int? = null,
    val totalBuilds: Int = 0
)

object ToolRepository {

    val tools = mutableListOf<ToolItem>()
    private val _logFlow = MutableSharedFlow<String>(replay = 50)
    val logFlow = _logFlow.asSharedFlow()

    private val _buildSession = MutableStateFlow(BuildSession())
    val buildSession = _buildSession.asStateFlow()

    @Volatile
    var activeProcess: Process? = null

    val logHistory: MutableList<String> = Collections.synchronizedList(mutableListOf<String>())
    private const val MAX_LOG_HISTORY = 3000

    init {
        initTools()
    }

    fun updateBuildSession(updater: (BuildSession) -> BuildSession) {
        _buildSession.value = updater(_buildSession.value)
    }

    fun cancelActiveBuild(): Boolean {
        return try {
            val p = activeProcess
            if (p != null) {
                p.destroyForcibly()
                activeProcess = null
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun emitLog(msg: String) {
        if (logHistory.size >= MAX_LOG_HISTORY) {
            logHistory.removeAt(0)
        }
        logHistory.add(msg)
        _logFlow.emit(msg)
    }

    fun clearLogs() {
        logHistory.clear()
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
                description = "Android-native (Bionic) OpenJDK 17 JVM runtime for Gradle Wrapper tasks.",
                sizeText = "150 MB",
                downloadUrl = "https://github.com/itsaky/openjdk-17-android/releases/download/01-01-2022/jdk17-arm64.tar.xz",
                archiveType = ArchiveType.TAR_XZ,
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

        // SDK Platforms (additional)
        tools.add(
            ToolItem(
                id = "platform_37",
                title = "Android SDK Platform 37",
                description = "Provides android.jar API 37 for projects targeting Android 16 QPR / Android 17 preview.",
                sizeText = "68 MB",
                downloadUrl = "https://dl.google.com/android/repository/platform-37.2_r02.zip",
                archiveType = ArchiveType.ZIP,
                targetDirName = "android-sdk/platforms",
                verifyRelativePath = "android-sdk/platforms/android-37/android.jar",
                category = ToolCategory.PLATFORMS
            )
        )
        tools.add(
            ToolItem(
                id = "platform_36",
                title = "Android SDK Platform 36",
                description = "Provides android.jar API 36 for projects targeting Android 16.",
                sizeText = "155 MB",
                downloadUrl = "https://dl.google.com/android/repository/platform-36_r01.zip",
                archiveType = ArchiveType.ZIP,
                targetDirName = "android-sdk/platforms",
                verifyRelativePath = "android-sdk/platforms/android-36/android.jar",
                category = ToolCategory.PLATFORMS
            )
        )
        tools.add(
            ToolItem(
                id = "platform_33",
                title = "Android SDK Platform 33",
                description = "Provides android.jar API 33 for projects targeting Android 13.",
                sizeText = "130 MB",
                downloadUrl = "https://dl.google.com/android/repository/platform-33-ext5_r05.zip",
                archiveType = ArchiveType.ZIP,
                targetDirName = "android-sdk/platforms",
                verifyRelativePath = "android-sdk/platforms/android-33/android.jar",
                category = ToolCategory.PLATFORMS
            )
        )

        // Utilities
        tools.add(
            ToolItem(
                id = "gradle_8_11",
                title = "Gradle 8.11.1 (Offline)",
                description = "Offline Gradle distribution for projects without network. Extracted to gradle wrapper cache.",
                sizeText = "165 MB",
                downloadUrl = "https://services.gradle.org/distributions/gradle-8.11.1-bin.zip",
                archiveType = ArchiveType.ZIP,
                targetDirName = "gradle-dist/gradle-8.11.1",
                verifyRelativePath = "gradle-dist/gradle-8.11.1/bin/gradle",
                category = ToolCategory.UTILITIES
            )
        )
        tools.add(
            ToolItem(
                id = "cmdline_tools",
                title = "SDK Command-Line Tools",
                description = "Android SDK command-line tools: sdkmanager, avdmanager, lint, retrace.",
                sizeText = "155 MB",
                downloadUrl = "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip",
                archiveType = ArchiveType.ZIP,
                targetDirName = "android-sdk/cmdline-tools/latest",
                verifyRelativePath = "android-sdk/cmdline-tools/latest/bin/sdkmanager",
                category = ToolCategory.UTILITIES
            )
        )
    }
}
