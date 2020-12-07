package telenav.demo.app

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datacollector.model.event.StartEngineEvent
import com.telenav.sdk.datacollector.model.event.StopEngineEvent
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.ota.api.OtaService


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        EntityService.initialize(getSDKOptions())
//        DataCollectorService.initialize(applicationContext, getSDKOptions())
//        OtaService.initialize(applicationContext, getSDKOptions())
//        registerActivityLifecycleCallbacks(AppLifecycleCallbacks())
    }
}

fun Context.getSDKOptions(): SDKOptions {
    val dataPath = getExternalFilesDir(null)?.absolutePath;
    val cachePath = getExternalCacheDir()?.absolutePath;

    return SDKOptions.builder()
        .setApiKey("7bd512e0-16bc-4a45-9bc9-09377ee8a913")
        .setApiSecret("89e872bc-1529-4c9f-857c-c32febbf7f5a")
        .setCloudEndPoint("https://restapistage.telenav.com")
        .setSdkDataDir(dataPath)
        .setSdkCacheDataDir(cachePath)
        .setLocale(Locale.EN_US)
        .build()
}

class AppLifecycleCallbacks : ActivityLifecycleCallbacks {
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }

    private var isAppLaunch = true

    private var isActivityChangingConfigurations = false
    private var activityCounter = 0

    override fun onActivityStarted(activity: Activity) {
        if (isAppLaunch && ++activityCounter == 1 && !isActivityChangingConfigurations) {
            isAppLaunch = false
            dataCollectorClient.sendEventRequest()
                .setEvent(StartEngineEvent.builder().build()).build().execute()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityCounter == 0 && !isActivityChangingConfigurations) {
            dataCollectorClient.sendEventRequest()
                .setEvent(StopEngineEvent.builder().build()).build().execute()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
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
