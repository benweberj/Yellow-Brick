package com.benjweber.yellowbrick

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var locationManager: YBLocationManager
    private lateinit var crimeManager: CrimeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        locationManager = (application as YBApp).locationManager
        crimeManager = (application as YBApp).crimeManager

        // Get the SupportMapFragment and get notified when its ready to be used
        val mapFrag = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)
    }

    // When the map is ready to use
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.dark_map_style))

        if (!locationManager.locationGranted()) getLocationPermission()
        if (locationManager.locationGranted()) {
            locationManager.getLastLocation { location ->
                val myLocation = LatLng(location.latitude, location.longitude)
                map.addMarker(MarkerOptions().position(myLocation))
                map.moveCamera(CameraUpdateFactory.newLatLng(myLocation))

                locationManager.startLocationUpdates()
            }
        }
        crimeManager.fetchCrimes { allCrimes ->
            var crimeLimit = 100
            allCrimes.forEach { crime ->
                if (crimeLimit > 0) {
                    val hue = when (crime.type) {
                        "CAR PROWL" -> BitmapDescriptorFactory.HUE_BLUE
                        "OTHER PROPERTY" -> BitmapDescriptorFactory.HUE_ORANGE
                        "BURGLARY" -> BitmapDescriptorFactory.HUE_VIOLET
                        "PROPERTY DAMAGE" -> BitmapDescriptorFactory.HUE_YELLOW
                        "VEHICLE THEFT" -> BitmapDescriptorFactory.HUE_CYAN
                        "ASSAULT" -> BitmapDescriptorFactory.HUE_AZURE
                        "WARRANT ARREST" -> BitmapDescriptorFactory.HUE_GREEN
                        "FRAUD" -> BitmapDescriptorFactory.HUE_ROSE
                        "THREATS" -> BitmapDescriptorFactory.HUE_MAGENTA
                        else -> {
                            BitmapDescriptorFactory.HUE_RED
                        }
                    }

                    map.addMarker(MarkerOptions()
                        .position(crime.pos)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue)))
                }
                crimeLimit--
            }
            // TODO: find a way to use all incidents, but only show relevant ones
            // TODO: make custom markers dots that are small and color-coded
            Log.i("ybyb", "we got ${allCrimes.size} crimes here")
        }
    }

    // Explains why we need location, then asks them for permission
    private fun getLocationPermission() {
        AlertDialog.Builder(this)
            .setTitle("YellowBrick Needs Your Location Permissions")
            .setMessage("In order to get you where you need to go safely, we need to know where you are.")
            .setPositiveButton("Got it") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),

                    LOCATION_PERMISSION_CODE
                )
            }
            .setNegativeButton("Piss off") { _, _ -> }
            .create()
            .show()
    }

    // Not sure if we'll need this specific function later on
    // Runs when the user has granted or denied location permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    Log.i("ybyb", "the location permissions have been granted")
                }
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_CODE = 254 // Random #
    }
}
