package telenav.demo.app.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.telenav.sdk.core.SDKRuntime
import ir.androidexception.filepicker.dialog.DirectoryPickerDialog
import kotlinx.android.synthetic.main.activity_settings.*
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.initialization.SearchMode
import telenav.demo.app.initialization.saveIndexDataPath
import telenav.demo.app.initialization.saveSearchMode
import telenav.demo.app.map.MapActivity.Companion.IS_ENV_CHANGED
import telenav.demo.app.search.PREDICTIONS_LIMIT_DEF
import telenav.demo.app.search.SEARCH_LIMIT_WITH_FILTERS
import telenav.demo.app.search.SUGGESTIONS_LIMIT_DEF
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

        findViewById<View>(R.id.settings_back).setOnClickListener { finish() }
        findViewById<View>(R.id.settings_select_index_data_path).setOnClickListener { openDirectoryForIndex() }
        findViewById<View>(R.id.settings_save).setOnClickListener { save() }
        search_bar.progress = App.readStringFromSharedPreferences(App.SEARCH_LIMIT,
                SEARCH_LIMIT_WITH_FILTERS.toString())!!.toInt()
        count_search.text = App.readStringFromSharedPreferences(App.SEARCH_LIMIT,
                SEARCH_LIMIT_WITH_FILTERS.toString())
        search_bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                count_search.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        sugg_bar.progress = App.readStringFromSharedPreferences(App.SUGGESTIONS_LIMIT,
                SUGGESTIONS_LIMIT_DEF.toString())!!.toInt()
        count_sugg.text = App.readStringFromSharedPreferences(App.SUGGESTIONS_LIMIT,
                SUGGESTIONS_LIMIT_DEF.toString())
        sugg_bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                count_sugg.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        predictions_bar.progress = App.readStringFromSharedPreferences(App.PREDICTIONS_LIMIT,
                PREDICTIONS_LIMIT_DEF.toString())!!.toInt()
        count_predictions.text = App.readStringFromSharedPreferences(App.PREDICTIONS_LIMIT,
                PREDICTIONS_LIMIT_DEF.toString())
        predictions_bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                count_predictions.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
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

    private fun saveLimits() {
        App.writeStringToSharedPreferences(App.SEARCH_LIMIT,
                search_bar.progress.toString())
        if (search_bar.progress < App.readFromSharedPreferences(App.FILTER_NUMBER)) {
            App.writeToSharedPreferences(
                    App.FILTER_NUMBER,
                    search_bar.progress)
        }
        App.writeStringToSharedPreferences(App.SUGGESTIONS_LIMIT,
                sugg_bar.progress.toString())
        App.writeStringToSharedPreferences(App.PREDICTIONS_LIMIT,
                predictions_bar.progress.toString())
    }

    private fun save() {
        saveIndexDataPath(indexDataPath)
        saveSearchMode(searchMode)
        saveLimits()
        SDKRuntime.setNetworkAvailable(searchMode == SearchMode.HYBRID)
        if (spinner.selectedItemPosition != environment) {
            App.writeStringToSharedPreferences(App.ENVIRONMENT,
                    spinner.selectedItemPosition.toString())
            val data = Intent()
            data.putExtra(IS_ENV_CHANGED, true)
            setResult(RESULT_OK, data);
        }
        finish()
    }
}