package com.zerostudio.lite

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private val TAG_DASHBOARD = "tag_dashboard"
    private val TAG_TOOLS = "tag_tools"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)

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
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fm = supportFragmentManager
            val dashboard = fm.findFragmentByTag(TAG_DASHBOARD)
            val tools = fm.findFragmentByTag(TAG_TOOLS)

            val tx = fm.beginTransaction()
            when (item.itemId) {
                R.id.nav_home -> {
                    if (tools != null) tx.hide(tools)
                    if (dashboard != null) tx.show(dashboard)
                }
                R.id.nav_tools -> {
                    if (dashboard != null) tx.hide(dashboard)
                    if (tools != null) tx.show(tools)
                }
            }
            tx.commit()
            true
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
