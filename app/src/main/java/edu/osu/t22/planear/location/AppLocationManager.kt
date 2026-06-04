package edu.osu.t22.planear.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper.getMainLooper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppLocationManager(
    private val context: Context,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow: StateFlow<Location?> = _locationFlow

    var lastKnownLocation: Location?
        get() = _locationFlow.value
        private set(value) { _locationFlow.value = value }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        2000L
    ).apply {
        setMinUpdateIntervalMillis(1000L)
        setWaitForAccurateLocation(false)
    }.build()

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (locationCallback != null) return

        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fine) {
            Log.w("LocationManager", "Location permission denied. Using fallback location.")
            lastKnownLocation = Location("default").apply {
                latitude = 37.6213
                longitude = -122.3790
                altitude = 0.0
            }
            return
        }

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