package telenav.demo.app.search

import android.R
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.telenav.sdk.core.Callback
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.prediction.EntityWordPredictionResponse
import com.telenav.sdk.entity.model.prediction.WordPrediction
import kotlinx.android.synthetic.main.search_bottom_fragment_layout.*
import telenav.demo.app.databinding.SearchBottomFragmentLayoutBinding
import telenav.demo.app.dip
import telenav.demo.app.homepage.HotCategory
import telenav.demo.app.homepage.SuggestionRecyclerAdapter
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.map.MapActivity
import telenav.demo.app.search.filters.Filter
import telenav.demo.app.search.filters.FiltersFragment
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.widgets.RoundedBottomSheetLayout
import java.util.concurrent.Executor

private const val TAG = "SearchBottomFragment"

class SearchBottomFragment : RoundedBottomSheetLayout() {

    companion object {
        const val CATEGORY_SEARCH = 0
    }

    private val viewModel: SearchViewModel by viewModels()
    private var searchType: Int = CATEGORY_SEARCH
    private var lastLaunchedPrediction: String = ""
    private lateinit var currentSearchHotCategory: String
    private var popupWindow: PopupWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = SearchBottomFragmentLayoutBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFilters()
        initSuggestions()
    }


    private fun predictSearchWord() {
        val location = (activity!! as MapActivity).lastKnownLocation ?: Location("")
        val text = search.text.toString()
        lastLaunchedPrediction = text
        hidePredictions()
        if (text.isEmpty())
            return
        activity?.getUIExecutor()?.let {
           viewModel.requestPrediction(text, location, it)
        }
    }

    private fun showPredictionPopup(predictions: List<WordPrediction>?) {
        hidePredictions()
        if (predictions == null) {
            return
        }
        val popupView: LinearLayout =
            LayoutInflater.from(requireActivity()).inflate(telenav.demo.app.R.layout.prediction_window, null) as LinearLayout

        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        )

        predictions.forEachIndexed { index, word ->
            if (index != 0) {
                val view = View(requireActivity())
                view.setBackgroundColor(0x80FFFFFF.toInt())
                popupView.addView(
                    view,
                    LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT)
                        .apply { setMargins(20, 0, 20, 0) }
                )
            }
            val view = TextView(requireActivity())
            view.text = word.predictWord
            view.setTextColor(0xFFFFFFFF.toInt())
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            view.setPadding(0, requireActivity().dip(10), 0, requireActivity().dip(10))
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
                var text = search.text.toString()
                val i = text.lastIndexOf(' ')
                if (i >= 0) {
                    text = text.replaceAfterLast(' ', word.predictWord + ' ')
                    search.setText(text)
                } else
                    search.setText(word.predictWord + ' ')
                search.setSelection(search.text.length)
                hidePredictions()
            }
        }
        popupWindow!!.showAsDropDown(search)
    }

    private fun hidePredictions() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun initSuggestions() {
        val location = (activity!! as MapActivity).lastKnownLocation ?: Location("")

        search.doOnTextChanged { text, _, _, _ ->

            if (text.isNullOrBlank()) {
                categories_layout.visibility = View.VISIBLE
                suggestions_layout.visibility = View.GONE
            } else {
                categories_layout.visibility = View.GONE
                suggestions_layout.visibility = View.VISIBLE
            }
            predictSearchWord()
            viewModel.writingText(text.toString(), location, activity!!.getUIExecutor())
        }

        search.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val loc = (activity!! as MapActivity).lastKnownLocation
                (activity!! as MapActivity).setLastSearch(search.text.toString())
                activity?.getUIExecutor()?.let {
                    currentSearchHotCategory = search.text.toString()
                    viewModel.search(search.text.toString(), null,loc,
                        it
                    )
                }
            }
            false
        })

        suggestionsList.layoutManager = LinearLayoutManager(activity)

        viewModel.suggestionsError.observe(this, Observer {
            if (!it) suggestionsError.visibility = View.GONE
            else
                suggestionsError.visibility = View.VISIBLE
        })
        viewModel.suggestionList.observe(this, Observer {
            if (it.isNotEmpty()) {
                suggestions.visibility = View.VISIBLE
                suggestionsEmpty.visibility = View.GONE
            } else {
                suggestions.visibility = View.GONE
                suggestionsEmpty.visibility = View.VISIBLE
            }
            suggestionsList.adapter = SuggestionRecyclerAdapter(it) { suggestion ->
                Log.d("test", "click suggestion ${Gson().toJson(suggestion)}")
                val loc = (activity!! as MapActivity).lastKnownLocation
                (activity!! as MapActivity).setLastSearch(suggestion.formattedLabel)
                activity?.getUIExecutor()?.let {
                    currentSearchHotCategory = suggestion.formattedLabel
                    viewModel.search(suggestion.formattedLabel, null, loc,
                        it
                    )
                }
            }
        })
        viewModel.predictions.observe(this, Observer {
            showPredictionPopup(it)
        })
    }

    private fun initFilters() {
        CategoryAndFiltersUtil.hotCategoriesList.forEach {
            addCategoryChip(it)
        }
        show_filters.setOnClickListener {
            goToFiltersFragment()
        }

        viewModel.searchResults.observe(this, Observer {
            Log.d(TAG, "result count ->  ${it.size} ")
            (activity as MapActivity).displaySearchResults(
                it as List<Entity>?,
                currentSearchHotCategory
            )
            (activity!! as MapActivity).showSearchHotCategoriesFragment(
                it as List<Entity>, currentSearchHotCategory
            )
            fragmentManager?.beginTransaction()?.remove(this)?.commit()
        })
    }

    private fun goToFiltersFragment() {
        val filtersFragment =
            FiltersFragment()
        activity?.supportFragmentManager?.let { it1 ->
            filtersFragment.show(
                it1,
                filtersFragment.tag
            )
        }
    }

    private fun addCategoryChip(hotCategory: HotCategory) {
        val chip = Chip(context)
        chip.text = hotCategory.name
        chip.chipIcon = ContextCompat.getDrawable(requireContext(), hotCategory.iconPurple)

        val states = arrayOf(
            intArrayOf(R.attr.state_enabled),
            intArrayOf(R.attr.state_pressed)
        )

        val colors = intArrayOf(
            Color.BLACK,
            Color.BLUE
        )

        chip.chipIconTint = ColorStateList(states, colors)
        // following lines are for the demo
        chip.chipBackgroundColor =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    CategoryAndFiltersUtil.categoriesColors.random()
                )
            )
        chip.isClickable = true
        chip.isCheckable = false
        chip.isCloseIconVisible = false
        chip_group.addView(chip as View)
        chip.setOnClickListener {
            if (hotCategory.tag.isEmpty()) {
                (activity as MapActivity).showSubcategoriesFragment()
                fragmentManager?.beginTransaction()?.remove(this)?.commit()
            } else {
                currentSearchHotCategory = hotCategory.tag
                val location = (activity!! as MapActivity).lastKnownLocation
                activity?.getUIExecutor()?.let { executor ->
                    when (searchType) {
                        CATEGORY_SEARCH -> {
                            (activity!! as MapActivity).setLastSearch(hotCategory.tag)
                            viewModel.search(
                                null, hotCategory.tag, location, executor
                            )
                        }
                    }
                }
            }
        }
    }

    fun setSearchType(searchType: Int) {
        this.searchType = searchType
    }

    fun setFilters(filters: List<Filter>) {
        viewModel.filters = filters
    }

    fun popUpLogicCLose() {
        if (popupWindow != null) {
            hidePredictions()
        }
    }
}