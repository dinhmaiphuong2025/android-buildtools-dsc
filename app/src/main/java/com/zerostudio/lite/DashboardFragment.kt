package com.zerostudio.lite

import android.graphics.Color
import android.os.Bundle
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

        btnClearLog.setOnClickListener {
            tvLogs.text = ""
        }

        updateDaemonStatus(true)

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
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateDaemonStatus(true)
        }
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
