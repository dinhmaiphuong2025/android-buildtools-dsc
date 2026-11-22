package com.zerostudio.lite

import java.io.File

enum class ToolStatus {
    NOT_INSTALLED,
    DOWNLOADING,
    EXTRACTING,
    INSTALLED
}

enum class ArchiveType {
    TAR_GZ,
    TAR_XZ,
    ZIP,
    ASSET_COPY
}

enum class ToolCategory(val title: String) {
    COMPILER("Compiler (Core)"),
    PLATFORMS("SDK Platforms"),
    NATIVE("Native C/C++"),
    UTILITIES("Utilities")
}

fun isBionicElf(file: File): Boolean {
    if (!file.exists() || file.length() < 512) return false
    return try {
        java.io.FileInputStream(file).use { input ->
            val buffer = ByteArray(1024)
            val read = input.read(buffer)
            if (read > 0) {
                val str = String(buffer, 0, read, Charsets.ISO_8859_1)
                str.contains("linker64") || str.contains("/system/bin/linker")
            } else false
        }
    } catch (_: Exception) {
        false
    }
}

data class ToolItem(
    val id: String,
    val title: String,
    val description: String,
    val sizeText: String,
    val downloadUrl: String,
    val archiveType: ArchiveType,
    val targetDirName: String,
    val verifyRelativePath: String,
    val category: ToolCategory,
    var status: ToolStatus = ToolStatus.NOT_INSTALLED,
    var progress: Int = 0
) {
    fun isInstalled(baseFilesDir: File): Boolean {
        val checkFile = File(baseFilesDir, verifyRelativePath)
        if (checkFile.exists()) return true

        if (id == "jdk17") {
            val defaultHome = File(baseFilesDir, "usr/opt/openjdk-17")
            if (File(defaultHome, "bin/java").exists()) return true
            val subdirs = defaultHome.listFiles { f -> f.isDirectory } ?: emptyArray()
            for (sub in subdirs) {
                if (File(sub, "bin/java").exists()) return true
            }
            return false
        }

        if (id == "build_tools_36") {
            if (File(baseFilesDir, "android-sdk/build-tools/36.0.0/build-tools/36.0.0/zipalign").exists()) return true
            if (File(baseFilesDir, "android-sdk/build-tools/36.0.0/aapt2").exists()) return true
            if (File(baseFilesDir, "android-sdk/build-tools/36.0.0/zipalign").exists()) return true
            return false
        }

        if (id == "ndk_29") {
            if (File(baseFilesDir, "android-sdk/ndk/29.0.14033849/source.properties").exists()) return true
            if (File(baseFilesDir, "android-sdk/ndk/29.0.13113456/android-ndk-r29-beta1/source.properties").exists()) return true
            if (File(baseFilesDir, "android-sdk/ndk/29.0.13113456/source.properties").exists()) return true
            val ndkDir = File(baseFilesDir, "android-sdk/ndk")
            if (ndkDir.exists()) {
                val found = ndkDir.walkTopDown().maxDepth(4).any { it.name == "source.properties" }
                if (found) return true
            }
            return false
        }

        if (id == "cmdline_tools") {
            if (File(baseFilesDir, "android-sdk/cmdline-tools/latest/cmdline-tools/bin/sdkmanager").exists()) return true
            if (File(baseFilesDir, "android-sdk/cmdline-tools/bin/sdkmanager").exists()) return true
            if (File(baseFilesDir, "android-sdk/cmdline-tools/latest/bin/sdkmanager").exists()) return true
            val cmdDir = File(baseFilesDir, "android-sdk/cmdline-tools")
            if (cmdDir.exists()) {
                val found = cmdDir.walkTopDown().maxDepth(4).any { it.name == "sdkmanager" }
                if (found) return true
            }
            return false
        }

        if (id == "gradle_8_11") {
            if (File(baseFilesDir, "gradle-dist/gradle-8.11.1/gradle-8.11.1/bin/gradle").exists()) return true
            if (File(baseFilesDir, "gradle-dist/bin/gradle").exists()) return true
            if (File(baseFilesDir, "gradle-dist/gradle-8.11.1/bin/gradle").exists()) return true
            val gradleDir = File(baseFilesDir, "gradle-dist")
            if (gradleDir.exists()) {
                val found = gradleDir.walkTopDown().maxDepth(4).any { it.name == "gradle" && it.isFile }
                if (found) return true
            }
            return false
        }

        if (id == "cmake_326") {
            val cmakeHome = File(baseFilesDir, "android-sdk/cmake/3.26.0")
            if (File(cmakeHome, "bin/cmake").exists()) return true
            val subdirs = cmakeHome.listFiles { f -> f.isDirectory } ?: emptyArray()
            for (sub in subdirs) {
                if (File(sub, "bin/cmake").exists()) return true
            }
            val cmakeParent = File(baseFilesDir, "android-sdk/cmake")
            if (cmakeParent.exists()) {
                val found = cmakeParent.walkTopDown().maxDepth(4).any { it.name == "cmake" && it.isFile }
                if (found) return true
            }
            return false
        }

        if (category == ToolCategory.PLATFORMS) {
            val platformsDir = File(baseFilesDir, "android-sdk/platforms")
            if (platformsDir.exists()) {
                val apiNum = id.removePrefix("platform_")
                val found = platformsDir.walkTopDown().maxDepth(3).any { jar ->
                    if (jar.name == "android.jar" && jar.isFile) {
                        val parentDir = jar.parentFile ?: return@any false
                        if (parentDir.name == "android-$apiNum" || parentDir.name.startsWith("android-$apiNum.") || parentDir.name.contains(apiNum)) {
                            true
                        } else {
                            try {
                                val props = File(parentDir, "source.properties")
                                props.exists() && props.readLines().any { line ->
                                    line.replace(" ", "").startsWith("AndroidVersion.ApiLevel=$apiNum")
                                }
                            } catch (_: Exception) {
                                false
                            }
                        }
                    } else false
                }
                if (found) return true
            }
            return false
        }

        // Generic fallback for any tool: search for verification file inside its target directory
        val targetDir = File(baseFilesDir, targetDirName)
        if (targetDir.exists()) {
            val verifyName = checkFile.name
            val found = targetDir.walkTopDown().maxDepth(5).any { it.name == verifyName && it.isFile }
            if (found) return true
        }

        return false
    }
}
