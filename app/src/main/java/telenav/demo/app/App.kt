package telenav.demo.app

import android.app.Application
import android.content.Context
import android.os.Looper

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

import com.instabug.library.Instabug
import com.instabug.library.invocation.InstabugInvocationEvent

class App : Application() {

    companion object {
        var application: App? = null

        const val FILTER_NUMBER = "number_of_results"
        const val LAST_ENTITY_RESPONSE_REF_ID = "last_entity_response_ref_id"
        const val SEARCH_LIMIT = "search_limit"
        const val SUGGESTIONS_LIMIT = "suggestions_limit"
        const val PREDICTIONS_LIMIT = "predictions_limit"
        const val ENVIRONMENT = "environment"
        const val FILTER_NUMBER_VALUE = 10

        const val RATE_STARS = "rate_stars"
        const val PRICE_LEVEL = "price_level"
        const val OPEN_TIME = "open_time"
        const val RESERVED = "reserved"
        const val FREE_CHARGER = "free_charger"
        const val CONNECTION_TYPES = "connection_types"
        const val CHARGER_BRAND = "charger_brand"
        const val POWER_FEED = "power_feed"
        const val PARKING_DURATION = "parking_duration"
        const val PARKING_START_FROM = "parking_start_from"

        const val KEY_CVP_LAT = "key_cvp_lat"
        const val KEY_CVP_LONG = "key_cvp_long"
        const val KEY_CVP_GPS = "key_cvp_gps"
        const val KEY_SAL_LAT = "key_sal_lat"
        const val KEY_SAL_LONG = "key_sal_long"
        const val KEY_SAL_GPS = "key_sal_gps"
        const val KEY_SAL_CVP = "key_sal_cvp"
        const val KEY_USER_ID = "key_user_id"
        const val KEY_EXPLORE_ENABLED = "key_explore_enabled"
        const val KEY_EXPLORE_INTENT = "key_explore_intent"
        const val KEY_EXPLORE_LIMIT = "key_explore_limit"
        const val KEY_EXPLORE_RADIUS = "key_explore_radius"


        fun writeToSharedPreferences(keyName: String, defaultValue: Int) {
            val prefs =
                application?.applicationContext?.getSharedPreferences(
                    application?.applicationContext?.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                )?.edit()
            prefs?.putInt(keyName, defaultValue)
            prefs?.apply()
        }

        fun readFromSharedPreferences(keyName: String): Int {
            val prefs =
                application?.applicationContext?.getSharedPreferences(
                    application?.applicationContext?.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                )
            return prefs?.getInt(keyName, FILTER_NUMBER_VALUE)?.toInt() ?: FILTER_NUMBER_VALUE
        }

        fun writeStringToSharedPreferences(keyName: String, string: String) {
            val prefs =
                application?.applicationContext?.getSharedPreferences(
                    application?.applicationContext?.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                )?.edit()
            prefs?.putString(keyName, string)
            prefs?.apply()
        }

        fun writeBooleanToSharedPreferences(keyName: String, state: Boolean) {
            val prefs =
                application?.applicationContext?.getSharedPreferences(
                    application?.applicationContext?.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                )?.edit()
            prefs?.putBoolean(keyName, state)
            prefs?.apply()
        }

        fun readStringFromSharedPreferences(keyName: String, def: String): String {
            val prefs =
                application?.applicationContext?.getSharedPreferences(
                    application?.applicationContext?.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                )
            return prefs?.getString(keyName, def) ?: def
        }

        fun readIntFromSharedPreferences(keyName: String, def: Int): Int {
            val prefs =
                application?.applicationContext?.getSharedPreferences(
                    application?.applicationContext?.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                )
            return prefs?.getInt(keyName, def) ?: 0
        }

        fun readBooleanFromSharedPreferences(keyName: String, def: Boolean): Boolean {
            val prefs =
                application?.applicationContext?.getSharedPreferences(
                    application?.applicationContext?.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                )
            return prefs?.getBoolean(keyName, def) ?: def
        }
    }

    init {
        application = this
    }

    override fun onCreate() {
        super.onCreate()

        // init Instabug
        Instabug.Builder(this, "db94621802370e5ba6429e1a504e821d")
            .setInvocationEvents(
                InstabugInvocationEvent.SHAKE,
                InstabugInvocationEvent.SCREENSHOT,
                InstabugInvocationEvent.FLOATING_BUTTON
            )
            .build()
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
    if (dist < 0) return ""
    val km = dist / 1000.0

    val iso = resources.configuration.locale.getISO3Country()
    return if (iso.equals("usa", true) || iso.equals("mmr", true)) {
        String.format("%.1f mi", km / 1.609)
    } else {
        String.format("%.1f km", km)
    }
}

fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
