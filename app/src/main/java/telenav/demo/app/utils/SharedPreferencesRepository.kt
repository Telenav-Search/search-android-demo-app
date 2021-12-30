package telenav.demo.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SharedPreferencesRepository(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val preferences = context.getSharedPreferences(
        PREF_FILE_KEY, Context.MODE_PRIVATE)

    private val encryptedPreferences = EncryptedSharedPreferences.create(
        PREF_FILE_KEY_ENCRYPTED,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var searchMode = IntPreference(preferences, PREF_SEARCH_MODE)
    var indexDataPath = StringPreference(preferences, PREF_INDEX_PATH)
    var cloudEndpoint = StringPreference(preferences, PREF_CLOUD_ENDPOINT)
    var apiKey = StringPreference(preferences, PREF_API_KEY)
    var deviceId = StringPreference(preferences, PREF_DEVICE_ID)
    var favoriteList = StringPreference(preferences, PREF_FAVORITE_LIST)
    var homeAddress = StringPreference(preferences, PREF_HOME_ADDRESS)
    var workAddress = StringPreference(preferences, PREF_WORK_ADDRESS)
    var apiSecret = StringPreference(encryptedPreferences, PREF_API_SECRET)
    var updateStatus = BooleanPreference(preferences, PREF_UPDATE_STATUS)

    fun isContainsHomeAddressKey() = preferences.contains(PREF_HOME_ADDRESS)
    fun isContainsWorkAddressKey() = preferences.contains(PREF_WORK_ADDRESS)

    class StringPreference(private val preferences: SharedPreferences,
                           private val key: String) {
        var value: String
            get() = getStringPreference()
            set(value) = saveStringPreference(value)

        private fun getStringPreference(): String {
            return preferences.getString(key, "") ?: ""
        }

        private fun saveStringPreference(newValue: String) {
            with(preferences.edit()) {
                putString(key, newValue)
                apply()
            }
        }

        fun removeStringPreference() {
            with(preferences.edit()) {
                remove(key)
                apply()
            }
        }
    }

    class IntPreference(private val preferences: SharedPreferences,
                        private val key: String) {
        var value: Int
            get() = getIntPreference()
            set(value) = saveIntPreference(value)

        private fun getIntPreference(): Int {
            return preferences.getInt(key, 1)
        }

        private fun saveIntPreference(newValue: Int) {
            with(preferences.edit()) {
                putInt(key, newValue)
                apply()
            }
        }
    }

    class BooleanPreference(private val preferences: SharedPreferences,
                            private val key: String) {
        var value: Boolean
            get() = getBooleanPreference()
            set(value) = saveBooleanPreference(value)

        private fun getBooleanPreference(): Boolean {
            return preferences.getBoolean(key, false)
        }

        private fun saveBooleanPreference(newValue: Boolean) {
            with(preferences.edit()) {
                putBoolean(key, newValue)
                apply()
            }
        }

        fun removeBooleanPreference() {
            with(preferences.edit()) {
                remove(key)
                apply()
            }
        }
    }

    companion object {
        const val PREF_FILE_KEY = "telenav.demo.app.storage"
        const val PREF_FILE_KEY_ENCRYPTED = "telenav.demo.app.storage.encrypted"
        const val PREF_API_KEY = "api_key"
        const val PREF_API_SECRET = "api_secret_key"
        const val PREF_INDEX_PATH = "index_data_path"
        const val PREF_SEARCH_MODE = "search_mode"
        const val PREF_CLOUD_ENDPOINT = "cloud_endpoint_key"
        const val PREF_DEVICE_ID = "device_id"
        const val PREF_FAVORITE_LIST = "favorite_list"
        const val PREF_HOME_ADDRESS = "home_address"
        const val PREF_WORK_ADDRESS = "work_address"
        const val PREF_UPDATE_STATUS = "update_status"

        private var instance : SharedPreferencesRepository? = null

        fun init(context: Context) {
            instance = SharedPreferencesRepository(context)
        }

        fun getInstance(): SharedPreferencesRepository {
            return instance!!
        }
    }
}