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

    fun parseGeoCoordinate(text: String): Location? {
        if (text.isEmpty()) return null

        val parts = text.split(",")
        if (parts.size != VALID_PART_NUM) return null

        // convert string to lat and long, assemble them into Location
        val lat = parts[0].toDouble()
        val long = parts[1].toDouble()

        return try {
            latLongValidate(lat, long)
            Location("").apply {
                latitude = lat
                longitude = long
            }
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

    class InvalidLatLongException(message: String): RuntimeException(message)
}