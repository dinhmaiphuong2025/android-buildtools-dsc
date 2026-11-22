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
    private lateinit var btnNavSettings: LinearLayout
    private lateinit var ivNavHome: ImageView
    private lateinit var ivNavTools: ImageView
    private lateinit var ivNavSettings: ImageView
    private lateinit var tvNavHome: TextView
    private lateinit var tvNavTools: TextView
    private lateinit var tvNavSettings: TextView

    private val TAG_DASHBOARD = "tag_dashboard"
    private val TAG_TOOLS = "tag_tools"
    private val TAG_SETTINGS = "tag_settings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnNavHome = findViewById(R.id.btnNavHome)
        btnNavTools = findViewById(R.id.btnNavTools)
        btnNavSettings = findViewById(R.id.btnNavSettings)
        ivNavHome = findViewById(R.id.ivNavHome)
        ivNavTools = findViewById(R.id.ivNavTools)
        ivNavSettings = findViewById(R.id.ivNavSettings)
        tvNavHome = findViewById(R.id.tvNavHome)
        tvNavTools = findViewById(R.id.tvNavTools)
        tvNavSettings = findViewById(R.id.tvNavSettings)

        startDaemonService()
        autoExtractBundledAapt2()
        checkStoragePermission()

        if (savedInstanceState == null) {
            val dashboard = DashboardFragment()
            val tools = ToolsFragment()
            val settings = SettingsFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, dashboard, TAG_DASHBOARD)
                .add(R.id.fragmentContainer, tools, TAG_TOOLS)
                .add(R.id.fragmentContainer, settings, TAG_SETTINGS)
                .hide(tools)
                .hide(settings)
                .commit()
            updateNavState("home")
        }

        btnNavHome.setOnClickListener {
            showFragment(TAG_DASHBOARD)
            updateNavState("home")
        }

        btnNavTools.setOnClickListener {
            showFragment(TAG_TOOLS)
            updateNavState("tools")
        }

        btnNavSettings.setOnClickListener {
            showFragment(TAG_SETTINGS)
            updateNavState("settings")
        }
    }

    private fun showFragment(activeTag: String) {
        val fm = supportFragmentManager
        val tx = fm.beginTransaction()
        val tags = listOf(TAG_DASHBOARD, TAG_TOOLS, TAG_SETTINGS)
        for (tag in tags) {
            val frag = fm.findFragmentByTag(tag)
            if (frag != null) {
                if (tag == activeTag) tx.show(frag) else tx.hide(frag)
            }
        }
        tx.commit()
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun updateNavState(active: String) {
        val primaryColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val mutedColor = getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)

        // Reset all tabs
        btnNavHome.background = null
        btnNavTools.background = null
        btnNavSettings.background = null

        ivNavHome.setColorFilter(mutedColor)
        tvNavHome.setTextColor(mutedColor)
        tvNavHome.setTypeface(null, android.graphics.Typeface.NORMAL)

        ivNavTools.setColorFilter(mutedColor)
        tvNavTools.setTextColor(mutedColor)
        tvNavTools.setTypeface(null, android.graphics.Typeface.NORMAL)

        ivNavSettings.setColorFilter(mutedColor)
        tvNavSettings.setTextColor(mutedColor)
        tvNavSettings.setTypeface(null, android.graphics.Typeface.NORMAL)

        // Highlight active tab
        when (active) {
            "home" -> {
                btnNavHome.setBackgroundResource(R.drawable.nav_item_active_bg)
                ivNavHome.setColorFilter(primaryColor)
                tvNavHome.setTextColor(primaryColor)
                tvNavHome.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            "tools" -> {
                btnNavTools.setBackgroundResource(R.drawable.nav_item_active_bg)
                ivNavTools.setColorFilter(primaryColor)
                tvNavTools.setTextColor(primaryColor)
                tvNavTools.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            "settings" -> {
                btnNavSettings.setBackgroundResource(R.drawable.nav_item_active_bg)
                ivNavSettings.setColorFilter(primaryColor)
                tvNavSettings.setTextColor(primaryColor)
                tvNavSettings.setTypeface(null, android.graphics.Typeface.BOLD)
            }
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

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        } else {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1001
            )
        }
    }
}
