package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    //Create an instance of a work manager
    //Work manager manages all your requests and workers
    //it also sets up the sequence for all your processes
    private val workManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        //Create a constraint of which your workers are bound to.
        //Here the workers cannot execute the given process if
        //there's no internet connection
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        //There are two types of work request:
        //OneTimeWorkRequest and PeriodicWorkRequest
        //OneTimeWorkRequest executes the request just once
        //PeriodicWorkRequest executed the request periodically

        //Create work requests for all three workers
        val firstRequest = OneTimeWorkRequest
            .Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        val secondRequest = OneTimeWorkRequest
            .Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        val thirdRequest = OneTimeWorkRequest
            .Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id))
            .build()

        //Sets up the process sequence from the work manager instance
        //Here it starts with FirstWorker, then SecondWorker, then ThirdWorker
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest)
            .enqueue()

        //All that's left to do is getting the output
        //Here, we receive the output and displaying the result as a toast message

        //Observer for FirstWorker
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("First process is done")
                }
            }

        //Observer for SecondWorker
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Second process is done")
                    Log.d("MainActivity", "About to launch notification service")
                    launchNotificationService()
                    Log.d("MainActivity", "Notification service launch called")
                }
            }

        //Observer for ThirdWorker
        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Third process is done")
                    Log.d("MainActivity", "About to launch second notification service")
                    launchSecondNotificationService()
                    Log.d("MainActivity", "Second notification service launch called")
                }
            }
    }

    //Build the data into the correct format before passing it to the worker as input
    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    //Show the result as toast
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //Launch the NotificationService
    private fun launchNotificationService() {
        Log.d("MainActivity", "launchNotificationService started")

        //Observe if the service process is done or not
        NotificationService.trackingCompletion.observe(this) { Id ->
            Log.d("MainActivity", "Received completion for ID: $Id")
            showResult("Process for Notification Channel ID $Id is done!")
        }

        //Create an Intent to start the NotificationService
        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(NotificationService.EXTRA_ID, "001")
        }

        Log.d("MainActivity", "Starting foreground service")
        //Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
        Log.d("MainActivity", "Foreground service started")
    }

    //Launch the SecondNotificationService
    private fun launchSecondNotificationService() {
        Log.d("MainActivity", "launchSecondNotificationService started")

        //Observe if the service process is done or not
        SecondNotificationService.trackingCompletion.observe(this) { Id ->
            Log.d("MainActivity", "Received completion for ID: $Id")
            showResult("Process for Notification Channel ID $Id is done!")
        }

        //Create an Intent to start the SecondNotificationService
        val serviceIntent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(SecondNotificationService.EXTRA_ID, "002")
        }

        Log.d("MainActivity", "Starting second foreground service")
        //Start the foreground service through the Service Intent
        ContextCompat.startForegroundService(this, serviceIntent)
        Log.d("MainActivity", "Second foreground service started")
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}