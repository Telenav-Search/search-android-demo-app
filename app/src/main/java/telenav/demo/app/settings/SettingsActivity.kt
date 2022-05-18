package telenav.demo.app.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_settings.*

import ir.androidexception.filepicker.dialog.DirectoryPickerDialog

import com.telenav.sdk.core.SDKRuntime

import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.initialization.SearchMode
import telenav.demo.app.initialization.saveIndexDataPath
import telenav.demo.app.initialization.saveSearchMode
import telenav.demo.app.map.MapActivity.Companion.IS_ENV_CHANGED

import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var vIndexDataPath: TextView
    private lateinit var vModeHybrid: RadioButton
    private lateinit var vModeOnBoard: RadioButton

    private var indexDataPath = ""
    private var searchMode = SearchMode.HYBRID
    private var environment = 0

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

        findViewById<View>(R.id.settings_back).setOnClickListener { save() }
        findViewById<View>(R.id.settings_select_index_data_path).setOnClickListener { openDirectoryForIndex() }
        environment = App.readStringFromSharedPreferences(App.ENVIRONMENT,
                "0")!!.toInt()
        val adapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(this, R.array.env, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(environment)
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
        if (spinner.selectedItemPosition != environment) {
            App.writeStringToSharedPreferences(App.ENVIRONMENT,
                    spinner.selectedItemPosition.toString())
            val data = Intent()
            data.putExtra(IS_ENV_CHANGED, true)
            setResult(RESULT_OK, data)
        }
        finish()
    }
}