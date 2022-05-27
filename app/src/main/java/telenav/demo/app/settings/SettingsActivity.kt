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

        private fun latLong2Location(lat: Double, long: Double): Location {
            return Location("").apply {
                latitude = lat
                longitude = long
            }
        }
    }

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var vIndexDataPath: TextView
    private lateinit var vModeHybrid: RadioButton
    private lateinit var vModeOnBoard: RadioButton
    private lateinit var vSearchLocations: SearchLocations

    private var indexDataPath = ""
    private var searchMode = SearchMode.HYBRID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        getSettingsFromSP()
        viewModel.init(this)
        initUI()

        updateUI()
    }

    private fun initUI() {
        vIndexDataPath = findViewById(R.id.settings_index_data_path)
        vModeHybrid = findViewById(R.id.settings_mode_hybrid)
        vModeOnBoard = findViewById(R.id.settings_mode_onboard)

        // listeners
        initSpinners()
        vModeHybrid.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) searchMode = SearchMode.HYBRID
        }
        vModeOnBoard.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) searchMode = SearchMode.ONBOARD
        }
        findViewById<View>(R.id.settings_back).setOnClickListener { save() }
        findViewById<View>(R.id.settings_select_index_data_path).setOnClickListener { openDirectoryForIndex() }

        // gps button
        cvp_reset.setOnClickListener {
            vSearchLocations.cvpFollowGPS = true
            updateLocations()
        }
        sal_reset.setOnClickListener {
            vSearchLocations.saFollowGPS = true
            updateLocations()
        }

        setLocationListener(cvp_text) {
            vSearchLocations.cvpFollowGPS = false
            vSearchLocations.cvpLocation = it
            if (vSearchLocations.saFollowCvp) {
                vSearchLocations.saFollowGPS = false
                vSearchLocations.searchAreaLocation = it
            }
            updateLocations()
        }

        setLocationListener(sal_text) {
            vSearchLocations.saFollowCvp = false
            vSearchLocations.saFollowGPS = false
            vSearchLocations.searchAreaLocation = it
            updateLocations()
        }

        // follow cvp checkbox
        cvp_same_checkbox.setOnCheckedChangeListener { _, isChecked ->
            vSearchLocations.saFollowCvp = isChecked

            // copy cvp location status if follow: 1. follow gps / 2. specified latLong
            if (isChecked) {
                vSearchLocations.saFollowGPS = vSearchLocations.cvpFollowGPS
                if (!vSearchLocations.cvpFollowGPS) {
                    vSearchLocations.searchAreaLocation.latitude = vSearchLocations.cvpLocation.latitude
                    vSearchLocations.searchAreaLocation.longitude = vSearchLocations.cvpLocation.longitude
                }
            }
            updateLocations()
        }

        // cvp locations
        updateLocations()
    }

    private fun initSpinners() {
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
                LocationUtil.parseGeoCoordinate(editText.text.toString())?.let {
                    successJob.invoke(it)
                    return@setOnDismissListener
                }
                Toast.makeText(this, R.string.invalid_latlong_toast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateLocations() {
        // priority: 1. follow cvp 2. follow gps 3. specified latLong
        // cvp
        if (vSearchLocations.cvpFollowGPS) {
            // clear text to show hint
            cvp_text.text = ""
        } else {
            cvp_text.text = vSearchLocations.cvpLocation.toLatLongString()
        }

        // sal
        cvp_same_checkbox.isChecked = vSearchLocations.saFollowCvp
        when {
            vSearchLocations.saFollowCvp -> {
                sal_text.text = cvp_text.text
            }
            vSearchLocations.saFollowGPS -> sal_text.text = ""
            else -> sal_text.text = vSearchLocations.searchAreaLocation.toLatLongString()
        }
    }

    private fun getSettingsFromSP() {
        val sp = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        // search mode
        searchMode = SearchMode.fromInt(sp.getInt(getString(R.string.saved_search_mode_key), 1))

        // saved index path
        indexDataPath = sp.getString(getString(R.string.saved_index_data_path_key), "")!!

        // CVP and Search Area Location
        val cvpLat = sp.getFloat(App.KEY_CVP_LAT, LocationUtil.DEFAULT_LAT).toDouble()
        val cvpLong = sp.getFloat(App.KEY_CVP_LONG, LocationUtil.DEFAULT_LONG).toDouble()
        val salLat = sp.getFloat(App.KEY_SAL_LAT, LocationUtil.DEFAULT_LAT).toDouble()
        val salLong = sp.getFloat(App.KEY_SAL_LONG, LocationUtil.DEFAULT_LONG).toDouble()
        vSearchLocations = SearchLocations(cvpLocation = latLong2Location(cvpLat, cvpLong),
            cvpFollowGPS = sp.getBoolean(App.KEY_CVP_GPS, true),
            searchAreaLocation = latLong2Location(salLat, salLong),
            saFollowGPS = sp.getBoolean(App.KEY_SAL_GPS, true),
            saFollowCvp = sp.getBoolean(App.KEY_SAL_CVP, true))
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
            putFloat(App.KEY_CVP_LAT, vSearchLocations.cvpLocation.latitude.toFloat())
            putFloat(App.KEY_CVP_LONG, vSearchLocations.cvpLocation.longitude.toFloat())
            putFloat(App.KEY_SAL_LAT, vSearchLocations.searchAreaLocation.latitude.toFloat())
            putFloat(App.KEY_SAL_LONG, vSearchLocations.searchAreaLocation.longitude.toFloat())
            putBoolean(App.KEY_CVP_GPS, vSearchLocations.cvpFollowGPS)
            putBoolean(App.KEY_SAL_GPS, vSearchLocations.saFollowGPS)
            putBoolean(App.KEY_SAL_CVP, vSearchLocations.saFollowCvp)

            // serverInfo
            viewModel.mCurrentServer?.let { ServerDataUtil.saveInfo(it, this) }

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