package com.zerostudio.lite

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.tukaani.xz.BasicArrayCache
import org.tukaani.xz.XZInputStream
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

object InstallerEngine {

    suspend fun installTool(
        context: Context,
        tool: ToolItem,
        onProgress: (Int) -> Unit,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val filesDir = context.filesDir
        val targetDir = File(filesDir, tool.targetDirName)
        targetDir.mkdirs()

        onLog(">> Preparing tool: ${tool.title}")
        onLog(">> Target location: ${targetDir.absolutePath}")

        if (tool.archiveType == ArchiveType.ASSET_COPY) {
            return@withContext try {
                val destFile = File(filesDir, tool.verifyRelativePath)
                destFile.parentFile?.mkdirs()
                context.assets.open(tool.downloadUrl).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.setExecutable(true, false)
                onLog(">> Successfully extracted ${tool.title}!")
                true
            } catch (e: Exception) {
                onLog("!! Asset extraction failed: ${e.message}")
                false
            }
        }

        val ext = when (tool.archiveType) {
            ArchiveType.ZIP -> ".zip"
            ArchiveType.TAR_GZ -> ".tar.gz"
            ArchiveType.TAR_XZ -> ".tar.xz"
            else -> ".bin"
        }
        val cacheDir = getSharedCacheDir(context)
        val cachedArchive = if (cacheDir != null) File(cacheDir, "${tool.id}$ext") else null

        val tempFile = File(context.cacheDir, "pkg_${tool.id}_${System.currentTimeMillis()}")
        var archiveToExtract: File? = null

        if (cachedArchive != null && cachedArchive.exists() && cachedArchive.length() > 1024 * 1024) {
            onLog(">> Tìm thấy bản cache cục bộ: ${cachedArchive.name} (${cachedArchive.length() / (1024 * 1024)} MB)")
            onLog(">> Bỏ qua tải từ mạng, tiến hành giải nén...")
            archiveToExtract = cachedArchive
        } else {
            try {
                onLog(">> Connecting to download source: ${tool.downloadUrl}")
                val url = URL(tool.downloadUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 20000
                conn.readTimeout = 30000
                conn.connect()

                if (conn.responseCode !in 200..299) {
                    onLog("!! HTTP Error: ${conn.responseCode} - ${conn.responseMessage}")
                    return@withContext false
                }

                val totalLength = conn.contentLength.toLong()
                var downloaded = 0L

                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var read: Int
                        var lastPercent = 0
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalLength > 0) {
                                val percent = ((downloaded * 100) / totalLength).toInt()
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(percent)
                                }
                            }
                        }
                    }
                }
                onLog(">> Download complete (${downloaded / (1024 * 1024)} MB).")

                if (cachedArchive != null) {
                    try {
                        tempFile.copyTo(cachedArchive, overwrite = true)
                        onLog(">> Đã lưu bản sao cache vào: ${cachedArchive.absolutePath}")
                    } catch (e: Exception) {
                        onLog("!! Không thể lưu cache SDCard: ${e.message}")
                    }
                }

                archiveToExtract = if (cachedArchive != null && cachedArchive.exists() && cachedArchive.length() == tempFile.length()) {
                    tempFile.delete()
                    cachedArchive
                } else {
                    tempFile
                }

            } catch (e: Exception) {
                onLog("!! Installation error: ${e.message}")
                tempFile.delete()
                return@withContext false
            }
        }

        try {
            onLog(">> Extracting archive...")
            val extractSuccess = when (tool.archiveType) {
                ArchiveType.ZIP -> {
                    val strip = if (tool.id == "ndk_29") 1 else 0
                    extractZip(archiveToExtract!!, targetDir, strip, onLog)
                }
                ArchiveType.TAR_GZ -> extractTarGz(archiveToExtract!!, targetDir, stripPaths = 1, onLog)
                ArchiveType.TAR_XZ -> {
                    val strip = if (tool.id == "build_tools_36") 2 else 0
                    extractTarXz(archiveToExtract!!, targetDir, stripPaths = strip, onLog)
                }
                else -> true
            }

            if (archiveToExtract != cachedArchive) {
                archiveToExtract?.delete()
            }

            if (extractSuccess) {
                makeExecutables(targetDir)
                onLog(">> Installation finished: ${tool.title}!")
                return@withContext true
            } else {
                onLog("!! Extraction failed for ${tool.title}")
                return@withContext false
            }

        } catch (e: Exception) {
            onLog("!! Extraction error: ${e.message}")
            if (archiveToExtract != cachedArchive) {
                archiveToExtract?.delete()
            }
            return@withContext false
        }
    }

    private fun getSharedCacheDir(context: Context): File? {
        val candidates = listOf(
            File("/storage/emulated/0/Download/ZeroStudio/archives"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ZeroStudio/archives"),
            context.getExternalFilesDir("archives")
        )
        for (dir in candidates) {
            if (dir != null && (dir.exists() || dir.mkdirs())) {
                return dir
            }
        }
        return null
    }

    private fun extractZip(zipFile: File, targetDir: File, stripPaths: Int, onLog: (String) -> Unit): Boolean {
        return try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val relativeName = stripPathSegments(entry.name, stripPaths)
                    if (relativeName.isNotEmpty()) {
                        val newFile = File(targetDir, relativeName)
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            if (newFile.name.endsWith(".sh") || newFile.parentFile?.name == "bin" || newFile.name in listOf("clang", "clang++", "make", "ndk-build", "ndk-which", "ndk-gdb")) {
                                newFile.setExecutable(true, false)
                                try {
                                    Runtime.getRuntime().exec(arrayOf("chmod", "+x", newFile.absolutePath))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            onLog("!! ZIP extraction error: ${e.message}")
            false
        }
    }

    private fun extractTarGz(tarFile: File, targetDir: File, stripPaths: Int, onLog: (String) -> Unit): Boolean {
        return try {
            FileInputStream(tarFile).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    GzipCompressorInputStream(bis).use { gzis ->
                        TarArchiveInputStream(gzis).use { tarIn ->
                            extractTarEntries(tarIn, targetDir, stripPaths)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            onLog("!! TAR.GZ extraction error: ${e.message}")
            false
        }
    }

    private fun extractTarXz(tarFile: File, targetDir: File, stripPaths: Int, onLog: (String) -> Unit): Boolean {
        return try {
            FileInputStream(tarFile).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    XZInputStream(bis, BasicArrayCache.getInstance()).use { xzis ->
                        TarArchiveInputStream(xzis).use { tarIn ->
                            extractTarEntries(tarIn, targetDir, stripPaths)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            onLog("!! TAR.XZ extraction error: ${e.message}")
            false
        }
    }

    private fun extractTarEntries(tarIn: TarArchiveInputStream, targetDir: File, stripPaths: Int) {
        var entry = tarIn.nextTarEntry
        val buffer = ByteArray(32768)
        while (entry != null) {
            val relativeName = stripPathSegments(entry.name, stripPaths)
            if (relativeName.isNotEmpty()) {
                val destFile = File(targetDir, relativeName)
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else if (entry.isSymbolicLink) {
                    destFile.parentFile?.mkdirs()
                    try {
                        destFile.delete()
                        android.system.Os.symlink(entry.linkName, destFile.absolutePath)
                    } catch (_: Exception) {}
                } else {
                    destFile.parentFile?.mkdirs()
                    FileOutputStream(destFile).use { fos ->
                        var len: Int
                        while (tarIn.read(buffer).also { len = it } != -1) {
                            fos.write(buffer, 0, len)
                        }
                    }
                    if ((entry.mode and 0b001_000_000) != 0 ||
                        entry.name.endsWith(".sh") ||
                        destFile.parentFile?.name == "bin" ||
                        destFile.name in listOf("java", "javac", "aapt2", "aapt", "zipalign", "aidl", "d8", "clang", "cmake")) {
                        destFile.setExecutable(true, false)
                        try {
                            Runtime.getRuntime().exec(arrayOf("chmod", "+x", destFile.absolutePath))
                        } catch (_: Exception) {}
                    }
                }
            }
            entry = tarIn.nextTarEntry
        }
    }

    private fun stripPathSegments(path: String, stripCount: Int): String {
        if (stripCount <= 0) return path
        val segments = path.split("/").filter { it.isNotEmpty() }
        if (segments.size <= stripCount) return ""
        return segments.drop(stripCount).joinToString("/")
    }

    private fun makeExecutables(dir: File) {
        try {
            val binDir = File(dir, "bin")
            if (binDir.exists() && binDir.isDirectory) {
                Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", binDir.absolutePath)).waitFor()
            }
            dir.listFiles { f -> f.isDirectory }?.forEach { sub ->
                val nestedBin = File(sub, "bin")
                if (nestedBin.exists() && nestedBin.isDirectory) {
                    Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", nestedBin.absolutePath)).waitFor()
                }
            }
        } catch (_: Exception) {}

        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                makeExecutables(f)
            } else if (f.name.endsWith(".sh") || f.parentFile?.name == "bin" || f.name in listOf("aapt2", "aapt", "zipalign", "aidl", "d8", "java", "javac", "clang", "cmake")) {
                f.setExecutable(true, false)
                try {
                    Runtime.getRuntime().exec(arrayOf("chmod", "+x", f.absolutePath))
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteTool(baseFilesDir: File, tool: ToolItem): Boolean {
        val target = File(baseFilesDir, tool.targetDirName)
        return target.deleteRecursively()
    }
}
