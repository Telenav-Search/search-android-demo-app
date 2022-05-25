package telenav.demo.app.model

import android.content.Context
import android.content.SharedPreferences

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

import telenav.demo.app.R

data class JsonDataList(val products: List<ProductJsonData>) {
    /**
     * convert json data to a map, with region as key to lookup.
     * All information of a server is stored as ServerInfo, value will be a list of it.
     *
     * @return map, key - region, value - list of ServerInfo
     */
    fun toServerMap(): Map<String, List<ServerInfo>> {
        val map = mutableMapOf<String, MutableList<ServerInfo>>()
        products.forEach { product ->
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
}

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
    val apiKey: String,
    val apiSecret: String
)

object ServerDataUtil {
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