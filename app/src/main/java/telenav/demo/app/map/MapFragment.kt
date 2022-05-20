package telenav.demo.app.map

import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
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
import telenav.demo.app.entitydetails.EntityDetailViewModel
import telenav.demo.app.model.SearchResult
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.utils.entityClick

class MapFragment : Fragment(), GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    private lateinit var map: SupportMapFragment
    private var googleMap: GoogleMap? = null
    private var searchResultList: MutableList<SearchResult> = mutableListOf()
    private var entitiesList: MutableList<Entity> = mutableListOf()
    private var coordinatesList: MutableList<LatLng> = mutableListOf()
    private lateinit var entityDetailViewModel: EntityDetailViewModel
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }
    private var alreadyInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMap()
        entityDetailViewModel =
            ViewModelProvider(requireActivity()).get(EntityDetailViewModel::class.java)
    }

    override fun onInfoWindowClick(marker: Marker?) {
        marker?.zIndex?.let {
            dataCollectorClient.entityClick(
                App.readStringFromSharedPreferences(
                    App.LAST_ENTITY_RESPONSE_REF_ID, ""
                ) ?: "",
                entitiesList[it.toInt() - 1].id,
                EntityActionEvent.DisplayMode.MAP_VIEW
            )
            entityDetailViewModel.setSearchResult(searchResultList[it.toInt() - 1])
            marker.hideInfoWindow()
        }
    }

    fun getRegion(): VisibleRegion? {
        return googleMap?.projection?.visibleRegion
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        marker?.zIndex?.let {
            val index = it.toInt() - 1
            if (index < searchResultList.size) {
                val searchResult = searchResultList[index]
                if (index < entitiesList.size) {
                    val entity = entitiesList[index]
                    (activity!! as MapActivity).showEntityDetails(searchResult, entity)
                }
            }
        }

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
            (activity!! as MapActivity).showEntityDetails(searchResult, entity)
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

            val location = (activity!! as MapActivity).getCVPLocation()
            positionMap(location.latitude, location.longitude)

            googleMap?.setOnMarkerClickListener(this)
            googleMap?.setOnInfoWindowClickListener(this)
            googleMap?.setOnMapClickListener(this)
        }
    }

    private fun positionMap(lat: Double, lon: Double) {
        val latLng = LatLng(lat, lon)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
        googleMap?.moveCamera(cameraUpdate)
    }

    fun getScreenLocation(screenPoint: Point): LatLng? =
        googleMap?.projection?.fromScreenLocation(screenPoint)

    override fun onMapClick(p0: LatLng?) {
        (activity!! as MapActivity).collapseEntityDetails()
    }
}