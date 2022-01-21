package telenav.demo.app.search

import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.*
import com.telenav.sdk.entity.model.discover.EntityDiscoverBrandResponse
import com.telenav.sdk.entity.model.discover.EntityDiscoverCategoryResponse
import com.telenav.sdk.entity.model.discover.EntityGetCategoriesResponse
import com.telenav.sdk.entity.model.lookup.EntityGetDetailResponse
import com.telenav.sdk.entity.model.prediction.EntitySuggestionPredictionResponse
import com.telenav.sdk.entity.model.prediction.EntityWordPredictionResponse
import com.telenav.sdk.entity.model.prediction.Suggestion
import com.telenav.sdk.entity.model.prediction.WordPrediction
import com.telenav.sdk.entity.model.search.*
import com.telenav.sdk.entity.utils.EntityJsonConverter
import telenav.demo.app.App
import telenav.demo.app.homepage.HotCategory
import telenav.demo.app.search.filters.OpenNowFilter
import telenav.demo.app.search.filters.PriceLevel
import telenav.demo.app.search.filters.StarsFilter
import java.util.concurrent.Executor

private const val TAG = "SearchViewModel"
const val SEARCH_LIMIT_WITH_FILTERS = 30
const val SUGGESTIONS_LIMIT_DEF = 10
const val PREDICTIONS_LIMIT_DEF = 3

@Suppress("DEPRECATION")
class SearchViewModel : ViewModel() {

    var filters: List<Any>? = null
    private val telenavEntityClient: EntityClient by lazy { EntityService.getClient() }
    var categories = MutableLiveData<List<Category>>().apply { listOf<HotCategory>() }

    var searchResults = MutableLiveData<List<Any>>().apply { listOf<Any>() }
    var loading = MutableLiveData<Boolean>()
    var testToSearch = MutableLiveData<String>()

    var suggestionsLoading = MutableLiveData<Boolean>().apply { postValue(false) }
    var suggestionsError = MutableLiveData<Boolean>().apply { postValue(false) }
    var suggestionList = MutableLiveData<List<Suggestion>>().apply { listOf<Suggestion>() }
    var predictions = MutableLiveData<List<WordPrediction>>().apply { listOf<WordPrediction>() }

    /**
     * make a request to the Entity service in order to retrieve "more.." categories
     */
    fun getCategories(executor: Executor) {
        loading.postValue(true)
        telenavEntityClient.categoriesRequest.asyncCall(
            executor,
            object : Callback<EntityGetCategoriesResponse> {
                override fun onSuccess(response: EntityGetCategoriesResponse) {
                    loading.postValue(false)
                    categories.postValue(response.results)
                }

                override fun onFailure(p1: Throwable?) {
                    loading.postValue(false)
                    Log.e(TAG, "", p1)
                }
            }
        )
    }

    /**
     * -------------------- HERE STARTS SUGGESTIONS CODE -----------------------
     */
    fun writingText(text: String, location: Location, executor: Executor) {
        Log.d(TAG, "text $text")
        testToSearch.postValue(text)

        if (!text.isBlank()) {
            requestSuggestions(text, location, executor)
        }
    }

    private fun requestSuggestions(text: String, location: Location, executor: Executor) {
        suggestionsLoading.postValue(true)
        suggestionsError.postValue(false)

        telenavEntityClient.suggestionPredictionRequest()
            .setQuery(text)
            .setLocation(location.latitude, location.longitude)
            .setLimit(App.readStringFromSharedPreferences(App.SUGGESTIONS_LIMIT,
                    SUGGESTIONS_LIMIT_DEF.toString())!!.toInt())
            .asyncCall(executor,
                object : Callback<EntitySuggestionPredictionResponse> {
                    override fun onSuccess(response: EntitySuggestionPredictionResponse) {
                        suggestionsLoading.postValue(false)
                        suggestionList.postValue(response.results)
                    }

                    override fun onFailure(error: Throwable) {
                        suggestionsLoading.postValue(false)
                        suggestionsError.postValue(true)
                        Log.e(TAG, "", error)
                    }
                })
    }

