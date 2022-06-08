package telenav.demo.app.settings

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_settings.*

import ir.androidexception.filepicker.dialog.DirectoryPickerDialog

import com.telenav.sdk.core.SDKRuntime

import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.initialization.SearchMode
import telenav.demo.app.map.MapActivity.Companion.IS_ENV_CHANGED
import telenav.demo.app.model.*
import telenav.demo.app.utils.LocationUtil

import java.io.File

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SettingsActivity"
    }

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var vIndexDataPath: TextView
    private lateinit var vModeHybrid: RadioButton
    private lateinit var vModeOnBoard: RadioButton

    private var indexDataPath = ""
    private var searchMode = SearchMode.HYBRID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        getSettingsFromSP()
        initUI()
        updateUI()
        viewModel.init(this)
    }

    private fun initUI() {
        vIndexDataPath = findViewById(R.id.settings_index_data_path)
        vModeHybrid = findViewById(R.id.settings_mode_hybrid)
        vModeOnBoard = findViewById(R.id.settings_mode_onboard)

        // listeners
        initServers()
        initLocations()
        vModeHybrid.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) searchMode = SearchMode.HYBRID
        }
        vModeOnBoard.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) searchMode = SearchMode.ONBOARD
        }
        findViewById<View>(R.id.settings_back).setOnClickListener { save() }
        findViewById<View>(R.id.settings_select_index_data_path).setOnClickListener { openDirectoryForIndex() }

        // user id
        val userIdText = findViewById<TextView>(R.id.user_id_text)
        userIdText.setOnClickListener { showEditTextDialog { viewModel.onUserIdChange(it) } }
        viewModel.userId.observe(this) { userIdText.text = it }
    }

    private fun initLocations() {
        cvp_reset.setOnClickListener { viewModel.onLocationChange(CvpFollowGPS(true)) }
        sal_reset.setOnClickListener { viewModel.onLocationChange(SalFollowGPS(true)) }
        setLocationListener(cvp_text) { viewModel.onLocationChange(CvpChange(it)) }
        setLocationListener(sal_text) { viewModel.onLocationChange(SalChange(it)) }
        cvp_quick_link.setOnClickListener { showQuickLinkDialog { viewModel.onLocationChange(CvpChange(it)) } }
        sal_quick_link.setOnClickListener { showQuickLinkDialog { viewModel.onLocationChange(SalChange(it)) } }

        // follow cvp checkbox
        cvp_same_checkbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onLocationChange(SalFollowCVP(isChecked))
        }

        // observe liveData and refresh views
        viewModel.locations.observe(this) {
            if (it.cvpFollowGPS) {
                // clear text to show hint
                cvp_text.text = ""
            } else {
                cvp_text.text = it.cvpLocation.toLatLongString()
            }

            // sal
            cvp_same_checkbox.isChecked = it.saFollowCvp
            when {
                it.saFollowCvp -> {
                    sal_text.text = cvp_text.text
                }
                it.saFollowGPS -> sal_text.text = ""
                else -> sal_text.text = it.searchAreaLocation.toLatLongString()
            }
        }
    }

    private fun initServers() {
        engine_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            var isFirst = true
            override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (isFirst) {
                    isFirst = false
                    return
                }
                viewModel.onEngineChange(position)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        env_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                viewModel.onEnvChange(position)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        val serverAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>())
        serverAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        engine_spinner.adapter = serverAdapter

        val envAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>())
        envAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        env_spinner.adapter = envAdapter

        viewModel.serverState.observe(this) {
            serverAdapter.clear()
            serverAdapter.addAll(it.serverNames)
            engine_spinner.setSelection(it.serverSelected)

            envAdapter.clear()
            envAdapter.addAll(it.envNames)
            env_spinner.setSelection(it.envSelected)
        }
    }

    private fun setLocationListener(view: View, successJob: (location: Location) -> Unit) {
        if (view !is TextView) return

        view.setOnClickListener {
            showEditTextDialog { text ->
                LocationUtil.parseGeoCoordinate(text)?.let {
                    successJob.invoke(it)
                    return@showEditTextDialog
                }
                Toast.makeText(this, R.string.invalid_latlong_toast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditTextDialog(callback: (String) -> Unit) {
        val dialog = AlertDialog.Builder(this).setView(R.layout.input_dialog).create()

        // set window attrs: 1. gravity bottom 2. show keyboard
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()

        // focus on edittext when dialog shown to start input
        val editText = dialog.findViewById<EditText>(R.id.input_text)!!
        editText.requestFocus()
        editText.setOnEditorActionListener { _, _, keyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                dialog.dismiss()
            }
            true
        }

        // when dismissed, check lat long validation to choose save it or not
        dialog.setOnDismissListener {
            callback.invoke(editText.text.toString())
        }
    }

    private fun showQuickLinkDialog(callback: (Location) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.quick_link)
            .setItems(LocationUtil.QUICK_LINKS.map { it.first }.toTypedArray()) { _, position ->
                callback(LocationUtil.QUICK_LINKS[position].second)
            }.setCancelable(true)
            .create()
            .show()
    }

    private fun getSettingsFromSP() {
        val sp = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        // search mode
        searchMode = SearchMode.fromInt(sp.getInt(getString(R.string.saved_search_mode_key), 1))

        // saved index path
        indexDataPath = sp.getString(getString(R.string.saved_index_data_path_key), "")!!
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
        val sp = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val serverChanged = ServerDataUtil.getInfo(sp) != viewModel.mCurrentServer

        //save settings to sp
        with(sp.edit()) {
            // search mode
            putInt(getString(R.string.saved_search_mode_key), searchMode.value)
            // index data path
            if (indexDataPath.isNotEmpty()) putString(getString(R.string.saved_index_data_path_key), indexDataPath)
            // cvp locations
            val searchLocations = viewModel.locations.value!!
            putFloat(App.KEY_CVP_LAT, searchLocations.cvpLocation.latitude.toFloat())
            putFloat(App.KEY_CVP_LONG, searchLocations.cvpLocation.longitude.toFloat())
            putFloat(App.KEY_SAL_LAT, searchLocations.searchAreaLocation.latitude.toFloat())
            putFloat(App.KEY_SAL_LONG, searchLocations.searchAreaLocation.longitude.toFloat())
            putBoolean(App.KEY_CVP_GPS, searchLocations.cvpFollowGPS)
            putBoolean(App.KEY_SAL_GPS, searchLocations.saFollowGPS)
            putBoolean(App.KEY_SAL_CVP, searchLocations.saFollowCvp)

            // serverInfo
            viewModel.mCurrentServer?.let { ServerDataUtil.saveInfo(it, this) }

            // User ID
            viewModel.userId.value?.let { putString(App.KEY_USER_ID, it) }

            // write batch
            apply()
        }

        // consider to kill the app to re-init sdk
        SDKRuntime.setNetworkAvailable(searchMode == SearchMode.HYBRID)
        if (serverChanged) {
            val data = Intent()
            data.putExtra(IS_ENV_CHANGED, true)
            setResult(RESULT_OK, data)
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_NoActionBar)
                .setTitle(R.string.alert_title)
                .setMessage(R.string.alert_text)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { _, _ -> finish() }
                .create()
                .show()
        } else {
            finish()
        }
    }
}

private fun Location.toLatLongString(): String = String.format("%.4f, ", latitude) + String.format("%.4f", longitude)

/**
 * View data for location settings
 *
 * @since 2022-05-18
 */
data class SearchLocations(var cvpLocation: Location,
                           var cvpFollowGPS: Boolean,
                           var searchAreaLocation: Location,
                           var saFollowGPS: Boolean,
                           var saFollowCvp: Boolean)