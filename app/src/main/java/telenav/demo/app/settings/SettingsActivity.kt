package telenav.demo.app.settings

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_settings.*

import ir.androidexception.filepicker.dialog.DirectoryPickerDialog

import com.telenav.sdk.core.SDKRuntime

import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.initialization.SearchMode
import telenav.demo.app.map.MapActivity.Companion.IS_ENV_CHANGED
import telenav.demo.app.utils.LocationUtil

import java.io.File

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SettingsActivity"

        // default latLong for cvp locations (Beijing)
        private const val DEFAULT_LAT = 39.92f
        private const val DEFAULT_LONG = 116.46f

        private fun latLong2Location(lat: Double, long: Double): Location {
            return Location("").apply {
                latitude = lat
                longitude = long
            }
        }
    }

    private lateinit var vIndexDataPath: TextView
    private lateinit var vModeHybrid: RadioButton
    private lateinit var vModeOnBoard: RadioButton
    private lateinit var vSearchLocations: SearchLocations

    private var indexDataPath = ""
    private var searchMode = SearchMode.HYBRID
    private var environment = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        getSettingsFromSP()
        initUI()

        updateUI()
    }

    private fun initUI() {
        vIndexDataPath = findViewById(R.id.settings_index_data_path)
        vModeHybrid = findViewById(R.id.settings_mode_hybrid)
        vModeOnBoard = findViewById(R.id.settings_mode_onboard)

        // listeners
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

        // TODO: this spinner is modified, need some work here
        val adapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(this, R.array.env, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        env_spinner.adapter = adapter
        env_spinner.setSelection(environment)
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

            // when dismissed, check lat long validation to choose save it or not
            dialog.setOnDismissListener {
                LocationUtil.parseGeoCoordinate(editText.text.toString())?.let {
                    successJob.invoke(it)
                }
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

        // environment TODO: new server list should replace this item
        environment = App.readStringFromSharedPreferences(App.ENVIRONMENT, "0")!!.toInt()

        // CVP and Search Area Location
        val cvpLat = sp.getFloat(App.KEY_CVP_LAT, DEFAULT_LAT).toDouble()
        val cvpLong = sp.getFloat(App.KEY_CVP_LONG, DEFAULT_LONG).toDouble()
        val salLat = sp.getFloat(App.KEY_SAL_LAT, DEFAULT_LAT).toDouble()
        val salLong = sp.getFloat(App.KEY_SAL_LONG, DEFAULT_LONG).toDouble()
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

            // Env TODO: need to change
            putString(App.ENVIRONMENT, env_spinner.selectedItemPosition.toString())

            // write batch
            apply()
        }

        // consider to kill the app to re-init sdk
        SDKRuntime.setNetworkAvailable(searchMode == SearchMode.HYBRID)
        if (env_spinner.selectedItemPosition != environment) {
            val data = Intent()
            data.putExtra(IS_ENV_CHANGED, true)
            setResult(RESULT_OK, data)
        }
        finish()
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