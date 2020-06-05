package com.benjweber.yellowbrick

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.benjweber.yellowbrick.fragment.FiltersFragment
import com.benjweber.yellowbrick.model.DirectionsApiManager
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_map.*
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.uiThread
import java.net.URL
import com.google.android.gms.maps.model.LatLngBounds //?
import com.google.android.gms.maps.model.PolylineOptions //?
import java.text.SimpleDateFormat
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback, AdapterView.OnItemSelectedListener {
    private lateinit var map: GoogleMap //made this public so it is accessible in DirectionsApiManager
    private lateinit var locationManager: YBLocationManager
    private lateinit var crimeManager: CrimeManager
    private lateinit var myLocation: LatLng

    // Filter's default date is one day ago relative to newest crime
    private val dateFormatter = SimpleDateFormat("M/dd/yyyy H:mm", Locale.US)
    private var filterDate = dateFormatter.parse("5/13/2013 0:00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        locationManager = (application as YBApp).locationManager
        crimeManager = (application as YBApp).crimeManager

        // Get the SupportMapFragment and get notified when its ready to be used
        val mapFrag = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)


        // Check if filters fragment is currently displayed
        // Show the back arrow if it is
        if(supportFragmentManager.findFragmentByTag(FiltersFragment.TAG) != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        btnFilters.setOnClickListener {
            val filtersFragment = FiltersFragment()

            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragContainer, filtersFragment, FiltersFragment.TAG)
                .addToBackStack(FiltersFragment.TAG)
                .commit()

            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            btnFilters.visibility = View.GONE
        }
    }

    // When the map is ready to use
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.dark_map_style))
//        val latLongB = LatLngBounds.Builder()


        if (!locationManager.locationGranted()) getLocationPermission()
        if (locationManager.locationGranted()) {
            locationManager.getLastLocation { location ->
                myLocation = LatLng(location.latitude, location.longitude)
                map.addMarker(MarkerOptions().position(myLocation))
                map.moveCamera(CameraUpdateFactory.newLatLng(myLocation))

                locationManager.startLocationUpdates()

                //this will probably need to move somewhere else
                val directionsApiManager = DirectionsApiManager(this)
                directionsApiManager.getDirectionData(map, myLocation)
            }
        }

        crimeManager.fetchCrimes { allCrimes ->
            var crimeLimit = 100
            allCrimes.forEach { crime ->
                if (crimeLimit > 0) {
                    if(crime.date > filterDate) {
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

                        map.addMarker(
                            MarkerOptions()
                                .position(crime.pos)
                                .title(crime.type)
                                .snippet(crime.date.toString())
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
                        )
                        crimeLimit--
                    }
                }
            }

            // Set infowindowAdapter, makes window pop up for markers
            map.setInfoWindowAdapter(CustomInfoWindowAdapter(this))

            // TODO: find a way to use all incidents, but only show relevant ones
           // Log.i("ybyb", "we got ${allCrimes.size} crimes here")
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

    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        btnFilters.visibility = View.VISIBLE
        return super.onNavigateUp()
    }

    override fun onBackPressed() {
        btnFilters.visibility = View.VISIBLE
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        super.onBackPressed()
    }

    companion object {
        private const val LOCATION_PERMISSION_CODE = 254 // Random #
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val itemContent = parent?.getItemAtPosition(position).toString()
        if (parent?.id == R.id.spinnerTimeFilter) {
            val newDate = when (itemContent) {
                "Past day" -> dateFormatter.parse("5/13/2013 0:00")
                "Past week" -> dateFormatter.parse("5/06/2013 0:00")
                "Past month" -> dateFormatter.parse("6/13/2013 0:00")
                "Past year" -> dateFormatter.parse("5/13/2012 0:00")
                "All times" -> dateFormatter.parse("12/31/2010 0:00")
                else -> dateFormatter.parse("5/13/2013 0:00")
            }

            if (newDate != filterDate) {
                filterDate = newDate
                onMapReady(map)
            }
        }
    }
}
