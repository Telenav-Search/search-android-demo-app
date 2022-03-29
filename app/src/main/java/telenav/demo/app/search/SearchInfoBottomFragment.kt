package telenav.demo.app.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.telenav.sdk.entity.model.base.Category
import com.telenav.sdk.entity.model.base.Entity
import telenav.demo.app.R
import telenav.demo.app.databinding.SearchInfoBottomFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.searchlist.SearchListInfoRecyclerAdapter
import telenav.demo.app.widgets.CategoryView
import telenav.demo.app.widgets.RoundedBottomSheetLayout
import java.util.ArrayList
import android.text.Editable
import android.text.TextWatcher
import telenav.demo.app.map.getUIExecutor
import telenav.demo.app.utils.CategoryAndFiltersUtil

private const val TAG = "SearchInfoBottomFragment"

class SearchInfoBottomFragment : RoundedBottomSheetLayout() {

    private val viewModel: SearchInfoViewModel by viewModels()
    private var currentSearchHotCategoryName: String? = null
    private var currentSearchHotCategoryTag: String? = null
    private var binding: SearchInfoBottomFragmentLayoutBinding? = null

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

    private fun init() {
        val location = (activity!! as MapActivity).lastKnownLocation
        activity?.getUIExecutor()?.let { executor ->
            currentSearchHotCategoryTag?.let {
                (activity!! as MapActivity).setLastSearch(it)
                if (it.isNotEmpty()) {
                    viewModel.search(null, it, location, executor, currentSearchHotCategoryTag, true)
                } else {
                    viewModel.search(currentSearchHotCategoryName, null, location, executor, currentSearchHotCategoryTag, true)
                }
            }
        }

        binding?.search?.setText(currentSearchHotCategoryName)
        binding?.search?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                val loc = (activity!! as MapActivity).lastKnownLocation
                (activity!! as MapActivity).setLastSearch(binding?.search?.text.toString())
                (activity!! as MapActivity).hideKeyboard(binding?.search!!)
                binding?.searchList?.removeAllViewsInLayout()
                activity?.getUIExecutor()?.let {
                    viewModel.search(binding?.search?.text.toString(), null, loc, it, currentSearchHotCategoryTag, true)
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
            binding?.search?.setText("")
            (activity as MapActivity).updateBottomView()
            (activity as MapActivity).updateBottomSheetState()
            dismiss()
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

        viewModel.categories.observe(viewLifecycleOwner, {
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
        val bottomSheetLayout = binding?.bottomSheet
        val flowLayout = binding?.flowCategory
        val set = ConstraintSet()
        set.clone(bottomSheetLayout)

        val hotCategoryIdArray = ArrayList<Int>()
        for (hotCategory in categories) {
            val categoryView = getCategoryView(hotCategory)
            bottomSheetLayout?.addView(categoryView)
            hotCategoryIdArray.add(categoryView.id)
        }

        if (hotCategoryIdArray.isEmpty()) {
            flowLayout?.visibility = View.GONE
        } else {
            flowLayout?.visibility = View.VISIBLE
        }
        flowLayout?.referencedIds = hotCategoryIdArray.toIntArray()
    }

    private fun getCategoryView(hotCategory: Category): CategoryView {
        val categoryView = CategoryView(requireContext())
        categoryView.init(hotCategory, currentSearchHotCategoryTag)
        categoryView.id = View.generateViewId()

        return categoryView
    }

    companion object {
        @JvmStatic
        fun newInstance(categoryName: String?, categoryTag: String?) =
            SearchInfoBottomFragment().apply {
                this.currentSearchHotCategoryName = categoryName
                this.currentSearchHotCategoryTag = categoryTag
            }
    }
}