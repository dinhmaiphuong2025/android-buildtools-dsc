package com.zerostudio.lite

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        val tempFile = File(context.cacheDir, "pkg_${tool.id}_${System.currentTimeMillis()}")
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
            onLog(">> Download complete (${downloaded / (1024 * 1024)} MB). Extracting archive...")

            // Extract archive
            val extractSuccess = when (tool.archiveType) {
                ArchiveType.ZIP -> extractZip(tempFile, targetDir, onLog)
                ArchiveType.TAR_GZ -> extractTar(tempFile, targetDir, "--strip-components=1", onLog)
                ArchiveType.TAR_XZ -> extractTar(tempFile, targetDir, "", onLog)
                else -> true
            }

            tempFile.delete()

            if (extractSuccess) {
                makeExecutables(targetDir)
                onLog(">> Installation finished: ${tool.title}!")
                return@withContext true
            } else {
                onLog("!! Extraction failed for ${tool.title}")
                return@withContext false
            }

        } catch (e: Exception) {
            onLog("!! Installation error: ${e.message}")
            tempFile.delete()
            return@withContext false
        }
    }

    private fun extractZip(zipFile: File, targetDir: File, onLog: (String) -> Unit): Boolean {
        return try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
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

    private fun extractTar(tarFile: File, targetDir: File, extraArg: String, onLog: (String) -> Unit): Boolean {
        return try {
            val cmdList = mutableListOf("tar", "-xf", tarFile.absolutePath)
            if (extraArg.isNotEmpty()) {
                cmdList.add(extraArg)
            }
            cmdList.add("-C")
            cmdList.add(targetDir.absolutePath)

            val pb = ProcessBuilder(cmdList)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // Read lines to prevent stream block
            }
            val code = proc.waitFor()
            code == 0
        } catch (e: Exception) {
            onLog("!! Tar command error: ${e.message}")
            false
        }
    }

    private fun makeExecutables(dir: File) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                makeExecutables(f)
            } else if (f.name.endsWith(".sh") || f.name in listOf("aapt2", "aapt", "zipalign", "aidl", "d8", "java", "javac", "clang", "cmake")) {
                f.setExecutable(true, false)
            }
        }
    }

    fun deleteTool(baseFilesDir: File, tool: ToolItem): Boolean {
        val target = File(baseFilesDir, tool.targetDirName)
        return target.deleteRecursively()
    }
}
