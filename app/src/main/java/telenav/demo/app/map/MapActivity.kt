package telenav.demo.app.map

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.telenav.sdk.entity.model.base.Entity
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.view_bottom.*
import telenav.demo.app.R
import telenav.demo.app.homepage.HotCategory
import kotlinx.android.synthetic.main.entity_detail_fragment_layout.*
import kotlinx.android.synthetic.main.info_window.view.*
import kotlinx.android.synthetic.main.view_entity_details_bottom.*
import telenav.demo.app.*
import telenav.demo.app.initialization.InitializationActivity
import telenav.demo.app.model.SearchResult
import telenav.demo.app.personalinfo.PersonalInfoActivity
import telenav.demo.app.personalinfo.PersonalInfoFragment
import telenav.demo.app.personalinfo.UserAddressFragment
import telenav.demo.app.search.*
import telenav.demo.app.search.filters.*
import telenav.demo.app.settings.SettingsActivity
import telenav.demo.app.utils.CategoryAndFiltersUtil.hotCategoriesList
import telenav.demo.app.widgets.CategoryView
import java.util.*
import java.util.concurrent.Executor
import android.view.View.OnFocusChangeListener

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
    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            lastKnownLocation = locationResult.lastLocation
            mapFragment?.animateCameraToCurrentLocation(
                LatLng(
                    lastKnownLocation.latitude,
                    lastKnownLocation.longitude
                )
            )
        }
    }
    lateinit var behavior: BottomSheetBehavior<*>
    var lastKnownLocation: Location = Location("")
    private var mapFragment: MapFragment? = null
    private var hotCategoryName = ""
    private var hotCategoryTag = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setupListeners()
        mapFragment = MapFragment()
        showMapFragment(mapFragment!!)
        displayHotCategories()
        displayUserInfo()
        resetFilters()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        fab_search.setOnClickListener { openSearch() }
        app_mode_select.setOnClickListener { showSettingsActivity() }
        app_personal_info.setOnClickListener { showPersonalInfoActivity() }
        user_icon.setOnClickListener {
            collapseBottomSheet()
            showPersonalInfoFragment()
        }

        entity_details_back.setOnClickListener {
            onBackSearchInfoFragment()
        }

        search.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showSearchListBottomFragment()
            }
        }
        search.setOnClickListener { showSearchListBottomFragment() }
    }

    fun onBackSearchInfoFragment() {
        entity_details.visibility = View.GONE
        top_navigation_panel.visibility = View.GONE
        if (navigationFromSearchInfo) {
            showSearchInfoBottomFragment(hotCategoryName, hotCategoryTag)
            navigationFromSearchInfo = true
        } else {
            expandBottomSheet()
        }
    }

    fun onBackFromFilterFragment() {
        showSearchInfoBottomFragment(hotCategoryName, hotCategoryTag)
    }

    private fun showPersonalInfoActivity() {
        startActivity(Intent(this, PersonalInfoActivity::class.java))
    }

    private fun showSettingsActivity() {
        startActivityForResult(Intent(this, SettingsActivity::class.java), CODE_SETTINGS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === CODE_SETTINGS) {
            if (resultCode === Activity.RESULT_OK) {
                if (data != null) {
                    val isChanged = data.getBooleanExtra(IS_ENV_CHANGED, false)
                    if (isChanged) {
                        val toInit = Intent(this@MapActivity, InitializationActivity::class.java)
                        toInit.putExtra(IS_ENV_CHANGED, true)
                        startActivity(toInit)
                        finish()
                    }
                }
            }
        }
    }

    var searchFragment: SearchBottomFragment? = null
    private fun openSearch() {
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
        mapFragment?.addSearchResultsOnMap(it, lastKnownLocation, currentSearchHotCategory)
    }

    fun expandBottomSheet() {
        if (this::behavior.isInitialized) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    fun collapseBottomSheet() {
        if (this::behavior.isInitialized) {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun getCategoryView(hotCategory: HotCategory): CategoryView {
        val categoryView = CategoryView(this)
        categoryView.init(hotCategory)
        categoryView.id = View.generateViewId()

        categoryView.setOnClickListener {
            hotCategoryName = hotCategory.name
            hotCategoryTag = hotCategory.tag
            collapseBottomSheet()
            showSearchInfoBottomFragment(hotCategory.name, hotCategory.tag)
        }

        return categoryView
    }

    private fun displayHotCategories() {
        val bottomSheetLayout = findViewById<ConstraintLayout>(R.id.bottom_sheet)
        val flowLayout = findViewById<Flow>(R.id.flow_main_root)
        val set = ConstraintSet()
        set.clone(bottomSheetLayout)

        val hotCategoryIdArray = ArrayList<Int>()
        for (hotCategory in hotCategoriesList) {
            val categoryView = getCategoryView(hotCategory)
            bottomSheetLayout.addView(categoryView)
            hotCategoryIdArray.add(categoryView.id)
        }

        flowLayout.referencedIds = hotCategoryIdArray.toIntArray()
        behavior = BottomSheetBehavior.from(bottomSheetLayout)
    }

    fun displayUserInfo() {
        supportFragmentManager.beginTransaction().replace(R.id.user_address,
            UserAddressFragment.newInstance()).commit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            searchFragment?.popUpLogicCLose()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun displayEntityClicked(entity: Entity, currentSearchHotCategory: String?,
                             navigationFromSearchInfo: Boolean = false) {
        mapFragment?.addEntityResultOnMap(
            entity,
            lastKnownLocation,
            currentSearchHotCategory
        )
        searchInfoBottomFragment?.dismiss()
        this.navigationFromSearchInfo = navigationFromSearchInfo
    }

    var searchListFragment: SearchHotCategoriesFragment? = null
    fun showSearchHotCategoriesFragment(results: List<Entity>, categoryId: String?) {
        searchListFragment = SearchHotCategoriesFragment.newInstance(results, categoryId)
        searchListFragment!!.show(supportFragmentManager, searchListFragment!!.tag)
    }

    var subcategoriesFragment: CategoriesResultFragment? = null
    fun showSubcategoriesFragment() {
        subcategoriesFragment = CategoriesResultFragment.newInstance()
        subcategoriesFragment!!.show(supportFragmentManager, subcategoriesFragment!!.tag)
    }

    var personalInfoFragment: PersonalInfoFragment? = null
    fun showPersonalInfoFragment() {
        personalInfoFragment = PersonalInfoFragment.newInstance()
        personalInfoFragment!!.show(supportFragmentManager, personalInfoFragment!!.tag)
    }

    var searchInfoBottomFragment: SearchInfoBottomFragment? = null
    private fun showSearchInfoBottomFragment(categoryName: String?, hotCategoryTag: String?) {
        searchInfoBottomFragment = SearchInfoBottomFragment.newInstance(categoryName, hotCategoryTag)
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
    private fun showSearchListBottomFragment() {
        searchListBottomFragment = SearchListBottomFragment.newInstance(hotCategoryTag)
        searchListBottomFragment!!.show(supportFragmentManager, searchListBottomFragment!!.tag)
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

        navigation_header.text = hotCategoryName

        val bottomSheetLayout = findViewById<ConstraintLayout>(R.id.entity_root)
        val behavior = BottomSheetBehavior.from(bottomSheetLayout)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        supportFragmentManager.beginTransaction().replace(R.id.frame_entity_details,
            EntityDetailsFragment.newInstance(searchResult, entity), FRAGMENT_TAG).commit()

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

                val fragment: Fragment? = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
                if (fragment != null && fragment is EntityDetailsFragment) {
                    fragment.updateItemVisibility(slideOffset)
                }
            }
        })

        bottomSheetLayout.setOnClickListener {
            if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    fun hideKeyboard(view: View) {
        view.hideKeyboard()
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
    }
}

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun Activity.getUIExecutor(): Executor {
    return Executor { r -> runOnUiThread(r) }
}