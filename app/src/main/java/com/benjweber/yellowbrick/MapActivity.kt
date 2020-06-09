package com.benjweber.yellowbrick

import java.util.*
import android.Manifest
import android.util.Log
import android.os.Bundle
import android.view.View
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.AdapterView
import java.text.SimpleDateFormat
import android.content.pm.PackageManager
import android.os.PersistableBundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.common.api.Status
import kotlinx.android.synthetic.main.activity_map.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.benjweber.yellowbrick.fragment.AboutFragment
import com.benjweber.yellowbrick.fragment.FiltersFragment
import com.benjweber.yellowbrick.model.DirectionsApiManager
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener



class MapActivity : AppCompatActivity(), OnMapReadyCallback, AdapterView.OnItemSelectedListener {
    lateinit var map: GoogleMap // public so it's accessible in DirectionsApiManager
    private lateinit var locationManager: YBLocationManager
    private lateinit var crimeManager: CrimeManager
//    private lateinit var myLocation: LatLng
    private lateinit var userSelectedLatLong: LatLng
    private val removeLines = mutableListOf<Polyline>()
    var tracker = 0
    private lateinit var polylineFinal : Polyline
    //private lateinit var autocompleteFragment: AutocompleteSupportFragment?

    lateinit var filtersFragment: FiltersFragment
    lateinit var aboutFragment: AboutFragment

    // Filter's default date is one day ago relative to newest crime
    private val dateFormatter = SimpleDateFormat("M/dd/yyyy H:mm", Locale.US)
    private val longDateFormatter = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US)
    private var filterDate = dateFormatter.parse("5/13/2013 0:00")

    private var filterCrimeTypes = "All crimes"

    private var timesSpinnerPos = 0
    private var typesSpinnerPos = 0

    companion object {
        private const val LOCATION_PERMISSION_CODE = 254 // Random #\
        const val OUT_TIMES_SELECTION = "OUT_TIMES_SELECTION"
        const val OUT_TYPES_SELECTION = "OUT_TYPES_SELECTION"
        const val OUT_TIMES_POS = "OUT_TIMES_POS"
        const val OUT_TYPES_POS = "OUT_TYPES_POS"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(OUT_TYPES_SELECTION, filterCrimeTypes)
        outState.putString(OUT_TIMES_SELECTION, filterDate?.toString())
        outState.putInt(OUT_TIMES_POS, timesSpinnerPos)
        outState.putInt(OUT_TYPES_POS, typesSpinnerPos)
        super.onSaveInstanceState(outState)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        supportActionBar?.elevation = 0f

        locationManager = (application as YBApp).locationManager
        crimeManager = (application as YBApp).crimeManager

        if (savedInstanceState != null) {
            with (savedInstanceState) {
                val time = getString(OUT_TIMES_SELECTION)
                val type = getString(OUT_TYPES_SELECTION)
                val timePos = getInt(OUT_TIMES_POS)
                val typePos = getInt(OUT_TYPES_POS)

                timesSpinnerPos = timePos
                typesSpinnerPos = typePos

                type?.let {
                    filterCrimeTypes = it
                }
                time?.let {
                    filterDate = longDateFormatter.parse(it)
                }
            }
        }

        // Get the SupportMapFragment and get notified when its ready to be used
        val mapFrag = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)


        // Check if filters fragment is currently displayed
        // Show the back arrow if it is
        if(supportFragmentManager.findFragmentByTag(FiltersFragment.TAG) != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        btnAbout.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(AboutFragment.TAG) == null) {
                aboutFragment = AboutFragment()
            } else {
                val frag = supportFragmentManager.findFragmentByTag(AboutFragment.TAG) as? AboutFragment
                frag?.let {
                    aboutFragment  = it
                }
            }

            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragContainer, aboutFragment, AboutFragment.TAG)
                .addToBackStack(AboutFragment.TAG)
                .commit()

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

            val bundle = Bundle()
            bundle.putInt(FiltersFragment.OUT_TIMES_SELECTION, timesSpinnerPos)
            bundle.putInt(FiltersFragment.OUT_TYPES_SELECTION, typesSpinnerPos)

            filtersFragment.arguments = bundle

            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragContainer, filtersFragment, FiltersFragment.TAG)
                .addToBackStack(FiltersFragment.TAG)
                .commit()

//            supportActionBar?.setDisplayHomeAsUpEnabled(true)
//            btnFilters.visibility = View.GONE
        }

        supportFragmentManager.addOnBackStackChangedListener {
            supportActionBar?.setDisplayHomeAsUpEnabled(
                supportFragmentManager.backStackEntryCount > 0
            )
        }
    }

    // When the map is ready to use
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.dark_map_style))

        if (!locationManager.locationGranted()) getLocationPermission()

        if (locationManager.locationGranted()) {
            locationManager.getLastLocation { loc ->
                map.addMarker(MarkerOptions().position(loc).title("currLocation"))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 13.0f))

                locationManager.startLocationUpdates()
            }
        }

        crimeManager.getCrimes(filterCrimeTypes, filterDate).forEach { crime ->
            val snippet = "${crime.date.toString()}...${crime.typeSpecific}...${crime.color}"
            map.addMarker(
                MarkerOptions()
                    .position(crime.pos)
                    .title(crime.type)
                    .snippet(snippet)
                    .icon(bitmapDescriptorFromDrawable(R.drawable.dot, getString(crime.color)))
            )
        }

        // Makes window pop-up for markers
        map.setInfoWindowAdapter(CustomInfoWindowAdapter(this))

        Places.initialize(applicationContext, "AIzaSyCROCh7-9oNChMfxra7YplVoRQXIXbwETg")
        Places.createClient(this)

        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment?
        autocompleteFragment.let {
            it?.setTypeFilter(TypeFilter.ESTABLISHMENT)
            it?.setPlaceFields(mutableListOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
            it?.setLocationBias(RectangularBounds.newInstance(
                LatLng(47.499864,  -122.232611),
                LatLng(47.734542, -122.433396)
            ))
            it?.setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(p0: Place) {
                    Log.i("what", removeLines.toString() + removeLines.size.toString())
                    if (removeLines.size > 0) {
                        removeLines[0].remove()
                        removeLines.removeAt(0)
                    }
                    Log.i("what", removeLines.toString() + removeLines.size.toString())
                    if (p0.latLng != null) {
                        userSelectedLatLong = p0.latLng as LatLng
                        locationManager.getLastLocation { loc ->
                            DirectionsApiManager(this).getDirectionData(
                                map, loc, userSelectedLatLong, removeLines
                            )
                        }

                    }
                }

                override fun onError(p0: Status) {
                    Log.i("what", p0.status.toString())
                }
            })
        }
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
