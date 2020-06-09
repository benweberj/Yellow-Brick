package com.benjweber.yellowbrick

import android.Manifest
import android.util.Log
import android.os.Looper
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

class YBLocationManager(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Returns whether or not the user has given permission to use their location
    fun locationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Gets the user's last known location and executes the given callback when done
    @SuppressLint("MissingPermission")
    fun getLastLocation(onLastLocation: (LatLng) -> Unit) {
        if (locationGranted()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    val location = LatLng(loc.latitude, loc.longitude)
                    onLastLocation(location)
                }
            }
        }
    }

    // Start making location requests at a specified interval
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 60000 // 1 minute
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        if (locationRequest != null) {
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val client = LocationServices.getSettingsClient(context)
            val task = client.checkLocationSettings(builder.build())
            task.addOnSuccessListener {
                if (locationGranted()) {
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }
        }
    }

    // Called when the user's location is retrieved
    private val locationCallback: LocationCallback = object: LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            Log.i("ybyb", "We got em")
        }
    }
}