package telenav.demo.app.homepage

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.discover.EntityGetCategoriesResponse
import telenav.demo.app.R
import telenav.demo.app.searchlist.SearchListFragment
import telenav.demo.app.utils.CategoryAndFiltersUtil

class CategoriesFragment : Fragment() {
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
        showHotCategories()
    }

    private fun showHotCategories() {
        vCategoryLoading.hide()
        vCategories.visibility = View.VISIBLE
        vCategoryTree.adapter = CategoriesHotRecyclerAdapter(CategoryAndFiltersUtil.hotCategoriesList) { category ->
            if (category.id.isEmpty()) {
                requestCategories()
            } else {
                (activity!! as HomePageActivity).showSearchFragment(
                    SearchListFragment.newInstance(category)
                )
            }
        }
    }

    private fun requestCategories() {
        vCategories.visibility = View.GONE
        vCategoryLoading.show()
        telenavService.getCategoriesRequest().asyncCall(
            activity?.getUIExecutor(),
            object : Callback<EntityGetCategoriesResponse> {
                override fun onSuccess(response: EntityGetCategoriesResponse) {
                    vCategoryLoading.hide()
                    vCategories.visibility = View.VISIBLE
                    vCategoryTree.adapter =
                        CategoriesRecyclerAdapter(response.results) { category ->
                            (activity!! as HomePageActivity).showSearchFragment(
                                SearchListFragment.newInstance(category)
                            )
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

}

class HotCategory(val name: String, val tag: String, val iconPurple: Int, val id: String)