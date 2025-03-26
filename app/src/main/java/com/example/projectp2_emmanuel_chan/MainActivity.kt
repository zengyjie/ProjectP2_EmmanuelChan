package com.example.projectp2_emmanuel_chan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.projectp2_emmanuel_chan.databinding.ActivityMainBinding
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment.Fridge
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment.Wine
import com.example.projectp2_emmanuel_chan.ui.settings.SettingsFragment.Companion.sendWineNotification
import com.example.projectp2_emmanuel_chan.ui.wines.WinesFragment.Filter
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        var moving = false
        var moveMode = "move"
        var selectedWine = Wine()
        var selectedIndices = mutableListOf(0, 0, 0, 0)
        var fridges = mutableListOf<Fridge>()
        var highlightedWineName: String = "null"
        var selectedFridge: Fridge = Fridge()
        var origSelectedFridge: Fridge = Fridge()
        var filter: Filter = Filter()

        fun getFridge(name: String): Int {
            for ((i, f) in fridges.withIndex()) {
                if (f.name == name) {
                    return i
                }
            }
            return -1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        when (sharedPreferences.getInt("theme", 0)) {
            0 -> setTheme(R.style.Theme_ProjectP2_EmmanuelChan_Default)
            1 -> setTheme(R.style.Theme_ProjectP2_EmmanuelChan_Dark)
        }
        if (sharedPreferences.getBoolean("first_run", true)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        extractAssets()
        sharedPreferences.edit { putBoolean("first_run", false) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "your_channel_id"
            val channelName = "Your Channel Name"
            val descriptionText = "Channel Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        if (sharedPreferences.getBoolean("notifications_enabled", true)) { sendWineNotification(this) }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_fridges, R.id.navigation_wines, R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view != null) {
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun extractAssets() {
        val assetManager = assets
        val files = assetManager.list("wineImages") ?: return

        val internalStorageDir = File(filesDir, "WineWise")
        if (!internalStorageDir.exists()) {
            internalStorageDir.mkdirs()
        }

        val jpgFiles = files.filter { it.endsWith(".jpg", ignoreCase = true) }

        for (fileName in jpgFiles) {
            val outFile = File(internalStorageDir, fileName)
            println("Output file: $outFile")
            if (!outFile.exists()) {
                try {
                    assetManager.open("wineImages/$fileName").use { inputStream ->
                        FileOutputStream(outFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
