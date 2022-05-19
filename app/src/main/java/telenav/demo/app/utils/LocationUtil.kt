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
        if (lat < -90 || lat > 90 || long < -180 || long > 180) {
            throw InvalidLatLongException("lat = $lat, long = $long")
        }
    }

    class InvalidLatLongException(message: String): RuntimeException(message)
}