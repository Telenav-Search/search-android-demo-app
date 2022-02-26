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
import com.telenav.sdk.entity.model.discover.EntityDiscoverCategoryResponse
import com.telenav.sdk.entity.model.search.*
import com.telenav.sdk.entity.utils.EntityJsonConverter
import telenav.demo.app.App
import telenav.demo.app.search.filters.OpenNowFilter
import telenav.demo.app.search.filters.PriceLevel
import telenav.demo.app.search.filters.StarsFilter
import java.util.concurrent.Executor

private const val TAG = "SearchInfoViewModel"

const val SEARCH_INFO_LIMIT_WITH_FILTERS = 30

@Suppress("DEPRECATION")
class SearchInfoViewModel : ViewModel() {

    var filters: List<Any>? = null
    private val telenavEntityClient: EntityClient by lazy { EntityService.getClient() }
    var searchResults = MutableLiveData<List<Any>>().apply { listOf<Any>() }
    var searchError = MutableLiveData<String>().apply { postValue("") }
    var loading = MutableLiveData<Boolean>().apply { postValue(false) }
    val categories = MutableLiveData<List<Category>>().apply { listOf<Any>() }

    fun search(
        query: String?,
        categoryTag: String?,
        location: Location,
        executor: Executor,
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

        telenavEntityClient.searchRequest()
            .apply {
                if (query != null)
                    setQuery(query)
            }.apply {
                    setFilters(filtersSearch.build())
                }
            .setLocation(37.39877104671623, -121.97739243507385)
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
                        handleSearchResponse(filters != null, response.results)
                        requestSubcategories(categoryTag, location, executor)
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
        executor: Executor
    ) {
        telenavEntityClient.discoverCategoryRequest()
            .setLocation(37.39877104671623, -121.97739243507385)
            .setLimit(App.readFromSharedPreferences(App.FILTER_NUMBER))
            .setCategory(categoryTag)
            .asyncCall(
                executor,
                object : Callback<EntityDiscoverCategoryResponse?> {
                override fun onSuccess(response: EntityDiscoverCategoryResponse?) {
                    loading.postValue(false)
                    // log response in JSON format
                    Log.d("TAG", EntityJsonConverter.toPrettyJson(response))
                    val categories1: List<Category> = response?.results as List<Category>
                    for (category in categories1) {
                        Log.d("TAG", " Categories ${category.name}")
                    }
                    categories.value = categories1
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

    private fun handleSearchResponse(filtersAvailable: Boolean, results: List<Entity>) {
       // loading.postValue(false)
        if (filtersAvailable) {
            searchResults.value = applyFilters(results)
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
}
