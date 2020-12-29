package telenav.demo.app.homepage

import android.app.Activity
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.telenav.sdk.core.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.prediction.EntityWordPredictionResponse
import com.telenav.sdk.entity.model.prediction.WordPrediction
import telenav.demo.app.R
import telenav.demo.app.dip
import telenav.demo.app.personalinfo.PersonalInfoActivity
import telenav.demo.app.searchlist.SearchListFragment
import telenav.demo.app.setGPSListener
import telenav.demo.app.settings.SettingsActivity
import telenav.demo.app.stopGPSListener
import java.util.concurrent.Executor


class HomePageActivity : AppCompatActivity() {
    private val telenavService: EntityClient by lazy { EntityService.getClient() }
    var lastKnownLocation: Location? = null
    var lastLaunchedPrediction: String = ""
    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            lastKnownLocation = locationResult.lastLocation
        }
    }

    private lateinit var vSearchInput: EditText
    private lateinit var vSearchInputClear: View

    private var popupWindow: PopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        vSearchInput = findViewById(R.id.search_input)
        vSearchInputClear = findViewById(R.id.search_input_clear)

        setupSearchField()

        findViewById<View>(R.id.app_personal_info).setOnClickListener { showPersonalInfoActivity() }
        findViewById<View>(R.id.app_mode_select).setOnClickListener { showSettingsActivity() }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (popupWindow != null) {
                hidePredictions()
            } else if (!removeTopFragment()) {
                showCategoriesFragment()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setupSearchField() {
        vSearchInputClear.setOnClickListener {
            vSearchInput.setText("")
            vSearchInput.hideKeyboard()
            hidePredictions()
        }
        vSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(text: CharSequence, p1: Int, p2: Int, p3: Int) {
                if (text.isEmpty()) {
                    vSearchInputClear.visibility = View.GONE
                    showCategoriesFragment()
                } else {
                    vSearchInputClear.visibility = View.VISIBLE
                    showSuggestionFragment(text.toString())
                }
                predictSearchWord()
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })
        vSearchInput.setText("")
        vSearchInput.setOnEditorActionListener { view, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                showSearchFragment(SearchListFragment.newInstance(view.text.toString()))
                true
            } else
                false
        }
    }

    fun removeTopFragment(): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return true
        }
        return false
    }

    fun showSearchFragment(fragment: SearchListFragment) {
        hidePredictions()
        supportFragmentManager.beginTransaction().add(R.id.fragmentFrame, fragment)
            .addToBackStack("").commit()
    }

    private fun showCategoriesFragment() {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentFrame, CategoriesFragment())
            .commit()
    }

    private fun showSuggestionFragment(text: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentFrame, SuggestionFragment.newInstance(text))
            .commit()
    }

    private fun predictSearchWord() {
        val location = lastKnownLocation ?: Location("")
        val text = vSearchInput.text.toString()
        lastLaunchedPrediction = text
        hidePredictions()
        if (text.isEmpty())
            return

        telenavService.wordPredictionRequest()
            .setQuery(text)
            .setLocation(location.latitude, location.longitude)
            .setLimit(10)
            .asyncCall(getUIExecutor(),
                object : Callback<EntityWordPredictionResponse> {
                    override fun onSuccess(response: EntityWordPredictionResponse) {
                        if (lastLaunchedPrediction == text)
                            showPredictionPopup(response.results)
                    }

                    override fun onFailure(p1: Throwable?) {
                        Log.e("testapp", "onFailure prediction ${text}", p1)
                    }
                }
            )
    }

    private fun showPredictionPopup(predictions: List<WordPrediction>?) {
        hidePredictions()
        if (predictions == null) {
            return
        }
        val popupView: LinearLayout =
            LayoutInflater.from(this).inflate(R.layout.prediction_window, null) as LinearLayout

        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        )

        predictions.forEachIndexed { index, word ->
            if (index != 0) {
                val view = View(this)
                view.setBackgroundColor(0x80FFFFFF.toInt())
                popupView.addView(
                    view,
                    LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT)
                        .apply { setMargins(20, 0, 20, 0) }
                )
            }
            val view = TextView(this)
            view.text = word.predictWord
            view.setTextColor(0xFFFFFFFF.toInt())
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            view.setPadding(0, dip(10), 0, dip(10))
            view.ellipsize = TextUtils.TruncateAt.END
            view.setLines(1)
            popupView.addView(
                view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            view.setOnClickListener {
                var text = vSearchInput.text.toString()
                val i = text.lastIndexOf(' ')
                if (i >= 0) {
                    text = text.replaceAfterLast(' ', word.predictWord + ' ')
                    vSearchInput.setText(text)
                } else
                    vSearchInput.setText(word.predictWord + ' ')
                vSearchInput.setSelection(vSearchInput.text.length)
                hidePredictions()
            }
        }

        popupWindow!!.showAsDropDown(vSearchInput)
    }

    private fun hidePredictions() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun showPersonalInfoActivity() {
        startActivity(Intent(this, PersonalInfoActivity::class.java))
    }

    private fun showSettingsActivity() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        setGPSListener(locationCallback)
    }

    override fun onPause() {
        stopGPSListener(locationCallback)
        super.onPause()
    }
}

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun Activity.getUIExecutor(): Executor {
    return Executor { r -> runOnUiThread(r) }
}