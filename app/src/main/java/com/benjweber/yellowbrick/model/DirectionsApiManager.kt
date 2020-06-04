package com.benjweber.yellowbrick.model

import android.app.Application
import android.content.Context
import android.graphics.Color
import com.benjweber.yellowbrick.MapActivity
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.uiThread
import java.net.URL


// help from //https://github.com/irenenaya/Kotlin-Practice/blob/master/MapsRouteActivity.kt
class DirectionsApiManager(context: Context) {
    private lateinit var takeMap: GoogleMap
    ///test code may not implement
    fun getDirectionData(map: GoogleMap, location: LatLng) {
        takeMap = map
        val myLocation: LatLng = location
        val latLongB = LatLngBounds.Builder()
        val testMarker = map.addMarker(MarkerOptions().position(LatLng(47.4168418, -122.1739783)))
        val options = PolylineOptions()
        options.color(Color.YELLOW)
        options.width(5f)
        val url = getURL(myLocation, testMarker.position)
        async { //use anko's async to connect to url, download contents, and put into string
            val result = URL(url).readText()
            uiThread { //extract json object from string using klaxon
                val parser: Parser = Parser()
                val stringBuilder: StringBuilder = StringBuilder(result)
                val json: JsonObject = parser.parse(stringBuilder) as JsonObject
                val routes = json.array<JsonObject>("routes") //traverse json to get points
                val points = routes!!["legs"]["steps"][0] as JsonArray<JsonObject>
                //convert to simple list
                val polypts = points.flatMap { decodePoly(it.obj("polyline")?.string("points")!!) }
                // Add  points to polyline and bounds
                options.add(myLocation)
                latLongB.include(myLocation)
                for (point in polypts) {
                    options.add(point)
                    latLongB.include(point)
                }
                options.add(testMarker.position)
                latLongB.include(testMarker.position)
                // build bounds
                val bounds = latLongB.build()
                // add polyline to the map
                map!!.addPolyline(options)
                // show map with route centered
                map!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        }
    }

    private fun getURL(from : LatLng, to : LatLng) : String {
        val origin = "origin=" + from.latitude + "," + from.longitude
        val dest = "destination=" + to.latitude + "," + to.longitude
        val sensor = "sensor=false"
        val params = "$origin&$dest&$sensor&"
        return "https://maps.googleapis.com/maps/api/directions/json?${params}key="
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(lat.toDouble() / 1E5,
                lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }
}
