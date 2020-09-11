package telenav.demo.app.entitydetails

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ContentLoadingProgressBar
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.AddressType
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType
import com.telenav.sdk.entity.model.lookup.EntityGetDetailResponse
import telenav.demo.app.R
import telenav.demo.app.searchlist.dip

class EntityDetailsActivity : AppCompatActivity() {
    private val telenavService: EntityClient by lazy { EntityService.getClient() }

    private lateinit var vLoading: ContentLoadingProgressBar
    private lateinit var vEntityName: TextView
    private lateinit var vEntityDetails: View
    private lateinit var vEntityAddress: TextView
    private lateinit var vEntityCall: View
    private lateinit var vEntityCallButton: Button
    private lateinit var vEntityCallNumber: TextView
    private lateinit var vEntityIcon: ImageView
    private lateinit var fMap: SupportMapFragment
    private var map: GoogleMap? = null
    private var lastKnowLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entity_details)

        val id = intent.getStringExtra(PARAM_ID) ?: ""
        val icon = intent.getIntExtra(PARAM_ICON, 0)

        vLoading = findViewById(R.id.entity_details_loading)
        vEntityName = findViewById(R.id.entity_details_name)
        vEntityDetails = findViewById(R.id.entity_details)
        vEntityAddress = findViewById(R.id.entity_details_address)
        vEntityCall = findViewById(R.id.entity_details_call)
        vEntityCallButton = findViewById(R.id.entity_details_call_button)
        vEntityCallNumber = findViewById(R.id.entity_details_call_number)
        vEntityIcon = findViewById(R.id.entity_details_icon)
        vLoading.show()

        if (icon != 0) {
            vEntityIcon.setImageResource(icon)
            vEntityIcon.visibility = View.VISIBLE
        }

        findViewById<View>(R.id.entity_details_back).setOnClickListener { finish() }

        getLocationAndDetails(id)
        initMap()

    }

    private fun getLocationAndDetails(id: String) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation
                .addOnSuccessListener(this) { location ->
                    if (location != null) {
                        lastKnowLocation = location
                        getDetails(id, location)
                        positionMap(location.latitude, location.longitude)
                    } else
                        getDetails(id)

                }
        } catch (e: SecurityException) {
            getDetails(id)
        }
    }

    private fun initMap() {
        fMap = SupportMapFragment()
        supportFragmentManager.beginTransaction().replace(R.id.entity_details_map, fMap).commit()

        fMap.getMapAsync {
            if (isFinishing)
                return@getMapAsync
            map = it
            it.mapType = GoogleMap.MAP_TYPE_NORMAL

            try {
                it.isMyLocationEnabled = true
            } catch (e: SecurityException) {
            }
            it.isTrafficEnabled = true
            lastKnowLocation?.apply {
                positionMap(latitude, longitude)
            }
        }
    }

    private fun positionMap(lat: Double, lon: Double) {
        val latLng = LatLng(lat, lon)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
        map?.moveCamera(cameraUpdate)
    }

    private fun positionMap(lat: Double, lon: Double, marker: Marker, zoom: Float) {
        val latLng = LatLng(lat, lon)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom)
        map?.animateCamera(cameraUpdate, object : GoogleMap.CancelableCallback {
            override fun onFinish() {
                marker.showInfoWindow()
            }

            override fun onCancel() {
            }
        })
    }

    private fun getDetails(id: String, location: Location = Location("")) {
        Log.w("test", "getDetails ${id}")
        telenavService.detailRequest
            .setEntityIds(listOf(id))
            .asyncCall(
                object : Callback<EntityGetDetailResponse> {
                    override fun onSuccess(response: EntityGetDetailResponse) {
                        runOnUiThread {
                            vLoading.hide()
                            if (response.results != null && response.results.size > 0) {
                                showEntityOnMap(response.results[0])
                                showDetails(response.results[0])
                            }
                        }
                    }

                    override fun onFailure(p1: Throwable?) {
                        Log.e("testapp", "onFailure", p1)
                    }
                }
            )
    }

    private fun showDetails(entity: Entity) {
        Log.w("test", Gson().toJson(entity))
        vEntityName.text = entity.place?.name ?: entity.address?.formattedAddress
        vEntityDetails.visibility = View.VISIBLE
        if (entity.type == EntityType.PLACE) {
            vEntityAddress.visibility = View.VISIBLE
            vEntityAddress.text = entity.place.address.formattedAddress
            if (entity.place.phoneNumbers.size > 0) {
                vEntityCall.visibility = View.VISIBLE
                vEntityCallNumber.text = "${entity.place.phoneNumbers[0]}"
                vEntityCallButton.setOnClickListener {
                    val intent = Intent(
                        Intent.ACTION_DIAL,
                        Uri.fromParts("tel", entity.place.phoneNumbers[0], null)
                    )
                    startActivity(intent)
                }
            }
        } else {
            vEntityAddress.visibility = View.VISIBLE
            vEntityAddress.text = entity.address.addressType.toString()
        }
    }

    private fun showEntityOnMap(entity: Entity) {
        val coords =
            entity.address?.geoCoordinates ?: entity.place?.address?.geoCoordinates ?: return
        val name = entity.place?.name ?: entity.address?.formattedAddress ?: ""
        val markerOptions = MarkerOptions()
            .position(LatLng(coords.latitude, coords.longitude))
            .anchor(0.5f, 0.5f)
            .icon(BitmapDescriptorFactory.fromBitmap(createMarker()))
            .title(name)
        map?.apply {
            val marker = addMarker(markerOptions)
            val zoom =
                if (entity.type == EntityType.PLACE) 17f else if (entity.address.addressType == AddressType.STREET) 16f else 13f
            positionMap(coords.latitude, coords.longitude, marker, zoom)
        }
    }

    private fun createMarker(): Bitmap {
        val size = dip(16)
        val centerX = (size / 2).toFloat()
        val centerY = (size / 2).toFloat()
        val newImage = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val c = Canvas(newImage)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = 0xFF5C5C5C.toInt()
        val circleSize = size / 2f
        c.drawCircle(centerY, centerX, circleSize, paint)
        paint.color = 0xFFEFEFEF.toInt()
        c.drawCircle(centerY, centerX, circleSize * 0.9f, paint)

        return newImage
    }

    companion object {
        const val PARAM_ID = "id"
        const val PARAM_ICON = "icon"
    }
}