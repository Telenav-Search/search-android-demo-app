package telenav.demo.app.search

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import com.telenav.sdk.entity.model.base.Category
import com.telenav.sdk.entity.model.base.Entity

import telenav.demo.app.R
import telenav.demo.app.databinding.SearchInfoBottomFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.searchlist.SearchListInfoRecyclerAdapter
import telenav.demo.app.map.getUIExecutor
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.utils.CategoryAndFiltersUtil.toViewData
import telenav.demo.app.widgets.CategoryAdapter

private const val TAG = "SearchInfoBottomFragment"

class SearchInfoBottomFragment : BottomSheetDialogFragment() {

    private val viewModel: SearchInfoViewModel by viewModels()
    private var currentSearchHotCategoryName: String? = null
    private var currentSearchHotCategoryTag: String? = null
    private var shouldLoadSaveData: Boolean = false
    private var binding: SearchInfoBottomFragmentLayoutBinding? = null

    // adapter for recyclerView to show sub category items
    private val mCategoryAdapter = CategoryAdapter { _, data ->
        // call back from onClick
        binding?.search?.setText(data.name)
        val mapActivity = activity as MapActivity
        mapActivity.getUIExecutor().let { executor ->
            // start search
            viewModel.search(
                data.name,
                null,
                mapActivity.getCVPLocation(),
                executor,
                mapActivity.getSearchAreaLocation(),
                filterCategory = currentSearchHotCategoryTag,
                filtersAvailable = true
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SearchInfoBottomFragmentLayoutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            override fun onBackPressed() {
                onBack()
            }
        }.apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
        }
    }

    private fun init() {
        val mapActivity = activity as MapActivity
        mapActivity.getUIExecutor()?.let { executor ->
            currentSearchHotCategoryTag?.let {
                if (shouldLoadSaveData &&
                    (viewModel.getRecentSearchData(requireContext()).isNotEmpty() ||
                    viewModel.getRecentCategoryData(requireContext()).isNotEmpty())) {

                    viewModel.setRecentSearchData(requireContext())
                    viewModel.setRecentCategoryData(requireContext())
                } else {
                    (activity!! as MapActivity).setLastSearch(it)
                    if (it.isNotEmpty()) {
                        viewModel.search(
                            null,
                            it,
                            mapActivity.getCVPLocation(),
                            executor,
                            mapActivity.getSearchAreaLocation(),
                            currentSearchHotCategoryTag,
                            true
                        )
                    } else {
                        viewModel.search(
                            currentSearchHotCategoryName,
                            null,
                            mapActivity.getCVPLocation(),
                            executor,
                            mapActivity.getSearchAreaLocation(),
                            currentSearchHotCategoryTag,
                            true
                        )
                    }
                }
            }
        }

        binding?.search?.setText(currentSearchHotCategoryName)
        binding?.search?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                mapActivity.setLastSearch(binding?.search?.text.toString())
                mapActivity.hideKeyboard(binding?.search!!)
                binding?.searchList?.removeAllViewsInLayout()
                activity?.getUIExecutor()?.let {
                    viewModel.search(binding?.search?.text.toString(),
                        null,
                        mapActivity.getCVPLocation(),
                        it,
                        mapActivity.getSearchAreaLocation(),
                        currentSearchHotCategoryTag,
                        true)
                }
            }
            false
        }

        binding?.search?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isNotEmpty()) {
                    binding?.clearText?.visibility = View.VISIBLE
                } else{
                    binding?.clearText?.visibility = View.GONE
                }
            }
        })

        binding?.clearText?.setOnClickListener {
            onBack()
        }

        binding?.searchList?.layoutManager = LinearLayoutManager(activity)

        viewModel.searchResults.observe(viewLifecycleOwner, {
            Log.d(TAG, "result count ->  ${it.size} ")
            if (it.isNotEmpty()) {
                binding?.searchList?.visibility = View.VISIBLE
                binding?.errorView?.visibility = View.GONE
                viewModel.saveRecentSearchData(requireContext())
            } else {
                binding?.searchList?.visibility = View.GONE
                binding?.searchError?.text = ""
                binding?.errorView?.visibility = View.VISIBLE
            }

            (activity as MapActivity).displaySearchResults(it, currentSearchHotCategoryTag)

            binding?.searchList?.adapter = SearchListInfoRecyclerAdapter(it,
                object : SearchListInfoRecyclerAdapter.OnEntityClickListener {
                    override fun onEntityClick(entity: Entity) {
                        (activity as MapActivity).hideKeyboard(binding?.search!!)
                        (activity as MapActivity).displayEntityClicked(entity,
                            currentSearchHotCategoryTag, true)
                    }
                }
            )
        })

        viewModel.searchError.observe(viewLifecycleOwner, {
            if (!it.isNullOrBlank()) {
                binding?.searchError?.text = it
                binding?.errorView?.visibility = View.VISIBLE
            } else {
                binding?.errorView?.visibility = View.GONE
            }
        })

        viewModel.loading.observe(viewLifecycleOwner, {
            if (it) {
                binding?.searchLoading?.visibility = View.VISIBLE
            } else {
                binding?.searchLoading?.visibility = View.GONE
            }
        })

        viewModel.categories.observe(viewLifecycleOwner, {
            viewModel.saveRecentCategoryData(requireContext())
            displayHotCategories(it)
        })

        binding?.searchFilter?.setOnClickListener {
            dismiss()
            currentSearchHotCategoryTag?.let {
                when {
                    it.equals(CategoryAndFiltersUtil.PARKING_TAG) -> {
                        (activity!! as MapActivity).showParkingFiltersFragment()
                    }
                    it.equals(CategoryAndFiltersUtil.EV_CHARGER_TAG) -> {
                        (activity!! as MapActivity).showEvFiltersFragment()
                    } else -> {
                        (activity!! as MapActivity).showGeneralFiltersFragment()
                    }
                }
            } ?: run {
                (activity!! as MapActivity).showGeneralFiltersFragment()
            }
        }
    }

    private fun displayHotCategories(categories: List<Category>) {
        binding ?: throw IllegalStateException("viewBinding is null")

        // setup recyclerView
        val recyclerView = binding!!.categoryContainer
        recyclerView.layoutManager = GridLayoutManager(requireContext(), CategoryAndFiltersUtil.DISPLAY_LIMIT,
            GridLayoutManager.VERTICAL, false)
        recyclerView.adapter = mCategoryAdapter.apply {
            // feed data from input
            setData(categories.map { it.toViewData(currentSearchHotCategoryTag ?: "") })
            setRowLimit(1) // limit to single row
            type = CategoryAdapter.TYPE_SUB_CATEGORY
        }

        recyclerView.visibility = if (mCategoryAdapter.itemCount == 0) View.GONE else View.VISIBLE
    }

    private fun onBack() {
        binding?.search?.setText("")
        (activity as MapActivity).updateBottomView()
        (activity as MapActivity).updateBottomSheetState()
        dismiss()
    }

    companion object {
        @JvmStatic
        fun newInstance(categoryName: String?, categoryTag: String?, shouldLoadSaveData: Boolean) =
            SearchInfoBottomFragment().apply {
                this.currentSearchHotCategoryName = categoryName
                this.currentSearchHotCategoryTag = categoryTag
                this.shouldLoadSaveData = shouldLoadSaveData
            }
    }
}