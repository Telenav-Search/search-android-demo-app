package telenav.demo.app.utils

import com.google.android.gms.maps.model.LatLng

object Converter {

    fun convertToPoint(latLon: LatLng) = MathUtil.Point(latLon.latitude, latLon.longitude)

    fun convertToLatLng(point: MathUtil.Point) = LatLng(point.x, point.y)

    fun convertToPoints(latLngs: List<LatLng>): List<MathUtil.Point> {
        val points = arrayListOf<MathUtil.Point>()
        for (latLng in latLngs) {
            points.add(convertToPoint(latLng))
        }
        return points
    }

    fun convertToLatLngs(points: List<MathUtil.Point>): List<LatLng> {
        val latLngs = arrayListOf<LatLng>()
        for (point in points) {
            latLngs.add(convertToLatLng(point))
        }
        return latLngs
    }

}