package telenav.demo.app.homepage

import android.Manifest
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.Editable
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
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.prediction.EntityWordPredictionResponse
import com.telenav.sdk.entity.model.prediction.WordPrediction
import telenav.demo.app.R
import telenav.demo.app.searchlist.SearchListActivity
import telenav.demo.app.searchlist.dip


class HomePageActivity : AppCompatActivity() {
    private val telenavService: EntityClient by lazy { EntityService.getClient() }
    var lastKnownLocation: Location? = null

    private lateinit var vSearchInput: EditText
    private lateinit var vSearchInputClear: View

    private var popupWindow: PopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        checkGPSPermission()

        vSearchInput = findViewById(R.id.search_input)
        vSearchInputClear = findViewById(R.id.search_input_clear)

        setupSearchField()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (popupWindow != null) {
                hidePredictions()
            } else {
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

                startActivity(Intent(this, SearchListActivity::class.java).apply {
                    putExtra(SearchListActivity.PARAM_QUERY, view.text.toString())
                })

                true
            } else
                false
        }
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
        if (text.isEmpty())
            return

        telenavService.wordPredictionRequest()
            .setQuery(text)
            .setLocation(location.latitude, location.longitude)
            .setLimit(10)
            .asyncCall(
                object : Callback<EntityWordPredictionResponse> {
                    override fun onSuccess(response: EntityWordPredictionResponse) {
                        runOnUiThread {
                            showPredictionPopup(response.results)
                        }
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
                vSearchInput.setSelection(vSearchInput.text.length);
                hidePredictions()
            }
        }

        popupWindow!!.showAsDropDown(vSearchInput);
    }

    private fun hidePredictions() {
        popupWindow?.dismiss();
        popupWindow = null;
    }

    private fun checkGPSPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !this.checkLocationPermission())
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        else {
            getLocation()
        }
    }

    private fun getLocation() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation
                .addOnSuccessListener(this) { location ->
                    if (location != null) {
                        lastKnownLocation = location;
                    }
                }
        } catch (e: SecurityException) {

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            getLocation()

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun Context.checkLocationPermission(): Boolean =
    checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED
