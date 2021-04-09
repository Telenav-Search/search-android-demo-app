package telenav.demo.app.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.discover.EntityGetCategoriesResponse
import telenav.demo.app.R
import telenav.demo.app.databinding.FragmentCategoriesBinding
import telenav.demo.app.homepage.CategoriesRecyclerAdapter
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.map.MapActivity
import telenav.demo.app.search.filters.Filter
import telenav.demo.app.widgets.RoundedBottomSheetLayout

private const val TAG = "CategoriesResultFragment"

open class CategoriesResultFragment : RoundedBottomSheetLayout() {
    private val telenavService: EntityClient by lazy { EntityService.getClient() }
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var vCategories: View
    private lateinit var vCategoryTree: RecyclerView
    private lateinit var vCategoryLoading: ContentLoadingProgressBar
    private lateinit var vCategoryError: TextView
    private var vCatId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vCategories = view.findViewById(R.id.categories)
        vCategoryTree = view.findViewById(R.id.categoriesTree)
        vCategoryLoading = view.findViewById(R.id.categoriesLoading)
        vCategoryError = view.findViewById(R.id.categoriesError)

        vCategoryTree.layoutManager = LinearLayoutManager(activity)
        requestCategories()
        (activity as MapActivity).setFiltersSub()
        viewModel.searchResults.observe(this, Observer {
            (activity as MapActivity).displaySearchResults(
                it as List<Entity>?,
                vCatId
            )
            (activity!! as MapActivity).showSearchHotCategoriesFragment(
                it as List<Entity>, vCatId
            )
            fragmentManager?.beginTransaction()?.remove(this)?.commit()
        })
    }

    private fun requestCategories() {
        vCategories.visibility = View.GONE
        vCategoryLoading.show()
        telenavService.categoriesRequest.asyncCall(
            activity?.getUIExecutor(),
            object : Callback<EntityGetCategoriesResponse> {
                override fun onSuccess(response: EntityGetCategoriesResponse) {
                    vCategoryLoading.hide()
                    vCategories.visibility = View.VISIBLE
                    vCategoryTree.adapter =
                        CategoriesRecyclerAdapter(response.results) { category ->
                            vCatId = category.name
                            (activity!! as MapActivity).setLastSearch(category.id)
                            (activity!! as MapActivity).redoButtonLogic()
                            val loc = (activity!! as MapActivity).lastKnownLocation
                            activity?.getUIExecutor()?.let {
                                viewModel.search(null, category.id, loc,
                                    it
                                )
                            }
                        }
                }

                override fun onFailure(p1: Throwable?) {
                    vCategoryLoading.hide()
                    vCategoryError.visibility = View.VISIBLE
                    Log.e("testapp", "", p1)
                }
            }
        )
    }

    fun setFilters(filters: List<Filter>?) {
        viewModel.filters = filters
    }

    companion object {
        fun newInstance() =
            CategoriesResultFragment()
    }
}