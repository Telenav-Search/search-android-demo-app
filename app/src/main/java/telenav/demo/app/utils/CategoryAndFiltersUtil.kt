package telenav.demo.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.telenav.sdk.entity.model.base.Entity
import telenav.demo.app.R
import telenav.demo.app.homepage.HotCategory
import telenav.demo.app.model.SearchResult
import java.util.*

object CategoryAndFiltersUtil {
    const val PREFS_NAME = "telenav.demo.app.searchwidgets.NewAppWidget"
    const val PREF_PREFIX_KEY = "appwidget_"

    val hotCategoriesList = arrayListOf(
        HotCategory("Food", R.drawable.ic_food_color, R.drawable.ic_food, "2040"),
        HotCategory("Banks / ATMs", R.drawable.ic_atm_color, R.drawable.ic_atm, "374"),
        HotCategory("Grocery", R.drawable.ic_grocery_color, R.drawable.ic_grocery, "221"),
        HotCategory("Shopping", R.drawable.ic_shopping_color, R.drawable.ic_shopping, "4090"),
        HotCategory("Coffee", R.drawable.ic_coffee_color, R.drawable.ic_coffee, "241"),
        HotCategory("Parking", R.drawable.ic_parking_color, R.drawable.ic_parking, "600"),
        HotCategory("Hotels / Motels", R.drawable.ic_hotel_color, R.drawable.ic_hotel, "595"),
        HotCategory("Attractions", R.drawable.ic_attraction_color, R.drawable.ic_attraction, "605"),
        HotCategory("Fuel", R.drawable.ic_gas_color, R.drawable.ic_gas, "811"),
        HotCategory("Electric Vehicle Charge Station", R.drawable.ic_ev_color, R.drawable.ic_ev, "771"),
        HotCategory("More", R.drawable.ic_more_color, R.drawable.ic_more, "")
    )

    val categoriesColors = arrayListOf(
        R.color.c1,
        R.color.c2,
        R.color.c3,
        R.color.c4,
        R.color.c5,
        R.color.c6,
        R.color.c7,
        R.color.c8,
        R.color.c9
    )

    val searchLabelsList = arrayListOf(
        "Pizza",
        "Coffee",
        "Breakfast",
        "Burgers",
        "Vegan",
        "Vegetarian",
        "Seafood",
        "Beagles",
        "Dinner",
        "Terrace"
    )

