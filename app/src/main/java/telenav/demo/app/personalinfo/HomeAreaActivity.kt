package telenav.demo.app.personalinfo

import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ContentLoadingProgressBar
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import telenav.demo.app.R

class HomeAreaActivity : AppCompatActivity() {

    private lateinit var vLoading: ContentLoadingProgressBar

    private lateinit var fMap: SupportMapFragment
    private var map: GoogleMap? = null

    var lastKnownLocation: Location? = null
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_area)

        vLoading = findViewById(R.id.home_area_loading)
        fMap = supportFragmentManager.findFragmentById(R.id.home_area_map) as SupportMapFragment

//        vLoading.show()
        findViewById<View>(R.id.home_area_back).setOnClickListener { finish() }
    }

    private fun initMap() {
        fMap.getMapAsync {
            map = it
            it.mapType = GoogleMap.MAP_TYPE_NORMAL

            try {
                it.isMyLocationEnabled = true
            } catch (e: SecurityException) {
            }
            it.isTrafficEnabled = true

            val location = lastKnownLocation;
            if (location != null) {
                positionMap(location.latitude, location.longitude)
            }
        }
    }

    private fun positionMap(lat: Double, lon: Double) {
        val latLng = LatLng(lat, lon)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
        map?.moveCamera(cameraUpdate)
    }

    override fun onResume() {
        super.onResume()
        getLocation()
        initMap()
    }

    override fun onPause() {
        stopGPSListner()
        super.onPause()
    }

    private fun getLocation() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 5000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    for (location in locationResult.locations) {
                        lastKnownLocation = location;
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
        }
    }

    private fun stopGPSListner() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (locationCallback != null)
                fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: SecurityException) {
        }
    }
}