package com.benjweber.yellowbrick

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.benjweber.yellowbrick.model.Crime
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class CrimeManager(private val context: Context) {
    private val q = Volley.newRequestQueue(context)
    private var crimes = mutableListOf<Crime>()
    private val CRIME_LIMIT = 3000

    fun fetchCrimes(onCrimesReady: (List<Crime>) -> Unit) {
        val crimesEndpoint = "https://raw.githubusercontent.com/lindsayrgwatt/neighborhood/master/neighborhood/data/historical/Seattle_Police_Department_Police_Report_Incident.csv"
        val req = StringRequest(
            Request.Method.GET, crimesEndpoint,
            { res -> convertToCrimes(res, onCrimesReady) },
            { err -> Log.i("ybyb", "err: $err") }
        )
        q.add(req)
    }

    fun getCrimes(crimeType: String, occuredAfter: Date): List<Crime> {
        Log.i("bjw", "type: ${crimeType}, occAfter: $occuredAfter")
        val filteredCrimes = mutableListOf<Crime>()

        Log.i("bjw", "crimes size: ${crimes.size}")
        for (i in 0 until crimes.size) {
            val curCrime = crimes[i]

            if (curCrime.date > occuredAfter) {
                if (crimeType == "All crimes" || crimeType == curCrime.type) {
                    filteredCrimes.add(curCrime)
                }
            }
            if (filteredCrimes.size >= CRIME_LIMIT || filteredCrimes.size >= crimes.size) {
                Log.i("bjw", "filteredCrimes.size is ${filteredCrimes.size}")
                return filteredCrimes
            }
        }
        return filteredCrimes
    }

    private fun convertToCrimes(csv: String, onCrimesReady: (List<Crime>) -> Unit) {
        val policeScanner = Scanner(csv)

        // Remove the headers
        policeScanner.nextLine()

        for (i in 1..CRIME_LIMIT) {
            if (policeScanner.hasNextLine()) {
                val rawCrime = policeScanner.nextLine().split(",")
                val pos = LatLng(rawCrime[15].toDouble(), rawCrime[14].toDouble())
                val type = rawCrime[6]
                val typeSpecific = rawCrime[4]
                val color = colorOf(type)
                val formatter = SimpleDateFormat("M/dd/yyyy H:mm", Locale.US)
                val date = formatter.parse(rawCrime[8])
                crimes.add(Crime(pos, type, typeSpecific, color, date as Date))
            }
        }

        onCrimesReady(crimes.sortedByDescending { it.date } as MutableList<Crime>)

        // Get the rest of the crimes while the initial ones are shown
        val thread = Thread {
            var allCrimes = mutableListOf<Crime>()
            while (policeScanner.hasNextLine()) {
                val rawCrime = policeScanner.nextLine().split(",")
                val pos = LatLng(rawCrime[15].toDouble(), rawCrime[14].toDouble())
                val type = rawCrime[6]
                val typeSpecific = rawCrime[4]
                val color = colorOf(type)
                val formatter = SimpleDateFormat("M/dd/yyyy H:mm", Locale.US)
                val date = formatter.parse(rawCrime[8])
                allCrimes.add(Crime(pos, type, typeSpecific, color, date as Date))
            }
            allCrimes.forEach {crime ->
                crimes.add(crime)
            }
            crimes = crimes.sortedByDescending { it.date } as MutableList<Crime>
        }
        thread.start()
    }

    private fun colorOf(crimeType: String): Int {
        return when (crimeType) {
            "HOMICIDE"        -> R.color.dark_red
            "ASSAULT"         -> R.color.orange_red
            "THREATS"         -> R.color.orange
            "PURSE SNATCH"    -> R.color.pink
            "CAR PROWL"       -> R.color.purple
            "VEHICLE THEFT"   -> R.color.light_green
            "BURGLARY"        -> R.color.blue
            "ROBBERY"         -> R.color.dark_blue
            "PICKPOCKET"      -> R.color.tan
            else -> {
                R.color.dark_yellow
            }
        }
    }
}