    fun setStarsViewBasedOnRating(view: View, ratingLevel: Double, context: Context) {
        val firstStar = view.findViewById<ImageView>(R.id.star_0)
        val secondStar = view.findViewById<ImageView>(R.id.star_1)
        val thirdStar = view.findViewById<ImageView>(R.id.star_2)
        val fourthStar = view.findViewById<ImageView>(R.id.star_3)
        val fifthStar = view.findViewById<ImageView>(R.id.star_4)

        when (ratingLevel) {

            0.0 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
            }

            0.5 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_start_half))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
            }

            1.0 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
            }

            1.5 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_start_half))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
            }

            2.0 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
            }

            2.5 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_start_half))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
            }

            3.0 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
            }

            3.5 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_start_half))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
            }

            4.0 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
            }

            4.5 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_start_half))
            }

            5.0 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_full))
            }

            else -> {
                firstStar.visibility = View.INVISIBLE
                secondStar.visibility = View.INVISIBLE
                thirdStar.visibility = View.INVISIBLE
                fourthStar.visibility = View.INVISIBLE
                fifthStar.visibility = View.INVISIBLE
            }
        }
    }

    fun setPriceIconBasedOnPriceLevel(view: View, priceLevel: Int, context: Context) {
        val firstLevel = view.findViewById<ImageView>(R.id.price_0)
        val secondLevel = view.findViewById<ImageView>(R.id.price_1)
        val thirdLevel = view.findViewById<ImageView>(R.id.price_2)

        when (priceLevel) {
            1 -> {
                firstLevel.visibility = View.VISIBLE
                secondLevel.visibility = View.INVISIBLE
                thirdLevel.visibility = View.INVISIBLE
            }

            2 -> {
                firstLevel.visibility = View.VISIBLE
                secondLevel.visibility = View.VISIBLE
                thirdLevel.visibility = View.INVISIBLE
            }

            3 -> {
                firstLevel.visibility = View.VISIBLE
                secondLevel.visibility = View.VISIBLE
                thirdLevel.visibility = View.VISIBLE
            }

            else -> {
                firstLevel.visibility = View.INVISIBLE
                secondLevel.visibility = View.INVISIBLE
                thirdLevel.visibility = View.INVISIBLE
            }
        }
    }

    fun bitmapDescriptorFromVector(context: Context, @DrawableRes vectorDrawableResourceId: Int): BitmapDescriptor? {
        val background = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
        background!!.setBounds(0, 0, background.intrinsicWidth, background.intrinsicHeight)
        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId)
        vectorDrawable?.setTint(context.getColor(R.color.telenav_indigo_mid_tone))
        vectorDrawable!!.setBounds(25, 15, vectorDrawable.intrinsicWidth + 15, vectorDrawable.intrinsicHeight + 5)
        val bitmap = Bitmap.createBitmap(background.intrinsicWidth, background.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background.draw(canvas)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    fun generateSearchResult(entityResult: Entity?, currentSearchHotCategory: String?) : SearchResult {
        if (entityResult == null) {
            return SearchResult.build("No result", "No category") {}
        }
        var categoryName = " "
        if (entityResult.place.categories.isNotEmpty() && entityResult.place.categories[0] != null && entityResult.place.categories[0].name != null) {
            categoryName = entityResult.place.categories[0].name
        }

        val searchResult = SearchResult.build(entityResult.place.name,  categoryName) {
            address = entityResult.place.address.formattedAddress

                if (entityResult.place.phoneNumbers.isNotEmpty()) {
                    phoneNo = entityResult.place.phoneNumbers[0]
                }

            if (entityResult.facets != null && entityResult.facets.rating.isNotEmpty()) {
                ratingLevel = entityResult.facets.rating[0].averageRating
            }

            if (entityResult.facets != null && entityResult.facets.priceInfo != null && entityResult.facets.priceInfo.priceLevel != null) {
                priceLevel = entityResult.facets.priceInfo.priceLevel
            }

            iconId = R.drawable.ic_more
            for (eachCategory in hotCategoriesList) {
                if ((eachCategory.id) == currentSearchHotCategory) {
                    iconId = eachCategory.iconPurple
                    break
                }
            }
            if (entityResult.place.websites.isNotEmpty()) {
                websitesList = entityResult.place.websites
            }

            if (entityResult.place.emails.isNotEmpty()) {
                email = entityResult.place.emails[0]
            }

            if (entityResult.facets != null && entityResult.facets.openHours != null && entityResult.facets.openHours.displayText != null) {
                hours = entityResult.facets.openHours.displayText
            }

            if (entityResult.distance != 0.0) {
                distance = entityResult.distance
            }

            if (entityResult.place != null && entityResult.place.address != null &&
                entityResult.place.address.navCoordinates != null) {
                if (entityResult.place.address.navCoordinates.latitude != null) {
                    latitude = entityResult.place.address.navCoordinates.latitude
                }

                if (entityResult.place.address.navCoordinates.longitude != null) {
                    longitude = entityResult.place.address.navCoordinates.longitude
                }
            }
        }
        return searchResult
    }

    fun placeCameraDependingOnSearchResults(googleMap: GoogleMap?, coordinatesList: MutableList<LatLng>, currentLocation: Location?) {
        if (coordinatesList.isNotEmpty() && coordinatesList.size > 2) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(createLatLngBounds(coordinatesList[0], coordinatesList[1]), 100))
        } else if (coordinatesList.isNotEmpty()) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(coordinatesList[0].latitude, coordinatesList[0].longitude), 17f))
        } else if (currentLocation != null) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLocation.latitude, currentLocation.longitude), 17f))
        }
    }

    fun createLatLngBounds(firstCoordinate : LatLng, secondCoordinate : LatLng): LatLngBounds? {
        val builder = LatLngBounds.Builder()
        builder.include(firstCoordinate)
        builder.include(secondCoordinate)
        return builder.build()
    }

    fun requestWebsite(searchResult: SearchResult, position: Int): String {
        var facebookSite = "http://www.facebook.com"
        var twitterSite = "http://www.twitter.com"
        var instagramSite = "http://www.instagram.com"

        if (searchResult.websitesList.isNotEmpty() && searchResult.websitesList.size > 2) {
            facebookSite = searchResult.websitesList[0]
            twitterSite = searchResult.websitesList[1]
            instagramSite = searchResult.websitesList[2]
        } else if (searchResult.websitesList.isNotEmpty() && searchResult.websitesList.size > 1) {
            facebookSite = searchResult.websitesList[0]
            twitterSite = searchResult.websitesList[1]
            instagramSite = searchResult.websitesList[1]
        } else if (searchResult.websitesList.isNotEmpty()) {
            facebookSite = searchResult.websitesList[0]
            twitterSite = searchResult.websitesList[0]
            instagramSite = searchResult.websitesList[0]
        }
        val websitesArray = arrayOf(facebookSite, twitterSite, instagramSite)

        return websitesArray[position]
    }

    fun generateRandomInt(): Int {
        val rand = Random()
        return rand.nextInt(100)
    }

    fun getOriginalQuery(fullQuery: String): String {
        val referenceWord = "originalQuery="
        val delimitingWord = ";source="
        val firstIndex = fullQuery.lastIndexOf(referenceWord) + referenceWord.length
        val lastIndex = fullQuery.indexOf(delimitingWord)
        return fullQuery.substring(firstIndex, lastIndex)
    }
}