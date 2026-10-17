package com.naol.privateapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val background = Color.rgb(7, 12, 25)
    private val panel = Color.rgb(17, 28, 49)
    private val neonBlue = Color.rgb(73, 163, 255)
    private val mainText = Color.rgb(242, 247, 255)

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        renderDashboard()
    }

    private fun askNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
    }

    private fun renderDashboard() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(background)
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(25), dp(20), dp(28))
        }

        scroll.addView(page)
        setContentView(scroll)

        page.addView(
            TextView(this).apply {
                text = "⬡  NAOL PRIVATE"
                textSize = 28f
                setTextColor(neonBlue)
                typeface = Typeface.create(
                    "sans-serif-black",
                    Typeface.BOLD
                )
                gravity = Gravity.CENTER
            },
            lp(bottom = 4)
        )

        page.addView(
            TextView(this).apply {
                text = "PERSONAL SECURITY • DEVICE CARE"
                textSize = 11f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
            },
            lp(bottom = 22)
        )

        statusText = TextView(this).apply {
            text = "● PROTECTION READY"
            textSize = 15f
            setTextColor(Color.rgb(94, 235, 177))
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(15), dp(12), dp(15))
            setBackgroundColor(panel)
        }

        page.addView(statusText, lp(bottom = 20))

        // ANTI-THEFT
        page.addView(section("ANTI-THEFT ALARM"))

        page.addView(
            description(
                "Start protection before leaving your phone unattended. " +
                "Movement and charger disconnection can trigger the alarm " +
                "while the foreground service is active."
            ),
            lp(bottom = 12)
        )

        page.addView(
            button("START ANTI-THEFT PROTECTION") {
                val intent = Intent(
                    this,
                    AntiTheftService::class.java
                ).setAction(AntiTheftService.ACTION_START)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }

                    statusText.text = "● PROTECTION START REQUESTED"

                    Toast.makeText(
                        this,
                        "Protection start requested",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Could not start: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            lp(bottom = 8)
        )

        page.addView(
            button("STOP PROTECTION", secondary = true) {
                stopService(
                    Intent(this, AntiTheftService::class.java)
                )

                statusText.text = "● PROTECTION STOPPED"

                Toast.makeText(
                    this,
                    "Protection stopped",
                    Toast.LENGTH_SHORT
                ).show()
            },
            lp(bottom = 20)
        )

        // HEALTH SCAN
        page.addView(section("PHONE HEALTH SCAN"))

        page.addView(
            button("SCAN BATTERY • STORAGE • MEMORY") {
                showHealthReport()
            },
            lp(bottom = 20)
        )

        // DND
        page.addView(section("SMART DO NOT DISTURB"))

        page.addView(
            description(
                "Android requires explicit Do Not Disturb access. " +
                "Use the button below to open its system settings."
            ),
            lp(bottom = 10)
        )

        page.addView(
            button("OPEN DND ACCESS SETTINGS") {
                try {
                    startActivity(
                        Intent(
                            Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                        )
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Open Do Not Disturb settings manually",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            lp(bottom = 20)
        )

        // INTRUDER PROTECTION
        page.addView(section("INTRUDER PROTECTION"))

        page.addView(
            description(
                "This starter does not secretly take photos or monitor " +
                "system lock-screen failures. Android restricts these " +
                "actions for ordinary apps."
            ),
            lp(bottom = 20)
        )

        page.addView(section("PRIVACY"))

        page.addView(
            description(
                "Basic health information is processed locally. " +
                "This scan does not prove whether installed apps contain malware. " +
                "Alarm reliability depends on Android and phone settings."
            )
        )

        page.addView(
            TextView(this).apply {
                text = "NAOL PRIVATE • ANDROID 15 • VERSION 1.0"
                textSize = 10f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
            },
            lp(top = 28)
        )
    }

    private fun showHealthReport() {
        val batteryManager =
            getSystemService(BATTERY_SERVICE) as BatteryManager

        val batteryPercent = batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        )

        val storage = StatFs(
            Environment.getDataDirectory().path
        )

        val totalGb =
            storage.totalBytes / (1024L * 1024L * 1024L)

        val freeGb =
            storage.availableBytes / (1024L * 1024L * 1024L)

        val runtime = Runtime.getRuntime()

        val appMaxMb =
            runtime.maxMemory() / (1024L * 1024L)

        val appFreeMb =
            runtime.freeMemory() / (1024L * 1024L)

        val report =
            "Battery: ${
                if (batteryPercent >= 0) "$batteryPercent%" else "Unavailable"
            }\n\n" +
            "Device storage: approximately $freeGb GB free of $totalGb GB\n\n" +
            "App memory: approximately $appFreeMb MB free / " +
            "$appMaxMb MB allocated to this app.\n\n" +
            "This scan cannot identify every harmful app."

        AlertDialog.Builder(this)
            .setTitle("Naol Private — Health Scan")
            .setMessage(report)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun section(text: String) =
        TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(neonBlue)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6), 0, dp(10))
        }

    private fun description(text: String) =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.rgb(190, 203, 225))
            setLineSpacing(dp(3).toFloat(), 1f)
        }

    private fun button(
        text: String,
        secondary: Boolean = false,
        action: () -> Unit
    ) = Button(this).apply {
        this.text = text
        textSize = 12f
        isAllCaps = false
        setTextColor(mainText)

        setBackgroundColor(
            if (secondary) {
                Color.rgb(52, 64, 84)
            } else {
                Color.rgb(24, 92, 171)
            }
        )

        setPadding(dp(12), dp(8), dp(12), dp(8))

        setOnClickListener {
            action()
        }
    }

    private fun lp(
        top: Int = 0,
        bottom: Int = 0
    ) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        topMargin = dp(top)
        bottomMargin = dp(bottom)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
