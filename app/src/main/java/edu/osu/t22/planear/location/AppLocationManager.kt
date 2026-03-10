package edu.osu.t22.planear.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Looper.getMainLooper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class AppLocationManager(
    private val context: Context,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    @Volatile
    var lastKnownLocation: Location? = null
        private set

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        2000L
    ).apply {
        setMinUpdateIntervalMillis(1000L)
        setWaitForAccurateLocation(false)
    }.build()

    private var locationCallback: LocationCallback? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun start() {
        if (locationCallback != null) return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    lastKnownLocation = it
                    Log.d("LocationManager", "Location update: ${it.latitude}, ${it.longitude}")
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            getMainLooper()
        )
    }

    fun stop() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }
}