package com.benjweber.yellowbrick.model

import com.google.android.gms.maps.model.LatLng
import java.util.*

data class Crime (
    val pos: LatLng,
    val type: String,
    val date: Date
)