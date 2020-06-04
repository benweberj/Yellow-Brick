package com.benjweber.yellowbrick

import android.app.Application

class YBApp : Application() {
    lateinit var locationManager: YBLocationManager private set
    lateinit var crimeManager: CrimeManager private set

    override fun onCreate() {
        super.onCreate()

        locationManager = YBLocationManager(this)
        crimeManager = CrimeManager(this)
    }


}