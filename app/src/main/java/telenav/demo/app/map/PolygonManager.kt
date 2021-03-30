package telenav.demo.app.map

import android.annotation.SuppressLint
import android.graphics.Point
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import telenav.demo.app.R
import telenav.demo.app.utils.Converter
import telenav.demo.app.utils.PolygonHelper

@SuppressLint("ClickableViewAccessibility")
class PolygonManager(val view: View, val mapFragment: MapFragment) {

    private var lastPolygon: Polygon? = null
    private var isDrawEnabled = false
    private var drewPolygonCoordinates = arrayListOf<LatLng>()
    private val linePolygons = arrayListOf<Polygon>()
    private var correctedPolygon: List<LatLng>? = null
    private val syncObj = Object()

    init {
        view.setOnTouchListener { _, event ->
            var consumed = false
            if (isDrawEnabled) {
                val screenPoint = Point(event.x.toInt(), event.y.toInt())
                val latLon = mapFragment.getScreenLocation(screenPoint)

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        initPolygon()
                        synchronized(syncObj) {
                            correctedPolygon = null
                        }
                        drewPolygonCoordinates.add(latLon!!)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        drawPolygonLine(drewPolygonCoordinates.last(), latLon!!)
                        drewPolygonCoordinates.add(latLon)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        endPolygonDraw()
                    }
                }
                consumed = true
            }
            consumed
        }
    }

    private fun initPolygon() {
        lastPolygon?.remove()
        removeLinePolygons()
        drewPolygonCoordinates.clear()
    }

    private fun endPolygonDraw() {
        synchronized(syncObj) {
            if (correctedPolygon != null)
                return
            correctPolygon()
        }
        removeLinePolygons()
        drawPolygon(correctedPolygon)
    }

    private fun correctPolygon() {
        if (drewPolygonCoordinates.isEmpty())
            return

        this.correctedPolygon = getCorrectedPolygon()
    }

    private fun removeLinePolygons() {
        for (linePol in linePolygons) {
            linePol.remove()
        }
        linePolygons.clear()
    }

    private fun drawPolygonLine(startCoordinate: LatLng, endCoordinate: LatLng) {
        val pol = PolygonOptions()
        pol.add(startCoordinate)
        pol.add(endCoordinate)
        pol.strokeColor(ContextCompat.getColor(mapFragment.context!!, R.color.telenav_indigo))
        pol.strokeWidth(7.toFloat())
        val lastLinePolygon = mapFragment.addPolygon(pol)
        if (lastLinePolygon != null)
            linePolygons.add(lastLinePolygon)

    }

    private fun drawPolygon(correctedPolygon: List<LatLng>?) {
        if (correctedPolygon == null || correctedPolygon.isEmpty())
            return

        val pol = PolygonOptions()
        pol.addAll(correctedPolygon)
        pol.strokeColor(ContextCompat.getColor(mapFragment.context!!, R.color.telenav_gray))
        pol.strokeWidth(7.toFloat())
        pol.fillColor(ContextCompat.getColor(mapFragment.context!!, R.color.telenav_yellow))
        lastPolygon = mapFragment.addPolygon(pol)
        lastPolygon?.isClickable = true
    }

    private fun getCorrectedPolygon(): List<LatLng> {
        val points =
            PolygonHelper.getClosedPolygonPoints(Converter.convertToPoints(drewPolygonCoordinates))
        return Converter.convertToLatLngs(points)
    }

    fun onDrawEnabled(enabled: Boolean) {
        isDrawEnabled = enabled
        if (isDrawEnabled) {
            initPolygon()
        } else {
            endPolygonDraw()
        }
    }

    fun getPolygonCoordinates() = correctedPolygon
}