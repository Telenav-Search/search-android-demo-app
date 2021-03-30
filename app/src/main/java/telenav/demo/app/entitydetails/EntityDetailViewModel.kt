package telenav.demo.app.entitydetails

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import telenav.demo.app.model.SearchResult

class EntityDetailViewModel : ViewModel() {


    private var searchResultLiveData = MutableLiveData<SearchResult>()


    fun setSearchResult(searchResult: SearchResult) {
        searchResultLiveData.postValue(searchResult)
    }

    fun getSearchResultLiveData(): MutableLiveData<SearchResult> {
        return searchResultLiveData
    }

}