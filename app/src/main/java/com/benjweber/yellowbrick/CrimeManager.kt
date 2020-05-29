package com.benjweber.yellowbrick

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.benjweber.yellowbrick.model.Crime
import com.google.android.gms.maps.model.LatLng
import java.util.*

class CrimeManager(private val context: Context) {
    private val q = Volley.newRequestQueue(context)
    private var crimes = mutableListOf<Crime>()

    fun fetchCrimes(onCrimesReady: (List<Crime>) -> Unit) {
        val crimesEndpoint = "https://raw.githubusercontent.com/lindsayrgwatt/neighborhood/master/neighborhood/data/historical/Seattle_Police_Department_Police_Report_Incident.csv"
        val req = StringRequest(
            Request.Method.GET, crimesEndpoint,
            { res -> convertToCrimes(res, onCrimesReady) },
            { err -> Log.i("ybyb", "err: $err") }
        )
        q.add(req)
    }

    private fun convertToCrimes(csv: String, onCrimesReady: (List<Crime>) -> Unit) {
        val policeScanner = Scanner(csv)

        // Remove the headers
        policeScanner.nextLine()

        while (policeScanner.hasNextLine()) {
            val rawCrime = policeScanner.nextLine().split(",")
            val pos = LatLng(rawCrime[15].toDouble(), rawCrime[14].toDouble())
            val type = rawCrime[6]
//            val date = rawCrime[8]
            // TODO: convert date string to Date obj
            val date = Date()
            crimes.add(Crime(pos, type, date))
        }
        onCrimesReady(crimes)
    }
}