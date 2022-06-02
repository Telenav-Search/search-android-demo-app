package telenav.demo.app.settings

import android.content.Context
import android.location.Location
import android.util.Log

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.google.gson.Gson
import com.telenav.sdk.entity.model.base.Polygon

import telenav.demo.app.App
import telenav.demo.app.BuildConfig
import telenav.demo.app.R
import telenav.demo.app.model.*
import telenav.demo.app.utils.LocationUtil
import telenav.demo.app.utils.RSAEncipherDecipher
import telenav.demo.app.utils.toGeoPoint

import java.io.File

/**
 * ViewModel for Settings Activity
 *
 * @since 2022-05-23
 */
class SettingsViewModel: ViewModel() {
    companion object {
        private const val TAG = "SettingsViewModel"
    }

    // current selected server info (will be null for the first time getting in settings)
    var mCurrentServer: ServerInfo? = null

    private lateinit var mServerData: Map<String, List<ServerInfo>>

    private val _serverState = MutableLiveData<ServerState>()

    val serverState: LiveData<ServerState> = _serverState

    private var mCurrentRegion = "NA"

    private val _locations = MutableLiveData<SearchLocations>()

    val locations: LiveData<SearchLocations> = _locations

    // polygons to locate region
    private lateinit var mRegionPolygons: Map<String, Polygon>


    /**
     * init components which need context
     *
     * @param context Context
     */
    fun init(context: Context) {
        Log.i(TAG, "init")
        initLocations(context)
        initServerData(context)
    }

    private fun initLocations(context: Context) {
        // init locations using SharedPreferences
        val sp = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val cvpLat = sp.getFloat(App.KEY_CVP_LAT, LocationUtil.DEFAULT_LAT).toDouble()
        val cvpLong = sp.getFloat(App.KEY_CVP_LONG, LocationUtil.DEFAULT_LONG).toDouble()
        val salLat = sp.getFloat(App.KEY_SAL_LAT, LocationUtil.DEFAULT_LAT).toDouble()
        val salLong = sp.getFloat(App.KEY_SAL_LONG, LocationUtil.DEFAULT_LONG).toDouble()
        _locations.value = SearchLocations(cvpLocation = LocationUtil.createLocation(cvpLat, cvpLong),
            cvpFollowGPS = sp.getBoolean(App.KEY_CVP_GPS, true),
            searchAreaLocation = LocationUtil.createLocation(salLat, salLong),
            saFollowGPS = sp.getBoolean(App.KEY_SAL_GPS, true),
            saFollowCvp = sp.getBoolean(App.KEY_SAL_CVP, true))

        // read polygons from assets
        mRegionPolygons = LocationUtil.readPolygons(context)
        mCurrentRegion = locateRegion(_locations.value!!.searchAreaLocation)
    }

    private fun initServerData(context: Context) {
        mServerData = fetchServerData(context)
        mCurrentServer = ServerDataUtil.getInfo(context)

        // no server in current location
        if (mServerData[mCurrentRegion] == null || mServerData[mCurrentRegion]!!.isEmpty()) {
            _serverState.value = ServerState(emptyList(), emptyList(), 0, 0)
            return
        }

        // no current server or current server not in current region, assign a default current server
        val serverList = mServerData[mCurrentRegion]!!
        if (mCurrentServer == null || mCurrentServer?.region != mCurrentRegion) {
            Log.i(TAG, "initServerData: set default currentServer")
            mCurrentServer = serverList[0]
        }

        // set selected server information
        val serverNames = serverList.getProductNames()
        val serverName = mCurrentServer!!.productName
        val serverIndex = serverNames.indexOf(serverName)
        val envNames = serverList.getEnvNames(serverName)
        val envName = mCurrentServer!!.envName
        val envIndex = envNames.indexOf(envName)
        _serverState.value = ServerState(serverNames, envNames, serverIndex, envIndex)
        Log.i(TAG, "initServerData: current serverName = $serverName, envName = $envName")

        // observe location changes
        _locations.observe(context as LifecycleOwner) {
            val newRegion = locateRegion(it.searchAreaLocation)
            if (newRegion != mCurrentRegion) {
                onRegionChange(newRegion)
            }
        }
    }

