package telenav.demo.app.map

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager

import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.search_info_bottom_fragment_layout.search
import kotlinx.android.synthetic.main.view_bottom.*
import kotlinx.android.synthetic.main.view_header_search.*

import com.telenav.sdk.entity.model.base.Entity

import telenav.demo.app.R
import telenav.demo.app.*
import telenav.demo.app.initialization.InitializationActivity
import telenav.demo.app.model.SearchResult
import telenav.demo.app.personalinfo.PersonalInfoFragment
import telenav.demo.app.personalinfo.UserAddressFragment
import telenav.demo.app.search.*
import telenav.demo.app.search.filters.*
import telenav.demo.app.settings.SettingsActivity
import telenav.demo.app.utils.CategoryAndFiltersUtil.hotCategoriesList
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.utils.CategoryAndFiltersUtil.toViewData
import telenav.demo.app.utils.LocationUtil
import telenav.demo.app.widgets.CategoryAdapter

import java.lang.reflect.Type
import java.util.concurrent.Executor

class MapActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = MapActivity::class.java.simpleName
        private const val MAP_FRAGMENT_TAG = "MapFragment"
        private const val CODE_SETTINGS = 3
        const val IS_ENV_CHANGED = "IS_ENV_CHANGED"
        private val FRAGMENT_TAG = "EntityDetailsFragment"
    }

    private var lastSearch: String = ""
    private var navigationFromSearchInfo = false
    private var navigationFromPersonalInfo = false
    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            gpsLocation = locationResult.lastLocation
            val cameraLocation = cvpLocation ?: gpsLocation
            mapFragment?.animateCameraToCurrentLocation(
                LatLng(
                    cameraLocation.latitude,
                    cameraLocation.longitude
                )
            )
        }
    }
    lateinit var behavior: BottomSheetBehavior<*>
    lateinit var entityDetailsBehavior: BottomSheetBehavior<*>
    private var gpsLocation: Location = Location("")
    private var cvpLocation: Location? = Location("") // null means use gpsLocation
    private var saLocation: Location? = Location("") // null means use gpsLocation
    private var mapFragment: MapFragment? = null
    private var hotCategoryName = ""
    private var hotCategoryTag = ""
    private var bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setupListeners()
        mapFragment = MapFragment()
        showMapFragment(mapFragment!!)
        displayHotCategories()
        displayUserInfo()
        displayRecentSearchInfo()
        resetFilters()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        user_icon.setOnClickListener {
            collapseBottomSheet()
            showPersonalInfoFragment()
        }

        entity_details_back.setOnClickListener {
            onBackSearchInfoFragment()
        }

        entity_header_clear.setOnClickListener {
            onBackSearchInfoFragment()
        }

        search.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showSearchListBottomFragment()
            }
        }
        search.setOnClickListener { showSearchListBottomFragment() }
    }

    fun onBackFromFilterFragment() {
        showSearchInfoBottomFragment(hotCategoryName, hotCategoryTag, false)
    }

    private fun onBackSearchInfoFragment() {
        entity_details.visibility = View.GONE
        top_navigation_panel.visibility = View.GONE
        view_bottom.visibility = View.VISIBLE
        when {
            navigationFromSearchInfo -> {
                showSearchInfoBottomFragment(hotCategoryName, hotCategoryTag, true)
                navigationFromSearchInfo = false
            }
            navigationFromPersonalInfo -> {
                showPersonalInfoFragment()
                navigationFromPersonalInfo = false
            }
            else -> {
                updateBottomSheetState()
            }
        }
    }

    fun showSettingsActivity() {
        startActivityForResult(Intent(this, SettingsActivity::class.java), CODE_SETTINGS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === CODE_SETTINGS) {
            // update CVP Locations when back from settings
            updateLocationsFromSP()
            if (resultCode === Activity.RESULT_OK) {
                if (data != null) {
                    val isChanged = data.getBooleanExtra(IS_ENV_CHANGED, false)
                    if (isChanged) {
                        finish()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onResume() {
        super.onResume()
        setGPSListener(locationCallback)
        updateLocationsFromSP()
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
        mapFragment?.addSearchResultsOnMap(it, getSearchAreaLocation(), currentSearchHotCategory)
    }

    fun updateBottomSheetState() {
        if (this::behavior.isInitialized) {
            behavior.state = bottomSheetState
        }
    }

    fun clearTextAndFocus() {
        search.setText("")
        search.clearFocus()
    }

    fun collapseBottomSheet() {
        if (this::behavior.isInitialized) {
            if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED ||
                behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetState = behavior.state
            }
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun displayHotCategories() {
        val bottomSheetLayout = findViewById<ConstraintLayout>(R.id.bottom_sheet)

        // setup recyclerView
        val recyclerView = bottomSheetLayout.findViewById<RecyclerView>(R.id.category_container)
        recyclerView.layoutManager = GridLayoutManager(this, CategoryAndFiltersUtil.DISPLAY_LIMIT,
            GridLayoutManager.VERTICAL, false)
        recyclerView.adapter = CategoryAdapter { position, _ ->
            // onClick call back, get name and tag from position, then start search fragment
            val category = hotCategoriesList[position]
            hotCategoryName = category.name
            hotCategoryTag = category.tag
            collapseBottomSheet()
            if (hotCategoryTag.isNotEmpty()) {
                showSearchInfoBottomFragment(category.name, category.tag, false)
            } else {
                showMoreCategoriesFragment()
            }
        }.apply {
            // feed data into adapter
            setData(hotCategoriesList.map { it.toViewData() })
        }

        behavior = BottomSheetBehavior.from(bottomSheetLayout)
    }

    fun updateBottomView() {
        displayUserInfo()
        displayRecentSearchInfo()
    }

    private fun displayUserInfo() {
        supportFragmentManager.beginTransaction().replace(R.id.user_address,
            UserAddressFragment.newInstance()).commit()
    }

    private fun displayRecentSearchInfo() {
        val entities: List<Entity>? = getRecentSearchData()
        supportFragmentManager.beginTransaction().replace(R.id.search_recent_data,
            RecentSearchListFragment.newInstance(entities)).commit()

        extend.setOnClickListener {
            collapseBottomSheet()
            showRecentSearchFullListFragment()
        }

        if (entities.isNullOrEmpty()) {
            search_recent_data.visibility = View.GONE
            extend.visibility = View.GONE
            search_recent_data_header.visibility = View.GONE
        } else {
            search_recent_data.visibility = View.VISIBLE
            extend.visibility = View.VISIBLE
            search_recent_data_header.visibility = View.VISIBLE
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            searchListBottomFragment?.popUpLogicCLose()
            if (top_navigation_panel.visibility == View.VISIBLE) {
                onBackSearchInfoFragment()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun displayEntityClicked(entity: Entity, currentSearchHotCategory: String?,
                             navigationFromSearchInfo: Boolean = false,
                             navigationFromPersonalInfo: Boolean = false) {
        mapFragment?.addEntityResultOnMap(
            entity,
            getSearchAreaLocation(),
            currentSearchHotCategory
        )
        searchInfoBottomFragment?.dismiss()
        this.navigationFromSearchInfo = navigationFromSearchInfo
        this.navigationFromPersonalInfo = navigationFromPersonalInfo
    }

    var personalInfoFragment: PersonalInfoFragment? = null
    fun showPersonalInfoFragment() {
        personalInfoFragment = PersonalInfoFragment.newInstance()
        personalInfoFragment!!.show(supportFragmentManager, personalInfoFragment!!.tag)
    }

    var searchInfoBottomFragment: SearchInfoBottomFragment? = null
    fun showSearchInfoBottomFragment(categoryName: String?, hotCategoryTag: String?, shouldLoadSaveData: Boolean) {
        categoryName?.let { this.hotCategoryName = it }
        searchInfoBottomFragment = SearchInfoBottomFragment.newInstance(categoryName, hotCategoryTag, shouldLoadSaveData)
        searchInfoBottomFragment!!.show(supportFragmentManager, searchInfoBottomFragment!!.tag)
    }

    var parkingFiltersFragment: ParkingFiltersFragment? = null
    fun showParkingFiltersFragment() {
        parkingFiltersFragment = ParkingFiltersFragment.newInstance()
        parkingFiltersFragment!!.show(supportFragmentManager, parkingFiltersFragment!!.tag)
    }

    var evFiltersFragment: EvFiltersFragment? = null
    fun showEvFiltersFragment() {
        evFiltersFragment = EvFiltersFragment.newInstance()
        evFiltersFragment!!.show(supportFragmentManager, evFiltersFragment!!.tag)
    }

    var generalFiltersFragment: GeneralFiltersFragment? = null
    fun showGeneralFiltersFragment() {
        generalFiltersFragment = GeneralFiltersFragment.newInstance()
        generalFiltersFragment!!.show(supportFragmentManager, generalFiltersFragment!!.tag)
    }

    var searchListBottomFragment: SearchListBottomFragment? = null
    fun showSearchListBottomFragment() {
        searchListBottomFragment = SearchListBottomFragment.newInstance(hotCategoryTag)
        searchListBottomFragment!!.show(supportFragmentManager, searchListBottomFragment!!.tag)
    }

    var recentSearchFullListFragment: RecentSearchFullListFragment? = null
    private fun showRecentSearchFullListFragment() {
        recentSearchFullListFragment = RecentSearchFullListFragment.newInstance(getRecentSearchData())
        recentSearchFullListFragment!!.show(supportFragmentManager, recentSearchFullListFragment!!.tag)
    }

    var moreCategoriesFragment: MoreCategoriesFragment? = null
    private fun showMoreCategoriesFragment() {
        moreCategoriesFragment = MoreCategoriesFragment.newInstance()
        moreCategoriesFragment!!.show(supportFragmentManager, moreCategoriesFragment!!.tag)
    }

    fun showSearchListBottomFragmentFromUserAddress(
        shouldUpdateWorkAddress: Boolean = false,
        shouldUpdateHomeAddress: Boolean = false) {
        searchListBottomFragment = if (personalInfoFragment?.isAdded == true) {
            personalInfoFragment?.dismiss()
            SearchListBottomFragment.newInstance(
                hotCategoryTag, shouldUpdateWorkAddress,
                shouldUpdateHomeAddress, false)
        } else {
            SearchListBottomFragment.newInstance(
                hotCategoryTag, shouldUpdateWorkAddress,
                shouldUpdateHomeAddress, true)
        }
        searchListBottomFragment!!.show(supportFragmentManager, searchListBottomFragment!!.tag)
    }

    fun setLastSearch(lastSearch: String) {
        this.lastSearch = lastSearch
    }

    fun showEntityDetails(searchResult: SearchResult, entity: Entity) {
        entity_details.visibility = View.VISIBLE
        top_navigation_panel.visibility = View.VISIBLE
        view_bottom.visibility = View.GONE

        navigation_header.text = hotCategoryName

        val bottomSheetLayout = findViewById<ConstraintLayout>(R.id.entity_root)
        entityDetailsBehavior= BottomSheetBehavior.from(bottomSheetLayout)
        entityDetailsBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        supportFragmentManager.beginTransaction().replace(R.id.frame_entity_details,
            EntityDetailsFragment.newInstance(searchResult, entity), FRAGMENT_TAG).commit()

        entityDetailsBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

                val fragment: Fragment? = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
                if (fragment != null && fragment is EntityDetailsFragment) {
                    fragment.updateItemVisibility(slideOffset)
                }
            }
        })

        bottomSheetLayout.setOnClickListener {
            if (entityDetailsBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                entityDetailsBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    fun collapseEntityDetails() {
        if (this::entityDetailsBehavior.isInitialized) {
            entityDetailsBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    fun hideKeyboard(view: View) {
        view.hideKeyboard()
    }
    private fun getRecentSearchData(): List<Entity>? {
        val prefs =
            getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

        val listType: Type = object : TypeToken<List<Entity>>() {}.type
        val recentSearchEntities: List<Entity>? =  Gson().fromJson(
            prefs?.getString(
                getString(R.string.saved_recent_search_key),
                ""
            ), listType
        )

        recentSearchEntities?.let {
            return  it
        }
        return null
    }

    private fun resetFilters() {
        App.writeToSharedPreferences(App.RATE_STARS, Stars.DEFAULT.starsNumber)
        App.writeToSharedPreferences(App.PRICE_LEVEL, PriceLevelType.DEFAULT.priceLevel)
        App.writeBooleanToSharedPreferences(App.OPEN_TIME, false)
        App.writeBooleanToSharedPreferences(App.RESERVED, false)
        App.writeStringToSharedPreferences(App.CONNECTION_TYPES, "")
        App.writeStringToSharedPreferences(App.CHARGER_BRAND, "")
        App.writeStringToSharedPreferences(App.POWER_FEED, "")
        App.writeToSharedPreferences(App.PARKING_DURATION, 0)
        App.writeToSharedPreferences(App.PARKING_START_FROM, 0)
    }

    private fun updateLocationsFromSP() {
        val sp = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        // read settings from sp
        val cvpFollowGPS = sp.getBoolean(App.KEY_CVP_GPS, true)
        if (cvpFollowGPS) {
            cvpLocation = null
        } else {
            val lat = sp.getFloat(App.KEY_CVP_LAT, LocationUtil.DEFAULT_LAT)
            val long = sp.getFloat(App.KEY_CVP_LONG, LocationUtil.DEFAULT_LONG)
            cvpLocation = Location("").apply {
                latitude = lat.toDouble()
                longitude = long.toDouble()
            }
        }

        val salFollowCVP = sp.getBoolean(App.KEY_SAL_CVP, true)
        val salFollowGPS = sp.getBoolean(App.KEY_SAL_GPS, true)
        saLocation = when {
            salFollowCVP -> cvpLocation
            salFollowGPS -> null
            else -> {
                val lat = sp.getFloat(App.KEY_SAL_LAT, LocationUtil.DEFAULT_LAT)
                val long = sp.getFloat(App.KEY_SAL_LONG, LocationUtil.DEFAULT_LONG)
                Location("").apply {
                    latitude = lat.toDouble()
                    longitude = long.toDouble()
                }
            }
        }
    }

    fun getCVPLocation(): Location = cvpLocation ?: gpsLocation

    fun getSearchAreaLocation(): Location = saLocation ?: gpsLocation
}

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun Activity.getUIExecutor(): Executor {
    return Executor { r -> runOnUiThread(r) }
}