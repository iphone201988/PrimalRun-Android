package com.primal.runs.ui.track_new

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.primal.runs.R
import com.primal.runs.utils.FileLogger

@RequiresApi(Build.VERSION_CODES.O)
class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null

    private var lastLocation: Location? = null
    private var distanceInMeters: Float = 0f
    private val binder = LocalBinder()

    companion object {
        const val CHANNEL_ID = "location_channel"
        const val NOTIFICATION_ID = 101
    }

    interface LocationCallbackListener {
        fun onLocationChanged(location: Location)
        fun onSpeedChanged(speed : Float)
    }

    private var listener: LocationCallbackListener? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        createNotificationChannel()
        initLocationCallback()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L // 1 second interval
        )
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5_000L) // more responsive, lower bounds
            .setMaxUpdateDelayMillis(15_000)
            .setMinUpdateDistanceMeters(3f)
            .build()
    }

    private fun initLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                //Log.d("onLocationResult", "onLocationResult: ${result.locations.size} ")
                result.locations.forEach { location ->
                    //listener?.onLocationChanged(location)
                    location.hasSpeed()


                    //listener?.onSpeedChanged(location.speed)

                    if (location.hasSpeed() && location.accuracy <= 15) {
                        //Log.d("onLocationResult", "onLocationResult: speed ${location.speed} latlng ${location.latitude}, ${location.longitude}  time ${location.time}")
                        val rawSpeed = location.speed
                        val finalSpeed =  getSanitizedSpeed(rawSpeed)
                        listener?.onSpeedChanged(finalSpeed)
                    } else {
                        listener?.onSpeedChanged(0f)
                    }
                }
            }
        }
    }

    private fun getSanitizedSpeed(speed: Float): Float {
        return if (speed < 0.5) 0f else speed
    }

    fun startLocationUpdates(callbackListener: LocationCallbackListener) {
        listener = callbackListener

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        startForeground(NOTIFICATION_ID, createNotification())

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        handler.post(locationRunnable)

    }

    val handler = Handler(Looper.getMainLooper())

    private val locationRunnable = object : Runnable {
        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        override fun run() {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->

                location?.let {
                    val acc = it.accuracy
                    val speed = it.speed
                    val lat = it.latitude
                    val lng = it.longitude

                    Log.d("LocationService", "Raw Location: $lat, $lng  acc=$acc  speed=$speed")

                    // ✅ Filter out inaccurate data
                    /*if (acc > 15) {
                        Log.e("LocationService", "Skipped due to low accuracy ($acc m)")
                        return@addOnSuccessListener
                    }*/

                    // Optional: notify your listener
                    listener?.onLocationChanged(it)
                }


                if (location != null) {
                    //Log.d("LocationService", "Manual Location: ${location.latitude}, ${location.longitude}")


                    if (location.hasSpeed() && location.accuracy <= 20) {

                        Log.d("onLocationResult", "onLocationResult: speed ${location.speed} latlng ${location.latitude}, ${location.longitude}  accuracy ${location.accuracy}")
                    }else{
                        Log.d("onLocationResult", "Null onLocationResult: speed ${location.speed} latlng ${location.latitude}, ${location.longitude}  accuracy ${location.accuracy}")
                    }
                }
            }

            handler.postDelayed(this, 5000) // Repeat every 1 second
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        handler.removeCallbacks(locationRunnable)
        stopForeground(true)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Location")
            .setContentText("Your location is being tracked...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Used for location tracking"
            setSound(null, null)
            enableVibration(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }
}
