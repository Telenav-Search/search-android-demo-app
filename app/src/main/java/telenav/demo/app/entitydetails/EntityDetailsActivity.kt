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
import com.telenav.sdk.entity.model.base.*
import com.telenav.sdk.entity.model.lookup.EntityGetDetailResponse
import telenav.demo.app.R
import telenav.demo.app.collapse
import telenav.demo.app.dip
import telenav.demo.app.expand
import telenav.demo.app.homepage.getUIExecutor
import java.util.*
import kotlin.collections.ArrayList

class EntityDetailsActivity : AppCompatActivity() {
    private val telenavService: EntityClient by lazy { EntityService.getClient() }

    private lateinit var vLoading: ContentLoadingProgressBar
    private lateinit var vEntityName: TextView
    private lateinit var vEntityDetails: View
    private lateinit var vEntityAddress: TextView
    private lateinit var vEntityCall: Button
    private lateinit var vEntityUrl: View
    private lateinit var vEntityTwitter: View
    private lateinit var vEntityFacebook: View
    private lateinit var vEntityYelp: View
    private lateinit var vEntityIcon: ImageView
    private lateinit var vEntityStars: View
    private lateinit var vEntityRating: TextView
    private lateinit var vEntityToggle: TextView
    private lateinit var vEntityOpenHours: TextView
    private val vEntityStar = ArrayList<ImageView>()
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
        vEntityUrl = findViewById(R.id.entity_details_url)
        vEntityTwitter = findViewById(R.id.entity_details_twitter)
        vEntityFacebook = findViewById(R.id.entity_details_facebook)
        vEntityYelp = findViewById(R.id.entity_details_yelp)
        vEntityIcon = findViewById(R.id.entity_details_icon)
        vEntityStars = findViewById(R.id.entity_stars)
        vEntityStar.add(findViewById(R.id.entity_star1))
        vEntityStar.add(findViewById(R.id.entity_star2))
        vEntityStar.add(findViewById(R.id.entity_star3))
        vEntityStar.add(findViewById(R.id.entity_star4))
        vEntityStar.add(findViewById(R.id.entity_star5))
        vEntityRating = findViewById(R.id.entity_rating)
        vEntityOpenHours = findViewById(R.id.entity_open_hours)
        vEntityToggle = findViewById(R.id.entity_details_toggle)

        vLoading.show()
        if (icon != 0) {
            vEntityIcon.setImageResource(icon)
            vEntityIcon.visibility = View.VISIBLE
        }

        findViewById<View>(R.id.entity_details_back).setOnClickListener { finish() }

        getLocationAndDetails(id)
        initMap()

    }

    private fun setToggler(opened: Boolean) {
        vEntityToggle.visibility = View.VISIBLE
        if (opened) {
            vEntityToggle.text = "COLLAPSE"
            vEntityToggle.setOnClickListener {
                vEntityDetails.collapse()
                setToggler(false)
            }
        } else {
            vEntityToggle.text = "EXPAND"
            vEntityToggle.setOnClickListener {
                vEntityDetails.expand()
                setToggler(true)
            }
        }
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
                getUIExecutor(),
                object : Callback<EntityGetDetailResponse> {
                    override fun onSuccess(response: EntityGetDetailResponse) {
                        Log.w("test", "result ${Gson().toJson(response.results)}")
                        vLoading.hide()
                        if (response.results != null && response.results.size > 0) {
                            showEntityOnMap(response.results[0])
                            showDetails(response.results[0])
                            setToggler(true)
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
        if (entity.type == EntityType.PLACE) {
            vEntityAddress.visibility = View.VISIBLE
            vEntityAddress.text = entity.place.address.formattedAddress

            if (entity.place.phoneNumbers != null && entity.place.phoneNumbers.size > 0) {
                vEntityCall.visibility = View.VISIBLE
                vEntityCall.text = "${entity.place.phoneNumbers[0]}"
                vEntityCall.setOnClickListener {
                    val intent = Intent(
                        Intent.ACTION_DIAL,
                        Uri.fromParts("tel", entity.place.phoneNumbers[0], null)
                    )
                    startActivity(intent)
                }
            }
            if (entity.place.websites != null && entity.place.websites.isNotEmpty()) {
                showWebsites(entity.place.websites)
            }

        } else {
            vEntityAddress.visibility = View.VISIBLE
            vEntityAddress.text = entity.address.addressType.toString()
        }

        if (entity.facets?.rating != null && entity.facets?.rating!!.size > 0) {
            showStars(entity.facets?.rating!![0])
        }

        if (entity.facets?.openHours != null)
            showOpenHours(entity.facets?.openHours!!)

        vEntityDetails.expand()
    }

    private fun showOpenHours(openHours: FacetOpenHours) {
        vEntityOpenHours.visibility = View.VISIBLE
        if (openHours.open24hours == true) {
            vEntityOpenHours.text = "Open Today: 24 hours"
        } else {
            val calendar: Calendar = Calendar.getInstance()
            val today: Int = calendar.get(Calendar.DAY_OF_WEEK)
            val scheduling = openHours.regularOpenHours.find { day -> day.day == today }
            if (scheduling != null) {
                val times =
                    scheduling.openTime.map { dt -> "${dt.from.trimSeconds()} - ${dt.to.trimSeconds()}" }
                vEntityOpenHours.text = "Open Today: ${times.joinToString(", ")}"
            } else
                vEntityOpenHours.text = "Closed"
        }
    }

    private fun showStars(rating: Rating) {
        vEntityStars.visibility = View.VISIBLE
        for (i in 0..5) {
            if (rating.averageRating >= i + 1) {
                vEntityStar[i].setImageResource(R.drawable.ic_star_full)
            } else if (rating.averageRating > i) {
                vEntityStar[i].setImageResource(R.drawable.ic_start_half)
            }
        }

        vEntityRating.text =
            "${String.format("%.1f", rating.averageRating)}  (${rating.totalCount} Reviews)"
        if (rating.url != null) {
            vEntityRating.paintFlags = vEntityRating.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            vEntityStars.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rating.url)))
            }
        }
    }

    private fun showWebsites(websites: List<String>) {
        val siteUrl = websites.find { name ->
            !name.contains("twitter.com") && !name.contains("facebook.com") && !name.contains("yelp.com")
        }
        val twitterUrl = websites.find { name -> name.contains("twitter.com") }
        val facebookUrl = websites.find { name -> name.contains("facebook.com") }
        val yelpUrl = websites.find { name -> name.contains("yelp.com") }

        if (siteUrl != null) {
            vEntityUrl.visibility = View.VISIBLE
            vEntityUrl.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(siteUrl)))
            }
        }
        if (twitterUrl != null) {
            vEntityTwitter.visibility = View.VISIBLE
            vEntityTwitter.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(twitterUrl)))
            }
        }
        if (facebookUrl != null) {
            vEntityFacebook.visibility = View.VISIBLE
            vEntityFacebook.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl)))
            }
        }
        if (yelpUrl != null) {
            vEntityYelp.visibility = View.VISIBLE
            vEntityYelp.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(yelpUrl)))
            }
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

fun String.trimSeconds(): String {
    return if (this.count { it == ':' } > 1)
        this.substring(0, this.lastIndexOf(':'))
    else
        this
}