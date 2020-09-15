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


class CategoriesFragment : Fragment() {
    private val telenavService: EntityClient by lazy { EntityService.getClient() }
    private lateinit var vCategories: View
    private lateinit var vCategoryTree: RecyclerView
    private lateinit var vCategoryLoading: ContentLoadingProgressBar
    private lateinit var vCategoryError: TextView

    private val hotCategoriesList = arrayListOf(
        HotCategory("Food", R.drawable.ic_food,"2040"),
        HotCategory("Coffee", R.drawable.ic_coffee,"241"),
        HotCategory("Grocery", R.drawable.ic_grocery,"221"),
        HotCategory("Shopping", R.drawable.ic_shopping,"4090"),
        HotCategory("Parking", R.drawable.ic_parking,"600"),
        HotCategory("Banks / ATMs", R.drawable.ic_atm,"374"),
        HotCategory("Hotels / Motels", R.drawable.ic_hotel,"595"),
        HotCategory("Attractions", R.drawable.ic_attraction,"605"),
        HotCategory("Fuel", R.drawable.ic_gas,"811"),
        HotCategory("Electric Vehicle Charge Station", R.drawable.ic_ev,"771"),
        HotCategory("More", R.drawable.ic_more,"")
    )

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
        vCategoryTree.setAdapter(CategoriesHotRecyclerAdapter(hotCategoriesList) {
            requestCategories()
        })
    }

    private fun requestCategories() {
        vCategories.visibility = View.GONE
        vCategoryLoading.show()
        telenavService.getCategoriesRequest().asyncCall(
            object : Callback<EntityGetCategoriesResponse> {
                override fun onSuccess(response: EntityGetCategoriesResponse) {
                    activity?.runOnUiThread {
                        vCategoryLoading.hide()
                        vCategories.visibility = View.VISIBLE
                        vCategoryTree.setAdapter(CategoriesRecyclerAdapter(response.results))
                    }
                }

                override fun onFailure(p1: Throwable?) {
                    activity?.runOnUiThread {
                        vCategoryLoading.hide()
                        vCategoryError.visibility = View.VISIBLE
                    }
                    Log.e("testapp", "", p1)
                }
            }
        )
    }

}

class HotCategory(val name: String, val icon: Int, val id:String)