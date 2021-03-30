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
import com.google.android.gms.maps.model.*
import com.telenav.sdk.entity.model.base.Entity
import kotlinx.android.synthetic.main.fragment_map.*
import telenav.demo.app.R
import telenav.demo.app.entitydetails.EntityDetailFragment
import telenav.demo.app.entitydetails.EntityDetailViewModel
import telenav.demo.app.infoui.CustomInfoWindowAdapter
import telenav.demo.app.model.SearchResult
import telenav.demo.app.utils.CategoryAndFiltersUtil


class MapFragment : Fragment(), GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMarkerClickListener {

    private lateinit var map: SupportMapFragment
    private var googleMap: GoogleMap? = null
    private lateinit var customInfoWindowAdapter: CustomInfoWindowAdapter
    private var searchResultList: MutableList<SearchResult> = mutableListOf()
    private var coordinatesList: MutableList<LatLng> = mutableListOf()
    private lateinit var entityDetailViewModel: EntityDetailViewModel
    private lateinit var entityDetailFragment: EntityDetailFragment
    private lateinit var polygonManager: PolygonManager
    private var alreadyInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMap()
        //TODO decomment for Telenav map
//        (view.findViewById(R.id.map_view) as MapView).initialize(savedInstanceState)


        entityDetailFragment = EntityDetailFragment()
        entityDetailViewModel =
            ViewModelProvider(requireActivity()).get(EntityDetailViewModel::class.java)

        polygonManager = PolygonManager(canvas_to_draw_polygon, this)
    }

    override fun onInfoWindowClick(marker: Marker?) {
        entityDetailViewModel.setSearchResult(searchResultList[marker?.zIndex!!.toInt() - 1])
        entityDetailFragment.show(fragmentManager!!, entityDetailFragment.tag)
        marker.hideInfoWindow()
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        marker?.hideInfoWindow()
        if (searchResultList.isEmpty()) {
            return true
        }
        customInfoWindowAdapter.setSearchResult(searchResultList[marker?.zIndex!!.toInt() - 1])
        marker.showInfoWindow()
        return false
    }

    fun addSearchResultsOnMap(
        searchResults: List<Entity>?,
        currentLocation: Location?,
        currentSearchHotCategory: String?
    ) {

        googleMap?.clear()
        coordinatesList.clear()
        searchResults?.forEach { result ->

            if(result == null) {
                return
            }

            val searchResult =
                CategoryAndFiltersUtil.generateSearchResult(result, currentSearchHotCategory)
            searchResultList.add(searchResult)
            coordinatesList.add(
                LatLng(
                    result.place.address.navCoordinates.latitude,
                    result.place.address.navCoordinates.longitude
                )
            )

            googleMap?.addMarker(
                MarkerOptions()
                    .position(
                        LatLng(
                            result.place.address.navCoordinates.latitude,
                            result.place.address.navCoordinates.longitude
                        )
                    )
                    .title(result.place.name)
                    .zIndex(searchResultList.size.toFloat())
                    .icon(
                        CategoryAndFiltersUtil.bitmapDescriptorFromVector(
                            activity!!,
                            searchResult.iconId
                        )
                    )
            )
        }

        CategoryAndFiltersUtil.placeCameraDependingOnSearchResults(
            googleMap,
            coordinatesList,
            currentLocation
        )
    }

    fun blockMap(block: Boolean) {
        googleMap?.uiSettings?.setAllGesturesEnabled(!block)
    }

    fun animateCameraToCurrentLocation(latLng: LatLng) {
        if (alreadyInitialized) {
            return
        }
        googleMap?.addMarker(MarkerOptions()
            .position(latLng)
            .icon(CategoryAndFiltersUtil.bitmapDescriptorFromVector(activity!!, R.drawable.ic_star_full)))

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

            setInfoWindow()

            googleMap?.setOnPolygonClickListener { it ->
                it.remove()
            }
        }
    }

    private fun setInfoWindow() {
        customInfoWindowAdapter =
            CustomInfoWindowAdapter(layoutInflater.inflate(R.layout.info_window, null))
        googleMap!!.setInfoWindowAdapter(customInfoWindowAdapter)
    }

    private fun positionMap(lat: Double, lon: Double) {
        val latLng = LatLng(lat, lon)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
        googleMap?.moveCamera(cameraUpdate)
    }

    fun getScreenLocation(screenPoint: Point): LatLng? =
        googleMap?.projection?.fromScreenLocation(screenPoint)

    fun addPolygon(pol: PolygonOptions): Polygon? = googleMap?.addPolygon(pol)

    fun onPolygonDrawEnabled(enabled: Boolean) {
        polygonManager.onDrawEnabled(enabled)
    }

    fun getPolygonCoordinates(): List<LatLng>? {
        return polygonManager.getPolygonCoordinates()
    }


}