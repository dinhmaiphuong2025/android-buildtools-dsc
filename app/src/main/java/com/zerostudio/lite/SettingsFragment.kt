package com.zerostudio.lite

import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsFragment : Fragment() {

    private lateinit var etDaemonPort: TextInputEditText
    private lateinit var etCacheDir: TextInputEditText
    private lateinit var tvToolsStorage: TextView
    private lateinit var tvCacheStorage: TextView
    private lateinit var btnClearCache: MaterialButton
    private lateinit var tvAppVersion: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etDaemonPort = view.findViewById(R.id.etDaemonPort)
        etCacheDir = view.findViewById(R.id.etCacheDir)
        tvToolsStorage = view.findViewById(R.id.tvToolsStorage)
        tvCacheStorage = view.findViewById(R.id.tvCacheStorage)
        btnClearCache = view.findViewById(R.id.btnClearCache)
        tvAppVersion = view.findViewById(R.id.tvAppVersion)

        etDaemonPort.setText(BuilderDaemonService.PORT.toString())

        val cacheDir = getCacheDirectory()
        etCacheDir.setText(cacheDir?.absolutePath ?: "N/A")

        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            tvAppVersion.text = "Android BuildTools DSC v${pInfo.versionName}"
        } catch (_: Exception) {}

        btnClearCache.setOnClickListener {
            clearDownloadCache()
        }

        calculateStorageUsage()
    }

    override fun onResume() {
        super.onResume()
        calculateStorageUsage()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) calculateStorageUsage()
    }

    private fun getCacheDirectory(): File? {
        val candidates = listOf(
            File("/storage/emulated/0/Download/ZeroStudio/archives"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ZeroStudio/archives"),
            requireContext().getExternalFilesDir("archives")
        )
        return candidates.firstOrNull { it != null && it.exists() }
    }

    private fun calculateStorageUsage() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val filesDir = requireContext().filesDir
            val toolsSize = getDirSize(filesDir)
            val cacheDir = getCacheDirectory()
            val cacheSize = if (cacheDir != null) getDirSize(cacheDir) else 0L

            withContext(Dispatchers.Main) {
                if (isAdded) {
                    tvToolsStorage.text = "Tools storage: ${formatSize(toolsSize)}"
                    tvCacheStorage.text = "Download cache: ${formatSize(cacheSize)}"
                }
            }
        }
    }

    private fun clearDownloadCache() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val cacheDir = getCacheDirectory()
            var cleared = 0L
            if (cacheDir != null && cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    val size = file.length()
                    if (file.delete()) cleared += size
                }
            }
            // Also clear app cache
            val appCache = requireContext().cacheDir
            appCache.listFiles()?.forEach { file ->
                if (file.name.startsWith("pkg_")) {
                    val size = file.length()
                    if (file.delete()) cleared += size
                }
            }

            withContext(Dispatchers.Main) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Cleared ${formatSize(cleared)}", Toast.LENGTH_SHORT).show()
                    calculateStorageUsage()
                }
            }
        }
    }

    private fun getDirSize(dir: File): Long {
        if (!dir.exists()) return 0
        var size = 0L
        val files = dir.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) getDirSize(f) else f.length()
        }
        return size
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
