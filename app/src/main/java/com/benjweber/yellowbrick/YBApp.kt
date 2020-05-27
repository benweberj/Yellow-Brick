package com.benjweber.yellowbrick

import android.app.Application

class YBApp : Application() {
    lateinit var locationManager: YBLocationManager private set

    override fun onCreate() {
        super.onCreate()

        locationManager = YBLocationManager(this)
    }

}