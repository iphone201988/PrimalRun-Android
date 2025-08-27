package com.primal.runs.ui.base.location
import android.location.Location
import com.google.android.gms.location.LocationRequest

interface LocationResultListener {
    fun getLocation(location: Location)
    fun enableLocation(locationRequest: LocationRequest)
}