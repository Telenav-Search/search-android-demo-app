package telenav.demo.app.search

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.*
import com.telenav.sdk.entity.model.discover.EntityDiscoverCategoryResponse
import com.telenav.sdk.entity.model.search.*
import com.telenav.sdk.entity.utils.EntityJsonConverter
import telenav.demo.app.App
import java.util.concurrent.Executor
import telenav.demo.app.R
import telenav.demo.app.search.filters.*
import java.lang.reflect.Type
import telenav.demo.app.utils.*
import com.telenav.sdk.entity.model.base.ParkingParameters
import com.telenav.sdk.entity.model.base.FacetParameters

private const val TAG = "SearchInfoViewModel"

const val SEARCH_INFO_LIMIT_WITH_FILTERS = 30

@Suppress("DEPRECATION")
class SearchInfoViewModel : ViewModel() {

    private val telenavEntityClient: EntityClient by lazy { EntityService.getClient() }
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }
    var searchResults = MutableLiveData<List<Entity>>().apply { listOf<Entity>() }
    var savedAddress = MutableLiveData<List<Entity>>().apply { listOf<Entity>() }
    var searchError = MutableLiveData<String>().apply { postValue("") }
    var loading = MutableLiveData<Boolean>().apply { postValue(false) }
    val categories = MutableLiveData<List<Category>>().apply { listOf<Any>() }

    fun search(
        query: String?,
        categoryTag: String?,
        location: Location,
        executor: Executor,
        filterCategory: String? = null,
        filtersAvailable: Boolean = false,
        nearLeft: LatLng? = null,
        farRight: LatLng? = null) {

        loading.postValue(true)
        searchError.postValue("")

        val filtersSearch = SearchFilters.builder()
        if (categoryTag != null) {
            filtersSearch.setCategoryFilter(
                    CategoryFilter.builder().addCategory(categoryTag).build()
            )
        }
        if (nearLeft != null && farRight != null) {
            val bBox: BBox = BBox
                    .builder()
                    .setBottomLeft(nearLeft.latitude, nearLeft.longitude)
                    .setTopRight(farRight.latitude, farRight.longitude)
                    .build()
            val geoFilter: BBoxGeoFilter = BBoxGeoFilter
                    .builder(bBox)
                    .build()
            filtersSearch.setGeoFilter(geoFilter)
        }

        if (filtersAvailable) {
            when {
                filterCategory.equals(CategoryAndFiltersUtil.PARKING_TAG) -> {
                    val builder: BusinessFilter.Builder = BusinessFilter.builder()
                    var shouldApplyFilter = false
                    if (isOpened()) {
                        shouldApplyFilter = true
                        builder.setNewlyOpen()
                    }
                    if (isReserved()) {
                        shouldApplyFilter = true
                        builder.setReservation()
                    }

                    if (shouldApplyFilter) {
                        filtersSearch.setBusinessFilter(builder.build())
                    }
                }
                filterCategory.equals(CategoryAndFiltersUtil.EV_CHARGER_TAG) -> {
                    val builder: EvFilter.Builder = EvFilter.builder()
                    val connectionTypes = getConnectionTypes()
                    val chargeBrands = getChargerBrands()
                    val powerFeed = getPowerFeed()

                    if (!connectionTypes.isNullOrEmpty()) {
                        builder.setConnectorTypes(connectionTypes.split(","))
                    }

                    if (!chargeBrands.isNullOrEmpty()) {
                        builder.setChargerBrands(chargeBrands.split(","))
                    }

                    if (!powerFeed.isNullOrEmpty()) {
                        builder.setChargerBrands(powerFeed.split(","))
                    }

                    builder.setFreeCharge(isFreeCharger())
                    filtersSearch.setEvFilter(builder.build())
                }
                else -> {
                    val builder: BusinessFilter.Builder = BusinessFilter.builder()
                    if (isOpened()) {
                        builder.setNewlyOpen()
                        filtersSearch.setBusinessFilter(builder.build())
                    }
                }
            }
        }

        telenavEntityClient.searchRequest()
            .apply {
                if (query != null)
                    setQuery(query)
            }.apply {
                setFilters(filtersSearch.build())
            }.apply {
                if (filtersAvailable && filterCategory.equals(CategoryAndFiltersUtil.PARKING_TAG)) {
                    val parkingDuration = App.readIntFromSharedPreferences(App.PARKING_DURATION, 0)
                    if (parkingDuration != 0) {
                        val facetParameters = FacetParameters.builder().setParkingParameters(
                            ParkingParameters.builder().setDuration(parkingDuration).build()).build()
                        setFacetParameters(facetParameters)
                    }
                }
            }
            .setLocation(location.longitude, location.latitude)
            .setLimit(App.readStringFromSharedPreferences(App.SEARCH_LIMIT,
                SEARCH_INFO_LIMIT_WITH_FILTERS.toString())!!.toInt())
            .asyncCall(
                executor,
                object : Callback<EntitySearchResponse> {
                    override fun onSuccess(response: EntitySearchResponse) {
                        Log.w(TAG, Gson().toJson(response.results))
                        App.writeStringToSharedPreferences(
                            App.LAST_ENTITY_RESPONSE_REF_ID,
                            response.referenceId
                        )
                        if (categories.value.isNullOrEmpty()) {
                            requestSubcategories(categoryTag,
                                location, executor, filtersAvailable, filterCategory, response.results)
                        } else {
                            handleSearchResponse(filtersAvailable, filterCategory, response.results)
                        }
                    }

                    override fun onFailure(p1: Throwable?) {
                        loading.postValue(false)
                        searchError.postValue(p1?.message)

                        Log.e(TAG, "onFailure", p1)
                    }
                }
            )
    }

    fun requestSubcategories(
        categoryTag: String?,
        location: Location,
        executor: Executor,
        filtersAvailable: Boolean,
        filterCategory: String?,
        results: List<Entity>
    ) {
        telenavEntityClient.discoverCategoryRequest()
            .setLocation(location.longitude, location.latitude)
            .setLimit(App.readFromSharedPreferences(App.FILTER_NUMBER))
            .setCategory(categoryTag)
            .asyncCall(
                executor,
                object : Callback<EntityDiscoverCategoryResponse?> {
                override fun onSuccess(response: EntityDiscoverCategoryResponse?) {
                    // log response in JSON format
                    Log.d("TAG", EntityJsonConverter.toPrettyJson(response))
                    categories.value= response?.results as List<Category>
                    handleSearchResponse(filtersAvailable, filterCategory, results)
                }

                override fun onFailure(t: Throwable?) {
                    handleSearchResponse(filtersAvailable, filterCategory, results)
                    Log.e(
                        "TAG",
                        "Get unsuccessful response or throwable happened when executing the request."
                    )
                }
            })
    }

    private fun handleSearchResponse(filtersAvailable: Boolean, filterCategory: String?, results: List<Entity>) {
        loading.postValue(false)
        if (filtersAvailable) {
            searchResults.value = applyFilters(results, filterCategory)
        } else {
            searchResults.postValue(results)
        }
    }

    private fun filterByOpen(results: List<Entity>): ArrayList<Entity> {
        val filteredResults = arrayListOf<Entity>()
        results.forEach { entity ->
            entity.facets.openHours?.isOpenNow?.let {
                filteredResults.add(entity)
            }
        }
        return filteredResults
    }

    private fun filterByStar(results: List<Entity>, starsNumber: Int): ArrayList<Entity> {
        val filteredResults = arrayListOf<Entity>()
        results.forEach { entity ->
            if (entity.facets.rating != null && entity.facets.rating.size > 0
                    && entity.facets.rating[0].averageRating - starsNumber.toDouble() >= 0.0 &&
                    entity.facets.rating[0].averageRating - starsNumber.toDouble() < 1.0
            ) {
                filteredResults.add(entity)
            }
        }
        return filteredResults
    }

    private fun filterByPrice(results: List<Entity>, priceLevel: Int): ArrayList<Entity> {
        val filteredResults = arrayListOf<Entity>()
        results.forEach { entity ->
            if (entity.facets?.priceInfo?.priceLevel == priceLevel) {
                filteredResults.add(entity)
            }
        }
        return filteredResults
    }

    private fun applyFilters(results: List<Entity>, filterCategory: String?): List<Entity> {
        var filteredResults = results.toMutableList()
        if (filteredResults.size == 0) {
            return filteredResults
        }

        if (!filterCategory.equals(CategoryAndFiltersUtil.PARKING_TAG) &&
            !filterCategory.equals(CategoryAndFiltersUtil.EV_CHARGER_TAG)) {
            val starsNumber = getRateStars()
            if (starsNumber != Stars.DEFAULT.starsNumber) {
                filteredResults = filterByStar(results, starsNumber)
            }
        } else if (filterCategory.equals(CategoryAndFiltersUtil.PARKING_TAG) &&
            !filterCategory.equals(CategoryAndFiltersUtil.EV_CHARGER_TAG)) {
            val priceNumber = getPriceLevel()
            if (priceNumber != PriceLevelType.DEFAULT.priceLevel) {
                filteredResults = filterByPrice(results, priceNumber)
            }
        }

        if (filteredResults.size > App.readFromSharedPreferences(App.FILTER_NUMBER)) {
            return filteredResults.subList(
                0,
                App.readFromSharedPreferences(App.FILTER_NUMBER)
            )
        }
        return filteredResults
    }

    fun getRecentSearchData(context: Context) {
        val prefs =
            context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE)

        val listType: Type = object : TypeToken<List<Entity>>() {}.type
        val searchResultEntities = Gson().fromJson<List<Entity>>(
            prefs?.getString(
                context.getString(R.string.saved_recent_search_key),
                ""
            ), listType
        )
        searchResultEntities?.let {
            searchResults.value = it
        }
    }

    fun saveRecentSearchData(context: Context) {
        val prefs =
            context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE)

        with(prefs.edit()) {
            putString(
                context.getString(R.string.saved_recent_search_key),
                Gson().toJson(searchResults.value)
            )
            apply()
        }
    }

    fun getHome(context: Context) : Entity? {
        val homeEntity = dataCollectorClient.getHome(context)
        homeEntity?.let {
            savedAddress.value = listOf(it)
        }
        return homeEntity

    }

    fun getWork(context: Context) : Entity? {
        val workEntity = dataCollectorClient.getWork(context)
        workEntity?.let {
            savedAddress.value = listOf(it)
        }
        return workEntity
    }

    private fun getRateStars(): Int {
        return App.readIntFromSharedPreferences(App.RATE_STARS, Stars.DEFAULT.starsNumber)
    }

    private fun getPriceLevel(): Int {
        return App.readIntFromSharedPreferences(App.PRICE_LEVEL, PriceLevelType.DEFAULT.priceLevel)
    }

    private fun isOpened(): Boolean {
        return App.readBooleanFromSharedPreferences(App.OPEN_TIME, false)
    }

    private fun isReserved(): Boolean {
        return App.readBooleanFromSharedPreferences(App.RESERVED, false)
    }

    private fun isFreeCharger(): Boolean {
        return App.readBooleanFromSharedPreferences(App.FREE_CHARGER, false)
    }

    private fun getConnectionTypes(): String? {
        return App.readStringFromSharedPreferences(App.CONNECTION_TYPES, "")
    }

    private fun getChargerBrands(): String? {
        return App.readStringFromSharedPreferences(App.CHARGER_BRAND, "")
    }

    private fun getPowerFeed(): String? {
        return App.readStringFromSharedPreferences(App.POWER_FEED, "")
    }
}
