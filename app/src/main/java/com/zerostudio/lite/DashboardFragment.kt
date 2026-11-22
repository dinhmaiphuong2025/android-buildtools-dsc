package com.zerostudio.lite

import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class DashboardFragment : Fragment() {

    private lateinit var tvLogs: TextView
    private lateinit var scrollLogs: ScrollView
    private lateinit var viewDaemonDot: View
    private lateinit var tvDaemonStatus: TextView
    private lateinit var tvDaemonEndpoint: TextView
    private lateinit var btnClearLog: MaterialButton
    private lateinit var tvDeviceInfo: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvLogs = view.findViewById(R.id.tvLogs)
        scrollLogs = view.findViewById(R.id.scrollLogs)
        viewDaemonDot = view.findViewById(R.id.viewDaemonDot)
        tvDaemonStatus = view.findViewById(R.id.tvDaemonStatus)
        tvDaemonEndpoint = view.findViewById(R.id.tvDaemonEndpoint)
        btnClearLog = view.findViewById(R.id.btnClearLog)
        tvDeviceInfo = view.findViewById(R.id.tvDeviceInfo)

        btnClearLog.setOnClickListener {
            tvLogs.text = ""
        }

        updateDaemonStatus(true)
        updateSystemInfo()

        viewLifecycleOwner.lifecycleScope.launch {
            ToolRepository.logFlow.collectLatest { msg ->
                tvLogs.append("$msg\n")
                scrollLogs.post { scrollLogs.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateDaemonStatus(true)
        updateSystemInfo()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateDaemonStatus(true)
            updateSystemInfo()
        }
    }

    private fun updateSystemInfo() {
        val ctx = context ?: return
        val sb = StringBuilder()

        // Device info
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("CPU: ${Build.SUPPORTED_ABIS.joinToString(", ")}")

        // RAM info
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem / (1024 * 1024)
        val availRam = memInfo.availMem / (1024 * 1024)
        sb.appendLine("RAM: ${availRam} MB free / ${totalRam} MB total")

        // Internal storage
        try {
            val stat = StatFs(ctx.filesDir.absolutePath)
            val availBytes = stat.availableBlocksLong * stat.blockSizeLong
            val totalBytes = stat.totalBlocksLong * stat.blockSizeLong
            val availGB = String.format("%.1f", availBytes / (1024.0 * 1024.0 * 1024.0))
            val totalGB = String.format("%.1f", totalBytes / (1024.0 * 1024.0 * 1024.0))
            sb.appendLine("Storage: ${availGB} GB free / ${totalGB} GB total")
        } catch (_: Exception) {}

        // Tools installed count
        val installedCount = ToolRepository.tools.count { it.isInstalled(ctx.filesDir) }
        val totalCount = ToolRepository.tools.size
        sb.append("Tools: $installedCount / $totalCount installed")

        tvDeviceInfo.text = sb.toString()
    }

    fun updateDaemonStatus(active: Boolean) {
        if (!::viewDaemonDot.isInitialized) return
        if (active) {
            viewDaemonDot.setBackgroundResource(R.drawable.dot_active)
            tvDaemonStatus.text = "Active"
            tvDaemonStatus.setTextColor(Color.parseColor("#2E7D32"))
            tvDaemonEndpoint.text = "127.0.0.1:8088"
        } else {
            viewDaemonDot.setBackgroundResource(R.drawable.dot_inactive)
            tvDaemonStatus.text = "Inactive"
            tvDaemonStatus.setTextColor(Color.parseColor("#F44336"))
            tvDaemonEndpoint.text = "Offline"
        }
    }
}
