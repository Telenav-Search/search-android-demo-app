package telenav.demo.app.initialization

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.telenav.sdk.core.ApplicationInfo
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.core.SDKRuntime
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datasource.api.DataSourceCenter
import com.telenav.sdk.datasource.api.error.DataSourceException
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.ota.api.OtaService
import ir.androidexception.filepicker.dialog.DirectoryPickerDialog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import telenav.demo.app.AppLifecycleCallbacks
import telenav.demo.app.BuildConfig
import telenav.demo.app.R
import telenav.demo.app.homepage.HomePageActivity
import telenav.demo.app.homepage.getUIExecutor
import java.io.File
import java.util.*


class InitializationActivity : AppCompatActivity() {

    private lateinit var vLoading: View
    private lateinit var vAccess: View
    private lateinit var vInitialization: View

    private var indexDataPath = ""
    private var searchMode = SearchMode.HYBRID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialization)

        vAccess = findViewById(R.id.initialization_access)
        vInitialization = findViewById(R.id.initialization)
        vLoading = findViewById(R.id.initialization_loading)

        findViewById<View>(R.id.initialization_select_index).setOnClickListener { openDirectoryForIndex() }
        findViewById<View>(R.id.initialization_next).setOnClickListener { initSDKs() }
        findViewById<View>(R.id.initialization_request_permissions).setOnClickListener {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

            val request: NetworkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.i("sdk", "Network available")
                    SDKRuntime.setNetworkAvailable(true)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.i("sdk", "Network Lost")
                    SDKRuntime.setNetworkAvailable(false)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.i("sdk", "Network unavailable")
                    SDKRuntime.setNetworkAvailable(false)
                }
            })

        } else {
            val filter = IntentFilter()
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)

            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                        val networkActive = isNetworkActive(context)
                        Log.i("sdk", "Network status changed : $networkActive")
                        SDKRuntime.setNetworkAvailable(networkActive)
                    }
                }

                private fun isNetworkActive(context: Context): Boolean {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val networkInfo = cm.activeNetworkInfo
                    return networkInfo != null && networkInfo.isAvailable && networkInfo.isConnected
                }

            }, filter)
        }
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

                val sdkOptions = getSDKOptions(deviceID, indexDataPath)

                EntityService.initialize(sdkOptions)

                getUIExecutor().execute {

                    DataCollectorService.initialize(this@InitializationActivity, sdkOptions)
                    try {
                        DataSourceCenter.initialize(this@InitializationActivity)
                    } catch (e: DataSourceException) {
                        Log.e("sdk", e.stackTraceToString())
                    }
                    DataSourceCenter.start()

                    OtaService.initialize(this@InitializationActivity, sdkOptions)
                    application.registerActivityLifecycleCallbacks(AppLifecycleCallbacks())

                    SDKRuntime.setNetworkAvailable(searchMode == SearchMode.HYBRID)

                    startActivity(Intent(this@InitializationActivity, HomePageActivity::class.java))
                    finish()
                }
            }
        } catch (e: Throwable) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            hideProgress()
        }
    }

    private fun openDirectoryForIndex() {
        val directoryPickerDialog = DirectoryPickerDialog(
            this, {}
        ) { files: Array<File> ->
            indexDataPath = files[0].path
        }
        directoryPickerDialog.show()
    }

    private fun checkPermissions() {
        if (!this.checkLocationPermission())
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        else if (!this.checkExternalStoragePermissions()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        } else if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            vLoading.visibility = View.GONE
            vInitialization.visibility = View.GONE
            vAccess.visibility = View.VISIBLE
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
    }

    private fun hideProgress() {
        vLoading.visibility = View.GONE
        vInitialization.visibility = View.VISIBLE
    }
}

fun Context.getSDKOptions(deviceId: String, pathToIndex: String = ""): SDKOptions {
    var dataPath = BuildConfig.telenav_data_dir
    if (pathToIndex.isNotEmpty()) {
        dataPath = pathToIndex
    }
    val cachePath = applicationContext.cacheDir.absolutePath

    saveIndexDataPath(dataPath)

    return SDKOptions.builder()
        .setDeviceGuid(deviceId)
        .setUserId("TelenavDemoApp-u001")
        .setApiKey("7bd512e0-16bc-4a45-9bc9-09377ee8a913")
        .setApiSecret("89e872bc-1529-4c9f-857c-c32febbf7f5a")
        .setCloudEndPoint("https://restapistage.telenav.com")
        .setSdkDataDir(dataPath)
        .setSdkCacheDataDir(cachePath)
        .setLocale(Locale.EN_US)
        .setDeviceGuid("TelenavDemoApp-d001")
        .setApplicationInfo(
            ApplicationInfo.builder("TelenavDemoApp", "150")
                .build()
        )
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
    checkCallingOrSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED &&
            checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED