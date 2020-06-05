package com.benjweber.yellowbrick.model

import android.graphics.Color
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.uiThread
import java.net.URL


// help from //https://github.com/irenenaya/Kotlin-Practice/blob/master/MapsRouteActivity.kt
class DirectionsApiManager(context: PlaceSelectionListener) {
    private lateinit var takeMap: GoogleMap
    private val options = PolylineOptions()
    ///test code may not implement
    fun getDirectionData(map: GoogleMap, location: LatLng, destination: LatLng, tracker: Int) {
        takeMap = map
        val myLocation: LatLng = location
        val latLongB = LatLngBounds.Builder()
        // Create PolyLine Object and set the color and width
        //val options = PolylineOptions()
        options.color(Color.YELLOW)
        options.width(5f)
//        if (tracker != 1) {
//            newPolyLine.remove()
//        }
        //Call url builder to fetch data from google
        val url = getURL(myLocation, destination)
        async { //Connect to the URL and get contents in string result, use Anko async so that we dont do this in UI thread
            val result = URL(url).readText()
            uiThread {

                //Use Klaxon to extract JSON object from String result
                val parser: Parser = Parser()
                val stringBuilder: StringBuilder = StringBuilder(result)
                val json: JsonObject = parser.parse(stringBuilder) as JsonObject

                val routes = json.array<JsonObject>("routes")
                val points = routes!!["legs"]["steps"][0] as JsonArray<JsonObject> //Json array of JsonObjects

                // find polyline object in each element of the array and get the "points" field
                val polypts = points.flatMap { decodePoly(it.obj("polyline")?.string("points")!!) }
                options.add(myLocation)
                latLongB.include(myLocation)
                for (point in polypts) {
                    options.add(point)
                    latLongB.include(point)
                }

                //Add starting points, points in polypts, and destination points
                options.add(destination)
                latLongB.include(destination)
                // build bounds
                val bounds = latLongB.build()
                // add polyline to the map
                map!!.addPolyline(options)
                // show map with route centered
                map!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        }
    }

    //Function to create URL for API calling, takes in starting Lat and end Lat and returns the url with passed in parameters
    private fun getURL(from : LatLng, to : LatLng) : String {
        val origin = "origin=" + from.latitude + "," + from.longitude
        val dest = "destination=" + to.latitude + "," + to.longitude
        val sensor = "sensor=false"
        val params = "$origin&$dest&$sensor"
        return "https://maps.googleapis.com/maps/api/directions/json?${params}&key=AIzaSyBcBs9zaSemryx-xXW0p-KtYHlffpAEDPQ"
    }


    //
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
