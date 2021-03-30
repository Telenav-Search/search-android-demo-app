package telenav.demo.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.telenav.sdk.datacollector.api.DataCollectorService
import telenav.demo.app.utils.gpsProbe
import telenav.demo.app.utils.startEngine
import telenav.demo.app.utils.stopEngine

class AppLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        private val dataCollectorClient by lazy { DataCollectorService.getClient() }

        private val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                dataCollectorClient.gpsProbe(locationResult.lastLocation)
            }
        }

        private var isAppLaunch = true

        private var isActivityChangingConfigurations = false
        private var activityCounter = 1

        override fun onActivityStarted(activity: Activity) {
            activityCounter++
            if ((isAppLaunch || activityCounter == 1) && !isActivityChangingConfigurations) {
                isAppLaunch = false
                dataCollectorClient.startEngine()
                activity.applicationContext.setGPSListener(locationCallback)
            }
        }

        override fun onActivityStopped(activity: Activity) {
            activityCounter--
            isActivityChangingConfigurations = activity.isChangingConfigurations
            if (!isAppLaunch && activityCounter == 0 && !isActivityChangingConfigurations) {
                activity.applicationContext.stopGPSListener(locationCallback)
                dataCollectorClient.stopEngine()
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    }