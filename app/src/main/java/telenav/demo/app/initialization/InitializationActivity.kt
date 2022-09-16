package telenav.demo.app.initialization

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.common.model.NavLogLevelType

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.telenav.sdk.core.ApplicationInfo
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.core.SDKRuntime
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.ota.api.OtaService
import com.telenav.sdk.prediction.api.PredictionService

import telenav.demo.app.App
import telenav.demo.app.AppLifecycleCallbacks
import telenav.demo.app.BuildConfig
import telenav.demo.app.R
import telenav.demo.app.map.MapActivity
import telenav.demo.app.model.ServerDataUtil

import ir.androidexception.filepicker.dialog.DirectoryPickerDialog

import java.io.File
import java.util.*
import java.util.concurrent.Executor

class InitializationActivity : AppCompatActivity() {
    companion object {
        private const val BOUNCE_DISTANCE = -100f
        private const val ANIMATION_DURATION = 2000L
        private const val ANIMATION_DELAY = 1000L
    }

    private lateinit var vLoading: View
    private lateinit var vAccess: View
    private lateinit var vInitialization: View

    // animate bouncing icon
    private lateinit var vIcon: View
    private val mAnimator by lazy {
        ObjectAnimator.ofFloat(vIcon, View.TRANSLATION_Y, 0f, BOUNCE_DISTANCE, 0f).apply {
            interpolator = BounceInterpolator()
            duration = ANIMATION_DURATION
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    startAnimation() // Loop animate
                }
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationRepeat(animation: Animator?) {}
            })
        }
    }
    private val mAnimate: Runnable = Runnable { mAnimator.start() }

    private var indexDataPath = ""
    private var searchMode = SearchMode.HYBRID
    var isChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialization)

        vAccess = findViewById(R.id.initialization_access)
        vInitialization = findViewById(R.id.initialization)
        vLoading = findViewById(R.id.initialization_loading)
        vIcon = findViewById(R.id.initialization_icon)

        findViewById<View>(R.id.initialization_next).setOnClickListener { initSDKs() }
        findViewById<View>(R.id.initialization_request_permissions).setOnClickListener {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), 1)
        }

        indexDataPath = BuildConfig.telenav_data_dir
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun getSavedIndexPath() {
        val prefs =
            getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

        val dataPath = prefs.getString(getString(R.string.saved_index_data_path_key), "")
        if (dataPath != null && dataPath.isNotEmpty()) {
            indexDataPath = dataPath
            initSDKs()
        } else {
            hideProgress()
        }
    }

    private fun getSavedSearchMode() {
        val prefs =
            getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

        val mode = prefs.getInt(getString(R.string.saved_search_mode_key), 1)
        searchMode = SearchMode.fromInt(mode)
    }

    private fun initSDKs() {
        showProgress()

        val prefs =
            getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

        var deviceID = prefs.getString(getString(R.string.saved_device_id), "")
        if (deviceID == null || deviceID.isEmpty()) {
            deviceID = UUID.randomUUID().toString()
        }
        with(prefs.edit()) {
            remove(getString(R.string.saved_update_status))
            putString(getString(R.string.saved_device_id), deviceID)
            apply()
        }

        try {
            GlobalScope.launch {
                delay(100L)
                if (intent != null) {
                     isChanged = intent.getBooleanExtra(MapActivity.IS_ENV_CHANGED, false)
                }
                val environment = App.readStringFromSharedPreferences(App.ENVIRONMENT,
                        "0")!!.toInt()
                val sdkOptions = getSDKOptions(deviceID, indexDataPath, environment)

                EntityService.initialize(sdkOptions)

                ServerDataUtil.fetchServerList(this@InitializationActivity)

                getUIExecutor().execute {
                    if (!isChanged) {
                        TaLog.enableLogs(true);
                        TaLog.setLogLevel(NavLogLevelType.INFO);
                        DataCollectorService.initialize(this@InitializationActivity, sdkOptions)
                        OtaService.initialize(this@InitializationActivity, sdkOptions)
                        PredictionService.initialize(sdkOptions)
                    }
                    application.registerActivityLifecycleCallbacks(AppLifecycleCallbacks())

                    SDKRuntime.setNetworkAvailable(searchMode == SearchMode.HYBRID)

                    startActivity(Intent(this@InitializationActivity, MapActivity::class.java))
                    finish()
                }
            }
        } catch (e: Throwable) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            hideProgress()
        }
    }


    private fun checkPermissions() {
        if (!this.checkLocationPermission() || !this.checkExternalStoragePermissions()) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    1
            )
        } else if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            vLoading.visibility = View.GONE
            vInitialization.visibility = View.GONE
            vAccess.visibility = View.VISIBLE
            stopAnimation()
        } else {
            getSavedSearchMode()
            getSavedIndexPath()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        checkPermissions()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        vInitialization.visibility = View.VISIBLE
        vAccess.visibility = View.GONE
    }

    private fun showProgress() {
        vLoading.visibility = View.VISIBLE
        vInitialization.visibility = View.GONE
        startAnimation()
    }

    private fun hideProgress() {
        vLoading.visibility = View.GONE
        vInitialization.visibility = View.VISIBLE
        stopAnimation()
    }

    private fun startAnimation() {
        vIcon.postDelayed(mAnimate, ANIMATION_DELAY)
    }

    private fun stopAnimation() {
        mAnimator.cancel()
        vIcon.translationY = 0f // move icon to original place
        vIcon.removeCallbacks(mAnimate)
    }

    override fun onStop() {
        stopAnimation()
        super.onStop()
    }
}

