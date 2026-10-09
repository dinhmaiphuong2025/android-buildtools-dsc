package com.zerostudio.lite

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private val dashboardFragment = DashboardFragment()
    private val toolsFragment = ToolsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)

        startDaemonService()
        autoExtractBundledAapt2()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, dashboardFragment)
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> dashboardFragment
                R.id.nav_tools -> toolsFragment
                else -> dashboardFragment
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
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
