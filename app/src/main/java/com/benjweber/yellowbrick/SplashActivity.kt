package com.benjweber.yellowbrick

    import android.content.Intent
    import androidx.appcompat.app.AppCompatActivity
    import android.os.Bundle
    import android.util.Log
    import com.google.android.gms.maps.model.BitmapDescriptorFactory
    import com.google.android.gms.maps.model.MarkerOptions

class SplashActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_splash)

            (applicationContext as YBApp).crimeManager.fetchCrimes {
                startActivity(Intent(this, MapActivity::class.java))
            }
        }
    }