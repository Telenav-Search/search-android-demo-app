package telenav.demo.app.searchlist

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.Category
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.prediction.Suggestion
import com.telenav.sdk.entity.model.search.CategoryFilter
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.entity.model.search.SearchFilters
import telenav.demo.app.R
import telenav.demo.app.dip
import telenav.demo.app.entitydetails.EntityDetailsActivity
import telenav.demo.app.homepage.HomePageActivity
import telenav.demo.app.homepage.HotCategory
import telenav.demo.app.homepage.getUIExecutor

class SearchListFragment : Fragment() {
    private val telenavService: EntityClient by lazy { EntityService.getClient() }

    private lateinit var vSearchTitle: TextView
    private lateinit var vSearchEmpty: TextView
    private lateinit var vSearchError: TextView
    private lateinit var vSearchLoading: ContentLoadingProgressBar
    private lateinit var vSearchList: RecyclerView
    private lateinit var vSearchListContainer: View
    private lateinit var vSearchIcon: ImageView
    private lateinit var vSearchToggle: TextView
    private lateinit var fMap: SupportMapFragment
    private var map: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_search_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val icon = arguments!!.getInt(PARAM_ICON, 0)
        val categoryId = arguments!!.getString(PARAM_CATEGORY)
        val query = arguments!!.getString(PARAM_QUERY)
        val title = arguments!!.getString(PARAM_TITLE) ?: query ?: ""

        vSearchIcon = view.findViewById(R.id.search_icon)
        vSearchTitle = view.findViewById(R.id.search_title)
        vSearchLoading = view.findViewById(R.id.search_loading)
        vSearchEmpty = view.findViewById(R.id.search_empty)
        vSearchError = view.findViewById(R.id.search_error)
        vSearchList = view.findViewById(R.id.search_list)
        vSearchListContainer = view.findViewById(R.id.search_list_container)
        vSearchToggle = view.findViewById(R.id.search_toggle)
        vSearchLoading.show()

        vSearchTitle.text = title
        vSearchList.layoutManager = LinearLayoutManager(activity)
        if (icon != 0) {
            vSearchIcon.setImageResource(icon)
            vSearchIcon.visibility = View.VISIBLE
        }

        view.findViewById<View>(R.id.search_back)
            .setOnClickListener {
                activity ?: return@setOnClickListener
                (activity as HomePageActivity).removeTopFragment()
            }