    fun requestPrediction(text: String, location: Location, executor: Executor) {
        telenavEntityClient.wordPredictionRequest()
            .setQuery(text)
            .setLocation(location.latitude, location.longitude)
            .setLimit(App.readStringFromSharedPreferences(App.PREDICTIONS_LIMIT,
                    PREDICTIONS_LIMIT_DEF.toString())!!.toInt())
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

    /**
     * -------------------- SEARCH APIs BELOW --------------------------
     */
    fun polygonSearch(categoryId: String?, executor: Executor, polygonCoordinates: List<LatLng>) {
        val polygonBuilder = Polygon.builder()
        for (polCoordinate in polygonCoordinates) {
            polygonBuilder.addPoint(polCoordinate.latitude, polCoordinate.longitude)
        }
        val polygon = polygonBuilder.build()

        val geoFilter = PolygonGeoFilter
            .builder(polygon)
            .build()


        val categoryFilter = CategoryFilter.builder().addCategory(categoryId).build()

        val polygonSearchFilters = SearchFilters.builder()
            .setGeoFilter(geoFilter)
            .setCategoryFilter(categoryFilter)
            .build()

        telenavEntityClient.searchRequest()
            .setLocation(
                GeoPoint(
                    polygonCoordinates.first().latitude,
                    polygonCoordinates.first().longitude
                )
            )
            .setFilters(polygonSearchFilters)
            .asyncCall(executor,
                object : Callback<EntitySearchResponse> {
                    override fun onSuccess(response: EntitySearchResponse) {
                        Log.w(TAG, Gson().toJson(response.results))
                        loading.postValue(false)
                        searchResults.postValue(response.results)
                    }

                    override fun onFailure(p1: Throwable?) {
                        loading.postValue(false)
                        //todo mariah add an error message on UI
                        Log.e(TAG, "onFailure", p1)
                    }
                })
    }

    fun getDetails(id: String) {
        Log.w("test", "getDetails ${id}")
        telenavEntityClient.detailRequest
            .setEntityIds(listOf(id))
            .asyncCall(
                object : Callback<EntityGetDetailResponse> {
                    override fun onSuccess(response: EntityGetDetailResponse) {
                        Log.w("TAG", "result ${Gson().toJson(response.results)}")
                    }

                    override fun onFailure(p1: Throwable?) {
                        Log.e("TAG", "onFailure", p1)
                    }
                }
            )
    }


    fun search(
        query: String?,
        categoryId: String?,
        location: Location,
        executor: Executor,
        nearLeft: LatLng? = null,
        farRight: LatLng? = null) {
        loading.postValue(true)
        val filtersSearch = SearchFilters.builder()
        if (categoryId != null) {
            filtersSearch.setCategoryFilter(
                    CategoryFilter.builder().addCategory(categoryId).build()
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
        telenavEntityClient.searchRequest()
            .apply {
                if (query != null)
                    setQuery(query)
            }.apply {
                    setFilters(filtersSearch.build())
                }
            .setLocation(37.78509, -122.41988)
            .setLimit(App.readStringFromSharedPreferences(App.SEARCH_LIMIT,
                    SEARCH_LIMIT_WITH_FILTERS.toString())!!.toInt())
            .asyncCall(
                executor,
                object : Callback<EntitySearchResponse> {
                    override fun onSuccess(response: EntitySearchResponse) {
                        Log.w(TAG, Gson().toJson(response.results))
                        App.writeStringToSharedPreferences(
                            App.LAST_ENTITY_RESPONSE_REF_ID,
                            response.referenceId
                        )
                        handleSearchResponse(filters != null, response.results)
                    }

                    override fun onFailure(p1: Throwable?) {
                        loading.postValue(false)
                        //todo mariah add an error message on UI
                        Log.e(TAG, "onFailure", p1)
                    }
                }
            )
    }

    private fun handleSearchResponse(filtersAvailable: Boolean, results: List<Entity>) {
        if (filtersAvailable) {
            searchResults.value = applyFilters(results)
        } else {
            searchResults.postValue(results)
        }
        loading.postValue(false)
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

    private fun filterByStar(results: List<Entity>, filter: StarsFilter): ArrayList<Entity> {
        val filteredResults = arrayListOf<Entity>()
        results.forEach { entity ->
            if (entity.facets.rating != null && entity.facets.rating.size > 0
                    && entity.facets.rating[0].averageRating - filter.stars.starsNumber.toDouble() >= 0.0 &&
                    entity.facets.rating[0].averageRating - filter.stars.starsNumber.toDouble() < 1.0
            ) {
                filteredResults.add(entity)
            }
        }
        return filteredResults
    }

    private fun filterByPrice(results: List<Entity>, filter: PriceLevel): ArrayList<Entity> {
        val filteredResults = arrayListOf<Entity>()
        results.forEach { entity ->
            if (entity.facets?.priceInfo?.priceLevel == filter?.priceLevel?.priceLevel) {
                filteredResults.add(entity)
            }
        }
        return filteredResults
    }

    private fun applyFilters(results: List<Entity>): List<Entity> {
        var filteredResults = results.toMutableList()
        if (filteredResults.size == 0) {
            return filteredResults
        }
        if (filters?.size == 0) {
            return results.subList(
                0,
                App.readFromSharedPreferences(App.FILTER_NUMBER)
            )
        }
        filters?.forEach { it ->
            if (it is OpenNowFilter) {
                filteredResults = filterByOpen(results)
            }
            if (it is StarsFilter) {
                filteredResults = filterByStar(results, it)
            }
            if (it is PriceLevel) {
                filteredResults = filterByPrice(results, it)
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

    fun requestBrands(
        categoryId: String?,
        location: Location
    ) {
        telenavEntityClient.discoverBrandRequest()
            .setLocation(location.latitude, location.longitude)
            .setLimit(App.readFromSharedPreferences(App.FILTER_NUMBER))
            .setCategory(categoryId)
            .asyncCall(object : Callback<EntityDiscoverBrandResponse> {
                override fun onSuccess(response: EntityDiscoverBrandResponse) {
                    loading.postValue(false)
                    searchResults.postValue(response.results)
                    // log response in JSON format
                    Log.i("sdk", EntityJsonConverter.toPrettyJson(response))
                    val brands = response.results

                }

                override fun onFailure(t: Throwable) {
                    Log.e(
                        "sdk",
                        "Get unsuccessful response or throwable happened when executing the request.",
                        t
                    )
                }
            })
    }

    fun requestSubcategories(
        categoryId: String?,
        location: Location
    ) {
        telenavEntityClient.discoverCategoryRequest()
            .setLocation(location.latitude, location.longitude)
            .setLimit(App.readFromSharedPreferences(App.FILTER_NUMBER))
            .setCategory(categoryId)
            .asyncCall(object : Callback<EntityDiscoverCategoryResponse?> {
                override fun onSuccess(response: EntityDiscoverCategoryResponse?) {
                    loading.postValue(false)
                    searchResults.postValue(response?.results)
                    // log response in JSON format
                    Log.d("TAG", EntityJsonConverter.toPrettyJson(response))
                    val categories: List<Category> = response?.results as List<Category>
                    for (category in categories) {
                        Log.d("TAG", " Categories ${category.name}")
                    }
                }

                override fun onFailure(t: Throwable?) {
                    loading.postValue(false)
                    Log.e(
                        "TAG",
                        "Get unsuccessful response or throwable happened when executing the request."
                    )
                }
            })
    }
}