    private fun onRegionChange(newRegion: String) {
        Log.i(TAG, "onRegionChange: newRegion = $newRegion")
        val serverList = mServerData[newRegion] ?: emptyList()
        mCurrentRegion = newRegion
        if (serverList.isEmpty()) {
            _serverState.value = ServerState(emptyList(), emptyList(), 0, 0)
            return
        }

        val serverNames = serverList.getProductNames()
        val envNames = serverList.getEnvNames(serverNames[0])
        _serverState.value = ServerState(serverNames, envNames, 0, 0)
    }

    /**
     * User select event
     *
     * @param position index in spinner
     */
    fun onEngineChange(position: Int) {
        Log.i(TAG, "onEngineChange: position = $position")
        // must have value before it can be clicked
        _serverState.value ?: return
        val state = _serverState.value!!
        val serverName = state.serverNames[position]

        // serverNames have no changes
        _serverState.value = state.copy(
            serverSelected = position,
            envNames = mServerData[mCurrentRegion]?.getEnvNames(serverName) ?: emptyList(),
            envSelected = 0)

        // record serverInfo
        mCurrentServer = mServerData[mCurrentRegion]?.find {
            it.productName == serverName && it.envName == _serverState.value!!.envNames[0]
        }
    }

    /**
     * User select event
     *
     * @param position index in spinner
     */
    fun onEnvChange(position: Int) {
        Log.i(TAG, "onEnvChange: position = $position")
        // must have value before it can be clicked
        _serverState.value ?: return
        val state = _serverState.value!!
        _serverState.value = state.copy(envSelected = position)

        // record serverInfo
        val serverName = state.serverNames[state.serverSelected]
        mCurrentServer = mServerData[mCurrentRegion]?.find {
            it.productName == serverName && it.envName == state.envNames[position]
        }
    }

    /**
     * User location intent
     *
     * @param intent user action
     */
    fun onLocationChange(intent: LocationIntent) {
        // location must be initialized before interaction
        val location = _locations.value ?: return
        _locations.value = when(intent) {
            is CvpFollowGPS -> {
                if (location.saFollowCvp) {
                    location.copy(cvpFollowGPS = intent.status, saFollowGPS = intent.status)
                } else {
                    location.copy(cvpFollowGPS = intent.status)
                }
            }
            is SalFollowGPS -> { location.copy(saFollowGPS = intent.status) }
            is SalFollowCVP -> {
                if (intent.status) {
                    location.copy(saFollowGPS = location.cvpFollowGPS,
                        searchAreaLocation = location.cvpLocation,
                        saFollowCvp = true)
                }
                else { location.copy(saFollowCvp = false) }
            }
            is CvpChange -> {
                if (location.saFollowCvp) {
                    location.copy(cvpLocation = intent.location,
                        searchAreaLocation = intent.location,
                        cvpFollowGPS = false,
                        saFollowGPS = false)
                } else {
                    location.copy(cvpLocation = intent.location, cvpFollowGPS = false)
                }
            }
            is SalChange -> {
                location.copy(searchAreaLocation = intent.location, saFollowGPS = false, saFollowCvp = false)
            }
        }
    }

    private fun fetchServerData(context: Context): Map<String, List<ServerInfo>> {
        // Server list json file should be downloaded in external files dir, read it to generate serverMap
        val dir = context.filesDir.absolutePath
        val file = File("$dir/${ServerDataUtil.SERVER_LIST_FILE}")

        // Decipher it before use
        val decipher = RSAEncipherDecipher(BuildConfig.rsa_public_key, BuildConfig.rsa_private_key)
        file.bufferedReader().use {
            return Gson()
                .fromJson(it, Array<ProductJsonData>::class.java)
                .toList()
                .toServerMap()
                .apply {
                    decipher(decipher)
                    Log.i(TAG, "fetchServerData: available regions = $keys," +
                            " total server count = ${values.flatten().size}")
                }
        }
    }

    private fun locateRegion(location: Location): String {
        mRegionPolygons.forEach {
            if (LocationUtil.isInPolygon(location.toGeoPoint(), it.value)) return it.key
        }
        Log.w(TAG, "locateRegion: location not in region, using default NA." +
                " lat = ${location.latitude}, long = ${location.longitude}")
        return "NA" // set "NA" as default, if location not in any polygon we have
    }

    data class ServerState(
        var serverNames: List<String>,
        var envNames: List<String>,
        var serverSelected: Int,
        var envSelected: Int
    )
}
