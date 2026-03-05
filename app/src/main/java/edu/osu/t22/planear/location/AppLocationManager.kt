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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AppLocationManager(private val context: Context, private val scope: CoroutineScope) {
    @Volatile var lastKnownLocation: Location? = null
        private set

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        10000L
    ).apply {
        setMinUpdateIntervalMillis(2000L)
        setWaitForAccurateLocation(true)
    }.build()

    private var locationCallback: LocationCallback? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun start() {
        if (locationCallback != null) {
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

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, getMainLooper())

        scope.launch {
            awaitLastLocation()?.let {
                lastKnownLocation = it
                Log.d("LocationManager", "Populated initial lastKnownLocation: ${it.latitude}, ${it.longitude}")
            }
        }
    }

    fun stop() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun awaitLastLocation(): Location? = try {
        fusedLocationClient.lastLocation.await()
    } catch (e: Exception) {
        Log.w("LocationManager", "Failed to get last location", e)
        null
    }
}