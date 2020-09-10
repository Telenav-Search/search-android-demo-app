package telenav.demo.app

import android.app.Application
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