package telenav.demo.app.initialization

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.telenav.sdk.core.SDKRuntime
import com.telenav.sdk.dataconnector.api.DataConnectorService
import com.telenav.sdk.entity.android.client.api.AndroidEntityService
import com.telenav.sdk.ota.api.OtaService
import ir.androidexception.filepicker.dialog.DirectoryPickerDialog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import telenav.demo.app.AppLifecycleCallbacks
import telenav.demo.app.R
import telenav.demo.app.application.TelenavApplication
import telenav.demo.app.homepage.HomePageActivity
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.utils.SharedPreferencesRepository
import java.io.File
import java.util.*


class InitializationActivity : AppCompatActivity() {

    private lateinit var vLoading: View
    private lateinit var vAccess: View
    private lateinit var vInitialization: View

    private var indexDataPath = ""
    private var searchMode = SearchMode.HYBRID
    private var sharedPreferencesRepository = SharedPreferencesRepository.getInstance()

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
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun getSavedIndexPath() {
        val dataPath = sharedPreferencesRepository.indexDataPath.value
        if (dataPath.isNotEmpty()) {
            indexDataPath = dataPath
            initSDKs()
        } else {
            hideProgress()
        }
    }

    private fun getSavedSearchMode() {
        val mode = sharedPreferencesRepository.searchMode.value
        searchMode = SearchMode.fromInt(mode)
    }

    private fun initSDKs() {
        showProgress()
        try {
            GlobalScope.launch {
                delay(100L)

                val sdkOptions = TelenavApplication.instance.getSDKOptions(indexDataPath)
                AndroidEntityService.initialize(applicationContext, sdkOptions)

                getUIExecutor().execute {
                    DataConnectorService.initialize(this@InitializationActivity, sdkOptions)
                    OtaService.initialize(this@InitializationActivity, sdkOptions)
                    application.registerActivityLifecycleCallbacks(AppLifecycleCallbacks())

                    SDKRuntime.setNetworkAvailable(searchMode == SearchMode.HYBRID)

                    startActivity(Intent(this@InitializationActivity, HomePageActivity::class.java))
                    finish()
                }
            }
        } catch (e: Throwable) {
            Toast.makeText(this, getString(R.string.error_message), Toast.LENGTH_LONG).show()
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
        if (permissions.isNotEmpty()) {
            checkPermissions()
        }
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

fun Context.checkLocationPermission(): Boolean =
    checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED

fun Context.checkExternalStoragePermissions(): Boolean =
    checkCallingOrSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED &&
            checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED