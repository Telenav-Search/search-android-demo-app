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
import com.telenav.sdk.entity.model.base.Entity
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.convertNumberToDistance
import telenav.demo.app.databinding.FragmentEntityDetailsBinding
import telenav.demo.app.model.SearchResult
import telenav.demo.app.utils.addFavorite
import telenav.demo.app.utils.deleteFavorite
import telenav.demo.app.utils.entityCall
import java.lang.reflect.Type
import telenav.demo.app.utils.Converter.convertDpToPixel
import telenav.demo.app.widgets.RoundedBottomSheetLayout

class EntityDetailsFragment : RoundedBottomSheetLayout() {

    private var binding: FragmentEntityDetailsBinding? = null
    private var searchResult: SearchResult? = null
    private var entity: Entity? = null
    private var isFavorite: Boolean = false
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

        checkFavorite(entity!!)
        attachAdditionalDetails()

        binding?.entityName?.text = searchResult?.name

        if (searchResult?.phoneNo.isNullOrEmpty()) {
            binding?.entityCall?.visibility = View.GONE
            binding?.entityFavorite?.compoundDrawablePadding = convertDpToPixel(requireContext(),-130f)
            binding?.entityFavorite?.setPadding(convertDpToPixel(requireContext(),140f), 0 ,0 ,0)
        } else {
            binding?.entityCall?.visibility = View.VISIBLE
            binding?.entityFavorite?.compoundDrawablePadding = convertDpToPixel(requireContext(),-30f)
            binding?.entityFavorite?.setPadding(convertDpToPixel(requireContext(),40f), 0 ,0 ,0)
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
                binding?.entityFavorite?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite,0,0,0)
                true
            } else {
                binding?.entityFavorite?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_border,0,0,0)
                false
            }
    }

    fun updateItemVisibility(slideOffset: Float) {
        val fragment: Fragment? = childFragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (fragment != null && fragment is GeneralDetailsFragment) {
            fragment.updateStartsVisibility(slideOffset)
        }
    }

    private fun attachAdditionalDetails() {
        var fragment: Fragment = GeneralDetailsFragment.newInstance(searchResult, entity)
        when {
            entity?.facets?.parking != null  -> {
                fragment = ParkingDetailsFragment.newInstance(searchResult, entity)
            }
            (entity?.facets?.evConnectors != null && entity?.facets?.chargeStations != null) -> {
                fragment = EvChargerDetailsFragment.newInstance(searchResult, entity)
            }
        }

        childFragmentManager.beginTransaction().replace(
            R.id.frame_entity_additional_details, fragment, FRAGMENT_TAG).commit()
    }

    companion object {
        private const val FRAGMENT_TAG = "EntityDetailsFragment"

        @JvmStatic
        fun newInstance(searchResult: SearchResult, entity: Entity) =
            EntityDetailsFragment().apply {
                this.searchResult = searchResult
                this.entity = entity
            }
    }
}
