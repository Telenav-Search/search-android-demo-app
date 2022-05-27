package telenav.demo.app.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.google.gson.Gson

import telenav.demo.app.BuildConfig
import telenav.demo.app.model.*
import telenav.demo.app.utils.RSAEncipherDecipher

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


    /**
     * init components which need context
     *
     * @param context Context
     */
    fun init(context: Context) {
        Log.i(TAG, "init")
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
            Log.i(TAG, "init: set default currentServer")
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
        Log.i(TAG, "init: current serverName = $serverName, envName = $envName")
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

    data class ServerState(
        var serverNames: List<String>,
        var envNames: List<String>,
        var serverSelected: Int,
        var envSelected: Int
    )
}
