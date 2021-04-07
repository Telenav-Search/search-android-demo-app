package telenav.demo.app.entitydetails

import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.lookup.EntityGetDetailResponse
import kotlinx.android.synthetic.main.entity_detail_fragment_layout.*
import telenav.demo.app.R
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.model.SearchResult
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.utils.setHome
import telenav.demo.app.utils.setWork
import telenav.demo.app.widgets.RoundedBottomSheetLayout

class EntityDetailFragment : RoundedBottomSheetLayout() {

    private lateinit var viewModel: EntityDetailViewModel
    private lateinit var entityDetailView: View
    private lateinit var searchResult: SearchResult
    private var entity: Entity? = null
    private var isFavorite: Boolean = false
    private val telenavService: EntityClient by lazy { EntityService.getClient() }
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        entityDetailView = inflater.inflate(R.layout.entity_detail_fragment_layout, container, false)
        return entityDetailView

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(EntityDetailViewModel::class.java)

        viewModel.getSearchResultLiveData().observe(viewLifecycleOwner, Observer {
            searchResult = it
            searchResult.id?.let { it1 -> getDetails(it1) }
            entity_name_field.text = it.name

            if (!it.categoryName.isNullOrEmpty()) {
                entity_category.text = it.categoryName
            } else {
                entity_category.visibility = View.INVISIBLE
            }

            if (this.context != null) {
                CategoryAndFiltersUtil.setStarsViewBasedOnRating(entityDetailView, it.ratingLevel, this.context!!)
                CategoryAndFiltersUtil.setPriceIconBasedOnPriceLevel(entityDetailView, it.priceLevel, this.context!!)
            }

            if (!it.address.isNullOrEmpty()) {
                entity_address_field.text = it.address
            } else {
                entity_address_field.visibility = View.INVISIBLE
            }

            if (it.iconId != 0) {
                poi_icon.setImageResource(it.iconId)
            } else {
                poi_icon.setImageResource(R.drawable.ic_more)
            }

            if (!it.email.isNullOrEmpty()) {
                entity_email_field.text = it.email
            }

            if (!it.hours.isNullOrEmpty()) {
                time_field.text = it.hours
            }

            entity_distance_field.text = it.distance.toString() + " mi"

            val crowdDensity = CategoryAndFiltersUtil.generateRandomInt()

            if (crowdDensity < 50) {
                entity_crowd_density.setTextColor(ResourcesCompat.getColor(this.context?.resources!!, R.color.telenav_gray_pressed, null))
            } else {
                entity_crowd_density.setTextColor(ResourcesCompat.getColor(this.context?.resources!!, R.color.red, null))
            }

            entity_crowd_density.text = "Crowd density ${crowdDensity}%"
        })

        as_home.setOnClickListener {
            setAsHomeAddress(entity)
        }

        as_work.setOnClickListener {
            setAsWorkAddress(entity)
        }

        book_button.setOnClickListener {
            Toast.makeText(this.context, "Booked now", Toast.LENGTH_SHORT).show()
        }

        call_button.setOnClickListener {
            try {
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:${searchResult.phoneNo}")
                startActivity(callIntent)
            } catch (activityException: ActivityNotFoundException) {
                Log.e(tag, "Call failed", activityException)
            }
        }


        navigate_button.setOnClickListener {
            if (searchResult.latitude != 0.0 && searchResult.longitude != 0.0) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${searchResult.latitude},${searchResult.longitude}"))
                startActivity(intent)
            }
        }

        facebook_button.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(CategoryAndFiltersUtil.requestWebsite(searchResult, 0))
            )
            startActivity(browserIntent)
        }

        twitter_button.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(CategoryAndFiltersUtil.requestWebsite(searchResult, 1))
            )
            startActivity(browserIntent)
        }

        instagram_button.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(CategoryAndFiltersUtil.requestWebsite(searchResult, 2))
            )
            startActivity(browserIntent)
        }
    }

    private fun setAsHomeAddress(entity: Entity?) {
        entity ?: return
        dataCollectorClient.setHome(requireActivity(), entity)
    }

    private fun setAsWorkAddress(entity: Entity?) {
        entity ?: return
        dataCollectorClient.setWork(requireActivity(), entity)
    }

    private fun getDetails(id: String) {
        Log.w("test", "getDetails ${id}")
        telenavService.detailRequest
            .setEntityIds(listOf(id))
            .asyncCall(
                requireActivity().getUIExecutor(),
                object : Callback<EntityGetDetailResponse> {
                    override fun onSuccess(response: EntityGetDetailResponse) {
                        Log.w("test", "result ${Gson().toJson(response.results)}")
                        if (response.results != null && response.results.size > 0) {
                            entity = response.results[0]
                            /*showEntityOnMap(response.results[0])
                            showDetails(response.results[0])
                            setToggler(true)*/
                        }
                    }

                    override fun onFailure(p1: Throwable?) {
                        Log.e("testapp", "onFailure", p1)
                    }
                }
            )
    }
}