package telenav.demo.app

import android.app.Application
import android.content.Context
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.entity.api.EntityService


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        EntityService.initialize(
            SDKOptions.builder()
                .setApiKey("7bd512e0-16bc-4a45-9bc9-09377ee8a913")
                .setApiSecret("89e872bc-1529-4c9f-857c-c32febbf7f5a")
                .setCloudEndPoint("https://restapistage.telenav.com")
                .setLocale(Locale.EN_US)
                .build()
        )
    }
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
