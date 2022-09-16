package telenav.demo.app.map

import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datacollector.model.event.EntityActionEvent
import com.telenav.sdk.entity.model.base.Entity
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.databinding.FragmentMapBinding
import telenav.demo.app.entitydetails.EntityDetailViewModel
import telenav.demo.app.model.SearchResult
import telenav.demo.app.search.SearchInfoViewModel
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.utils.entityClick

class MapFragment : Fragment(), GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener {

    private lateinit var map: SupportMapFragment
    private var googleMap: GoogleMap? = null
    private var searchResultList: MutableList<SearchResult> = mutableListOf()
    private var entitiesList: MutableList<Entity> = mutableListOf()
    private var coordinatesList: MutableList<LatLng> = mutableListOf()
    private lateinit var entityDetailViewModel: EntityDetailViewModel
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }
    private var alreadyInitialized = false
    private val viewModel: SearchInfoViewModel by viewModels()
    private var polylineList: MutableList<Polyline> = mutableListOf()



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

    private fun clearMarkers() {
        googleMap?.clear()
        coordinatesList.clear()
    }

    fun getRegion(): VisibleRegion? {
        return googleMap?.projection?.visibleRegion
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        marker?.zIndex?.let {
            val index = it.toInt() - 1
            if (index in searchResultList.indices) {
                val searchResult = searchResultList[index]
                if (index < entitiesList.size) {
                    val entity = entitiesList[index]
                    (requireActivity() as MapActivity).showEntityDetails(searchResult, entity)
                }
            }
        }

        return false
    }

    fun addSearchResultsAndPolygonOnMap(
        searchResults: List<Entity>?,
        currentLocation: Location?,
        currentSearchHotTag: String?,
        polygon: PolygonOptions?
    ) {

        googleMap?.clear()
        coordinatesList.clear()
        searchResults?.forEach { result ->
            addSearchResultsOnMap(result, currentSearchHotTag)
        }
        polygon?.let { addPolygon(it) }

        CategoryAndFiltersUtil.placeCameraDependingOnSearchResults(
            googleMap,
            coordinatesList,
            currentLocation
        )
    }

    fun addSearchResultsOnMap(
        searchResults: List<Entity>?
    ) {
        searchResults?.forEach { result ->
            addSearchResultsOnMap(result, "")
        }

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
        shouldOpenEntityDetails: Boolean = false
    ) {

        val geoPoint = when {
            entity.place != null -> entity.place.address.navCoordinates
            entity.address != null -> entity.address.geoCoordinates
            else -> return
        }
        val title = when {
            entity.place != null -> entity.place.name
            entity.address != null -> entity.address.formattedAddress
            else -> return
        }
        entitiesList.add(entity)
        val searchResult =
            CategoryAndFiltersUtil.generateSearchResult(entity, currentSearchHotTag)
        searchResultList.add(searchResult)
        coordinatesList.add(LatLng(geoPoint.latitude, geoPoint.longitude))

        googleMap?.addMarker(
            MarkerOptions()
                .position(LatLng(geoPoint.latitude, geoPoint.longitude))
                .title(title)
                .zIndex(searchResultList.size.toFloat())
                .icon(
                    CategoryAndFiltersUtil.bitmapDescriptorFromVector(
                        requireActivity(),
                        searchResult.iconId
                    )
                )
        )

        if (shouldOpenEntityDetails) {
            (requireActivity() as MapActivity).showEntityDetails(searchResult, entity)
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
                        requireActivity(),
                        R.drawable.ic_star_full
                    )
                )
        )

        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0f))

        alreadyInitialized = true
    }

    fun moveToCurrentLocation(latLng: LatLng) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0f))
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
            it.isTrafficEnabled = false

            val location = (requireActivity() as MapActivity).getCVPLocation()
            positionMap(location.latitude, location.longitude)

            googleMap?.setOnMarkerClickListener(this)
            googleMap?.setOnInfoWindowClickListener(this)
            googleMap?.setOnMapClickListener(this)
            googleMap?.setOnMapLongClickListener(this)
            googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.hide_landmark
                )
            )
            googleMap?.uiSettings?.isMapToolbarEnabled = true
        }
    }

    private fun positionMap(lat: Double, lon: Double) {
        val latLng = LatLng(lat, lon)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
        googleMap?.moveCamera(cameraUpdate)
    }

    fun getScreenLocation(screenPoint: Point): LatLng? =
        googleMap?.projection?.fromScreenLocation(screenPoint)

    override fun onMapClick(latlon: LatLng?) {
        Log.i("MapFragment","map click.")
        (requireActivity() as MapActivity).collapseEntityDetails()
        (requireActivity() as MapActivity).getPolylineOption()
            .add(latlon)
        if((requireActivity() as MapActivity).isTouchEnabled()){

            googleMap?.addPolyline((requireActivity() as MapActivity).getPolylineOption())
                ?.let { polylineList.add(it) }
        }

    }

    fun addPolygon(polygon: PolygonOptions): Polygon? {
        return googleMap?.addPolygon(polygon)
    }

    fun removeAllPolyline(){
        for (line in polylineList) {
            line.remove()
        }
        polylineList.clear()
    }

    override fun onMapLongClick(latlon: LatLng?) {
        Log.i("MapFragment","Long click.")
        clearMarkers()
        googleMap?.addMarker(
            latlon?.let {
                MarkerOptions()
                    .position(it)
                    .icon(
                        CategoryAndFiltersUtil.bitmapDescriptorFromVector(
                            requireActivity(),
                            R.drawable.ic_star_full
                        )
                    )
            }
        )
        var location = Location("")
        latlon?.let {
            positionMap(it.latitude, latlon.longitude)
            location.latitude = it.latitude
            location.longitude = it.longitude
        }
        var lastId = viewModel?.searchResults?.value?.get(0)?.id.toString()
        this.activity?.getUIExecutor().let {
            it?.let { it1 ->
                viewModel.explore("", null, location, it1, location)

            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) {

            if(lastId!=viewModel?.searchResults?.value?.get(0)?.id.toString()){
                addSearchResultsOnMap(viewModel.searchResults.value)
            }

        }

    }


}