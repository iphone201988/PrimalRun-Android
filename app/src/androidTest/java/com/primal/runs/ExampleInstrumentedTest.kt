package com.primal.runs

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.os.postDelayed
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.manipulation.Ordering
import org.objectweb.asm.Handle

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.myapplication", appContext.packageName)
    }


    private lateinit var context: Context
    private lateinit var locationManager: LocationManager
    private val providerName = LocationManager.GPS_PROVIDER

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Add test provider
        locationManager.addTestProvider(
            providerName, false, false, false, false,
            true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_FINE
        )
        locationManager.setTestProviderEnabled(providerName, true)
    }

    @Test
    fun simulateMovementAtOneMeterPerSecond() {
        val durationInSeconds = 600 // Simulate movement for 10 seconds
        /*val startLatitude = 37.4219999
        val startLongitude = -122.0840575*/
        val startLatitude = 30.709082
        val startLongitude = 76.692683

        for (i in 0 until durationInSeconds) {
            val location = Location(providerName).apply {
                latitude = startLatitude + (i * 0.0000089) // ~1m in latitude
                longitude = startLongitude
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                accuracy = 1.0f
                speed = 5.5f // meters/second
            }

            locationManager.setTestProviderLocation(providerName, location)

            Thread.sleep(1000) // Wait 1 second to simulate real time
        }
    }

    @Test
    fun simulatePreciseMovementFor11Meters() {
        val durationInSeconds = 300 // Simulate movement for 10 seconds
        val startLatitude = 37.4219999
        val startLongitude = -122.0840575

        val distanceInMeters = 10
        val meterInDegrees = 0.0000089
        val totalOffset = distanceInMeters * meterInDegrees

        var totalDistance = 0.0


        for (i in 0 until durationInSeconds) {

            totalDistance += distanceInMeters
            Log.d("totalDistance", "simulatePreciseMovementFor11Meters: totalDistance $totalDistance ")
            if(totalDistance >= 100){
                val location = Location(providerName).apply {
                    //latitude = startLatitude + (i * 0.0000089) // ~1m in latitude
                    latitude = startLatitude + (i * totalOffset)
                    longitude = startLongitude
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    accuracy = 1.0f
                    speed = 4.0f // meters/second
                }
                locationManager.setTestProviderLocation(providerName, location)
            }else{
                Log.d("totalDistance", "simulatePreciseMovementFor11Meters: reached at 100 $totalDistance ")
            }

            Thread.sleep(1000) // Wait 1 second to simulate real time
        }
    }

    @Test
    fun simulateMovementAtOneMeterPerSecond1() = runBlocking {
        val providerName = LocationManager.GPS_PROVIDER
        val context = ApplicationProvider.getApplicationContext<Context>()
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationManager.addTestProvider(
            providerName, false, false, false, false,
            true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_FINE
        )
        locationManager.setTestProviderEnabled(providerName, true)

        val durationInSeconds = 600
        val startLatitude = 30.709082
        val startLongitude = 76.692683
        val meterInDegrees = 0.0000089 // ~1m in degrees


        val distanceInMeters = 10
        val totalOffset = distanceInMeters * meterInDegrees

        var totalDistance = 0.0

        // 🚀 Wait for 50 seconds before starting movement
        delay(50_000)

        for (i in 0 until durationInSeconds) {
            totalDistance += distanceInMeters
            Log.d("totalDistance", "simulatePreciseMovementFor11Meters: totalDistance $totalDistance ")
            if(totalDistance <= 100) {
                val location = Location(providerName).apply {
                    latitude = startLatitude + (i * totalOffset)    
                    longitude = startLongitude
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    accuracy = 1.0f
                    speed = 1.9f // Simulate 1 m/s
                }
                locationManager.setTestProviderLocation(providerName, location)
            }
            delay(1000) // ⏱️ Simulate 1 second per movement
        }

        locationManager.removeTestProvider(providerName)
    }


    @After
    fun teardown() {
        locationManager.removeTestProvider(providerName)
    }

}