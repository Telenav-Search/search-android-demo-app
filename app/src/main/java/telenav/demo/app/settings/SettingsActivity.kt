package telenav.demo.app.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.telenav.sdk.core.SDKRuntime
import ir.androidexception.filepicker.dialog.DirectoryPickerDialog
import telenav.demo.app.R
import telenav.demo.app.initialization.saveIndexDataPath
import telenav.demo.app.initialization.saveSearchMode
import telenav.demo.app.model.SearchMode
import java.io.File


class SettingsActivity : AppCompatActivity() {

    private lateinit var vIndexDataPath: TextView
    private lateinit var vModeHybrid: RadioButton
    private lateinit var vModeOnBoard: RadioButton

    private var indexDataPath = ""
    private var searchMode = SearchMode.HYBRID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        vIndexDataPath = findViewById(R.id.settings_index_data_path)
        vModeHybrid = findViewById(R.id.settings_mode_hybrid)
        vModeOnBoard = findViewById(R.id.settings_mode_onboard)

        vModeHybrid.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) searchMode = SearchMode.HYBRID
        }
        vModeOnBoard.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) searchMode = SearchMode.ONBOARD
        }

        findViewById<View>(R.id.settings_back).setOnClickListener { finish() }
        findViewById<View>(R.id.settings_select_index_data_path).setOnClickListener { openDirectoryForIndex() }
        findViewById<View>(R.id.settings_save).setOnClickListener { save() }

        getSavedIndexPath()
        getSavedSearchMode()

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

    private fun getSavedIndexPath() {
        val prefs =
            getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

        val dataPath = prefs.getString(getString(R.string.saved_index_data_path_key), "")
        if (dataPath != null && dataPath.isNotEmpty()) {
            indexDataPath = dataPath
        }
    }

    private fun getSavedSearchMode() {
        val prefs =
            getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

        val mode = prefs.getInt(getString(R.string.saved_search_mode_key), 1)
        searchMode = SearchMode.fromInt(mode)
    }

    private fun save() {
        saveIndexDataPath(indexDataPath)
        saveSearchMode(searchMode)
        SDKRuntime.setNetworkAvailable(searchMode == SearchMode.HYBRID)
        finish()
    }
}