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
    COMPILER("Biên dịch (Core)"),
    PLATFORMS("SDK Platforms"),
    NATIVE("Native C/C++")
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
        return checkFile.exists() && (checkFile.length() > 0 || checkFile.isDirectory)
    }
}
