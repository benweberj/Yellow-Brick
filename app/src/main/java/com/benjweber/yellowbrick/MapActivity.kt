package com.benjweber.yellowbrick

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Get the SupportMapFragment and get notified when its ready to be used.
        val mapFrag = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)
    }


    // When the map is ready to use.
    // Add markers, lines, listeners, or move the camera.
    // Make sure the user has GP services installed (prompted to install if not)
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val uw = LatLng(47.655334, -122.303520)
        map.addMarker(MarkerOptions().position(uw).title("Marker at UW"))
        map.moveCamera(CameraUpdateFactory.newLatLng(uw))
    }
}
