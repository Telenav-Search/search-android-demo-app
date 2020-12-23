package telenav.demo.app.personalinfo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolygonOptions
import com.telenav.sdk.core.Callback
import com.telenav.sdk.ota.api.OtaService
import com.telenav.sdk.ota.model.AreaStatus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import telenav.demo.app.R
import telenav.demo.app.setGPSListener
import telenav.demo.app.stopGPSListener
import java.text.SimpleDateFormat
import java.util.*


class HomeAreaActivity : AppCompatActivity() {
    private val homeAreaClient by lazy { OtaService.getHomeAreaClient() }

    private lateinit var vLastUpdate: TextView
    private lateinit var vUpdateInProgress: View
    private lateinit var vUpdate: View
    private lateinit var vReset: View
    private lateinit var fMap: SupportMapFragment
    private var map: GoogleMap? = null

    private var homeArea: AreaStatus? = null

    var lastKnownLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val areaStatus = homeArea

            val isHomeAreaSet = areaStatus != null && areaStatus.areaGeometry != null
            val isFirstLocation = lastKnownLocation == null

            lastKnownLocation = locationResult.lastLocation
            if (isFirstLocation && !isHomeAreaSet) {
                Log.d("onLocationResult", "positionMap")
                positionMap()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_area)

        fMap = supportFragmentManager.findFragmentById(R.id.home_area_map) as SupportMapFragment

        vLastUpdate = findViewById(R.id.home_area_last_update)
        vUpdateInProgress = findViewById(R.id.home_area_updating)
        vUpdate = findViewById(R.id.home_area_update)
        vReset = findViewById(R.id.home_area_clear)

        vUpdate.setOnClickListener {
            updateHomeArea()
        }
        vReset.setOnClickListener {
            resetHomeArea()
        }
        findViewById<View>(R.id.home_area_back).setOnClickListener { finish() }

        updateUI()
    }

    private fun getUpdateStatus(): Boolean {
        val prefs =
            getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )
        return prefs.getBoolean(getString(R.string.saved_update_status), false)
    }

    private fun setUpdateStatus(status: Boolean) {
        val prefs =
            getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )
        with(prefs.edit()) {
            putBoolean(getString(R.string.saved_update_status), status)
            apply()
        }
    }

    private fun getHomeArea() {
        homeArea = homeAreaClient.statusRequest().execute()
        updateUI()
    }

    private fun updateHomeArea() {
        setUpdateStatus(true)
        homeAreaClient.updateRequest()
            .setCurrentLocation(
                lastKnownLocation?.latitude ?: .0,
                lastKnownLocation?.longitude ?: .0
            )
            .setTimeout(1800) // in seconds
            .asyncCall(object : Callback<AreaStatus?> {

                override fun onSuccess(areaStatus: AreaStatus?) {
                    showUpdateNotification(true)
                    finalize(areaStatus)
                }

                override fun onFailure(error: Throwable) {
                    showUpdateNotification(false)
                    finalize(null)
                }

                private fun finalize(areaStatus: AreaStatus?) {
                    setUpdateStatus(false)
                    EventBus.getDefault().post(HomeAreaUpdateEvent(areaStatus))
                }
            })
        updateUI()
    }

    private fun resetHomeArea() {
        homeAreaClient.resetRequest().execute()
        homeArea = null
        updateUI()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHomeAreaUpdateEvent(event: HomeAreaUpdateEvent) {
        homeArea = event.areaStatus
        updateUI()
    }

    private fun showUpdateNotification(isSuccessful: Boolean = true) {
        val message =
            if (isSuccessful) "Home Area Updated Successfully" else "Home Area Update Failed"
        val channelId = getString(CHANNEL_ID)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle("Update Home Area")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(100, builder.build())
        }
    }

    private fun updateUI() {
        val areaStatus = homeArea
        val isUpdating = getUpdateStatus()

        if (isUpdating) {
            vUpdateInProgress.visibility = View.VISIBLE
            vLastUpdate.visibility = View.GONE
            vUpdate.visibility = View.GONE
            vReset.visibility = View.GONE
        } else {
            vUpdateInProgress.visibility = View.GONE
            vLastUpdate.visibility = View.VISIBLE
            vUpdate.visibility = View.VISIBLE
            if (areaStatus != null) {
                vReset.visibility = View.VISIBLE
            } else {
                vReset.visibility = View.GONE
            }
        }

        setLastUpdateTime()
        positionMap()
    }

    private fun setLastUpdateTime() {
        val status = homeArea
        var dateString = "Never"

        if (status != null && status.lastUpdatedTime > 0) {
            val formatter = SimpleDateFormat("dd/MM/yyyy")
            dateString = formatter.format(Date(status.lastUpdatedTime))
        }

        vLastUpdate.text = getString(R.string.home_area_last_update, dateString)
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

            map?.setOnMapLoadedCallback {
                positionMap()
            }
        }
    }

    private fun positionMap() {
        val status = homeArea

        map?.clear()
        if (status == null || status.areaGeometry == null) {
            val location = lastKnownLocation
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
                map?.moveCamera(cameraUpdate)
            }
        } else {
            val coordinates = status.areaGeometry.coordinates

            var minLat = coordinates[0].latitude
            var maxLat = coordinates[0].latitude
            var minLng = coordinates[0].longitude
            var maxLng = coordinates[0].longitude
            val polygonPoints: MutableList<LatLng> = ArrayList()

            coordinates.forEach { geoPoint ->
                if (geoPoint.latitude <= minLat) {
                    minLat = geoPoint.latitude
                }
                if (geoPoint.latitude >= maxLat) {
                    maxLat = geoPoint.latitude
                }

                if (geoPoint.longitude <= minLng) {
                    minLng = geoPoint.longitude
                }
                if (geoPoint.longitude >= maxLng) {
                    maxLng = geoPoint.longitude
                }
                polygonPoints.add(LatLng(geoPoint.latitude, geoPoint.longitude))
            }

            val bounds = LatLngBounds(
                LatLng(minLat, minLng),
                LatLng(maxLat, maxLng)
            )
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 24)

            map?.moveCamera(cameraUpdate)
            map?.addPolygon(
                PolygonOptions().addAll(polygonPoints).strokeColor(Color.BLUE)
                    .fillColor(Color.argb(20, 0, 255, 0))
            )
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
        getHomeArea()
        initMap()
        setGPSListener(locationCallback)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this);
        stopGPSListener(locationCallback)
    }

    companion object {
        const val CHANNEL_ID = R.string.app_name
    }

    class HomeAreaUpdateEvent(status: AreaStatus?) {
        val areaStatus: AreaStatus? = status
    }
}