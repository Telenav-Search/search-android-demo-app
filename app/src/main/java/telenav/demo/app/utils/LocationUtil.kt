package telenav.demo.app.utils

import android.location.Location
import android.util.Log

import java.lang.NumberFormatException

/**
 * Util class for generating and verifying loacations
 *
 * @since 2022-05-19
 */
object LocationUtil {
    private const val TAG = "LocationUtil"
    private const val VALID_PART_NUM = 2
    private const val LAT_MIN = -90
    private const val LAT_MAX = 90
    private const val LONG_MIN = -180
    private const val LONG_MAX = 180

    // default latLong for cvp locations (NYC)
    const val DEFAULT_LAT = 40.730610f
    const val DEFAULT_LONG = -73.935242f

    val QUICK_LINKS = listOf(
        "New York" to createLocation(40.760726, -73.983564),
        "Farmington" to createLocation(42.497287, -83.436210),
        "Ottawa" to createLocation(45.42351, -75.69305),
        "Mexico City" to createLocation(19.43195, -99.13313),
        "San Francisco" to createLocation(37.78509, -122.41988),
        "Santa Clara" to createLocation(37.398174, -121.977487),
        "Toronto" to createLocation(43.643499, -79.379106),
        "Berlin" to createLocation(52.51798, 13.38866 ),
        "London" to createLocation(51.51087, -0.12754),
        "Istanbul" to createLocation(41.0136, 28.9550),
        "Shanghai" to createLocation(31.206220, 121.398507),
        "Xi’an" to createLocation(34.265075, 108.953349)
    )

    fun parseGeoCoordinate(text: String): Location? {
        if (text.isEmpty()) return null

        val parts = text.split(",")
        if (parts.size != VALID_PART_NUM) return null

        // convert string to lat and long, assemble them into Location
        val lat = parts[0].toDouble()
        val long = parts[1].toDouble()

        return try {
            latLongValidate(lat, long)
            createLocation(lat, long)
        } catch (e: NumberFormatException) {
            Log.e(TAG, "parseGeoCoordinate: NumberFormatException! lat = ${parts[0]}, long = ${parts[1]}")
            null
        } catch (e: InvalidLatLongException) {
            Log.e(TAG, "parseGeoCoordinate: InvalidLatLongException! ${e.message}")
            null
        }
    }

    fun latLongValidate(lat: Double, long: Double) {
        if (lat < LAT_MIN || lat > LAT_MAX || long < LONG_MIN || long > LONG_MAX) {
            throw InvalidLatLongException("lat = $lat, long = $long")
        }
    }

    fun createLocation(lat: Double, long: Double): Location {
        return Location("").apply {
            latitude = lat
            longitude = long
        }
    }

    class InvalidLatLongException(message: String): RuntimeException(message)
}