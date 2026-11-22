package com.zerostudio.lite

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private enum class LogFilter { ALL, ERRORS, TASKS }

    // Daemon & System views
    private lateinit var viewDaemonDot: View
    private lateinit var tvDaemonStatus: TextView
    private lateinit var tvDaemonEndpoint: TextView
    private lateinit var tvDeviceModel: TextView
    private lateinit var tvDeviceRam: TextView
    private lateinit var tvDeviceStorage: TextView
    private lateinit var tvDeviceTools: TextView

    // Live Build Monitor views
    private lateinit var tvBuildStateBadge: TextView
    private lateinit var tvBuildElapsed: TextView
    private lateinit var tvBuildDetails: TextView
    private lateinit var progressBuilding: LinearProgressIndicator
    private lateinit var btnCancelBuild: MaterialButton

    // Terminal views & controls
    private lateinit var tvLogs: TextView
    private lateinit var scrollLogs: ScrollView
    private lateinit var tvLogCount: TextView
    private lateinit var btnPinLog: MaterialButton
    private lateinit var btnSearchLog: MaterialButton
    private lateinit var btnCopyLog: MaterialButton
    private lateinit var btnClearLog: MaterialButton
    private lateinit var layoutSearchContainer: LinearLayout
    private lateinit var etSearchLog: EditText
    private lateinit var btnCloseSearch: MaterialButton
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var chipFilterAll: Chip
    private lateinit var chipFilterErrors: Chip
    private lateinit var chipFilterTasks: Chip

    // State
    private var isPinned: Boolean = true
    private var currentFilter: LogFilter = LogFilter.ALL
    private var searchQuery: String = ""
    private var tickerJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupControls()
        updateDaemonStatus(true)
        updateSystemInfo()
        observeBuildSession()
        observeLogs()
    }

    private fun bindViews(view: View) {
        viewDaemonDot = view.findViewById(R.id.viewDaemonDot)
        tvDaemonStatus = view.findViewById(R.id.tvDaemonStatus)
        tvDaemonEndpoint = view.findViewById(R.id.tvDaemonEndpoint)
        tvDeviceModel = view.findViewById(R.id.tvDeviceModel)
        tvDeviceRam = view.findViewById(R.id.tvDeviceRam)
        tvDeviceStorage = view.findViewById(R.id.tvDeviceStorage)
        tvDeviceTools = view.findViewById(R.id.tvDeviceTools)

        tvBuildStateBadge = view.findViewById(R.id.tvBuildStateBadge)
        tvBuildElapsed = view.findViewById(R.id.tvBuildElapsed)
        tvBuildDetails = view.findViewById(R.id.tvBuildDetails)
        progressBuilding = view.findViewById(R.id.progressBuilding)
        btnCancelBuild = view.findViewById(R.id.btnCancelBuild)

        tvLogs = view.findViewById(R.id.tvLogs)
        scrollLogs = view.findViewById(R.id.scrollLogs)
        tvLogCount = view.findViewById(R.id.tvLogCount)
        btnPinLog = view.findViewById(R.id.btnPinLog)
        btnSearchLog = view.findViewById(R.id.btnSearchLog)
        btnCopyLog = view.findViewById(R.id.btnCopyLog)
        btnClearLog = view.findViewById(R.id.btnClearLog)
        layoutSearchContainer = view.findViewById(R.id.layoutSearchContainer)
        etSearchLog = view.findViewById(R.id.etSearchLog)
        btnCloseSearch = view.findViewById(R.id.btnCloseSearch)
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter)
        chipFilterAll = view.findViewById(R.id.chipFilterAll)
        chipFilterErrors = view.findViewById(R.id.chipFilterErrors)
        chipFilterTasks = view.findViewById(R.id.chipFilterTasks)
    }

    private fun setupControls() {
        updatePinUi()

        // Pin / Auto-follow toggle
        btnPinLog.setOnClickListener {
            isPinned = !isPinned
            updatePinUi()
            if (isPinned) {
                scrollToBottom()
                Toast.makeText(requireContext(), "Auto-follow enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Auto-follow disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Search toggle
        btnSearchLog.setOnClickListener {
            val willShow = layoutSearchContainer.visibility != View.VISIBLE
            layoutSearchContainer.visibility = if (willShow) View.VISIBLE else View.GONE
            if (willShow) {
                etSearchLog.requestFocus()
            } else {
                searchQuery = ""
                etSearchLog.setText("")
                renderLogs()
            }
        }

        btnCloseSearch.setOnClickListener {
            layoutSearchContainer.visibility = View.GONE
            searchQuery = ""
            etSearchLog.setText("")
            renderLogs()
        }

        etSearchLog.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                renderLogs()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Copy log to clipboard
        btnCopyLog.setOnClickListener {
            val text = tvLogs.text.toString()
            if (text.isNotBlank()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("BuildTools Logs", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        // Clear log
        btnClearLog.setOnClickListener {
            ToolRepository.clearLogs()
            tvLogs.text = ""
            tvLogCount.text = "0 lines"
        }

        // Filter chips
        chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipFilterErrors) -> LogFilter.ERRORS
                checkedIds.contains(R.id.chipFilterTasks) -> LogFilter.TASKS
                else -> LogFilter.ALL
            }
            renderLogs()
        }

        // Cancel build button
        btnCancelBuild.setOnClickListener {
            val cancelled = ToolRepository.cancelActiveBuild()
            if (cancelled) {
                Toast.makeText(requireContext(), "Build cancellation requested", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePinUi() {
        if (isPinned) {
            btnPinLog.text = "Pinned"
            btnPinLog.setIconTintResource(R.color.md_theme_light_primary)
            btnPinLog.setTextColor(Color.parseColor("#006495"))
        } else {
            btnPinLog.text = "Pin"
            btnPinLog.setIconTintResource(android.R.color.darker_gray)
            btnPinLog.setTextColor(Color.GRAY)
        }
    }

    private fun observeBuildSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            ToolRepository.buildSession.collectLatest { session ->
                if (session.isBuilding) {
                    tvBuildStateBadge.text = "BUILDING"
                    tvBuildStateBadge.setBackgroundResource(R.drawable.badge_building_bg)
                    tvBuildStateBadge.setTextColor(Color.parseColor("#E65100"))
                    progressBuilding.visibility = View.VISIBLE
                    btnCancelBuild.visibility = View.VISIBLE
                    tvBuildElapsed.visibility = View.VISIBLE
                    tvBuildDetails.text = "Project: ${session.projectPath} [${session.tasks}]"

                    startElapsedTicker(session.startTime)
                } else {
                    stopElapsedTicker()
                    tvBuildStateBadge.text = "IDLE"
                    tvBuildStateBadge.setBackgroundResource(R.drawable.badge_idle_bg)
                    tvBuildStateBadge.setTextColor(Color.parseColor("#2E7D32"))
                    progressBuilding.visibility = View.GONE
                    btnCancelBuild.visibility = View.GONE
                    tvBuildElapsed.visibility = View.GONE

                    val lastStatus = when {
                        session.lastExitCode == null -> "None"
                        session.lastExitCode == 0 -> "Success in ${session.lastDurationMs / 1000}s"
                        else -> "Failed (exit ${session.lastExitCode}) in ${session.lastDurationMs / 1000}s"
                    }
                    tvBuildDetails.text = "Total builds: ${session.totalBuilds} • Last: $lastStatus"
                }
            }
        }
    }

    private fun startElapsedTicker(startTime: Long) {
        tickerJob?.cancel()
        tickerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
                val min = elapsedSec / 60
                val sec = elapsedSec % 60
                tvBuildElapsed.text = String.format("%02d:%02d", min, sec)
                delay(1000)
            }
        }
    }

    private fun stopElapsedTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun observeLogs() {
        viewLifecycleOwner.lifecycleScope.launch {
            ToolRepository.logFlow.collectLatest { line ->
                if (matchesFilter(line)) {
                    tvLogs.append("$line\n")
                    updateLineCount()
                    if (isPinned) {
                        scrollToBottom()
                    }
                }
            }
        }
    }

    private fun renderLogs() {
        val filtered = synchronized(ToolRepository.logHistory) {
            ToolRepository.logHistory.filter { matchesFilter(it) }
        }
        val sb = StringBuilder()
        for (l in filtered) {
            sb.append(l).append("\n")
        }
        tvLogs.text = sb.toString()
        updateLineCount(filtered.size)
        if (isPinned) {
            scrollToBottom()
        }
    }

    private fun matchesFilter(line: String): Boolean {
        // Query check
        if (searchQuery.isNotEmpty() && !line.contains(searchQuery, ignoreCase = true)) {
            return false
        }
        // Filter type check
        return when (currentFilter) {
            LogFilter.ALL -> true
            LogFilter.ERRORS -> line.startsWith("!!") || line.contains("ERROR", ignoreCase = true) || line.contains("FAILED", ignoreCase = true) || line.contains("Exception")
            LogFilter.TASKS -> line.startsWith(">>") || line.contains("Task :") || line.contains("BUILD", ignoreCase = true)
        }
    }

    private fun updateLineCount(count: Int? = null) {
        val total = count ?: synchronized(ToolRepository.logHistory) {
            ToolRepository.logHistory.count { matchesFilter(it) }
        }
        tvLogCount.text = "$total lines"
    }

    private fun scrollToBottom() {
        scrollLogs.post {
            scrollLogs.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onResume() {
        super.onResume()
        updateDaemonStatus(true)
        updateSystemInfo()
        renderLogs()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateDaemonStatus(true)
            updateSystemInfo()
            renderLogs()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopElapsedTicker()
    }

    private fun updateSystemInfo() {
        val ctx = context ?: return

        tvDeviceModel.text = "📱 ${Build.MODEL} (API ${Build.VERSION.SDK_INT})"

        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val availRam = memInfo.availMem / (1024 * 1024)
        tvDeviceRam.text = "⚡ RAM: ${availRam}MB"

        try {
            val stat = StatFs(ctx.filesDir.absolutePath)
            val availBytes = stat.availableBlocksLong * stat.blockSizeLong
            val availGB = String.format("%.1f", availBytes / (1024.0 * 1024.0 * 1024.0))
            tvDeviceStorage.text = "💾 Disk: ${availGB}GB"
        } catch (_: Exception) {
            tvDeviceStorage.text = "💾 Disk: --"
        }

        val installedCount = ToolRepository.tools.count { it.isInstalled(ctx.filesDir) }
        tvDeviceTools.text = "🧰 Tools: $installedCount/${ToolRepository.tools.size}"
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
