package telenav.demo.app.settings

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity

import com.google.gson.Gson

import kotlinx.android.synthetic.main.activity_settings.*

import ir.androidexception.filepicker.dialog.DirectoryPickerDialog

import com.telenav.sdk.core.SDKRuntime

import telenav.demo.app.App
import telenav.demo.app.BuildConfig
import telenav.demo.app.R
import telenav.demo.app.initialization.SearchMode
import telenav.demo.app.map.MapActivity.Companion.IS_ENV_CHANGED
import telenav.demo.app.model.*
import telenav.demo.app.utils.LocationUtil
import telenav.demo.app.utils.RSAEncipherDecipher

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

//    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var serverData: Map<String, List<ServerInfo>>

    // current selected server info (will be null for the first time getting in settings)
    var currentServer: ServerInfo? = null

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
        fetchServerData() // TODO: move it to viewModel
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

        // update server list info
        updateServers(true)
    }

    private fun initSpinners() {
        engine_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            var isFirst = true
            override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (isFirst) {
                    isFirst = false
                    return
                }
                adapterView ?: return
                val serverName = adapterView.adapter.getItem(position) as String
                val serverInfo = serverData["NA"]?.find { it.productName == serverName }
                currentServer = serverInfo
                updateServers(false)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        env_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                adapterView ?: return
                currentServer ?: return
                val envName = (adapterView.adapter.getItem(position) as String)
                currentServer = serverData["NA"]?.find { it.productName == currentServer!!.productName && it.envName == envName }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}

        }
    }

    private fun updateServers(isInit: Boolean) {
        // TODO: "NA" is mock data, real data need to be calc from CVP
        val region = "NA"

        // TODO: currently using full server list without cvp filter to make demo
        if (isInit) {
            val serverAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                serverData[region]?.getProductNames() ?: emptyList())
            serverAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            engine_spinner.adapter = serverAdapter
            currentServer?.let { engine_spinner.setSelection(serverAdapter.getPosition(it.productName)) }
        }

//        val envAdapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(this, R.array.env, android.R.layout.simple_spinner_item)
        Log.i(TAG, "updateServers: server name =" +
                " ${engine_spinner.getItemAtPosition(engine_spinner.selectedItemPosition)}," +
                " position = ${engine_spinner.selectedItemPosition}")
        val envNames = serverData[region]?.getEnvNames(
            engine_spinner.getItemAtPosition(engine_spinner.selectedItemPosition) as String) ?: emptyList()
        val envAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, envNames)
        envAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        env_spinner.adapter = envAdapter

        if (isInit) currentServer?.let {
            Log.i(TAG, "updateServers: env position = ${envAdapter.getPosition(it.envName)}")
            env_spinner.setSelection(envAdapter.getPosition(it.envName)) }
    }

    private fun fetchServerData() {
        // Server list json file should be downloaded in external files dir, read it to generate serverMap
        val dir = filesDir.absolutePath
        val file = File("$dir/${ServerDataUtil.SERVER_LIST_FILE}")

        // Decipher it before use
        val decipher = RSAEncipherDecipher(BuildConfig.rsa_public_key, BuildConfig.rsa_private_key)
        file.bufferedReader().use {
            serverData = Gson()
                .fromJson(it, Array<ProductJsonData>::class.java)
                .toList()
                .toServerMap()
                .apply { decipher(decipher) }
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

        // environment TODO: new server list should replace this item
        environment = App.readStringFromSharedPreferences(App.ENVIRONMENT, "0")!!.toInt()

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

        // selected server information
        currentServer = ServerDataUtil.getInfo(sp)
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

        val serverChanged = ServerDataUtil.getInfo(sp) != currentServer

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
            currentServer?.let { ServerDataUtil.saveInfo(it, this) }

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

    private fun getFullServerList(serverMap: Map<String, List<ServerInfo>>): List<ServerInfo> {
        return serverMap.flatMap { it.value }
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