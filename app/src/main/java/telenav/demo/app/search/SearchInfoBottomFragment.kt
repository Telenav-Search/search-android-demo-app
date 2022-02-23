package telenav.demo.app.search

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.telenav.sdk.entity.model.base.Category
import com.telenav.sdk.entity.model.base.Entity
import kotlinx.android.synthetic.main.search_info_bottom_fragment_layout.*
import kotlinx.android.synthetic.main.search_info_bottom_fragment_layout.search
import telenav.demo.app.R
import telenav.demo.app.databinding.SearchInfoBottomFragmentLayoutBinding
import telenav.demo.app.homepage.HotCategory
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.map.MapActivity
import telenav.demo.app.searchlist.SearchListInfoRecyclerAdapter
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.widgets.CategoryView
import telenav.demo.app.widgets.RoundedBottomSheetLayoutNew
import java.util.ArrayList

private const val TAG = "SearchInfoBottomFragment"

class SearchInfoBottomFragment : RoundedBottomSheetLayoutNew() {

    private val viewModel: SearchInfoViewModel by viewModels()
    private var currentSearchHotCategoryId: String? = null
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
            currentSearchHotCategoryId?.let {
                (activity!! as MapActivity).setLastSearch(it)
                (activity!! as MapActivity).redoButtonLogic()
                viewModel.search(null, it, currentSearchHotCategoryTag, location, executor)
                //viewModel.requestSubcategories(it, location, executor)
            }
        }

        search.setText(currentSearchHotCategoryName)
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                val loc = (activity!! as MapActivity).lastKnownLocation
                (activity!! as MapActivity).setLastSearch(search.text.toString())
               // (activity!! as MapActivity).redoButtonLogic()
                (activity!! as MapActivity).hideKeyboard(search)
                searchList.removeAllViewsInLayout()
                activity?.getUIExecutor()?.let {
                    currentSearchHotCategoryId = search.text.toString()
                    viewModel.search(search.text.toString(), null, currentSearchHotCategoryTag, loc, it)
                }
            }
            false
        }

        searchList.layoutManager = LinearLayoutManager(activity)

        viewModel.searchResults.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "result count ->  ${it.size} ")
            if (it.isNotEmpty()) {
                searchList.visibility = View.VISIBLE
                searchError.visibility = View.GONE
            } else {
                searchList.visibility = View.GONE
                searchError.visibility = View.GONE
                searchError.text = getString(R.string.no_result)
            }

            (activity as MapActivity).displaySearchResults(
                it as List<Entity>?, currentSearchHotCategoryId)

            searchList.adapter = SearchListInfoRecyclerAdapter(it,
                object : SearchListInfoRecyclerAdapter.OnEntityClickListener {
                    override fun onEntityClick(entity: Entity) {
                        (activity as MapActivity).displayEntityClicked(entity, currentSearchHotCategoryId)
                    }
                }
            )
        })

        viewModel.searchError.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrBlank()) {
                searchError.visibility = View.VISIBLE
                searchError.text = it
            } else {
                searchError.visibility = View.GONE
            }
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer {
            if (it) {
                searchLoading.visibility = View.VISIBLE
            } else {
                searchLoading.visibility = View.GONE
            }
        })

        viewModel.categories.observe(viewLifecycleOwner, Observer {
            displayHotCategories(it)
        })
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

        flowLayout?.referencedIds = hotCategoryIdArray.toIntArray()
    }

    private fun getCategoryView(hotCategory: Category): CategoryView {
        val categoryView = CategoryView(requireContext())
        categoryView.init(hotCategory)
        categoryView.id = View.generateViewId()

        return categoryView
    }

    companion object {
        @JvmStatic
        fun newInstance(categoryId: String?, categoryName: String?, categoryTag: String?) =
            SearchInfoBottomFragment().apply {
                this.currentSearchHotCategoryId = categoryId
                this.currentSearchHotCategoryName = categoryName
                this.currentSearchHotCategoryTag = categoryTag
            }
    }
}