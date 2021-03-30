package telenav.demo.app.map

import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.entity.model.search.SearchOptions
import telenav.demo.app.App
import java.util.concurrent.Executor

class SearchViewModel : ViewModel() {

    companion object {
        private val TAG: String = SearchViewModel::class.java.simpleName
    }

    private val telenavEntityClient: EntityClient by lazy { EntityService.getClient() }
    var searchResults = MutableLiveData<List<Entity>>().apply { listOf<Entity>() }


    fun voiceSearch(
        query: String,
        location: Location,
        executor: Executor
    ) {

        val searchOptions = SearchOptions.builder()
            .setTrigger(SearchOptions.Trigger.VOICE)
            .build()

        telenavEntityClient.searchRequest()
            .setQuery("WHAT=$query")
            .setSearchOptions(searchOptions)
            .setLocation(location.latitude, location.longitude)
            .setLimit(App.readFromSharedPreferences(App.FILTER_NUMBER))
            .asyncCall(
                executor,
                object : Callback<EntitySearchResponse> {
                    override fun onSuccess(response: EntitySearchResponse) {
                        Log.w(TAG, Gson().toJson(response.results))
                        searchResults.postValue(response.results)
                    }

                    override fun onFailure(p1: Throwable?) {
                        Log.e(TAG, "onFailure", p1)
                    }
                }
            )
    }
}