package telenav.demo.app.search

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.telenav.sdk.entity.model.base.Entity
import telenav.demo.app.R
import telenav.demo.app.map.MapActivity
import telenav.demo.app.widgets.RoundedBottomSheetLayout
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import telenav.demo.app.utils.*
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.entity.model.prediction.WordPrediction
import telenav.demo.app.databinding.SearchListBottomFragmentLayoutBinding
import telenav.demo.app.dip
import java.util.*
import telenav.demo.app.map.getUIExecutor

private const val TAG = "SearchListBottomFragment"

class SearchListBottomFragment : RoundedBottomSheetLayout() {

    private val dataCollectorClient by lazy { DataCollectorService.getClient() }
    private val viewModel: SearchInfoViewModel by viewModels()
    private var currentSearchHotCategoryTag: String? = null
    private var binding: SearchListBottomFragmentLayoutBinding? = null
    private var shouldUpdateWorkAddress = false
    private var shouldUpdateHomeAddress = false
    private var openedFromMainScreen = false
    private var popupWindow: PopupWindow? = null
    private var searchByPrediction = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SearchListBottomFragmentLayoutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        var shouldRequestRecentData = true
        if (shouldUpdateHomeAddress) {
            val homeEntity = viewModel.getHome(requireContext())
            homeEntity?.let {
                shouldRequestRecentData = false
                val name = if (homeEntity.place != null) {
                    homeEntity.place?.address?.formattedAddress
                } else {
                    homeEntity.address?.formattedAddress
                }

                binding?.search?.setText(name)
            }
        } else if (shouldUpdateWorkAddress) {
            val workEntity = viewModel.getWork(requireContext())
            workEntity?.let {
                shouldRequestRecentData = false
                val name = if (workEntity.place != null) {
                    workEntity.place?.address?.formattedAddress
                } else {
                    workEntity.address?.formattedAddress
                }
                binding?.search?.setText(name)
            }
        }

        if (shouldRequestRecentData) {
            viewModel.getRecentSearchData(requireContext())
        }

