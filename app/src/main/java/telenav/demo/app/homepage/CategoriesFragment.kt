package telenav.demo.app.homepage

import android.content.Intent
import android.location.Location
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
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.DestinationPredictionClient
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.discover.EntityGetCategoriesResponse
import com.telenav.sdk.entity.model.prediction.EntityDestinationPredictionResponse
import telenav.demo.app.R
import telenav.demo.app.entitydetails.EntityDetailsActivity
import telenav.demo.app.searchlist.SearchListFragment


class CategoriesFragment : Fragment() {
    private val telenavService: EntityClient by lazy { EntityService.getClient() }
    private val dpalgoService: DestinationPredictionClient by lazy { EntityService.getDestinationPredictionClient() }
    private lateinit var vCategories: View
    private lateinit var vCategoryTree: RecyclerView
    private lateinit var vCategoryLoading: ContentLoadingProgressBar
    private lateinit var vCategoryError: TextView
    private lateinit var vPredictionList: RecyclerView
    private lateinit var vDestinations: View
    private lateinit var vPredictionEmpty: TextView

    private val hotCategoriesList = arrayListOf(
        HotCategory("Food", R.drawable.ic_food, "226"),
        HotCategory("Coffee", R.drawable.ic_coffee, "241"),
        HotCategory("Grocery", R.drawable.ic_grocery, "221"),
        HotCategory("Shopping", R.drawable.ic_shopping, "4090"),
        HotCategory("Parking", R.drawable.ic_parking, "600"),
        HotCategory("Banks / ATMs", R.drawable.ic_atm, "374"),
        HotCategory("Hotels / Motels", R.drawable.ic_hotel, "595"),
        HotCategory("Attractions", R.drawable.ic_attraction, "605"),
        HotCategory("Fuel", R.drawable.ic_gas, "811"),
        HotCategory("Electric Vehicle Charge Station", R.drawable.ic_ev, "771"),
        HotCategory("More", R.drawable.ic_more, "")
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
        vPredictionList = view.findViewById(R.id.predictionList)
        vDestinations = view.findViewById(R.id.destiantions)
        vCategoryTree = view.findViewById(R.id.categoriesTree)
        vCategoryLoading = view.findViewById(R.id.categoriesLoading)
        vCategoryError = view.findViewById(R.id.categoriesError)
        vPredictionEmpty = view.findViewById(R.id.prediction_no_result)

        vCategoryTree.layoutManager = LinearLayoutManager(activity)
        vPredictionList.layoutManager = LinearLayoutManager(activity)
        destinationPredict()
        showHotCategories()
    }

    private fun showHotCategories() {
        vCategoryLoading.hide()
        vCategories.visibility = View.VISIBLE
        vDestinations.visibility = View.VISIBLE
        vCategoryTree.adapter = CategoriesHotRecyclerAdapter(hotCategoriesList) { category ->
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


    private fun destinationPredict() {
        val location = (activity!! as HomePageActivity).lastKnownLocation ?: Location("")
        Log.w("test","location:"+location.latitude+","+location.longitude)

        dpalgoService.destinationPredictionRequest().location(location.latitude, location.longitude)
            .asyncCall(activity!!.getUIExecutor(),
                object : Callback<EntityDestinationPredictionResponse> {
                    override fun onSuccess(response: EntityDestinationPredictionResponse) {
                        if (activity == null)
                            return
                        Log.w("test", "prediction response:")
                        Log.w("test", Gson().toJson(response.results))
                        //vDestinations.visibility = View.VISIBLE
                        if(response.results.isEmpty())
                            vPredictionEmpty.visibility = View.VISIBLE
                        else
                            vPredictionEmpty.visibility = View.GONE
                        vPredictionList.setAdapter(PredictionRecyclerAdapter(response.results) { destination ->
                            activity?.startActivity(
                                Intent(activity, EntityDetailsActivity::class.java).apply {
                                    putExtra(EntityDetailsActivity.PARAM_ID, destination.entity.id)
                                })

                        })
                    }

                    override fun onFailure(error: Throwable) {
                        Log.e("test", "", error)
                    }
                })

    }

}



class HotCategory(val name: String, val icon: Int, val id: String)