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
import telenav.demo.app.model.SearchResult

object CategoryAndFiltersUtil {

    const val FOOD_TAG = "RESTAURANT"
    const val PARKING_TAG = "PARKING"
    const val COFFEE_TAG = "COFFEE_HOUSE"
    const val FUEL_TAG = "FUEL_STATION"
    const val SHOPPING_TAG = "SHOPPING"
    const val HOTELS_TAG = "HOTEL_MOTEL"
    const val BANKS_TAG = "BANKS"
    const val ATTRACTIONS_TAG = "TOURIST_ATTRACTION"
    const val EV_CHARGER_TAG = "ELECTRIC_CHARGE_STATION"
    const val ENTERTAINMENT_ARTS_TAG = "ENTERTAINMENT_ARTS"
    const val HOSPITAL_TAG = "HEALTH_MEDICINE"

    // category views limit 6 in a row
    const val DISPLAY_LIMIT = 6

    val hotCategoriesList = arrayListOf(
        HotCategory("Food", FOOD_TAG, R.drawable.ic_food_color),
        HotCategory("Parking", PARKING_TAG, R.drawable.ic_parking_color),
        HotCategory("Coffee", COFFEE_TAG, R.drawable.ic_coffee_color),
        HotCategory("Fuel", FUEL_TAG, R.drawable.ic_fuel_color),
        HotCategory("Shopping", SHOPPING_TAG, R.drawable.ic_shopping_color),
        HotCategory("Hotels", HOTELS_TAG, R.drawable.ic_hotel_color),
        HotCategory("Banks", BANKS_TAG, R.drawable.ic_banks_color),
        HotCategory("Attractions", ATTRACTIONS_TAG, R.drawable.ic_attraction_color),
        HotCategory("EV Charger", EV_CHARGER_TAG, R.drawable.ic_ev_color),
        HotCategory("Entertainment ", ENTERTAINMENT_ARTS_TAG, R.drawable.ic_entertainment_color),
        HotCategory("Hospital", HOSPITAL_TAG, R.drawable.ic_hospital_color),
        HotCategory("More", "",  R.drawable.ic_more_color)
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

    val chargerBrandsArrayList = arrayListOf(
        "Circuit Electrique",
        "Tesla Destination",
        "ElectrifyAmerica",
        "Lastestasjoner",
        "GE WattStation",
        "Aerovironment",
        "Supercharger",
        "ChargePoint",
        "Oplaadpalen",
        "SemaConnect",
        "CarCharging",
        "Sun Country",
        "Greenlots",
        "OpConnect",
        "Shorepower",
        "EV Connect",
        "ChargeNet",
        "EnelDrive",
        "Recargo",
        "Endesa",
        "Innogy",
        "Volta",
        "Blink",
        "POLAR",
        "FLO",
        "eVgo",
        "JNSH",
        "RWE",
        "KSI"
    )

    val connectionTypesArrayList = arrayListOf(
        "J1772",
        "Sae Combo",
        "CHAdeMo",
        "NEMA",
        "NEMA 14-50",
        "Plug Type F",
        "Type 2",
        "Type 3",
        "Teala"
    )

    val powerFeedLevelsArrayList = arrayListOf(
        "Level 1",
        "Level 2",
        "DC Fast"
    )

    fun getCategoryIcon(categoryTag: String, name: String): Int {
        when {
            categoryTag.equals(FOOD_TAG) -> {
                return when {
                    name.contains("Burger") -> {
                        R.drawable.ic_burger_color
                    }
                    name.contains("Pizza") -> {
                        R.drawable.ic_pizza_color
                    }
                    else -> {
                        R.drawable.ic_food_color
                    }
                }
            }
            categoryTag.equals(PARKING_TAG) -> {
                return R.drawable.ic_parking_color
            }
            categoryTag.equals(COFFEE_TAG) -> {
                return R.drawable.ic_coffee_color
            }
            categoryTag.equals(FUEL_TAG) -> {
                return R.drawable.ic_fuel_color
            }
            categoryTag.equals(SHOPPING_TAG) -> {
                return R.drawable.ic_shopping_color
            }
            categoryTag.equals(HOTELS_TAG) -> {
                return R.drawable.ic_hotel_color
            }
            categoryTag.equals(BANKS_TAG) -> {
                return R.drawable.ic_atm_color
            }
            categoryTag.equals(ATTRACTIONS_TAG) -> {
                return R.drawable.ic_attraction_color
            }
            categoryTag.equals(EV_CHARGER_TAG) -> {
                return R.drawable.ic_ev_color
            }
            categoryTag.equals(ENTERTAINMENT_ARTS_TAG) -> {
                return R.drawable.ic_entertainment_color
            }
            categoryTag.equals(HOSPITAL_TAG) -> {
                return R.drawable.ic_hospital_color
            }
        }

        return 0
    }

    fun setStarsViewBasedOnRating(view: View, ratingLevel: Double, context: Context) {
        val firstStar = view.findViewById<ImageView>(R.id.star_0)
        val secondStar = view.findViewById<ImageView>(R.id.star_1)
        val thirdStar = view.findViewById<ImageView>(R.id.star_2)
        val fourthStar = view.findViewById<ImageView>(R.id.star_3)
        val fifthStar = view.findViewById<ImageView>(R.id.star_4)

        when (ratingLevel) {

            -1.0 -> {
                firstStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                secondStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                thirdStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fourthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
                fifthStar.setImageDrawable(context.getDrawable(R.drawable.ic_star_empty))
            }

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

    fun setYelpStarsViewBasedOnRating(view: View, ratingLevel: Double, context: Context) {
        val star = view.findViewById<ImageView>(R.id.star)

        when (ratingLevel) {

            -1.0 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_0))
            }

            0.0 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_0))
            }

            1.0 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_1))
            }

            1.5 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_1_half))
            }

            2.0 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_2))
            }

            2.5 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_2_half))
            }

            3.0 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_3))
            }

            3.5 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_3_half))
            }

            4.0 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_4))
            }

            4.5 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_4_half))
            }

            5.0 -> {
                star.setImageDrawable(context.getDrawable(R.drawable.stars_regular_5))
            }

            else -> {
                star.visibility = View.INVISIBLE
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
        vectorDrawable?.setTint(context.getColor(android.R.color.white))

        if (vectorDrawableResourceId == R.drawable.ic_parking_color) {
            vectorDrawable!!.setBounds(
                40,
                15,
                vectorDrawable.intrinsicWidth + 25,
                vectorDrawable.intrinsicHeight + 5
            )
        } else {
            vectorDrawable!!.setBounds(
                35,
                15,
                vectorDrawable.intrinsicWidth + 15,
                vectorDrawable.intrinsicHeight + 5
            )
        }
        val bitmap = Bitmap.createBitmap(background.intrinsicWidth, background.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background.draw(canvas)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    fun generateSearchResult(entityResult: Entity?, currentSearchHotCategory: String?) : SearchResult {
        if (entityResult == null || entityResult.place==null) {
            return SearchResult.build("No result", "No category") {}
        }
        var categoryName = " "
        if (entityResult.place.categories.isNotEmpty() && entityResult.place.categories[0] != null && entityResult.place.categories[0].name != null) {
            categoryName = entityResult.place.categories[0].name
        }

        val searchResult = SearchResult.build(entityResult.place.name,  categoryName) {
            id = entityResult.id
            permanentlyClosed = entityResult.place.isPermanentlyClosed
            parking = entityResult.facets?.parking
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

            iconId = R.drawable.ic_more_color
            for (eachCategory in hotCategoriesList) {
                if ((eachCategory.tag) == currentSearchHotCategory) {
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

    /**
     * Convert HotCategory data into CategoryAdapter needed format
     *
     * @return CategoryViewData
     */
    fun HotCategory.toViewData(): CategoryViewData = CategoryViewData(StringUtil.formatName(name), iconPurple)

    class HotCategory(val name: String, val tag: String, val iconPurple: Int)

    /**
     * Data class to storage the items view needed to show
     *
     * @since 2022-05-11
     */
    data class CategoryViewData(val name: String, val icon: Int)
}