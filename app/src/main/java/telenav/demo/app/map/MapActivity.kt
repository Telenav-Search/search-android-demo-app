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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.prediction.Suggestion
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.view_bottom.*
import telenav.demo.app.R
import telenav.demo.app.initialization.InitializationActivity
import telenav.demo.app.personalinfo.PersonalInfoActivity
import telenav.demo.app.personalinfo.PersonalInfoFragment
import telenav.demo.app.search.CategoriesResultFragment
import telenav.demo.app.search.SearchBottomFragment
import telenav.demo.app.search.SearchHotCategoriesFragment
import telenav.demo.app.search.filters.Filter
import telenav.demo.app.setGPSListener
import telenav.demo.app.settings.SettingsActivity
import telenav.demo.app.stopGPSListener
import telenav.demo.app.utils.CategoryAndFiltersUtil.getOriginalQuery
import telenav.demo.app.utils.CategoryAndFiltersUtil.hotCategoriesList
import telenav.demo.app.widgets.CategoryView
import java.util.*
import java.util.concurrent.Executor

class MapActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = MapActivity::class.java.simpleName
        private const val MAP_FRAGMENT_TAG = "MapFragment"
        private const val CODE_SETTINGS = 3
        const val IS_ENV_CHANGED = "IS_ENV_CHANGED"
    }

    private var filters: List<Filter>? = null
    private var lastSearch: String = ""
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
        displayHotCategories()
    }

    fun redoButtonLogic() {
        if (lastSearch.isEmpty()) {
            redo_button.visibility = View.GONE
        } else {
            redo_button.visibility = View.VISIBLE
        }
        redo_button.setOnClickListener {
            enableDisableRedoButton(false)
            val region = mapFragment?.getRegion()
            if (filters != null) {
                mapFragment?.setFilters(filters!!)
            } else {
                mapFragment?.setFilters(null)
            }
            try {
                lastSearch.toInt()
                mapFragment?.searchInRegion(null, lastSearch, lastKnownLocation, getUIExecutor(),
                        region?.nearLeft, region?.farRight)
            } catch (e: Exception) {
                mapFragment?.searchInRegion(lastSearch, null, lastKnownLocation, getUIExecutor(),
                        region?.nearLeft, region?.farRight)
            }
        }
    }

    fun enableDisableRedoButton(enable: Boolean) {
        redo_button.isEnabled = enable
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        fab_search.setOnClickListener { openSearch() }
        app_mode_select.setOnClickListener { showSettingsActivity() }
        app_personal_info.setOnClickListener { showPersonalInfoActivity() }
        user_icon.setOnClickListener { showPersonalInfoFragment() }
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
        filters = null
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

    fun displaySuggestion(suggestion: Suggestion) {
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

    private fun displayHotCategories() {
        val bottomSheetLayout = findViewById<ConstraintLayout>(R.id.bottom_sheet)
        val flowLayout = findViewById<Flow>(R.id.flow_main_root)
        val set = ConstraintSet()
        set.clone(bottomSheetLayout)

        val hotCategoryIdArray = ArrayList<Int>()

        for (eachHotCategory in hotCategoriesList) {
            val categoryView = CategoryView(this)
            categoryView.init(eachHotCategory)
            categoryView.id = View.generateViewId()

            bottomSheetLayout.addView(categoryView)
            hotCategoryIdArray.add(categoryView.id)
        }
        flowLayout.referencedIds = hotCategoryIdArray.toIntArray()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            searchFragment?.popUpLogicCLose()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun displayEntityClicked(entity: Entity, currentSearchHotCategory: String?) {
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

    var subcategoriesFragment: CategoriesResultFragment? = null
    fun showSubcategoriesFragment() {
        subcategoriesFragment = CategoriesResultFragment.newInstance()
        subcategoriesFragment!!.show(supportFragmentManager, subcategoriesFragment!!.tag)
    }

    var personalInfoFragment: PersonalInfoFragment? = null
    private fun showPersonalInfoFragment() {
        personalInfoFragment = PersonalInfoFragment.newInstance()
        personalInfoFragment!!.show(supportFragmentManager, personalInfoFragment!!.tag)
    }

    fun setFiltersSub() {
        if (filters != null) {
            subcategoriesFragment?.setFilters(filters!!)
        }
    }

    fun setFilters(filters: List<telenav.demo.app.search.filters.Filter>) {
        this.filters = filters
        searchFragment?.setFilters(filters)
    }

    fun setLastSearch(lastSearch: String) {
        this.lastSearch = lastSearch
    }
}

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun Activity.getUIExecutor(): Executor {
    return Executor { r -> runOnUiThread(r) }
}