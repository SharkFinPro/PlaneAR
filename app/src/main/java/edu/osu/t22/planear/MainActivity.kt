package edu.osu.t22.planear

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.androidgamesdk.GameActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import edu.osu.t22.planear.adsb.AdsbModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resumeWithException

class MainActivity : GameActivity() {
    companion object {
        init {
            System.loadLibrary("GraphicsEngine")
        }

        private const val CAMERA_PERMISSION_CODE = 0
        private const val LOCATION_PERMISSION_CODE = 1

//        @JvmStatic external fun nativeUpdateAircraftBuffer(buffer: ByteBuffer, count: Int)
    }

    private var arSession: Session? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null
    @Volatile private var lastKnownLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get a new instance of the Retrofit API provider
        val api = AdsbModule.provideApi()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10000L
        ).apply {
            setMinUpdateIntervalMillis(2000L)
            setWaitForAccurateLocation(true)
        }.build()


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    try {

                        val loc = lastKnownLocation ?: getLastLocationOrNull()

                        if (loc == null) {
                            Log.w("ADSB", "No location available")
                        } else {

                            val lat = loc.latitude
                            val lon = loc.longitude

                            // get all nearby aircraft within 50nm
                            val nearby = async(Dispatchers.IO) {
                                api.getNearbyAircraft(lat, lon, 50)
                            }

                            // log results
                            val nearbyData = nearby.await()
                            Log.i("ADSB", "Got ${nearbyData.total} aircraft")
                            Log.i("ADSB", "Timing data: (now: ${nearbyData.now}, cTime: ${nearbyData.cTime}, pTime: ${nearbyData.pTime})")

                            val aircraft = nearbyData.ac
                            val count = aircraft.size

                            val elementSize = 32

                            val buf = ByteBuffer.allocateDirect(count * elementSize)
                                .order(ByteOrder.nativeOrder())

                            for (ac in aircraft) {
                                buf.putDouble(ac.lat)
                                buf.putDouble(ac.lon)
                                buf.putFloat(ac.alt_geom.toFloat())
                                buf.putDouble(ac.track)
                                buf.putFloat(ac.gs)
                            }

                            buf.rewind()
//                        nativeUpdateAircraftBuffer(buf, count) // TODO
                        }
                    } catch (e: Exception) {
                        Log.e("ADSB_TEST", "API call failed", e)
                    }

                    // repeat every 5 seconds
                    delay(5_000L)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            maybeCreateSession()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        } else {
            startLocationUpdates()
        }

    }
    @Suppress("MissingPermission")


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            return
        }
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                maybeCreateSession()
            }
            LOCATION_PERMISSION_CODE -> {
                startLocationUpdates()
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun getLastLocationOrNull(): Location? {
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            Log.w("PlaneAR", "Failed to get last location", e)
            null
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            Log.w("LOCATION", "Location permission not granted")
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    lastKnownLocation = loc
                    Log.d("LOCATION", "Location update: ${loc.latitude}, ${loc.longitude}")
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback as LocationCallback, mainLooper)

        lifecycleScope.launch {
            val last = getLastLocationOrNull()
            if (last != null) {
                lastKnownLocation = last
                Log.d("LOCATION", "Populated initial lastKnownLocation: ${last.latitude}, ${last.longitude}")
            }
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun maybeCreateSession() {
        try {
            val installStatus = ArCoreApk.getInstance().requestInstall(this, true)
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                return
            }

            arSession = Session(this)
            Log.i("PlaneAR", "ARCore session created.")
        } catch (e: Exception) {
            Log.e("PlaneAR", "Failed to create a ARCore session", e)
        }
    }

    override fun onResume() {
        super.onResume()
        arSession?.resume()
        startLocationUpdates()
        hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        arSession?.pause()
        stopLocationUpdates()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    private fun hideSystemUi() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}