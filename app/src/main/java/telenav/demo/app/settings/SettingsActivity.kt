package telenav.demo.app.settings

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.telenav.sdk.core.SDKRuntime
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.entity.android.client.api.AndroidEntityService
import com.telenav.sdk.ota.api.OtaService
import ir.androidexception.filepicker.dialog.DirectoryPickerDialog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import telenav.demo.app.BuildConfig
import telenav.demo.app.R
import telenav.demo.app.application.TelenavApplication
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.initialization.*
import telenav.demo.app.utils.SharedPreferencesRepository
import java.io.File


class SettingsActivity : AppCompatActivity() {

    private lateinit var vIndexDataPath: TextView
    private lateinit var vApiKey: EditText
    private lateinit var vApiSecret: EditText
    private lateinit var vCloudEndpoint: EditText
    private lateinit var vModeHybrid: RadioButton
    private lateinit var vModeOnBoard: RadioButton

    private var indexDataPath = ""
    private var searchMode = SearchMode.HYBRID
    private var apiKey = ""
    private var apiSecret = ""
    private var cloudEndpoint = ""

    private var sharedPreferencesRepository = SharedPreferencesRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        vIndexDataPath = findViewById(R.id.settings_index_data_path)
        vModeHybrid = findViewById(R.id.settings_mode_hybrid)
        vModeOnBoard = findViewById(R.id.settings_mode_onboard)
        vApiKey = findViewById(R.id.settings_api_key)
        vApiSecret = findViewById(R.id.settings_api_secret)
        vCloudEndpoint = findViewById(R.id.settings_cloud_endpoint)

        vModeHybrid.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) searchMode = SearchMode.HYBRID
        }
        vModeOnBoard.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) searchMode = SearchMode.ONBOARD
        }

        findViewById<View>(R.id.settings_back).setOnClickListener { finish() }
        findViewById<View>(R.id.settings_select_index_data_path).setOnClickListener { openDirectoryForIndex() }
        findViewById<View>(R.id.settings_save).setOnClickListener { save() }

        val mode = sharedPreferencesRepository.searchMode.value
        searchMode = SearchMode.fromInt(mode)

        indexDataPath = sharedPreferencesRepository.indexDataPath.value
        if (indexDataPath.isEmpty()) {
            indexDataPath = BuildConfig.telenav_data_dir
        }

        apiKey = sharedPreferencesRepository.apiKey.value
        if (apiKey.isEmpty()) {
            apiKey = BuildConfig.telenav_api_key
        }

        apiSecret = sharedPreferencesRepository.apiSecret.value
        if (apiSecret.isEmpty()) {
            apiSecret =  BuildConfig.telenav_api_secret
        }

        cloudEndpoint = sharedPreferencesRepository.cloudEndpoint.value
        if (cloudEndpoint.isEmpty()) {
            cloudEndpoint =  BuildConfig.telenav_cloud_endpoint
        }

        updateUI()
    }

    private fun openDirectoryForIndex() {
        val directoryPickerDialog = DirectoryPickerDialog(
            this, {}
        ) { files: Array<File> ->
            indexDataPath = files[0].path
            updateUI()
        }
        directoryPickerDialog.show()
    }

    private fun updateUI() {
        vIndexDataPath.text = indexDataPath
        vApiKey.setText(apiKey)
        vApiSecret.setText(apiSecret)
        vCloudEndpoint.setText(cloudEndpoint)

        when (searchMode) {
            SearchMode.HYBRID -> {
                vModeHybrid.isChecked = true
                vModeOnBoard.isChecked = false
            }
            SearchMode.ONBOARD -> {
                vModeHybrid.isChecked = false
                vModeOnBoard.isChecked = true
            }
        }
    }

    private fun save() {
        val apiKey = vApiKey.text.toString()
        val apiSecret = vApiSecret.text.toString()
        val cloudEndpoint = vCloudEndpoint.text.toString()
        var shouldReInitializeSDKs = false

        if (sharedPreferencesRepository.indexDataPath.value != indexDataPath ||
            sharedPreferencesRepository.apiKey.value != apiKey ||
            sharedPreferencesRepository.apiSecret.value != apiSecret ||
            sharedPreferencesRepository.cloudEndpoint.value != cloudEndpoint) {
            shouldReInitializeSDKs = true
        }

        sharedPreferencesRepository.indexDataPath.value = indexDataPath
        sharedPreferencesRepository.apiKey.value = apiKey
        sharedPreferencesRepository.apiSecret.value = apiSecret
        sharedPreferencesRepository.cloudEndpoint.value = cloudEndpoint

        if (shouldReInitializeSDKs) {
            reInitializeSDKs()
        } else {
            if (sharedPreferencesRepository.searchMode.value != searchMode.value) {
                SDKRuntime.setNetworkAvailable(searchMode == SearchMode.HYBRID)
                sharedPreferencesRepository.searchMode.value = searchMode.value
            }
            finish()
        }
    }

    private fun reInitializeSDKs() {
        try {
            GlobalScope.launch {
                delay(100L)

                val sdkOptions = TelenavApplication.instance.getSDKOptions(indexDataPath)
                AndroidEntityService.reInitialize(applicationContext, sdkOptions)

                getUIExecutor().execute {
                    DataCollectorService.initialize(this@SettingsActivity, sdkOptions)
                    OtaService.shutdown()
                    OtaService.initialize(this@SettingsActivity, sdkOptions)

                    SDKRuntime.setNetworkAvailable(searchMode == SearchMode.HYBRID)
                    finish()
                }
            }
        } catch (e: Throwable) {
            Toast.makeText(this, this.getText(R.string.error_message), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
