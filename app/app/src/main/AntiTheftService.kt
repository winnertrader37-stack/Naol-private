package com.naol.privateapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.abs
import kotlin.math.sqrt

class AntiTheftService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START = "com.naol.privateapp.START"

        private const val CHANNEL_ID = "naol_protection"
        private const val NOTIFICATION_ID = 7001
    }

    private lateinit var sensors: SensorManager

    private var tone: ToneGenerator? = null
    private var receiverRegistered = false
    private var lastAlarmTime = 0L
    private var previousMagnitude = 0f

    private val chargerReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {
            if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
                triggerAlarm("Charger disconnected")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        sensors = getSystemService(SENSOR_SERVICE) as SensorManager

        tone = ToneGenerator(
            AudioManager.STREAM_ALARM,
            100
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_START) {

            startForeground(
                NOTIFICATION_ID,
                notification("Anti-theft protection is active")
            )

            sensors.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )?.let { sensor ->
                sensors.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }

            if (!receiverRegistered) {

                val filter = IntentFilter(
                    Intent.ACTION_POWER_DISCONNECTED
                )

                if (Build.VERSION.SDK_INT >= 33) {
                    registerReceiver(
                        chargerReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                    )
                } else {
                    @Suppress("DEPRECATION")
                    registerReceiver(chargerReceiver, filter)
                }

                receiverRegistered = true
            }
        }

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(
            x * x + y * y + z * z
        )

        val change = abs(
            magnitude - previousMagnitude
        )

        previousMagnitude = magnitude

        if (change > 5.5f) {
            triggerAlarm("Movement detected")
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) = Unit

    private fun triggerAlarm(reason: String) {

        val now = System.currentTimeMillis()

        // Avoid repeating the trigger too frequently.
        if (now - lastAlarmTime < 2500L) {
            return
        }

        lastAlarmTime = now

        try {
            tone?.startTone(
                ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                1000
            )

            val vibrator =
                getSystemService(VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        700,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(700)
            }

        } catch (_: Exception) {
            // Audio or vibration can be restricted by device settings.
        }

        val manager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        manager.notify(
            NOTIFICATION_ID,
            notification(reason)
        )
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Naol Private protection",
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.description =
                "Visible status notification for anti-theft protection"

            (
                getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager
            ).createNotificationChannel(channel)
        }
    }

    private fun notification(message: String): Notification {

        val builder =
            if (Build.VERSION.SDK_INT >= 26) {
                Notification.Builder(this, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
            }

        return builder
            .setContentTitle("Naol Private")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {

        sensors.unregisterListener(this)

        if (receiverRegistered) {
            try {
                unregisterReceiver(chargerReceiver)
            } catch (_: Exception) {
                // Receiver might already be unregistered.
            }
        }

        tone?.stopTone()
        tone?.release()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
