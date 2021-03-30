package telenav.demo.app.infoui

import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import kotlinx.android.synthetic.main.info_window.view.*
import telenav.demo.app.model.SearchResult
import telenav.demo.app.utils.CategoryAndFiltersUtil

class CustomInfoWindowAdapter(var myContentsView: View) : GoogleMap.InfoWindowAdapter {

    private lateinit var searchResult: SearchResult


    fun setSearchResult(searchResult: SearchResult) {
        this.searchResult = searchResult
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun getInfoWindow(marker: Marker?): View? {

        myContentsView.poi_name.text = searchResult.name

        if (searchResult.address != null) {
            myContentsView.poi_address.text = searchResult.address
        }

        if (searchResult.phoneNo != null) {
            myContentsView.poi_phone_no.text = searchResult.phoneNo
        }

        if (searchResult.iconId != 0) {
            myContentsView.poi_icon.setImageResource(searchResult.iconId)
        }

        CategoryAndFiltersUtil.setStarsViewBasedOnRating(myContentsView, searchResult.ratingLevel, myContentsView.context)
        CategoryAndFiltersUtil.setPriceIconBasedOnPriceLevel(myContentsView, searchResult.priceLevel, myContentsView.context)

        return myContentsView
    }
}