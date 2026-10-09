package com.zerostudio.lite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class DashboardFragment : Fragment() {

    private lateinit var tvLogs: TextView
    private lateinit var scrollLogs: ScrollView
    private lateinit var chipDaemonStatus: Chip
    private lateinit var tvToolchainSummary: TextView
    private lateinit var btnClearLog: MaterialButton

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
        chipDaemonStatus = view.findViewById(R.id.chipDaemonStatus)
        tvToolchainSummary = view.findViewById(R.id.tvToolchainSummary)
        btnClearLog = view.findViewById(R.id.btnClearLog)

        btnClearLog.setOnClickListener {
            tvLogs.text = ""
        }

        updateStatusSummary()

        viewLifecycleOwner.lifecycleScope.launch {
            ToolRepository.logFlow.collectLatest { msg ->
                tvLogs.append("$msg\n")
                scrollLogs.post { scrollLogs.fullScroll(ScrollView.FOCUS_DOWN) }
                updateStatusSummary()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatusSummary()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateStatusSummary()
        }
    }

    private fun updateStatusSummary() {
        val context = context ?: return
        val filesDir = context.filesDir
        val javaOk = File(filesDir, "usr/opt/openjdk-17/bin/java").exists()
        val aapt2Ok = File(filesDir, "android-sdk/build-tools/36.0.0/aapt2").exists()
        val sdk35Ok = File(filesDir, "android-sdk/platforms/android-35/android.jar").exists()
        val ndkOk = File(filesDir, "android-sdk/ndk/29.0.14033849").exists()

        val statusList = mutableListOf<String>()
        statusList.add(if (aapt2Ok) "AAPT2 ✓" else "AAPT2 ✗")
        statusList.add(if (javaOk) "JDK17 ✓" else "JDK17 ✗")
        statusList.add(if (sdk35Ok) "SDK35 ✓" else "SDK35 ✗")
        if (ndkOk) statusList.add("NDK r29 ✓")

        tvToolchainSummary.text = "Toolchain Status: " + statusList.joinToString(" | ")
    }
}
