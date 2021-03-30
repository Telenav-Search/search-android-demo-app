package telenav.demo.app.search

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.Category
import com.telenav.sdk.entity.model.discover.EntityGetCategoriesResponse
import com.telenav.sdk.entity.model.search.CategoryFilter
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.entity.model.search.SearchFilters
import telenav.demo.app.R
import telenav.demo.app.homepage.CategoriesRecyclerAdapter
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.map.MapActivity
import telenav.demo.app.widgets.RoundedBottomSheetLayout

private const val TAG = "CategoriesResultFragment"

open class CategoriesResultFragment : RoundedBottomSheetLayout() {
    private val telenavService: EntityClient by lazy { EntityService.getClient() }
    private lateinit var vCategories: View
    private lateinit var vCategoryTree: RecyclerView
    private lateinit var vCategoryLoading: ContentLoadingProgressBar
    private lateinit var vCategoryError: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vCategories = view.findViewById(R.id.categories)
        vCategoryTree = view.findViewById(R.id.categoriesTree)
        vCategoryLoading = view.findViewById(R.id.categoriesLoading)
        vCategoryError = view.findViewById(R.id.categoriesError)

        vCategoryTree.layoutManager = LinearLayoutManager(activity)
        requestCategories()
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

                            search(category)
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

    private fun search(category: Category) {
        val location = (activity!! as MapActivity).lastKnownLocation ?: Location("")
        vCategoryLoading.show()
        telenavService.searchRequest()
            .apply {
                setFilters(
                    SearchFilters.builder()
                        .setCategoryFilter(
                            CategoryFilter.builder().addCategory(category.id).build()
                        )
                        .build()
                )
            }
            .setLocation(location.latitude, location.longitude)
            .setLimit(20)
            .asyncCall(
                activity?.getUIExecutor(),
                object : Callback<EntitySearchResponse> {
                    override fun onSuccess(response: EntitySearchResponse) {
                        Log.w("test", Gson().toJson(response.results))
                        vCategoryLoading.hide()
                        if (response.results != null && response.results.size > 0) {

                            (activity as MapActivity).displaySearchResults(
                                response.results,
                                category.id
                            )
                            (activity as MapActivity).showSearchHotCategoriesFragment(
                                response.results,
                                category.id
                            )
                            dismiss()
                        }
                    }

                    override fun onFailure(p1: Throwable?) {
                        vCategoryLoading.hide()
                        Log.e("testapp", "onFailure", p1)
                    }
                }
            )
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            CategoriesResultFragment()
    }
}