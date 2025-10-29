package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    //Create the notification builder that'll be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder

    //Create a system handler which controls what thread the process is being executed on
    private lateinit var serviceHandler: Handler

    //This is used to bind a two-way communication
    override fun onBind(intent: Intent): IBinder? = null

    //this is a callback and part of the life cycle
    override fun onCreate() {
        super.onCreate()
        Log.d("SecondNotifService", "onCreate called")

        //Create the notification with all of its contents and configurations
        notificationBuilder = startForegroundService()

        //Create the handler to control which thread the notification will be executed on
        val handlerThread = HandlerThread("ThirdThread")
            .apply { start() }
        serviceHandler = Handler(handlerThread.looper)

        Log.d("SecondNotifService", "onCreate completed")
    }

    //Create the notification with all of its contents and configurations all set up
    private fun startForegroundService(): NotificationCompat.Builder {
        Log.d("SecondNotifService", "startForegroundService called")

        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val notificationBuilder = getNotificationBuilder(pendingIntent, channelId)

        try {
            startForeground(NOTIFICATION_ID, notificationBuilder.build())
            Log.d("SecondNotifService", "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e("SecondNotifService", "Error starting foreground service", e)
        }

        return notificationBuilder
    }

    //A pending Intent is the Intent used to be executed when the user clicks the notification
    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    //To make a notification, a channel is required to set up the required configurations
    private fun createNotificationChannel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "002"
            val channelName = "002 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )

            val service = requireNotNull(
                ContextCompat.getSystemService(
                    this,
                    NotificationManager::class.java
                )
            )

            service.createNotificationChannel(channel)
            Log.d("SecondNotifService", "Notification channel created: $channelId")

            channelId
        } else {
            ""
        }

    //Build the notification with all of its contents and configurations
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Third worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Third worker process is done, check it out!")
            .setOngoing(true)

    //This callback will be called when the service is started
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SecondNotifService", "onStartCommand called")
        val returnValue = super.onStartCommand(intent, flags, startId)

        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        Log.d("SecondNotifService", "Received channel ID: $Id")

        serviceHandler.post {
            Log.d("SecondNotifService", "Starting countdown")

            //Count down from 5 to 0 (different from first service)
            countDownFromFiveToZero(notificationBuilder)

            Log.d("SecondNotifService", "Countdown completed")

            notifyCompletion(Id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

            Log.d("SecondNotifService", "Service stopped")
        }

        return returnValue
    }

    //A function to update the notification to display a count down from 5 to 0
    private fun countDownFromFiveToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        //Count down from 5 to 0
        for (i in 5 downTo 0) {
            Thread.sleep(1000L)
            notificationBuilder.setContentText("$i seconds until final warning")
                .setSilent(true)
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
            Log.d("SecondNotifService", "Countdown: $i")
        }
    }

    //Update the LiveData with the returned channel id through the Main Thread
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
            Log.d("SecondNotifService", "Completion notified for ID: $Id")
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA8
        const val EXTRA_ID = "Id"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}