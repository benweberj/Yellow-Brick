package com.benjweber.yellowbrick

import android.content.Context
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
        val date = "DATE: ${marker.snippet.substring(4, 9)}," +
                          marker.snippet.substring(23, marker.snippet.length)
        val time = "TIME: ${marker.snippet.substring(10, 22)}"
        val tvTitle = view.findViewById<TextView>(R.id.tvCrimeTitle)
        val tvDate  = view.findViewById<TextView>(R.id.tvCrimeDate)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        tvTitle.text = title
        tvDate.text = date
        tvTime.text = time
        tvTitle.isSelected = true
    }
    override fun getInfoContents(p0: Marker?): View? {
        if (p0 != null) {
            renderWindowText(p0, mWindow)
        }
        return mWindow;
    }

    override fun getInfoWindow(p0: Marker?): View? {
        if (p0 != null) {
            renderWindowText(p0, mWindow)
        }
        return mWindow;
    }


}