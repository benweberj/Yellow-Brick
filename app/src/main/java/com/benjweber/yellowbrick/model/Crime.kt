package com.benjweber.yellowbrick.model

import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import java.util.Date

data class Crime (
    val pos: LatLng,
    val type: String,
    val typeSpecific: String,
    val color: Int,
    val date: Date
)