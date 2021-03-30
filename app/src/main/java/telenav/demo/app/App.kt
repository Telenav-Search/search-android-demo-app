package telenav.demo.app

import android.app.Application
import android.content.Context
import android.os.Looper
import android.preference.PreferenceManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.entity.api.EntityService

class App : Application() {

    companion object {
        var application: App? = null
        private const val TAG = "ApplicationClass"

        const val FILTER_NUMBER = "number_of_results"
        const val FILTER_STARS = "stars"
        const val FILTER_PRICE_LEVEL = "price_level"
        const val FILTER_NUMBER_VALUE = 10

        fun writeToSharedPreferences(keyName: String, filterNr: Int) {
            val prefs =
                PreferenceManager.getDefaultSharedPreferences(application?.applicationContext)
                    .edit()
            prefs.putInt(keyName, filterNr)
            prefs.apply()
        }

        fun readFromSharedPreferences(keyName: String): Int {
            val prefs =
                PreferenceManager.getDefaultSharedPreferences(application?.applicationContext)
            return prefs.getInt(keyName, FILTER_NUMBER_VALUE)
        }
    }

    init {
        application = this
    }
}


fun Context.setGPSListener(locationCallback: LocationCallback) {
    val locationRequest = LocationRequest.create()?.apply {
        interval = 15000
        fastestInterval = 15000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    } catch (e: SecurityException) {
    }
}

fun Context.stopGPSListener(locationCallback: LocationCallback) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    } catch (e: SecurityException) {
    }
}

fun Context.convertNumberToDistance(dist: Double): String {
    val km = dist / 1000.0

    val iso = resources.configuration.locale.getISO3Country()
    return if (iso.equals("usa", true) || iso.equals("mmr", true)) {
        String.format("%.1f mi", km / 1.609)
    } else {
        String.format("%.1f km", km)
    }
}

fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
