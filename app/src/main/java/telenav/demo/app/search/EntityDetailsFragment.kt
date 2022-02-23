package telenav.demo.app.search

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datacollector.model.event.EntityActionEvent
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.lookup.EntityGetDetailResponse
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.convertNumberToDistance
import telenav.demo.app.databinding.FragmentEntityDetailsBinding
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.model.SearchResult
import telenav.demo.app.utils.addFavorite
import telenav.demo.app.utils.deleteFavorite
import telenav.demo.app.utils.entityCall
import java.lang.reflect.Type

class EntityDetailsFragment : Fragment() {

    private var binding: FragmentEntityDetailsBinding? = null
    private var searchResult: SearchResult? = null
    private var entity: Entity? = null
    private var isFavorite: Boolean = false
    private val telenavService: EntityClient by lazy { EntityService.getClient() }
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEntityDetailsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchResult?.id?.let { it1 -> getDetails(it1) }

        binding?.entityName?.text = searchResult?.name

        if (searchResult?.phoneNo.isNullOrEmpty()) {
            binding?.entityCall?.visibility = View.GONE
        } else {
            binding?.entityCall?.visibility = View.VISIBLE
        }

        binding?.entityDistance?.text = binding?.entityDistance?.context?.convertNumberToDistance(searchResult?.distance!!)

        binding?.entityCall?.setOnClickListener {
            try {
                entity?.id?.let { it1 ->
                    dataCollectorClient.entityCall(
                        App.readStringFromSharedPreferences(
                            App.LAST_ENTITY_RESPONSE_REF_ID, ""
                        ) ?: "",
                        it1,
                        EntityActionEvent.DisplayMode.MAP_VIEW
                    )
                }
                val callIntent = Intent(Intent.ACTION_DIAL)
                callIntent.data = Uri.parse("tel:${searchResult?.phoneNo}")
                startActivity(callIntent)
            } catch (activityException: ActivityNotFoundException) {
                Log.e(tag, "Call failed", activityException)
            }
        }
        binding?.entityFavorite?.setOnClickListener {
            entity?.let {
                if (isFavorite) {
                    dataCollectorClient.deleteFavorite(requireContext(), it)
                } else {
                    dataCollectorClient.addFavorite(requireContext(), it)
                }
                checkFavorite(it)
            }
        }

        var fragment: Fragment = FoodDetailsFragment.newInstance(searchResult)
        when {
            searchResult?.categoryName.equals("Parking lot") -> {
                fragment = ParkingDetailsFragment.newInstance(searchResult)
            }
            searchResult?.categoryName.equals("Fast Food") -> {
                fragment = FoodDetailsFragment.newInstance(searchResult)
            }
            searchResult?.categoryName.equals("Electric Charge") -> {
                fragment = EvChargerDetailsFragment.newInstance(searchResult)
            }
        }

         childFragmentManager.beginTransaction().replace(
            R.id.frame_entity_additional_details, fragment).commit()
    }

    private fun getDetails(id: String) {
        telenavService.detailRequest
            .setEntityIds(listOf(id))
            .asyncCall(
                requireActivity().getUIExecutor(),
                object : Callback<EntityGetDetailResponse> {
                    override fun onSuccess(response: EntityGetDetailResponse) {
                        Log.w("test", "result ${Gson().toJson(response.results)}")
                        if (response.results != null && response.results.size > 0) {
                            entity = response.results[0]
                            checkFavorite(entity!!)
                        }
                    }

                    override fun onFailure(p1: Throwable?) {
                        Log.e("testapp", "onFailure", p1)
                    }
                }
            )
    }

    private fun checkFavorite(entity: Entity) {
        val prefs = requireActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val listType: Type = object : TypeToken<List<Entity>>() {}.type
        val favoriteEntities = Gson().fromJson<List<Entity>>(
            prefs.getString(
                getString(R.string.saved_favorite_list_key),
                ""
            ), listType
        )

        isFavorite =
            if (favoriteEntities != null && favoriteEntities.any { e -> e.id == entity.id }) {
               // entity_favorite.setImageResource(R.drawable.ic_favorite)
                true
            } else {
               // entity_favorite.setImageResource(R.drawable.ic_favorite_border)
                false
            }
    }


    companion object {
        @JvmStatic
        fun newInstance(searchResult: SearchResult) =
            EntityDetailsFragment().apply {
                this.searchResult = searchResult
            }
    }
}
