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
    NATIVE("Native C/C++")
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
        if (id == "jdk17") {
            if (checkFile.exists() && isBionicElf(checkFile)) return true
            val defaultHome = File(baseFilesDir, "usr/opt/openjdk-17")
            val subdirs = defaultHome.listFiles { f -> f.isDirectory } ?: emptyArray()
            for (sub in subdirs) {
                val subJava = File(sub, "bin/java")
                if (subJava.exists() && isBionicElf(subJava)) return true
            }
            return false
        }
        if (checkFile.exists()) return true
        if (id == "build_tools_36") {
            val nestedCheck = File(baseFilesDir, "android-sdk/build-tools/36.0.0/build-tools/36.0.0/zipalign")
            if (nestedCheck.exists()) return true
        }
        if (id == "ndk_29") {
            val oldNdk = File(baseFilesDir, "android-sdk/ndk/29.0.14033849/source.properties")
            if (oldNdk.exists()) return true
            val nestedZipNdk = File(baseFilesDir, "android-sdk/ndk/29.0.13113456/android-ndk-r29-beta1/source.properties")
            if (nestedZipNdk.exists()) return true
        }
        return false
    }
}
