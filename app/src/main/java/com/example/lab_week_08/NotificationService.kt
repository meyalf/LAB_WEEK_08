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

class NotificationService : Service() {

    //In order to make the required notification, a service is required
    //to do the job for us in the foreground process

    //Create the notification builder that'll be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder

    //Create a system handler which controls what thread the process is being executed on
    private lateinit var serviceHandler: Handler

    //This is used to bind a two-way communication
    //In this tutorial, we will only be using a one-way communication
    //therefore, the return can be set to null
    override fun onBind(intent: Intent): IBinder? = null

    //this is a callback and part of the life cycle
    //the onCreate callback will be called when this service
    //is created for the first time
    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationService", "onCreate called")

        //Create the notification with all of its contents and configurations
        //in the startForegroundService() custom function
        notificationBuilder = startForegroundService()

        //Create the handler to control which thread the
        //notification will be executed on.
        val handlerThread = HandlerThread("SecondThread")
            .apply { start() }
        serviceHandler = Handler(handlerThread.looper)

        Log.d("NotificationService", "onCreate completed")
    }

    //Create the notification with all of its contents and configurations all set up
    private fun startForegroundService(): NotificationCompat.Builder {
        Log.d("NotificationService", "startForegroundService called")

        //Create a pending Intent which is used to be executed
        //when the user clicks the notification
        val pendingIntent = getPendingIntent()

        //To make a notification, you should know the keyword 'channel'
        //Notification uses channels that'll be used to
        //set up the required configurations
        val channelId = createNotificationChannel()

        //Combine both the pending Intent and the channel
        //into a notification builder
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )

        //After all has been set and the notification builder is ready,
        //start the foreground service and the notification
        //will appear on the user's device
        try {
            startForeground(NOTIFICATION_ID, notificationBuilder.build())
            Log.d("NotificationService", "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e("NotificationService", "Error starting foreground service", e)
        }

        return notificationBuilder
    }

    //A pending Intent is the Intent used to be executed
    //when the user clicks the notification
    private fun getPendingIntent(): PendingIntent {
        //In order to create a pending Intent, a Flag is needed
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        //Here, we're setting MainActivity into the pending Intent
        //When the user clicks the notification, they will be
        //redirected to the Main Activity of the app
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    //To make a notification, a channel is required to
    //set up the required configurations
    private fun createNotificationChannel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create the channel id
            val channelId = "001"
            //Create the channel name
            val channelName = "001 Channel"
            //Create the channel priority
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            //Build the channel notification based on all 3 previous attributes
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )

            //Get the NotificationManager class
            val service = requireNotNull(
                ContextCompat.getSystemService(
                    this,
                    NotificationManager::class.java
                )
            )

            //Binds the channel into the NotificationManager
            service.createNotificationChannel(channel)

            Log.d("NotificationService", "Notification channel created: $channelId")

            //Return the channel id
            channelId
        } else {
            ""
        }

    //Build the notification with all of its contents and configurations
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            //Sets the title
            .setContentTitle("Second worker process is done")
            //Sets the content
            .setContentText("Check it out!")
            //Sets the notification icon
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            //Sets the action/intent to be executed when the user clicks the notification
            .setContentIntent(pendingIntent)
            //Sets the ticker message
            .setTicker("Second worker process is done, check it out!")
            //setOnGoing() controls whether the notification is dismissible or not
            .setOngoing(true)

    //This is a callback and part of a life cycle
    //This callback will be called when the service is started
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NotificationService", "onStartCommand called")
        val returnValue = super.onStartCommand(intent, flags, startId)

        //Gets the channel id passed from the MainActivity through the Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        Log.d("NotificationService", "Received channel ID: $Id")

        //Posts the notification task to the handler,
        //which will be executed on a different thread
        serviceHandler.post {
            Log.d("NotificationService", "Starting countdown")
            //Sets up what happens after the notification is posted
            //Here, we're counting down from 10 to 0 in the notification
            countDownFromTenToZero(notificationBuilder)

            Log.d("NotificationService", "Countdown completed")

            //Here we're notifying the MainActivity that the service process is done
            //by returning the channel ID through LiveData
            notifyCompletion(Id)

            //Stops the foreground service, which closes the notification
            stopForeground(STOP_FOREGROUND_REMOVE)

            //Stop and destroy the service
            stopSelf()

            Log.d("NotificationService", "Service stopped")
        }

        return returnValue
    }

    //A function to update the notification to display a count down from 10 to 0
    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        //Gets the notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        //Count down from 10 to 0
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            //Updates the notification content text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)
            //Notify the notification manager about the content update
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
            Log.d("NotificationService", "Countdown: $i")
        }
    }

    //Update the LiveData with the returned channel id through the Main Thread
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
            Log.d("NotificationService", "Completion notified for ID: $Id")
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        //this is a LiveData which is a data holder that automatically
        //updates the UI based on what is observed
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}