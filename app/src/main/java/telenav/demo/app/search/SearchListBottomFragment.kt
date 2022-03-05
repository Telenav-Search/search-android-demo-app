package telenav.demo.app.search

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.telenav.sdk.entity.model.base.Entity
import kotlinx.android.synthetic.main.search_info_bottom_fragment_layout.*
import telenav.demo.app.R
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.map.MapActivity
import telenav.demo.app.widgets.RoundedBottomSheetLayout
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import telenav.demo.app.utils.*
import com.telenav.sdk.datacollector.api.DataCollectorService
import telenav.demo.app.databinding.SearchListBottomFragmentLayoutBinding

private const val TAG = "SearchListBottomFragment"

class SearchListBottomFragment : RoundedBottomSheetLayout() {

    private val dataCollectorClient by lazy { DataCollectorService.getClient() }
    private val viewModel: SearchInfoViewModel by viewModels()
    private var currentSearchHotCategoryTag: String? = null
    private var binding: SearchListBottomFragmentLayoutBinding? = null
    private var shouldUpdateWorkAddress = false
    private var shouldUpdateHomeAddress = false
    private var openedFromMainScreen = false

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
                search.setText(homeEntity.place.address.formattedAddress)
            }
        } else if (shouldUpdateWorkAddress) {
            val workEntity = viewModel.getWork(requireContext())
            workEntity?.let {
                shouldRequestRecentData = false
                search.setText(workEntity.place.address.formattedAddress)
            }
        }

        if (shouldRequestRecentData) {
            viewModel.getRecentSearchData(requireContext())
        }

        search.requestFocus()
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                val loc = (activity!! as MapActivity).lastKnownLocation
                (activity!! as MapActivity).setLastSearch(search.text.toString())
                (activity!! as MapActivity).hideKeyboard(search)
                searchList.removeAllViewsInLayout()
                activity?.getUIExecutor()?.let {
                    viewModel.search(search.text.toString(), null, loc, it)
                }
            }
            false
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isNotEmpty()) {
                    binding?.clearText?.visibility = View.VISIBLE
                    binding?.recentHeader?.visibility = View.GONE
                } else {
                    binding?.clearText?.visibility = View.GONE
                    binding?.recentHeader?.visibility = View.VISIBLE
                    viewModel.getRecentSearchData(requireContext())
                }
            }
        })

        binding?.clearText?.setOnClickListener {
            search.setText("")
            binding?.recentHeader?.visibility = View.VISIBLE
            viewModel.getRecentSearchData(requireContext())
        }

        searchList.layoutManager = LinearLayoutManager(activity)

        viewModel.searchResults.observe(viewLifecycleOwner, {
            Log.d(TAG, "result count ->  ${it.size} ")
            if (it.isNotEmpty()) {
                searchList.visibility = View.VISIBLE
                searchError.visibility = View.GONE
                viewModel.saveRecentSearchData(requireContext())
            } else {
                searchList.visibility = View.GONE
                searchError.visibility = View.GONE
                searchError.text = getString(R.string.no_result)
            }
            initAdapter(it)
            (activity as MapActivity).displaySearchResults(it, currentSearchHotCategoryTag)

        })

        viewModel.savedAddress.observe(viewLifecycleOwner, {
            Log.d(TAG, "result count ->  ${it.size} ")
            if (it.isNotEmpty()) {
                searchList.visibility = View.VISIBLE
                searchError.visibility = View.GONE
            } else {
                searchList.visibility = View.GONE
                searchError.visibility = View.GONE
                searchError.text = getString(R.string.no_result)
            }
            initAdapter(it)
        })

        viewModel.searchError.observe(viewLifecycleOwner, {
            if (!it.isNullOrBlank()) {
                searchError.visibility = View.VISIBLE
                searchError.text = it
            } else {
                searchError.visibility = View.GONE
            }
        })

        viewModel.loading.observe(viewLifecycleOwner, {
            if (it) {
                searchLoading.visibility = View.VISIBLE
            } else {
                searchLoading.visibility = View.GONE
            }
        })

        binding?.searchListAreaBack?.setOnClickListener {
            if (shouldUpdateHomeAddress || shouldUpdateWorkAddress) {
                if (!openedFromMainScreen) {
                    (activity!! as MapActivity).showPersonalInfoFragment()
                }
            } else {
                (activity!! as MapActivity).expandBottomSheet()
            }
            (activity!! as MapActivity).hideKeyboard(search)
            dismiss()
        }
        if (search.text.isNotEmpty()) {
            binding?.clearText?.visibility = View.VISIBLE
        } else {
            binding?.clearText?.visibility = View.GONE
        }

        showKeyboard()
    }

    private fun initAdapter(items: List<Entity>) {
        searchList.adapter = SearchResultsListRecyclerAdapter(items,
            object : SearchResultsListRecyclerAdapter.OnEntityClickListener {
                override fun onEntityClick(entity: Entity) {
                    (activity!! as MapActivity).hideKeyboard(search)
                    when {
                        shouldUpdateHomeAddress -> {
                            dataCollectorClient.setHome(requireActivity(), entity)
                            if (!openedFromMainScreen) {
                                (activity!! as MapActivity).showPersonalInfoFragment()
                            }
                        }
                        shouldUpdateWorkAddress -> {
                            dataCollectorClient.setWork(requireActivity(), entity)
                            if (!openedFromMainScreen) {
                                (activity!! as MapActivity).showPersonalInfoFragment()
                            }
                        }
                        else -> {
                            (activity as MapActivity).collapseBottomSheet()
                            (activity as MapActivity).displayEntityClicked(entity, currentSearchHotCategoryTag)
                        }
                    }
                    (activity as MapActivity).displayUserInfo()
                    dismiss()
                }
            }
        )
    }

    private fun showKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
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