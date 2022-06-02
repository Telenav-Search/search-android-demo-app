package telenav.demo.app.utils

import android.content.Context
import android.location.Location
import android.util.Log

import com.telenav.sdk.entity.model.base.GeoPoint
import com.telenav.sdk.entity.model.base.Polygon

/**
 * Util class for generating and verifying locations
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

    private const val POLYGON_FILE_NAME = "region-polygon.txt"

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

    /**
     * read region polygons from file in assets
     *
     * @param context Context
     * @return a map: key - region name, value - Polygon instance
     */
    fun readPolygons(context: Context): Map<String, Polygon> {
        val map = mutableMapOf<String, Polygon>()
        context.assets.open(POLYGON_FILE_NAME).bufferedReader().use { reader ->
            // each line contains a region name and a series of points to make a polygon.
            // eg: NA=6.489642209940081,-79.0411376953125,9.708718663165994...
            reader.readLines().forEach { line ->
                val regionPolygon = line.split("=")
                val region = regionPolygon[0]

                var latLongs = regionPolygon[1].split(",").map { it.toDouble() }
                // Special logic for only two-point cases, 4 numbers form 2 points
                if (latLongs.size == 4) {
                    // add 2 more points to form a rectangle
                    latLongs = latLongs.toMutableList().apply {
                        add(latLongs[0])
                        add(latLongs[3])
                        add(latLongs[2])
                        add(latLongs[1])
                    }
                }

                // build a polygon using the points
                val polygon = Polygon.builder().apply {
                    latLongs.chunked(2) { // 2 number form a point
                        addPoint(GeoPoint(it[0], it[1]))
                    }
                }.build()
                map[region] = polygon
            }
        }
        return map
    }

    /**
     * judge whether the point is in the polygon
     *
     * @return is in or not
     */
    fun isInPolygon(point: GeoPoint, polygon: Polygon): Boolean {
        val transferPointList: List<GeoPoint> = generateTransferPoints(point, polygon.points)
        val transferSize = transferPointList.size
        var crossWithRightX = 0
        for (i in 0 until transferSize) {
            val start: GeoPoint = transferPointList[i]
            val end: GeoPoint = if (i == transferSize - 1) {
                transferPointList[0]
            } else {
                transferPointList[i + 1]
            }
            if (hasCrossWithRightX(start, end)) {
                crossWithRightX++
            }
        }
        return crossWithRightX % 2 != 0
    }

    private fun generateTransferPoints(geoPoint: GeoPoint, pointList: List<GeoPoint>): List<GeoPoint> {
        val transferPointList: MutableList<GeoPoint> = ArrayList()
        for (point in pointList) {
            transferPointList.add(GeoPoint(point.latitude - geoPoint.latitude, point.longitude - geoPoint.longitude))
        }
        return transferPointList
    }

    private fun hasCrossWithRightX(start: GeoPoint, end: GeoPoint): Boolean {
        if (start.latitude > 0 && end.latitude > 0) {
            return false
        }
        if (start.latitude < 0 && end.latitude < 0) {
            return false
        }
        if (start.latitude == 0.0) {
            return true
        }
        // ax+b=y; when a = 0, check b > 0 or not
        if (start.longitude == end.longitude) {
            return start.longitude > 0
        }
        // ax+b=y; caught a and b, then let y = 0, check x > 0 or not
        val a: Double = (start.latitude - end.latitude) / (start.longitude - end.longitude)
        val b: Double =
            start.latitude - start.longitude * ((start.latitude - end.latitude) / (start.longitude - end.longitude))
        val crossWithX = -b / a
        return crossWithX >= 0
    }

    class InvalidLatLongException(message: String): RuntimeException(message)
}

fun Location.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)