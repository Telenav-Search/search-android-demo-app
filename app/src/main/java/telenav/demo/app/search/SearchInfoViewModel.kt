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
import com.telenav.sdk.entity.model.base.ParkingParameters
import com.telenav.sdk.entity.model.base.FacetParameters
import com.telenav.sdk.entity.model.discover.EntityGetCategoriesResponse
import com.telenav.sdk.entity.model.prediction.EntitySuggestionPredictionResponse
import com.telenav.sdk.entity.model.prediction.EntityWordPredictionResponse
import com.telenav.sdk.entity.model.prediction.WordPrediction

import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.search.filters.*
import telenav.demo.app.utils.*

import kotlin.collections.ArrayList

import java.lang.reflect.Type
import java.util.concurrent.Executor

private const val TAG = "SearchInfoViewModel"

const val SEARCH_INFO_LIMIT_WITH_FILTERS = 30
const val PREDICTIONS_LIMIT = 5

@Suppress("DEPRECATION")
class SearchInfoViewModel : ViewModel() {

    private val telenavEntityClient: EntityClient by lazy { EntityService.getClient() }
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }
    var searchResults = MutableLiveData<List<Entity>>().apply { listOf<Entity>() }
    var suggestionResults = MutableLiveData<List<Entity>>().apply { listOf<Entity>() }
    var savedAddress = MutableLiveData<List<Entity>>().apply { listOf<Entity>() }
    var searchError = MutableLiveData<String>().apply { postValue("") }
    var loading = MutableLiveData<Boolean>().apply { postValue(false) }
    val categories = MutableLiveData<List<Category>>().apply { listOf<Any>() }
    var predictions = MutableLiveData<List<WordPrediction>>().apply { listOf<WordPrediction>() }

    fun explore(
        query: String?,
        categoryTag: String?,
        cvpLocation: Location,
        executor: Executor,
        searchAreaLocation: Location? = null,
        nearLeft: LatLng? = null,
        farRight: LatLng? = null
    ) {
        //explore
        val exploreIntent =
            App.readStringFromSharedPreferences(App.KEY_EXPLORE_INTENT, "0")!!.toInt()
        val exploreEnabled = App.readBooleanFromSharedPreferences(App.KEY_EXPLORE_ENABLED, false)
        if (!exploreEnabled) {
            searchResults.postValue(emptyList())
            return
        }
        var exploreCommand = "";
        if (query?.startsWith("explore:") == false) {
            when (exploreIntent) {
                0 -> exploreCommand = "explore:poi"
                1 -> exploreCommand = "explore:address"
                2 -> exploreCommand = "explore:all"
            }
        } else {
            exploreCommand = query!!
        }
        search(exploreCommand, categoryTag, cvpLocation, executor, searchAreaLocation,null,true,nearLeft,farRight)
    }

    fun search(
        query: String?,
        categoryTag: String?,
        cvpLocation: Location,
        executor: Executor,
        searchAreaLocation: Location? = null,
        filterCategory: String? = null,
        filtersAvailable: Boolean = false,
        nearLeft: LatLng? = null,
        farRight: LatLng? = null
    ) {

        loading.postValue(true)
        searchError.postValue("")

        val filtersSearch = SearchFilters.builder()
        var limit = App.readStringFromSharedPreferences(
            App.SEARCH_LIMIT,
            SEARCH_INFO_LIMIT_WITH_FILTERS.toString()
        )!!.toInt();
        //explore
        val exploreLimit = App.readIntFromSharedPreferences(App.KEY_EXPLORE_LIMIT, 10)
        val radius = App.readIntFromSharedPreferences(App.KEY_EXPLORE_RADIUS, 3000)
        val exploreEnabled = App.readBooleanFromSharedPreferences(App.KEY_EXPLORE_ENABLED, false)
        val doExploreFlag = query?.startsWith("explore:") == true && exploreEnabled
        if (doExploreFlag) {
            filtersSearch.setGeoFilter(RadiusGeoFilter.builder(radius).build())
            limit = exploreLimit
        }

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
                    val connectionTypes = getConnectorTypes()
                    val chargeBrands = getChargerBrands()
                    val powerFeed = getPowerFeedLevels()

                    if (!connectionTypes.isNullOrEmpty()) {
                        builder.setConnectorTypes(
                            connectionTypes.split(",").filter { it.isNotEmpty() })
                    }

                    if (!chargeBrands.isNullOrEmpty()) {
                        builder.setChargerBrands(chargeBrands.split(",").filter { it.isNotEmpty() })
                    }

                    if (!powerFeed.isNullOrEmpty()) {
                        builder.setPowerFeedLevels(powerFeed.split(",").filter { it.isNotEmpty() }
                            .map { it.toInt() })
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
                    val parkingDuration = App.readIntFromSharedPreferences(
                        App.PARKING_DURATION,
                        0
                    ) * 60 // convert hours to minutes
                    val entryTime = App.readStringFromSharedPreferences(App.PARKING_START_FROM, "")
                    Log.i(
                        TAG,
                        "search: parking params, entryTime = $entryTime, duration = $parkingDuration"
                    )
                    if (parkingDuration != 0 || !entryTime.isNullOrEmpty()) {
                        when {
                            entryTime.isNullOrEmpty() -> {
                                val facetParameters = FacetParameters.builder()
                                    .setParkingParameters(
                                        ParkingParameters.builder()
                                            .setDuration(parkingDuration)
                                            .build()
                                    ).build()
                                setFacetParameters(facetParameters)
                            }
                            parkingDuration == 0 -> {
                                val facetParameters = FacetParameters.builder()
                                    .setParkingParameters(
                                        ParkingParameters.builder()
                                            .setEntryTime(entryTime)
                                            .build()
                                    ).build()
                                setFacetParameters(facetParameters)
                            }
                            else -> {
                                val facetParameters = FacetParameters.builder()
                                    .setParkingParameters(
                                        ParkingParameters.builder()
                                            .setDuration(parkingDuration)
                                            .setEntryTime(entryTime)
                                            .build()
                                    ).build()
                                setFacetParameters(facetParameters)
                            }
                        }
                    }
                }
            }.apply {
                // setAnchor when search area location is specified
                searchAreaLocation?.let { setAnchor(it.latitude, it.longitude) }
            }
            .setLocation(cvpLocation.latitude, cvpLocation.longitude)
            .setLimit(limit)
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
                            requestSubcategories(
                                categoryTag,
                                searchAreaLocation ?: cvpLocation,
                                executor,
                                filtersAvailable,
                                filterCategory,
                                response.results
                            )
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
            .setLocation(location.latitude, location.longitude)
            .setLimit(App.readFromSharedPreferences(App.FILTER_NUMBER))
            .setCategory(categoryTag)
            .asyncCall(
                executor,
                object : Callback<EntityDiscoverCategoryResponse?> {
                    override fun onSuccess(response: EntityDiscoverCategoryResponse?) {
                        // log response in JSON format
                        Log.d("TAG", EntityJsonConverter.toPrettyJson(response))
                        categories.value = response?.results as List<Category>
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

    fun requestSuggestions(text: String, location: Location, executor: Executor) {
        searchError.postValue("")
        telenavEntityClient.suggestionPredictionRequest()
            .setQuery(text)
            .setLocation(location.latitude, location.longitude)
            .setLimit(
                App.readStringFromSharedPreferences(
                    App.SUGGESTIONS_LIMIT,
                    SUGGESTIONS_LIMIT_DEF.toString()
                )!!.toInt()
            )
            .asyncCall(executor,
                object : Callback<EntitySuggestionPredictionResponse> {
                    override fun onSuccess(response: EntitySuggestionPredictionResponse) {
                        val entities = ArrayList<Entity>()
                        for (result in response.results) {
                            if (result.entity != null) {
                                entities.add(result.entity)
                            }
                        }

                        if (!entities.isNullOrEmpty()) {
                            suggestionResults.postValue(entities)
                        }
                    }

                    override fun onFailure(error: Throwable) {
                        Log.e(TAG, "", error)
                    }
                })
    }

    fun requestPrediction(text: String, location: Location, executor: Executor) {
        telenavEntityClient.wordPredictionRequest()
            .setQuery(text)
            .setLocation(location.latitude, location.longitude)
            .setLimit(
                App.readStringFromSharedPreferences(
                    App.PREDICTIONS_LIMIT,
                    PREDICTIONS_LIMIT.toString()
                )!!.toInt()
            )
            .asyncCall(executor,
                object : com.telenav.sdk.core.Callback<EntityWordPredictionResponse> {
                    override fun onSuccess(response: EntityWordPredictionResponse) {
                        predictions.postValue(response.results)
                    }

                    override fun onFailure(p1: Throwable?) {
                        Log.e("testapp", "onFailure prediction ${text}", p1)
                    }
                }
            )
    }

    fun requestCategories(executor: Executor) {
        loading.postValue(true)
        searchError.postValue("")
        telenavEntityClient.categoriesRequest.asyncCall(executor,
            object : Callback<EntityGetCategoriesResponse> {
                override fun onSuccess(response: EntityGetCategoriesResponse) {
                    loading.postValue(false)
                    categories.value = response.results as List<Category>
                }

                override fun onFailure(p1: Throwable?) {
                    loading.postValue(false)
                    searchError.postValue(p1?.message)
                    Log.e("testapp", "", p1)
                }
            }
        )
    }

    private fun handleSearchResponse(
        filtersAvailable: Boolean,
        filterCategory: String?,
        results: List<Entity>
    ) {
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
            !filterCategory.equals(CategoryAndFiltersUtil.EV_CHARGER_TAG)
        ) {
            val starsNumber = getRateStars()
            if (starsNumber != Stars.DEFAULT.starsNumber) {
                filteredResults = filterByStar(results, starsNumber)
            }
        } else if (filterCategory.equals(CategoryAndFiltersUtil.PARKING_TAG) &&
            !filterCategory.equals(CategoryAndFiltersUtil.EV_CHARGER_TAG)
        ) {
            val priceNumber = getPriceLevel()
            if (priceNumber != PriceLevelType.DEFAULT.priceLevel) {
                filteredResults = filterByPrice(results, priceNumber)
            }
        }

        /*if (filteredResults.size > App.readFromSharedPreferences(App.FILTER_NUMBER)) {
            return filteredResults.subList(
                0,
                App.readFromSharedPreferences(App.FILTER_NUMBER)
            )
        }*/
        return filteredResults
    }

    fun getRecentSearchData(context: Context): List<Entity> {
        val prefs =
            context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

        val listType: Type = object : TypeToken<List<Entity>>() {}.type
        return Gson().fromJson(
            prefs?.getString(
                context.getString(R.string.saved_recent_search_key),
                "[]"
            ), listType
        )
    }

    fun saveRecentSearchData(context: Context) {
        val prefs =
            context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

        with(prefs.edit()) {
            putString(
                context.getString(R.string.saved_recent_search_key),
                Gson().toJson(searchResults.value)
            )
            apply()
        }
    }

    fun setRecentSearchData(context: Context) {
        searchResults.value = getRecentSearchData(context)
    }

    fun getRecentCategoryData(context: Context): List<Category> {
        val prefs =
            context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

        val listType: Type = object : TypeToken<List<Category>>() {}.type
        return Gson().fromJson(
            prefs?.getString(
                context.getString(R.string.saved_recent_category_key),
                ""
            ), listType
        )
    }

    fun saveRecentCategoryData(context: Context) {
        val prefs =
            context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

        with(prefs.edit()) {
            putString(
                context.getString(R.string.saved_recent_category_key),
                Gson().toJson(categories.value)
            )
            apply()
        }
    }

    fun setRecentCategoryData(context: Context) {
        categories.value = getRecentCategoryData(context)
    }

    fun getHome(context: Context): Entity? {
        val homeEntity = dataCollectorClient.getHome(context)
        homeEntity?.let {
            savedAddress.value = listOf(it)
        }
        return homeEntity

    }

    fun getWork(context: Context): Entity? {
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

    private fun getConnectorTypes(): String? {
        val strConnectorTypes = App.readStringFromSharedPreferences(App.CONNECTION_TYPES, "")

        if (strConnectorTypes == null || strConnectorTypes.isEmpty()) {
            return null;
        }
        return strConnectorTypes.split(",").joinToString(separator = ",") {
            CategoryAndFiltersUtil.connectorTypesMap.getOrDefault(
                it,
                ""
            )
        }
    }

    private fun getChargerBrands(): String? {
        val strChargeBrands = App.readStringFromSharedPreferences(App.CHARGER_BRAND, "")
        if (strChargeBrands == null || strChargeBrands.isEmpty()) {
            return null;
        }
        return strChargeBrands.split(",").joinToString(separator = ",") {
            CategoryAndFiltersUtil.chargerBrandsMap.getOrDefault(
                it,
                ""
            )
        }
    }

    private fun getPowerFeedLevels(): String? {
        val strPowerFeedLevels = App.readStringFromSharedPreferences(App.POWER_FEED, "")
        if (strPowerFeedLevels == null || strPowerFeedLevels.isEmpty()) {
            return null;
        }
        return strPowerFeedLevels.split(",").joinToString(separator = ",") {
            CategoryAndFiltersUtil.powerFeedLevelsMap.getOrDefault(
                it,
                ""
            )
        }
    }
}
