package telenav.demo.app.application

import android.app.Application
import com.telenav.sdk.core.ApplicationInfo
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import telenav.demo.app.BuildConfig
import telenav.demo.app.R
import telenav.demo.app.utils.SharedPreferencesRepository
import java.util.*

class TelenavApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        SharedPreferencesRepository.init(this)
    }

     fun getSDKOptions(pathToIndex: String = ""): SDKOptions {
         val sharedPreferencesRepository = SharedPreferencesRepository.getInstance()
         var deviceId = sharedPreferencesRepository.deviceId.value
         if (deviceId.isEmpty()) {
             deviceId = UUID.randomUUID().toString()
         }
        val cachePath = cacheDir.absolutePath
        var apiKey = sharedPreferencesRepository.apiKey.value
        var apiSecret = sharedPreferencesRepository.apiSecret.value
        var apiEndpoint = sharedPreferencesRepository.cloudEndpoint.value
        val dataPath = if (pathToIndex.isNotEmpty()) {
            pathToIndex
        } else {
            BuildConfig.telenav_data_dir
        }

        sharedPreferencesRepository.indexDataPath.value = dataPath
        sharedPreferencesRepository.updateStatus.removeBooleanPreference()
        sharedPreferencesRepository.deviceId.value = deviceId

        if (apiKey.isEmpty()) {
            apiKey = BuildConfig.telenav_api_key
            sharedPreferencesRepository.apiKey.value = apiKey
        }

        if (apiSecret.isEmpty()) {
            apiSecret = BuildConfig.telenav_api_secret
            sharedPreferencesRepository.apiSecret.value = apiSecret
        }

        if (apiEndpoint.isEmpty()) {
            apiEndpoint = BuildConfig.telenav_cloud_endpoint
            sharedPreferencesRepository.cloudEndpoint.value = apiEndpoint
        }

        val userId = BuildConfig.telenav_user_id

        return SDKOptions.builder()
            .setDeviceGuid(deviceId)
            .setUserId(userId)
            .setApiKey(apiKey)
            .setApiSecret(apiSecret)
            .setCloudEndPoint(apiEndpoint)
            .setSdkDataDir(dataPath)
            .setSdkCacheDataDir(cachePath)
            .setLocale(Locale.EN_US)
            .setApplicationInfo(ApplicationInfo.builder(getString(R.string.app_name), BuildConfig.VERSION_NAME).build())
            .build()
    }

    companion object {
        lateinit var instance: TelenavApplication
            private set
    }
}