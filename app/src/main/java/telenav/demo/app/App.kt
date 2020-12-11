package telenav.demo.app

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import com.telenav.sdk.datacollector.api.DataCollectorService
import telenav.demo.app.utils.startEngine
import telenav.demo.app.utils.stopEngine

class AppLifecycleCallbacks : ActivityLifecycleCallbacks {
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }

    private var isAppLaunch = true

    private var isActivityChangingConfigurations = false
    private var activityCounter = 1

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityCounter++
        if (isAppLaunch) {
            isAppLaunch = false
            dataCollectorClient.startEngine()
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (!isAppLaunch && --activityCounter == 0 && !isActivityChangingConfigurations) {
            dataCollectorClient.stopEngine()
        }
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}

fun Context.convertNumberToDistance(dist: Double): String {
    val km = dist / 1000.0;

    val iso = resources.configuration.locale.getISO3Country()
    return if (iso.equals("usa", true) || iso.equals("mmr", true)) {
        String.format("%.1f mi", km / 1.609)
    } else {
        String.format("%.1f km", km)
    }
}

fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
