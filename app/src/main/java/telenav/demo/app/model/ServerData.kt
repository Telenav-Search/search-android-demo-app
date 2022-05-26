package telenav.demo.app.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

import telenav.demo.app.R
import telenav.demo.app.utils.RSAEncipherDecipher

import java.io.File
import java.net.URL

data class ProductJsonData(
    @SerializedName("product_name") val productName: String,
    val environments: List<EnvJsonData>
)

data class EnvJsonData(
    @SerializedName("env_name") val envName: String,
    @SerializedName("available_regions") val regions: List<ServerJsonData>
)

data class ServerJsonData(
    val region: String,
    val endpoint: String,
    @SerializedName("api_key") val apiKey: String,
    @SerializedName("api_secret") val apiSecret: String
)

data class ServerInfo(
    val productName: String,
    val envName: String,
    val region: String,
    val endpoint: String,
    var apiKey: String, // apiKey & Secret can be encrypted, might need replacement after decipher
    var apiSecret: String
)

object ServerDataUtil {
    // server list file will be saved to files dir(internal).
    const val SERVER_LIST_FILE = "server_list.json"
    private const val DOWNLOAD_URL = "https://resourcedev.telenav.com/resources/services/entity/server_list.json"
    private const val TAG = "ServerDataUtil"
    private const val KEY_SERVER_INFO = "key_server_info"

    /**
     * save current ServerInfo to SharedPreference
     *
     * @param info ServerInfo instance to save
     * @param context Context
     */
    fun saveInfo(info: ServerInfo, context: Context) = saveInfo(info,
        context.getSharedPreferences(context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE).edit()).apply()

    /**
     * save current ServerInfo to SharedPreference.Editor without apply/commit
     * in case of batch write
     *
     * @param info ServerInfo instance to save
     * @param editor SharedPreferences.Editor
     * @return Editor for chain write
     */
    fun saveInfo(info: ServerInfo, editor: SharedPreferences.Editor): SharedPreferences.Editor =
        editor.apply { putString(KEY_SERVER_INFO, Gson().toJson(info)) }

    /**
     * restore ServerInfo from SharedPreference
     *
     * @param context Context
     * @return ServerInfo saved in SharedPreference (will return null if not saved yet)
     */
    fun getInfo(context: Context): ServerInfo? =
        getInfo(context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE))

    /**
     * restore ServerInfo from SharedPreference
     *
     * @param sharedPreferences SharedPreferences
     * @return ServerInfo saved in SharedPreference (will return null if not saved yet)
     */
    fun getInfo(sharedPreferences: SharedPreferences): ServerInfo? {
        sharedPreferences.getString(KEY_SERVER_INFO, null)?.let {
            return Gson().fromJson(it, ServerInfo::class.java)
        }
        return null
    }

    /**
     * Fetch server list from webserver
     *
     * @param context Context
     */
    fun fetchServerList(context: Context) {
        val dir = context.filesDir.absolutePath
        val oldFile = File("$dir/$SERVER_LIST_FILE")
        if (oldFile.exists()) {
            // old list exist, download new one and replace it.
            val newFile = File("$dir/${SERVER_LIST_FILE}_temp")
            newFile.delete()
            downloadFromUrl(newFile)
            oldFile.delete()
            newFile.renameTo(oldFile)
        } else {
            downloadFromUrl(oldFile)
        }
    }

    private fun downloadFromUrl(output: File) {
        output.outputStream().use { outputStream ->
            URL(DOWNLOAD_URL).openConnection().getInputStream().use {
                it.copyTo(outputStream)
            }
        }
        Log.i(TAG, "downloadFromUrl: download finish")
    }
}

/**
 * get every product names from a list of server info
 *
 * @return list of product names
 */
fun List<ServerInfo>.getProductNames(): List<String> {
    return map { it.productName }.distinct()
}

/**
 * get available env names with a product name
 *
 * @return list of env names
 */
fun List<ServerInfo>.getEnvNames(name: String): List<String> {
    return filter { it.productName == name }.map { it.envName }
}

/**
 * convert json data to a map, with region as key to lookup.
 * All information of a server is stored as ServerInfo, value will be a list of it.
 *
 * @return map, key - region, value - list of ServerInfo
 */
fun List<ProductJsonData>.toServerMap(): Map<String, List<ServerInfo>> {
    val map = mutableMapOf<String, MutableList<ServerInfo>>()
    forEach { product ->
        product.environments.forEach { env ->
            env.regions.forEach { server ->
                val list = map.getOrPut(server.region) { mutableListOf() }
                list.add(
                    ServerInfo(
                        productName = product.productName,
                        envName = env.envName,
                        region = server.region,
                        endpoint = server.endpoint,
                        apiKey = server.apiKey,
                        apiSecret = server.apiSecret
                    )
                )
            }
        }
    }
    return map
}

/**
 * ServerInfo come from webservers will be encrypted, need to decipher before use.
 * Decipher apiKeys and apiSecrets
 *
 * @param rsaEncipherDecipher Decipher
 */
fun Map<String, List<ServerInfo>>.decipher(rsaEncipherDecipher: RSAEncipherDecipher) {
    forEach { (_, serverInfoList) ->
        serverInfoList.forEach {
            it.apiKey = rsaEncipherDecipher.decrypt(it.apiKey)
            it.apiSecret = rsaEncipherDecipher.decrypt(it.apiSecret)
        }
    }
}