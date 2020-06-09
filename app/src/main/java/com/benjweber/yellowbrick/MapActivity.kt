package com.benjweber.yellowbrick

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.benjweber.yellowbrick.fragment.FiltersFragment
import com.benjweber.yellowbrick.model.DirectionsApiManager
import com.google.android.gms.common.api.Status
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener


class MapActivity : AppCompatActivity(), OnMapReadyCallback, AdapterView.OnItemSelectedListener {
    private lateinit var map: GoogleMap //made this public so it is accessible in DirectionsApiManager
    private lateinit var locationManager: YBLocationManager
    private lateinit var crimeManager: CrimeManager
    private lateinit var myLocation: LatLng
    private lateinit var userSelectedLatLong: LatLng
    private val removeLines = mutableListOf<Polyline>()
    var tracker = 0
    private lateinit var polylineFinal : Polyline
    //private lateinit var autocompleteFragment: AutocompleteSupportFragment?

    lateinit var filtersFragment: FiltersFragment

    // Filter's default date is one day ago relative to newest crime
    private val dateFormatter = SimpleDateFormat("M/dd/yyyy H:mm", Locale.US)
    private var filterDate = dateFormatter.parse("5/13/2013 0:00")

    private var filterCrimeTypes = "All crimes"

    private var timesSpinnerPos = 0
    private var typesSpinnerPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        supportActionBar?.elevation = 0f

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

        btnAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity:: class.java)
            startActivity(intent)
        }

        btnFilters.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(FiltersFragment.TAG) == null) {
                filtersFragment = FiltersFragment()
            } else {
                val frag = supportFragmentManager.findFragmentByTag(FiltersFragment.TAG) as? FiltersFragment
                frag?.let {
                    filtersFragment = it
                }
            }

            supportFragmentManager.addOnBackStackChangedListener {
                supportActionBar?.setDisplayHomeAsUpEnabled(
                    supportFragmentManager.backStackEntryCount > 0
                )
            }

            val bundle = Bundle()
            bundle.putInt(FiltersFragment.OUT_TIMES_SELECTION, timesSpinnerPos)
            bundle.putInt(FiltersFragment.OUT_TYPES_SELECTION, typesSpinnerPos)

            filtersFragment.arguments = bundle

            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragContainer, filtersFragment, FiltersFragment.TAG)
                .addToBackStack(FiltersFragment.TAG)
                .commit()

            supportActionBar?.setDisplayHomeAsUpEnabled(true)
//            btnFilters.visibility = View.GONE
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
                map.addMarker(MarkerOptions().position(myLocation).title("currLocation"))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 13.0f))


                locationManager.startLocationUpdates()

                //this will probably need to move somewhere else
//                DirectionsApiManager(this).getDirectionData(map, myLocation)
//                val directionsApiManager = DirectionsApiManager(this)
//                directionsApiManager.getDirectionData(map, myLocation)
            }
        }

        crimeManager.getCrimes(filterCrimeTypes, filterDate).forEach { crime ->
            val snippet = "${crime.date.toString()}...${crime.typeSpecific}...${crime.color}"
            if (crime.date > filterDate && (crime.type == filterCrimeTypes || filterCrimeTypes == "All crimes")) {
                map.addMarker(
                    MarkerOptions()
                        .position(crime.pos)
                        .title(crime.type)
                        .snippet(snippet)
                        .icon(
                            bitmapDescriptorFromDrawable(
                                R.drawable.dot,
                                getString(crime.color)
                            )
                        )
                )
            }
        }

        // Set infowindowAdapter, makes window pop up for markers
        map.setInfoWindowAdapter(CustomInfoWindowAdapter(this))
        //initalize sdk
        Places.initialize(applicationContext, "AIzaSyCROCh7-9oNChMfxra7YplVoRQXIXbwETg")
        // Create a new Places client instance
        val placesClient: PlacesClient = Places.createClient(this)

        //Initialize AutocompleteSupportFragment
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment?

        autocompleteFragment?.setTypeFilter(TypeFilter.ESTABLISHMENT)
        //TypeFilter.ADDRESS,
        // set location bias to Seattle only
        //autocompleteFragment?.setLocationRestriction()
        autocompleteFragment?.setPlaceFields(mutableListOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment?.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(p0: Place) {
                Log.i("what", removeLines.toString() + removeLines.size.toString())
                if (removeLines.size > 0) {
                    removeLines[0].remove()
                    removeLines.removeAt(0)
                }
                Log.i("what", removeLines.toString() + removeLines.size.toString())
                if (p0.latLng != null) {
                    userSelectedLatLong = p0.latLng!!
                    DirectionsApiManager(this).getDirectionData(map, myLocation, userSelectedLatLong, removeLines)
                }
            }

            override fun onError(p0: Status) {
                Log.i("what", p0.status.toString())
            }

        })

    }

    // Convert drawable into BitmapDescriptor to be used for markers
    private fun bitmapDescriptorFromDrawable(drawableId: Int, color: String): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, drawableId)

        drawable!!.setTint(Color.parseColor(color))

        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bm = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bm)
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
            .setNegativeButton("No thanks dude") { _, _ -> }
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

    // When spinner items are selected, filters the map points
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val itemContent = parent?.getItemAtPosition(position).toString()
        if (parent?.id == R.id.spinnerTimeFilter) {
            val newDate = when (itemContent) {
                "Past day" -> dateFormatter.parse("5/13/2013 0:00")
                "Past week" -> dateFormatter.parse("5/06/2013 0:00")
                "Past month" -> dateFormatter.parse("4/13/2013 0:00")
                "Past year" -> dateFormatter.parse("5/13/2012 0:00")
                "All times" -> dateFormatter.parse("12/31/2010 0:00")
                else -> dateFormatter.parse("5/13/2013 0:00")
            }

            if (newDate != filterDate) {
                filterDate = newDate
                map.clear()
                onMapReady(map)
            }

            timesSpinnerPos = position

        } else if (parent?.id == R.id.spinnerTypeFilter) {
            val newCrimeType = when (itemContent) {
                "All types of crime" -> "All crimes"
                "Homicide" -> "HOMICIDE"
                "Assault" -> "ASSAULT"
                "Threats" -> "THREATS"
                "Purse snatching" -> "PURSE SNATCH"
                "Car prowling" -> "CAR PROWL"
                "Vehicle theft" -> "VEHICLE THEFT"
                "Burglary" -> "BURGLARY"
                "Robbery" -> "ROBBERY"
                "Pickpocketing" -> "PICKPOCKET"
                else -> "All crimes"
            }

            if (newCrimeType != filterCrimeTypes) {
                filterCrimeTypes = newCrimeType
                map.clear()
                onMapReady(map)
            }

            typesSpinnerPos = position
        }
    }
}