        search(query, categoryId)
        initMap()
    }

    private fun setToggler(opened: Boolean) {
        vSearchToggle.visibility = View.VISIBLE
        if (opened) {
            vSearchToggle.text = "COLLAPSE"
            vSearchToggle.setOnClickListener {
                vSearchListContainer.visibility = View.GONE
                setToggler(false)
            }
        } else {
            vSearchToggle.text = "EXPAND"
            vSearchToggle.setOnClickListener {
                vSearchListContainer.visibility = View.VISIBLE
                setToggler(true)
            }
        }
    }

    private fun search(query: String?, categoryId: String?) {
        activity ?: return
        val location = (activity as HomePageActivity).lastKnownLocation ?: Location("");
        telenavService.searchRequest()
            .apply {
                if (query != null)
                    setQuery(query)
            }
            .apply {
                if (categoryId != null)
                    setFilters(
                        SearchFilters.builder()
                            .setCategoryFilter(
                                CategoryFilter.builder().addCategory(categoryId).build()
                            )
                            .build()
                    )
            }
            .setLocation(location.latitude, location.longitude)
            .setLimit(20)
            .asyncCall(
                activity?.getUIExecutor(),
                object : Callback<EntitySearchResponse> {
                    override fun onSuccess(response: EntitySearchResponse) {
                        activity ?: return
                        Log.w("test", Gson().toJson(response.results))
                        vSearchLoading.hide()
                        if (response.results != null && response.results.size > 0) {
                            vSearchList.adapter = SearchListRecyclerAdapter(
                                response.results,
                                arguments!!.getInt(PARAM_ICON, 0)
                            )
                            vSearchListContainer.visibility = View.VISIBLE
                            setToggler(true)
                            showMapEntities(response.results)
                        } else
                            vSearchEmpty.visibility = View.VISIBLE
                    }

                    override fun onFailure(p1: Throwable?) {
                        vSearchLoading.hide()
                        vSearchError.visibility = View.VISIBLE
                        Log.e("testapp", "onFailure", p1)
                    }
                }
            )
    }

    private fun initMap() {
        fMap = SupportMapFragment()
        childFragmentManager.beginTransaction().replace(R.id.search_map, fMap).commit()

        fMap.getMapAsync {
            map = it
            it.mapType = GoogleMap.MAP_TYPE_NORMAL

            try {
                it.isMyLocationEnabled = true
            } catch (e: SecurityException) {
            }
            it.isTrafficEnabled = true

            activity ?: return@getMapAsync
            val location = (activity as HomePageActivity).lastKnownLocation;
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

    private fun showMapEntities(entities: List<Entity>) {
        val boundBuilder = LatLngBounds.Builder()

        entities.forEach { entity ->
            val coords = entity.address?.geoCoordinates ?: entity.place?.address?.geoCoordinates
            if (coords != null) {
                boundBuilder.include(LatLng(coords.latitude, coords.longitude))
            }
        }


        entities.forEachIndexed { index, entity ->
            val coords = entity.address?.geoCoordinates ?: entity.place?.address?.geoCoordinates
            ?: return@forEachIndexed
            val name = entity.place?.name ?: entity.address?.formattedAddress ?: ""
            val markerImage = createMarker("${index + 1}")
            val marker = MarkerOptions()
                .position(LatLng(coords.latitude, coords.longitude))
                .anchor(0.5f, 0.5f)
                .title(name)
                .icon(BitmapDescriptorFactory.fromBitmap(markerImage))
            map?.addMarker(marker)?.tag = entity.id

            map?.setOnInfoWindowClickListener { marker ->
                val id = marker.tag as String
                activity ?: return@setOnInfoWindowClickListener
                startActivity(
                    Intent(activity, EntityDetailsActivity::class.java).apply {
                        putExtra(EntityDetailsActivity.PARAM_ID, id)
                        if (arguments!!.containsKey(PARAM_ICON))
                            putExtra(
                                EntityDetailsActivity.PARAM_ICON,
                                arguments!!.getInt(PARAM_ICON, 0)
                            )
                    })
            }
        }

        val size = resources.displayMetrics.widthPixels
        val bound = boundBuilder.build()
        val space = CameraUpdateFactory.newLatLngBounds(bound, size, size, size / 10)
        map?.animateCamera(space)
    }

    private fun createMarker(text: String): Bitmap {
        val size = activity?.dip(32) ?: 1
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

        paint.color = 0xFF5C5C5C.toInt()
        paint.style = Paint.Style.FILL

        paint.textSize = size / 2f

        val textBoundRect = Rect()
        paint.getTextBounds("00", 0, text.length, textBoundRect)
        val textWidth = paint.measureText(text)
        val textHeight = textBoundRect.height().toFloat()

        c.drawText(
            text,
            centerX - textWidth / 2f,
            centerY + textHeight / 2f,
            paint
        )
        return newImage
    }

    companion object {
        const val PARAM_QUERY = "query"
        const val PARAM_TITLE = "title"
        const val PARAM_ICON = "icon"
        const val PARAM_CATEGORY = "category"

        @JvmStatic
        fun newInstance(query: String) =
            SearchListFragment().apply {
                arguments = Bundle().apply {
                    putString(PARAM_QUERY, query)
                }
            }

        @JvmStatic
        fun newInstance(category: HotCategory) =
            SearchListFragment().apply {
                arguments = Bundle().apply {
                    putString(PARAM_CATEGORY, category.id)
                    putInt(PARAM_ICON, category.icon)
                    putString(PARAM_TITLE, category.name)
                }
            }

        @JvmStatic
        fun newInstance(category: Category) =
            SearchListFragment().apply {
                arguments = Bundle().apply {
                    putString(PARAM_CATEGORY, category.id)
                    putString(PARAM_TITLE, category.name)
                }
            }

        @JvmStatic
        fun newInstance(suggestion: Suggestion) =
            SearchListFragment().apply {
                arguments = Bundle().apply {
                    putString(PARAM_QUERY, suggestion.query)
                    putString(PARAM_TITLE, suggestion.formattedLabel)
                }
            }
    }
}