        binding?.search?.requestFocus()
        binding?.search?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                searchText()
            }
            false
        }

        binding?.search?.addTextChangedListener(
            object : TextWatcher {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                private var timer: Timer = Timer()
                private val DELAY: Long = 1000
                override fun afterTextChanged(text: Editable) {
                    timer.cancel()
                    timer = Timer()
                    timer.schedule(
                        object : TimerTask() {
                            override fun run() {
                                activity?.runOnUiThread {
                                    hidePredictions()
                                    if (text.toString().isNotBlank()) {
                                        binding?.clearText?.visibility = View.VISIBLE
                                        binding?.recentHeader?.visibility = View.GONE
                                        activity?.getUIExecutor()?.let {
                                            if (searchByPrediction) {
                                                searchByPrediction = false
                                                searchText()
                                            } else {
                                                val location =
                                                    (activity!! as MapActivity).lastKnownLocation
                                                viewModel.requestPrediction(
                                                    text.toString(),
                                                    location,
                                                    it
                                                )
                                                viewModel.requestSuggestions(
                                                    text.toString(),
                                                    location,
                                                    activity!!.getUIExecutor()
                                                )
                                            }
                                        }
                                    } else {
                                        binding?.clearText?.visibility = View.GONE
                                        binding?.recentHeader?.visibility = View.VISIBLE
                                        viewModel.getRecentSearchData(requireContext())
                                    }
                                }
                            }
                        },
                        DELAY
                    )
                }
            }
        )

        binding?.clearText?.setOnClickListener {
            hidePredictions()
            binding?.search?.setText("")
            binding?.recentHeader?.visibility = View.VISIBLE
            viewModel.getRecentSearchData(requireContext())
        }

        binding?.searchList?.layoutManager = LinearLayoutManager(activity)

        viewModel.searchResults.observe(viewLifecycleOwner, {
            Log.d(TAG, "result count ->  ${it.size} ")
            if (it.isNotEmpty()) {
                binding?.searchList?.visibility = View.VISIBLE
                binding?.searchError?.visibility = View.GONE
                viewModel.saveRecentSearchData(requireContext())
            } else {
                binding?.searchList?.visibility = View.GONE
                binding?.searchError?.visibility = View.GONE
                binding?.searchError?.text = getString(R.string.no_result)
            }
            initAdapter(it)
            (activity as MapActivity).displaySearchResults(it, currentSearchHotCategoryTag)
        })

        viewModel.searchResults.observe(viewLifecycleOwner, {
            Log.d(TAG, "result count ->  ${it.size} ")
            if (it.isNotEmpty()) {
                binding?.searchList?.visibility = View.VISIBLE
                binding?.searchError?.visibility = View.GONE
                viewModel.saveRecentSearchData(requireContext())
            } else {
                binding?.searchList?.visibility = View.GONE
                binding?.searchError?.visibility = View.GONE
                binding?.searchError?.text = getString(R.string.no_result)
            }
            initAdapter(it)
            (activity as MapActivity).displaySearchResults(it, currentSearchHotCategoryTag)
        })

        viewModel.suggestionResults.observe(viewLifecycleOwner, {
            Log.d(TAG, "result count ->  ${it.size} ")
            if (it.isNotEmpty()) {
                binding?.searchList?.visibility = View.VISIBLE
            } else {
                binding?.searchList?.visibility = View.GONE
            }
            initAdapter(it, binding?.search?.text.toString())
        })

        viewModel.savedAddress.observe(viewLifecycleOwner, {
            Log.d(TAG, "result count ->  ${it.size} ")
            if (it.isNotEmpty()) {
                binding?.searchList?.visibility = View.VISIBLE
                binding?.searchError?.visibility = View.GONE
            } else {
                binding?.searchList?.visibility = View.GONE
                binding?.searchError?.visibility = View.GONE
                binding?.searchError?.text = getString(R.string.no_result)
            }
            initAdapter(it)
        })

        viewModel.searchError.observe(viewLifecycleOwner, {
            if (!it.isNullOrBlank()) {
                binding?.searchError?.visibility = View.VISIBLE
                binding?.searchError?.text = it
            } else {
                binding?.searchError?.visibility = View.GONE
            }
        })

        viewModel.loading.observe(viewLifecycleOwner, {
            if (it) {
                binding?.searchLoading?.visibility = View.VISIBLE
            } else {
                binding?.searchLoading?.visibility = View.GONE
            }
        })

        viewModel.predictions.observe(viewLifecycleOwner, {
            showPredictionPopup(it)
        })

        binding?.searchListAreaBack?.setOnClickListener {
            (activity!! as MapActivity).hideKeyboard(binding?.search!!)
            if (shouldUpdateHomeAddress || shouldUpdateWorkAddress) {
                if (!openedFromMainScreen) {
                    (activity!! as MapActivity).showPersonalInfoFragment()
                }
            } else {
                (activity!! as MapActivity).updateBottomSheetState()
                (activity!! as MapActivity).updateBottomView()
            }
            dismiss()
        }
        if (binding?.search?.text?.isEmpty() == true) {
            binding?.clearText?.visibility = View.GONE
        } else {
            binding?.clearText?.visibility = View.VISIBLE
        }

        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        binding?.search?.postDelayed({
            binding?.search?.requestFocus()
            binding?.search?.let {
                inputMethodManager.showSoftInput(it, 0)
            }
        }, 500)
    }

    private fun searchText() {
        val loc = (activity!! as MapActivity).lastKnownLocation
        (activity!! as MapActivity).setLastSearch(binding?.search?.text.toString())
        (activity!! as MapActivity).hideKeyboard(binding?.search!!)
        binding?.searchList?.removeAllViewsInLayout()
        activity?.getUIExecutor()?.let {
            hidePredictions()
            viewModel.search(binding?.search?.text.toString(), null, loc, it)
        }
    }

    private fun initAdapter(items: List<Entity>, pattern: String = "") {
        binding?.searchList?.adapter = SearchResultsListRecyclerAdapter(items, requireContext(),
            object : SearchResultsListRecyclerAdapter.OnEntityClickListener {
                override fun onEntityClick(entity: Entity) {
                    (activity!! as MapActivity).hideKeyboard(binding?.search!!)
                    when {
                        shouldUpdateHomeAddress -> {
                            dataCollectorClient.setHome(requireActivity(), entity)
                            if (!openedFromMainScreen) {
                                (activity!! as MapActivity).showPersonalInfoFragment()
                            }
                            (activity as MapActivity).updateBottomView()
                        }
                        shouldUpdateWorkAddress -> {
                            dataCollectorClient.setWork(requireActivity(), entity)
                            if (!openedFromMainScreen) {
                                (activity!! as MapActivity).showPersonalInfoFragment()
                            }
                            (activity as MapActivity).updateBottomView()
                        }
                        else -> {
                            (activity as MapActivity).collapseBottomSheet()
                            (activity as MapActivity).displayEntityClicked(entity, currentSearchHotCategoryTag)
                        }
                    }
                    dismiss()
                }
            }, pattern
        )
    }

    private fun showPredictionPopup(predictions: List<WordPrediction>?) {
        hidePredictions()
        if (predictions == null) {
            return
        }
        binding?.recentHeader?.visibility = View.GONE
        val popupView: LinearLayout =
            LayoutInflater.from(requireActivity()).inflate(R.layout.prediction_window, null) as LinearLayout

        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        )

        predictions.forEachIndexed { index, word ->
            if (index != 0) {
                val view = View(requireActivity())
                view.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                popupView.addView(
                    view,
                    LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT)
                        .apply { setMargins(requireActivity().dip(10), 0,  requireActivity().dip(10), 0) }
                )
            }
            val view = TextView(requireActivity())
            view.text = word.predictWord
            view.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_c1))
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            if (index == 0) {
                view.setPadding(0, 0, requireActivity().dip(10), requireActivity().dip(30))
            } else {
                view.setPadding(0, 0, requireActivity().dip(10), requireActivity().dip(5))
            }
            view.ellipsize = TextUtils.TruncateAt.END
            view.setLines(1)

            val viewLine = LayoutInflater.from(requireActivity()).inflate(R.layout.view_verical_line, null)
            popupView.addView(
                view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            popupView.addView(
                viewLine,
                requireActivity().dip(1),
                requireActivity().dip(26)
            )

            view.setOnClickListener {
                if (it is TextView) {
                    searchByPrediction = true
                    var text = binding?.search?.text.toString()
                    val i = text.lastIndexOf(' ')
                    if (i >= 0) {
                        text = text.replaceAfterLast(' ', word.predictWord + ' ')
                        binding?.search?.setText(text)
                    } else
                        binding?.search?.setText(word.predictWord + ' ')
                    binding?.search?.setSelection(binding?.search?.text?.length ?: 0)
                    hidePredictions()
                }
            }
        }
        popupWindow!!.showAsDropDown(binding?.search)
    }

    private fun hidePredictions() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    fun popUpLogicCLose() {
        if (popupWindow != null) {
            hidePredictions()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(categoryTag: String?,
                        shouldUpdateWorkAddress: Boolean = false,
                        shouldUpdateHomeAddress: Boolean = false,
                        openedFromMainScreen: Boolean = false) =
            SearchListBottomFragment().apply {
                this.currentSearchHotCategoryTag = categoryTag
                this.shouldUpdateWorkAddress = shouldUpdateWorkAddress
                this.shouldUpdateHomeAddress = shouldUpdateHomeAddress
                this.openedFromMainScreen = openedFromMainScreen
            }
    }
}