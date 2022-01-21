package telenav.demo.app.map

import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.VisibleRegion
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datacollector.model.event.EntityActionEvent
import com.telenav.sdk.entity.model.base.Entity
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.databinding.FragmentMapBinding
import telenav.demo.app.entitydetails.EntityDetailFragment
import telenav.demo.app.entitydetails.EntityDetailViewModel
import telenav.demo.app.infoui.CustomInfoWindowAdapter
import telenav.demo.app.model.SearchResult
import telenav.demo.app.search.SearchViewModel
import telenav.demo.app.search.filters.Filter
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.utils.entityClick
import java.util.concurrent.Executor

class MapFragment : Fragment(), GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMarkerClickListener {

    private lateinit var map: SupportMapFragment
    private var googleMap: GoogleMap? = null
    private lateinit var customInfoWindowAdapter: CustomInfoWindowAdapter
    private var searchResultList: MutableList<SearchResult> = mutableListOf()
    private var entitiesList: MutableList<Entity> = mutableListOf()
    private var coordinatesList: MutableList<LatLng> = mutableListOf()
    private lateinit var entityDetailViewModel: EntityDetailViewModel
    private lateinit var entityDetailFragment: EntityDetailFragment
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }
    private val viewModel: SearchViewModel by viewModels()
    private var alreadyInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMapBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMap()
        entityDetailFragment = EntityDetailFragment()
        entityDetailViewModel =
            ViewModelProvider(requireActivity()).get(EntityDetailViewModel::class.java)
    }

    fun searchInRegion(
        query: String?,
        categoryId: String?,
        location: Location,
        executor: Executor,
        nearLeft: LatLng? = null,
        farRight: LatLng? = null
    ) {
        viewModel.search(query, categoryId, location, executor, nearLeft, farRight)
    }

    fun setFilters(filters: List<Filter>?) {
        viewModel.filters = filters
    }

    override fun onInfoWindowClick(marker: Marker?) {
        dataCollectorClient.entityClick(
            App.readStringFromSharedPreferences(
                App.LAST_ENTITY_RESPONSE_REF_ID, ""
            ) ?: "",
            entitiesList[marker?.zIndex!!.toInt() - 1].id,
            EntityActionEvent.DisplayMode.MAP_VIEW
        )
        entityDetailViewModel.setSearchResult(searchResultList[marker?.zIndex!!.toInt() - 1])
        //entityDetailFragment.show(fragmentManager!!, entityDetailFragment.tag)
        marker.hideInfoWindow()
    }

    fun getRegion(): VisibleRegion? {
        return googleMap?.projection?.visibleRegion
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        (activity!! as MapActivity).showEntityDetails(searchResultList[marker?.zIndex!!.toInt() - 1])
        return false
    }

    fun addSearchResultsOnMap(
        searchResults: List<Entity>?,
        currentLocation: Location?,
        currentSearchHotTag: String?
    ) {

        googleMap?.clear()
        coordinatesList.clear()
        searchResults?.forEach { result ->
            addSearchResultsOnMap(result, currentSearchHotTag)
        }

        CategoryAndFiltersUtil.placeCameraDependingOnSearchResults(
            googleMap,
            coordinatesList,
            currentLocation
        )
    }

    fun addEntityResultOnMap(
        entity: Entity,
        currentLocation: Location?,
        currentSearchHotTag: String?
    ) {

        googleMap?.clear()
        coordinatesList.clear()
        addSearchResultsOnMap(entity, currentSearchHotTag, true)
        CategoryAndFiltersUtil.placeCameraDependingOnSearchResults(
            googleMap,
            coordinatesList,
            currentLocation
        )
    }

    private fun addSearchResultsOnMap(
        entity: Entity,
        currentSearchHotTag: String?,
        shouldOpenEntityDetails: Boolean = false) {

        if (entity.place == null) {
            return
        }
        entitiesList.add(entity)
        val searchResult =
            CategoryAndFiltersUtil.generateSearchResult(entity, currentSearchHotTag)
        searchResultList.add(searchResult)
        coordinatesList.add(
            LatLng(
                entity.place.address.navCoordinates.latitude,
                entity.place.address.navCoordinates.longitude
            )
        )

        googleMap?.addMarker(
            MarkerOptions()
                .position(
                    LatLng(
                        entity.place.address.navCoordinates.latitude,
                        entity.place.address.navCoordinates.longitude
                    )
                )
                .title(entity.place.name)
                .zIndex(searchResultList.size.toFloat())
                .icon(
                    CategoryAndFiltersUtil.bitmapDescriptorFromVector(
                        activity!!,
                        searchResult.iconId
                    )
                )
        )

        if (shouldOpenEntityDetails) {
            (activity!! as MapActivity).showEntityDetails(searchResult)
        }
    }

    fun blockMap(block: Boolean) {
        googleMap?.uiSettings?.setAllGesturesEnabled(!block)
    }

    fun animateCameraToCurrentLocation(latLng: LatLng) {
        if (alreadyInitialized) {
            return
        }
        googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .icon(
                    CategoryAndFiltersUtil.bitmapDescriptorFromVector(
                        activity!!,
                        R.drawable.ic_star_full
                    )
                )
        )

        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0f))

        alreadyInitialized = true
    }

    private fun initMap() {
        map = SupportMapFragment()
        childFragmentManager.beginTransaction().add(R.id.map_fragment, map).commit()

        map.getMapAsync {
            googleMap = it
            it.mapType = GoogleMap.MAP_TYPE_NORMAL

            try {
                it.isMyLocationEnabled = true
            } catch (e: SecurityException) {
            }
            it.isTrafficEnabled = true

            val location = (activity!! as MapActivity).lastKnownLocation;
            positionMap(location.latitude, location.longitude)

            googleMap?.setOnMarkerClickListener(this)
            googleMap?.setOnInfoWindowClickListener(this)
        }
    }

    private fun positionMap(lat: Double, lon: Double) {
        val latLng = LatLng(lat, lon)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
        googleMap?.moveCamera(cameraUpdate)
    }

    fun getScreenLocation(screenPoint: Point): LatLng? =
        googleMap?.projection?.fromScreenLocation(screenPoint)
}