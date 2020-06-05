package com.benjweber.yellowbrick

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.benjweber.yellowbrick.model.Crime
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter   {

  //  private val mWindow = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)
    private val mWindow = View.inflate(context, R.layout.custom_info_window, null)

    private fun renderWindowText(marker: Marker, view: View) {
        val title = "TYPE: ${marker.title}"
        val dateSnippet = marker.snippet.split("...")[0]
        val typeSpecificSnippet = marker.snippet.split("...")[1]
        val date = "DATE: ${dateSnippet.substring(4, 9)}," +
                          dateSnippet.substring(23, dateSnippet.length)
        val time = "TIME: $dateSnippet.substring(10, 22)}"
        val crimeCode = "CRIME CODE: $typeSpecificSnippet"
        val tvTitle = view.findViewById<TextView>(R.id.tvCrimeTitle)
        val tvDate  = view.findViewById<TextView>(R.id.tvCrimeDate)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val tvTypeSpecific = view.findViewById<TextView>(R.id.tvTypeSpecific)
        tvTitle.text = title
        tvDate.text = date
        tvTime.text = time
        tvTypeSpecific.text = crimeCode
        tvTitle.isSelected = true
    }
    override fun getInfoContents(p0: Marker?): View? {
        if (p0 != null) {
            renderWindowText(p0, mWindow)
        }
        return mWindow
    }

    override fun getInfoWindow(p0: Marker?): View? {
        if (p0 != null) {
            renderWindowText(p0, mWindow)
        }
        return mWindow
    }


}