fun Context.getSDKOptions(deviceId: String, pathToIndex: String = "", environment: Int): SDKOptions {
    var dataPath = BuildConfig.telenav_data_dir
    if (pathToIndex.isNotEmpty()) {
        dataPath = pathToIndex
    }
    val cachePath = applicationContext.cacheDir.absolutePath

    saveIndexDataPath(dataPath)

    // read server settings from sp
    val serverData = ServerDataUtil.getInfo(this)

    val apiKey = serverData?.apiKey ?: BuildConfig.telenav_api_key
    val apiSecret = serverData?.apiSecret ?: BuildConfig.telenav_api_secret
    val apiEndpoint = serverData?.endpoint ?: BuildConfig.telenav_cloud_endpoint
    val userId = App.readStringFromSharedPreferences(App.KEY_USER_ID, "").ifEmpty { BuildConfig.telenav_user_id }
    val application = ApplicationInfo.builder(
        BuildConfig.telenav_data_collector_applicationName,
        BuildConfig.telenav_data_cpllector_applicationVersion
    ).build()
    if (environment == 1) {
        /*
        USE RELEASE REAL DATA
        apiKey = "ENTER_KEY"
        apiSecret = "ENTER_KEY"
        apiEndpoint = "https://restapi.telenav.com"*/
    }

    return SDKOptions.builder()
        .setDeviceGuid(deviceId)
        .setUserId(userId)
        .setApiKey(apiKey)
        .setApiSecret(apiSecret)
        .setCloudEndPoint(apiEndpoint)
        .setSdkDataDir(dataPath)
        .setSdkCacheDataDir(cachePath)
        .setLocale(Locale.EN_US)
        .setApplicationInfo(application)
        .build()
}

fun Context.saveIndexDataPath(pathToIndex: String = "") {
    if (pathToIndex.isEmpty()) {
        return
    }

    val prefs =
        getSharedPreferences(
            getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )
    with(prefs.edit()) {
        putString(getString(R.string.saved_index_data_path_key), pathToIndex)
        apply()
    }
}

fun Context.saveSearchMode(searchMode: SearchMode = SearchMode.HYBRID) {

    val prefs =
        getSharedPreferences(
            getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )
    with(prefs.edit()) {
        putInt(getString(R.string.saved_search_mode_key), searchMode.value)
        apply()
    }
}

fun Context.checkLocationPermission(): Boolean =
    checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED

fun Context.checkExternalStoragePermissions(): Boolean =
    checkCallingOrSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED

fun Activity.getUIExecutor(): Executor {
    return Executor { r -> runOnUiThread(r) }
}