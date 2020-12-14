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
import androidx.core.widget.ContentLoadingProgressBar
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.ota.api.OtaService
import ir.androidexception.filepicker.dialog.DirectoryPickerDialog
import telenav.demo.app.AppLifecycleCallbacks
import telenav.demo.app.R
import telenav.demo.app.homepage.HomePageActivity
import java.io.File


class InitializationActivity : AppCompatActivity() {

    private lateinit var vLoading: ContentLoadingProgressBar
    private lateinit var vAccess: View
    private lateinit var vInitialization: View

    private var indexDataPath = "";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialization)

        vAccess = findViewById(R.id.initialization_access)
        vInitialization = findViewById(R.id.initialization)
        vLoading = findViewById(R.id.initialization_loading)
        showProgress()

        findViewById<View>(R.id.initialization_select_index).setOnClickListener { openDirectoryForIndex() }
        findViewById<View>(R.id.initialization_next).setOnClickListener { initSDKs() }
        findViewById<View>(R.id.initialization_request_permissions).setOnClickListener {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), 1)
        }
        checkPermissions()
    }

    private fun initSDKs() {
        showProgress()

        try {
            EntityService.initialize(getSDKOptions(indexDataPath))
            DataCollectorService.initialize(applicationContext, getSDKOptions())
            OtaService.initialize(applicationContext, getSDKOptions())

            application.registerActivityLifecycleCallbacks(AppLifecycleCallbacks())

            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
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
            vLoading.hide()
            vInitialization.visibility = View.GONE
            vAccess.visibility = View.VISIBLE
        } else {
            hideProgress()
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
        vLoading.show()
        vInitialization.visibility = View.GONE
    }

    private fun hideProgress() {
        vLoading.hide()
        vInitialization.visibility = View.VISIBLE
    }
}

fun Context.getSDKOptions(pathToIndex: String = ""): SDKOptions {
    var dataPath = "/storage/emulated/0/Telenav";
    if (pathToIndex.isNotEmpty()) {
        dataPath = pathToIndex
    }
    val cachePath = applicationContext.cacheDir.absolutePath;

    return SDKOptions.builder()
        .setUserId("telenavDemoApp")
        .setApiKey("7bd512e0-16bc-4a45-9bc9-09377ee8a913")
        .setApiSecret("89e872bc-1529-4c9f-857c-c32febbf7f5a")
        .setCloudEndPoint("https://restapistage.telenav.com")
        .setSdkDataDir(dataPath)
        .setSdkCacheDataDir(cachePath)
        .setLocale(Locale.EN_US)
        .build()
}

fun Context.checkLocationPermission(): Boolean =
    checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED

fun Context.checkExternalStoragePermissions(): Boolean =
    checkCallingOrSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED &&
            checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED