
package com.primal.runs.ui.base.location
import android.Manifest
import android.app.Activity
import android.content.Context.LOCATION_SERVICE
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

class LocationHandler(
    private val activity: Activity,
    val locationResultListener: LocationResultListener
) {
    init {
        initLocationVariables()
    }

    private lateinit var locationManager: LocationManager
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private fun initLocationVariables() {
        locationManager = activity.getSystemService(LOCATION_SERVICE) as LocationManager
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
        locationRequest = LocationRequest
            .create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(5000)
            .setFastestInterval(0)
        initLocationCallBack()
    }

    private fun initLocationCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { locationResultListener.getLocation(it) }
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun promptUserToEnableLocation() {

        locationResultListener.enableLocation(locationRequest)

        /*val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        LocationServices
            .getSettingsClient(activity)
            .checkLocationSettings(builder.build())
            .addOnSuccessListener {
                getLastKnownLocation()

            }
            .addOnFailureListener { e ->
                when ((e as ResolvableApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        e.startResolutionForResult(activity, 10)
                    } catch (exception: IntentSender.SendIntentException) {
                        exception.printStackTrace()
                    }
                }
            }*/
    }

    fun getUserLocation() {
        if (!isGooglePlayServicesAvailable(activity)) {
            return
        }

        if (!isLocationEnabled()) {
            promptUserToEnableLocation()
            return
        }
        getLastKnownLocation()
    }

    fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val location = it.result
            if (location == null) {
                Looper.myLooper()?.let { it1 ->
                    fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        it1
                    )
                }
            } else {
                locationResultListener.getLocation(location)
            }
        }
    }

    private fun isGooglePlayServicesAvailable(activity: Activity): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(activity)
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404)?.show()
            }
            return false
        }
        return true
    }

    fun removeLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            .addOnCompleteListener {
                //postValue(LocCallback(LocCallback.Type.STOPPED))
            }
        //
    }




}