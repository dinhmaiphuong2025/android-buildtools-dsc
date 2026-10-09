package com.zerostudio.lite

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var btnNavHome: LinearLayout
    private lateinit var btnNavTools: LinearLayout
    private lateinit var ivNavHome: ImageView
    private lateinit var ivNavTools: ImageView
    private lateinit var tvNavHome: TextView
    private lateinit var tvNavTools: TextView

    private val TAG_DASHBOARD = "tag_dashboard"
    private val TAG_TOOLS = "tag_tools"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnNavHome = findViewById(R.id.btnNavHome)
        btnNavTools = findViewById(R.id.btnNavTools)
        ivNavHome = findViewById(R.id.ivNavHome)
        ivNavTools = findViewById(R.id.ivNavTools)
        tvNavHome = findViewById(R.id.tvNavHome)
        tvNavTools = findViewById(R.id.tvNavTools)

        startDaemonService()
        autoExtractBundledAapt2()

        if (savedInstanceState == null) {
            val dashboard = DashboardFragment()
            val tools = ToolsFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, dashboard, TAG_DASHBOARD)
                .add(R.id.fragmentContainer, tools, TAG_TOOLS)
                .hide(tools)
                .commit()
            updateNavState(isHome = true)
        }

        btnNavHome.setOnClickListener {
            val fm = supportFragmentManager
            val dashboard = fm.findFragmentByTag(TAG_DASHBOARD)
            val tools = fm.findFragmentByTag(TAG_TOOLS)
            val tx = fm.beginTransaction()
            if (tools != null) tx.hide(tools)
            if (dashboard != null) tx.show(dashboard)
            tx.commit()
            updateNavState(isHome = true)
        }

        btnNavTools.setOnClickListener {
            val fm = supportFragmentManager
            val dashboard = fm.findFragmentByTag(TAG_DASHBOARD)
            val tools = fm.findFragmentByTag(TAG_TOOLS)
            val tx = fm.beginTransaction()
            if (dashboard != null) tx.hide(dashboard)
            if (tools != null) tx.show(tools)
            tx.commit()
            updateNavState(isHome = false)
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun updateNavState(isHome: Boolean) {
        val primaryColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val mutedColor = getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)

        if (isHome) {
            btnNavHome.setBackgroundResource(R.drawable.nav_item_active_bg)
            btnNavTools.background = null

            ivNavHome.setColorFilter(primaryColor)
            tvNavHome.setTextColor(primaryColor)
            tvNavHome.setTypeface(null, android.graphics.Typeface.BOLD)

            ivNavTools.setColorFilter(mutedColor)
            tvNavTools.setTextColor(mutedColor)
            tvNavTools.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            btnNavTools.setBackgroundResource(R.drawable.nav_item_active_bg)
            btnNavHome.background = null

            ivNavTools.setColorFilter(primaryColor)
            tvNavTools.setTextColor(primaryColor)
            tvNavTools.setTypeface(null, android.graphics.Typeface.BOLD)

            ivNavHome.setColorFilter(mutedColor)
            tvNavHome.setTextColor(mutedColor)
            tvNavHome.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun startDaemonService() {
        val intent = Intent(this, BuilderDaemonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun autoExtractBundledAapt2() {
        val aapt2 = ToolRepository.tools.firstOrNull { it.id == "aapt2_bundled" }
        if (aapt2 != null && !aapt2.isInstalled(filesDir)) {
            CoroutineScope(Dispatchers.IO).launch {
                InstallerEngine.installTool(
                    context = applicationContext,
                    tool = aapt2,
                    onProgress = {},
                    onLog = { msg ->
                        CoroutineScope(Dispatchers.Main).launch {
                            ToolRepository.emitLog(msg)
                        }
                    }
                )
            }
        }
    }
}
