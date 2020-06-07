package com.benjweber.yellowbrick

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.benjweber.yellowbrick.model.Crime
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import java.lang.Integer.parseInt

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter   {

  //  private val mWindow = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)
    private val mWindow = View.inflate(context, R.layout.custom_info_window, null)

    @SuppressLint("SetTextI18n")
    private fun renderWindowText(marker: Marker, view: View) {
        val snippetInfo = marker.snippet.split("...")
        val rawDate = snippetInfo[0]
        val typeSpecific = snippetInfo[1]
        val rawColor = snippetInfo[2].toInt()
        val color = context.getColor(rawColor)

        val tvTitle = view.findViewById<TextView>(R.id.tvCrimeTitle)
        val tvDate  = view.findViewById<TextView>(R.id.tvCrimeDate)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val tvTypeSpecific = view.findViewById<TextView>(R.id.tvTypeSpecific)
        val infoWindow = view.findViewById<LinearLayout>(R.id.infoWindow)

        tvTitle.text = "TYPE: ${marker.title}"
        tvDate.text = "DATE: ${rawDate.substring(4, 9)}, ${rawDate.substring(23, rawDate.length)}" // day, year
        tvTime.text = "TIME: ${rawDate.substring(10, 22)}"
        tvTypeSpecific.text = "CRIME CODE: $typeSpecific"
        tvTitle.isSelected = true

//        Log.i("bjw", "and the int version is ${}")
        infoWindow.setBackgroundColor(color)
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