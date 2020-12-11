package telenav.demo.app.entitydetails

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.google.gson.reflect.TypeToken
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.*
import com.telenav.sdk.entity.model.lookup.EntityGetDetailResponse
import telenav.demo.app.R
import telenav.demo.app.dip
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.utils.addFavorite
import telenav.demo.app.utils.deleteFavorite
import telenav.demo.app.utils.setHome
import telenav.demo.app.utils.setWork
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList

class EntityDetailsActivity : AppCompatActivity() {
    private val telenavService: EntityClient by lazy { EntityService.getClient() }
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }

    private lateinit var vLoading: ContentLoadingProgressBar
    private lateinit var vEntitySetHomeTitle: TextView
    private lateinit var vEntitySetWorkTitle: TextView
    private lateinit var vEntityFavorite: ImageView
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
    private lateinit var vEntityParkings: LinearLayout
    private lateinit var vEntityPrices: LinearLayout
    private lateinit var vEntityConnectors: LinearLayout
    private lateinit var vEntityYelpSign: View
    private val vEntityStar = ArrayList<ImageView>()
    private lateinit var fMap: SupportMapFragment
    private var map: GoogleMap? = null
    private var lastKnowLocation: Location? = null

    private var entity: Entity? = null
    private var isFavorite: Boolean = false
    private var isHome: Boolean = false
    private var isWork: Boolean = false


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
        vEntityYelpSign = findViewById(R.id.entity_yelp_sign)
        vEntityStar.add(findViewById(R.id.entity_star1))
        vEntityStar.add(findViewById(R.id.entity_star2))
        vEntityStar.add(findViewById(R.id.entity_star3))
        vEntityStar.add(findViewById(R.id.entity_star4))
        vEntityStar.add(findViewById(R.id.entity_star5))
        vEntityRating = findViewById(R.id.entity_rating)
        vEntityOpenHours = findViewById(R.id.entity_open_hours)
        vEntityToggle = findViewById(R.id.entity_details_toggle)
        vEntityConnectors = findViewById(R.id.entity_connectors)
        vEntityPrices = findViewById(R.id.entity_prices)
        vEntityParkings = findViewById(R.id.entity_parking)
        vEntitySetHomeTitle = findViewById(R.id.entity_details_set_home_title)
        vEntitySetWorkTitle = findViewById(R.id.entity_details_set_work_title)
        vEntityFavorite = findViewById(R.id.entity_favorite)

        vLoading.show()
        if (icon != 0) {
            vEntityIcon.setImageResource(icon)
            vEntityIcon.visibility = View.VISIBLE
        }

        findViewById<View>(R.id.entity_details_back).setOnClickListener { finish() }

        findViewById<View>(R.id.entity_details_set_home).setOnClickListener {
            setAsHomeAddress(
                entity
            )
        }
        findViewById<View>(R.id.entity_details_set_work).setOnClickListener {
            setAsWorkAddress(
                entity
            )
        }
        vEntityFavorite.setOnClickListener { toggleFavorite(entity) }

        getLocationAndDetails(id)
        initMap()

    }

    private fun setAsHomeAddress(entity: Entity?) {
        entity ?: return
        if (!isHome) {
            dataCollectorClient.setHome(this, entity)
            vEntitySetHomeTitle.text = getString(R.string.home_title)
        }
    }

    private fun setAsWorkAddress(entity: Entity?) {
        entity ?: return
        if (!isWork) {
            dataCollectorClient.setWork(this, entity)
            vEntitySetWorkTitle.text = getString(R.string.work_title)
        }
    }

    private fun toggleFavorite(entity: Entity?) {
        entity ?: return
        if (isFavorite) {
            dataCollectorClient.deleteFavorite(this, entity)
        } else {
            dataCollectorClient.addFavorite(this, entity)
        }
        checkFavorite(entity)
    }

    private fun setToggler(opened: Boolean) {
        vEntityToggle.visibility = View.VISIBLE
        if (opened) {
            vEntityToggle.text = "COLLAPSE"
            vEntityToggle.setOnClickListener {
                vEntityDetails.visibility = View.GONE
                setToggler(false)
            }
        } else {
            vEntityToggle.text = "EXPAND"
            vEntityToggle.setOnClickListener {
                vEntityDetails.visibility = View.VISIBLE
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
                            entity = response.results[0]
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

        if (entity.facets?.evConnectors != null) {
            showEVConnectors(entity.facets?.evConnectors!!)
        }
        if (entity.facets?.priceInfo != null) {
            showPriceDetails(entity.facets?.priceInfo!!)
        }
        if (entity.facets?.parking != null) {
            showParking(entity.facets?.parking!!)
        }

        Log.w("test", "parkin: " + Gson().toJson(entity.facets?.parking))

        if (entity.facets?.openHours != null)
            showOpenHours(entity.facets?.openHours!!)

        checkAddress(entity)
        checkFavorite(entity)

        vEntityDetails.visibility = View.VISIBLE
    }

    private fun checkAddress(entity: Entity) {
        val prefs =
            getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val storedHome = Gson().fromJson(
            prefs.getString(getString(R.string.saved_home_address_key), ""),
            Entity::class.java
        )
        val storedWork = Gson().fromJson(
            prefs.getString(getString(R.string.saved_work_address_key), ""),
            Entity::class.java
        )
        if (storedHome != null && storedHome.id == entity.id) {
            vEntitySetHomeTitle.text = getString(R.string.home_title)
            isHome = true
        }
        if (storedWork != null && storedWork.id == entity.id) {
            vEntitySetWorkTitle.text = getString(R.string.work_title)
            isHome = false
        }
    }

    private fun checkFavorite(entity: Entity) {
        val prefs =
            getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val listType: Type = object : TypeToken<List<Entity>>() {}.type
        val favoriteEntities = Gson().fromJson<List<Entity>>(
            prefs.getString(
                getString(R.string.saved_favorite_list_key),
                ""
            ), listType
        )

        isFavorite =
            if (favoriteEntities != null && favoriteEntities.any { e -> e.id == entity.id }) {
                vEntityFavorite.setImageResource(R.drawable.ic_favorite)
                true
            } else {
                vEntityFavorite.setImageResource(R.drawable.ic_favorite_border)
                false
            }
    }

    private fun showOpenHours(openHours: FacetOpenHours) {
        vEntityOpenHours.visibility = View.VISIBLE
        if (openHours.open24hours == true) {
            vEntityOpenHours.text = "Open Today: 24 hours"
        } else {
            val calendar: Calendar = Calendar.getInstance()
            var today: Int = calendar.get(Calendar.DAY_OF_WEEK) - 1
            if (today == 0)
                today = 7 //adjustment to match SDK week days
            val scheduling = openHours.regularOpenHours.find { day -> day.day == today }
            if (scheduling != null) {
                val times =
                    scheduling.openTime.map { dt ->
                        if (dt.from == "00:00:00" && dt.to == "24:00:00")
                            "24 hours"
                        else
                            "${dt.from.trimSeconds().convertHour()} - " +
                                    "${dt.to.trimSeconds().convertHour()}"
                    }
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

        vEntityYelpSign.visibility = if (rating.source == "YELP") View.VISIBLE else View.GONE

        vEntityRating.text =
            "${String.format("%.1f", rating.averageRating)}  (${rating.totalCount} Reviews)"
        if (rating.url != null) {
            vEntityRating.paintFlags = vEntityRating.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            vEntityStars.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rating.url)))
            }
        }
    }

    private fun showEVConnectors(evConnectors: FacetEvConnectors) {
        val connectors = evConnectors.connectors
        if (connectors.size == 0)
            return
        vEntityConnectors.visibility = View.VISIBLE
        connectors.forEach { connector ->
            val view = TextView(this)
            view.text =
                "${connector.powerFeedLevel.name}: total ${connector.connectorNumber ?: 0}, available ${connector.available ?: 0}"
            view.setTextColor(0xFFFFFFFF.toInt())
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            view.setPadding(dip(5), dip(3), dip(5), dip(3))
            view.ellipsize = TextUtils.TruncateAt.END
            view.setLines(1)
            vEntityConnectors.addView(
                view,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

        }
    }

    private fun showPriceDetails(priceInfo: FacetPriceInfo) {
        val prices = priceInfo.priceDetails ?: return
        if (prices.size == 0)
            return
        vEntityPrices.visibility = View.VISIBLE
        prices.forEach { price ->
            val view = TextView(this)
            view.text =
                "${price.label}: ${price.currency} ${String.format("%.3f", price.amount)} / gal"
            view.setTextColor(0xFFFFFFFF.toInt())
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            view.setPadding(dip(5), dip(3), dip(5), dip(3))
            view.ellipsize = TextUtils.TruncateAt.END
            view.setLines(1)
            vEntityPrices.addView(
                view,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

        }
    }

    private fun showParking(parking: Parking) {
        vEntityParkings.visibility = View.VISIBLE
        val prices = parking.pricing?.prices

        val uniqPrices = ArrayList<ParkingPriceItem>()

        prices?.forEach { price ->
            if (uniqPrices.find { uniq -> uniq.unitText == price.unitText } == null) {
                uniqPrices.add(price)
            }
        }

        uniqPrices.forEachIndexed { index, price ->
            if (index > 3)
                return@forEachIndexed
            val view = TextView(this)
            view.text =
                "Price: ${price.symbol} ${String.format("%.1f", price.amount)} / ${price.unitText}"
            view.setTextColor(0xFFFFFFFF.toInt())
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            view.setPadding(dip(5), dip(3), dip(5), dip(3))
            view.ellipsize = TextUtils.TruncateAt.END
            view.setLines(1)
            vEntityParkings.addView(
                view,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        if (parking.spacesTotal != null) {
            val view = TextView(this)
            view.text = "Total spaces: ${parking.spacesTotal}"
            view.setTextColor(0xFFFFFFFF.toInt())
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            view.setPadding(dip(5), dip(3), dip(5), dip(3))
            view.ellipsize = TextUtils.TruncateAt.END
            view.setLines(1)
            vEntityParkings.addView(
                view,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            if (parking.spacesAvailable != null) {
                val view = TextView(this)
                view.text = "Available: ${parking.spacesAvailable}"
                view.setTextColor(0xFFFFFFFF.toInt())
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                view.setPadding(dip(5), dip(3), dip(5), dip(3))
                view.ellipsize = TextUtils.TruncateAt.END
                view.setLines(1)
                vEntityParkings.addView(
                    view,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
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

fun String.convertHour(): String {
    try {
        var h = this.substring(0, this.lastIndexOf(':')).toInt()
        val ampm = if (h <= 12) "AM" else "PM"
        if (h > 12)
            h -= 12

        var half = if (this.endsWith(":00")) "" else this.substring(this.lastIndexOf(':'))

        return "${h}${half} ${ampm}"
    } catch (e: Exception) {
        return this
    }
}