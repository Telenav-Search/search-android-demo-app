package telenav.demo.app.initialization

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import telenav.demo.app.R
import telenav.demo.app.homepage.HomePageActivity
import java.io.File


class InitializationActivity : AppCompatActivity() {

    private lateinit var vLoading: ContentLoadingProgressBar

    private var indexDataPath = "";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialization)

        vLoading = findViewById(R.id.initialization_loading)
        vLoading.show()

        findViewById<View>(R.id.initialization_select_index).setOnClickListener { openDirectoryForIndex() }
        findViewById<View>(R.id.initialization_next).setOnClickListener { initSDKs() }
        checkStoragePermissions()
    }

    private fun initSDKs() {
        vLoading.show()

        try {
            EntityService.initialize(getSDKOptions(indexDataPath))
            DataCollectorService.initialize(applicationContext, getSDKOptions())
            OtaService.initialize(applicationContext, getSDKOptions())
//        registerActivityLifecycleCallbacks(AppLifecycleCallbacks())

            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        } catch (e: Throwable) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            vLoading.hide()
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

    private fun checkStoragePermissions() {
        if (!this.checkExternalStoragePermissions()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        } else {
            vLoading.hide()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            checkStoragePermissions()

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

fun Context.getSDKOptions(pathToIndex: String = ""): SDKOptions {
    var dataPath = "/storage/emulated/0/Telenav";
    if (pathToIndex.isNotEmpty()) {
        dataPath = pathToIndex
    }
    val cachePath = getExternalCacheDir()?.absolutePath;

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

fun Context.checkExternalStoragePermissions(): Boolean =
    checkCallingOrSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED &&
            checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED