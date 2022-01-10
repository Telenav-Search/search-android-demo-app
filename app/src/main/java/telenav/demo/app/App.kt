package telenav.demo.app

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.telenav.sdk.dataconnector.api.DataConnectorService
import telenav.demo.app.utils.gpsProbe
import telenav.demo.app.utils.startEngine
import telenav.demo.app.utils.stopEngine

class AppLifecycleCallbacks : ActivityLifecycleCallbacks {
    private val dataConnectorClient by lazy { DataConnectorService.getClient() }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            dataConnectorClient.gpsProbe(locationResult.lastLocation)
        }
    }

    private var isAppLaunch = true

    private var isActivityChangingConfigurations = false
    private var activityCounter = 1

    override fun onActivityStarted(activity: Activity) {
        activityCounter++
        if ((isAppLaunch || activityCounter == 1) && !isActivityChangingConfigurations) {
            isAppLaunch = false
            dataConnectorClient.startEngine()
            activity.applicationContext.setGPSListener(locationCallback)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        activityCounter--
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (!isAppLaunch && activityCounter == 0 && !isActivityChangingConfigurations) {
            activity.applicationContext.stopGPSListener(locationCallback)
            dataConnectorClient.stopEngine()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
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
