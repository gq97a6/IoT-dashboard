package com.netDashboard.foreground_service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.app.NotificationCompat.VISIBILITY_SECRET
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.netDashboard.R
import com.netDashboard.createNotification
import com.netDashboard.createVibration
import java.io.Serializable

class ForegroundService : Serializable, LifecycleService() {

    private var isRunning = false
    lateinit var daemonGroupCollection: DaemonGroupCollection

    val alarm = Alarm(this)

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val notification = NotificationCompat.Builder(this, "foreground_service_id")
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentTitle("Server working in background")
            //.setContentText("Running servers: MQTT")
            .setSmallIcon(R.drawable.icon_main)
            .setPriority(PRIORITY_MIN)
            .setVisibility(VISIBILITY_SECRET)
            .setSilent(true)

        startForeground(1, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (!isRunning) {
            daemonGroupCollection = DaemonGroupCollection(this, filesDir.canonicalPath)

            for (g in daemonGroupCollection.daemonsGroups) {
                g.mqttd?.data?.observe(this, { p ->
                    if (p.first != null && p.second != null) {
                        alarm.on(2000)
                        createNotification(this, "${g.name}: ${p.first}", p.second.toString(), true)
                    }
                })
            }

            isRunning = true
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {

        if (isRunning) isRunning = false

        super.onDestroy()
    }

    private val binder = ForegroundServiceBinder()

    inner class ForegroundServiceBinder : Binder() {
        fun getService(): ForegroundService = this@ForegroundService
    }

    override fun onBind(intent: Intent): IBinder {

        super.onBind(intent)

        return binder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "foreground_service_id",
            "Server service notification",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "com/netDashboard/notification_channel"
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun kill() {
        if (isRunning) daemonGroupCollection.stop()
    }

    fun rerun() {
        if (isRunning) daemonGroupCollection.rerun()
    }

    fun rerun(name: String) {
        if (isRunning) daemonGroupCollection.rerun(name)
    }

    inner class Alarm(val context: Context) {
        private var isOn = false

        private fun on() {
            if (isOn) return
            isOn = true
            loop()
        }

        private fun off() {
            isOn = false
        }

        private fun loop() {
            if (!isOn) return

            Thread {
                createVibration(context, 250)
                ToneGenerator(AudioManager.STREAM_ALARM, 40).startTone(37, 500)
            }.start()

            Handler(Looper.getMainLooper()).postDelayed({
                loop()
            }, 500)
        }

        fun on(t: Long) {
            alarm.on()
            Handler(Looper.getMainLooper()).postDelayed({
                alarm.off()
            }, t)
        }
    }
}

class ForegroundServiceHandler(var context: Context) {

    var isBounded: Boolean = false
    var service: MutableLiveData<ForegroundService?> = MutableLiveData(null)

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, IBinder: IBinder) {
            val binder = IBinder as ForegroundService.ForegroundServiceBinder
            service.postValue(binder.getService())
            isBounded = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBounded = false
        }
    }

    fun start() {
        Intent(context, ForegroundService::class.java).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(it)
            } else {
                context.startService(it)
            }
        }
    }

    fun stop() {
        Intent(context, ForegroundService::class.java).also {
            context.stopService(it)
        }
    }

    fun bind() {
        Intent(context, ForegroundService::class.java).also {
            context.bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbind() {
        if (isBounded) {
            context.unbindService(connection)
            isBounded = false
        }
    }
}