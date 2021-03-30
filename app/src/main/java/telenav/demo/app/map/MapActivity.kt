package telenav.demo.app.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.prediction.Suggestion
//import com.telenav.sdk.map.SDK
import kotlinx.android.synthetic.main.activity_map.*
import telenav.demo.app.R
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.personalinfo.PersonalInfoActivity
import telenav.demo.app.search.CategoriesResultFragment
import telenav.demo.app.search.SearchBottomFragment
import telenav.demo.app.search.SearchHotCategoriesFragment
import telenav.demo.app.setGPSListener
import telenav.demo.app.settings.SettingsActivity
import telenav.demo.app.stopGPSListener
import telenav.demo.app.utils.CategoryAndFiltersUtil.getOriginalQuery
import telenav.demo.app.utils.CategoryAndFiltersUtil.hotCategoriesList
import telenav.demo.app.widgets.SearchButton
import java.util.*

class MapActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = MapActivity::class.java.simpleName
        private const val MAP_FRAGMENT_TAG = "MapFragment"
    }

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            lastKnownLocation = locationResult.lastLocation
            mapFragment?.animateCameraToCurrentLocation(
                LatLng(
                    lastKnownLocation!!.latitude,
                    lastKnownLocation!!.longitude
                )
            )
        }
    }
    var lastKnownLocation: Location = Location("")
    private var mapFragment: MapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setupListeners()
        mapFragment = MapFragment()
        showMapFragment(mapFragment!!)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        fab_search.setOnClickListener { openSearch() }
        app_mode_select.setOnClickListener { showSettingsActivity() }
        app_personal_info.setOnClickListener { showPersonalInfoActivity() }
    }

    private fun showPersonalInfoActivity() {
        startActivity(Intent(this, PersonalInfoActivity::class.java))
    }

    private fun showSettingsActivity() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    var searchFragment: SearchBottomFragment? = null
    private fun openSearch() {
        Log.d(MAP_FRAGMENT_TAG, " Click on search")
        searchFragment = SearchBottomFragment()
        searchFragment!!.setSearchType(SearchBottomFragment.CATEGORY_SEARCH)
        searchFragment!!.show(supportFragmentManager, searchFragment!!.tag)
    }

    override fun onResume() {
        super.onResume()
        setGPSListener(locationCallback)
    }

    override fun onPause() {
        stopGPSListener(locationCallback)
        super.onPause()
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count < 2) {
            super.onBackPressed()
            this.finish()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    private fun showMapFragment(fragment: MapFragment) {
        supportFragmentManager.beginTransaction().add(R.id.map_fragment_frame, fragment)
            .addToBackStack(MAP_FRAGMENT_TAG).commit()
    }

    /**
     * --------------- display search results part---------
     *
     * -------- !!!!!!! this methods are called by fragments ---------
     */
    fun displaySearchResults(it: List<Entity>?, currentSearchHotCategory: String?) {

        Log.e(MAP_FRAGMENT_TAG, "results size ${it?.size}")
        mapFragment?.addSearchResultsOnMap(it, lastKnownLocation, currentSearchHotCategory)
    }

    fun displaySuggestion(suggestion: Suggestion) {
        Log.d(MAP_FRAGMENT_TAG, "Suggestion clicked -> ${suggestion.formattedLabel}")

        for (eachHotCategory in hotCategoriesList) {
            if (eachHotCategory.name.toLowerCase(Locale.ROOT).indexOf(getOriginalQuery(suggestion.query).toLowerCase(
                    Locale.ROOT)) != -1) {
                mapFragment?.addSearchResultsOnMap(listOf(suggestion.entity), lastKnownLocation, eachHotCategory.id)
                break
            } else {
                mapFragment?.addSearchResultsOnMap(listOf(suggestion.entity), lastKnownLocation, "")
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            searchFragment?.popUpLogicCLose()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun displayEntityClicked(entity: Entity, currentSearchHotCategory: String?) {
        Log.d(MAP_FRAGMENT_TAG, "Single entity clicked -> ${entity.distance}")
        mapFragment?.addSearchResultsOnMap(
            listOf(entity),
            lastKnownLocation,
            currentSearchHotCategory
        )
    }

    var searchListFragment: SearchHotCategoriesFragment? = null
    fun showSearchHotCategoriesFragment(results: List<Entity>, categoryId: String?) {
        searchListFragment = SearchHotCategoriesFragment.newInstance(results, categoryId)
        searchListFragment!!.show(supportFragmentManager, searchListFragment!!.tag)
    }

    fun showSubcategoriesFragment() {
        val subcategoriesFragment = CategoriesResultFragment.newInstance()
        subcategoriesFragment.show(supportFragmentManager, subcategoriesFragment.tag)
    }

    fun setFilters(filters: List<telenav.demo.app.search.filters.Filter>) {
        Log.d(TAG, "filters: ${filters.count()}")
        searchFragment?.setFilters(filters)
    }
}