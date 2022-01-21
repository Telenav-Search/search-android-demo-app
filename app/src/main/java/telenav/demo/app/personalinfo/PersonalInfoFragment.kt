package telenav.demo.app.personalinfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datacollector.model.event.EntityCacheActionEvent
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType
import telenav.demo.app.R
import telenav.demo.app.databinding.FragmentPersonalInfoBottomBinding
import telenav.demo.app.entitydetails.EntityDetailsActivity
import telenav.demo.app.utils.deleteFavorite
import telenav.demo.app.utils.entityCachedClick
import telenav.demo.app.widgets.RoundedBottomSheetLayout
import java.lang.reflect.Type

private const val TAG = "PersonalInfoFragment"

class PersonalInfoFragment : RoundedBottomSheetLayout() {

    private var binding : FragmentPersonalInfoBottomBinding? = null
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPersonalInfoBottomBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.personalInfoAreaBack?.setOnClickListener { dismiss() }
        binding?.personalInfoOta?.setOnClickListener { showHomeAreaActivity() }

        getFavoriteData()
        fillHomeInfo()
        fillWorkInfo()
    }

    private fun fillFavoriteList(favoriteEntities: List<Entity>?) {
        if (favoriteEntities.isNullOrEmpty()) {
            binding?.personalFavoriteList?.visibility = View.GONE
            return
        }
        binding?.personalFavoriteList?.adapter = FavoriteResultsListRecyclerAdapterNew(
            favoriteEntities,
            object : OnDeleteFavoriteListener {
                override fun onDelete(entity: Entity) {
                    deleteFavoriteEntity(entity)
                }
            }
        )
    }

    private fun deleteFavoriteEntity(entity: Entity) {
        dataCollectorClient.deleteFavorite(requireContext(), entity)
        getFavoriteData()
    }

    private fun getFavoriteData() {
        val prefs =
            requireContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val listType: Type = object : TypeToken<List<Entity>>() {}.type
        val favoriteEntities = Gson().fromJson<List<Entity>>(
            prefs?.getString(
                getString(R.string.saved_favorite_list_key),
                ""
            ), listType
        )
        fillFavoriteList(favoriteEntities)
    }

    private fun fillHomeInfo() {
        val prefs =
            requireContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val storedHome = Gson().fromJson(
            prefs.getString(getString(R.string.saved_home_address_key), ""),
            Entity::class.java
        )

        if (storedHome == null) {
            binding?.homeAddressConfigure?.text = requireContext().getString(R.string.setup)
            return
        } else {
            binding?.homeAddressConfigure?.text = requireContext().getString(R.string.edit)
        }

        val name =
            if (storedHome.type == EntityType.ADDRESS) storedHome.address.formattedAddress else storedHome.place.name

        binding?.homeAddress?.text = name
        binding?.homeAddress?.setOnClickListener {
            dataCollectorClient.entityCachedClick(storedHome.id, EntityCacheActionEvent.SourceType.HOME)
            startActivity(
                Intent(
                    requireContext(),
                    EntityDetailsActivity::class.java
                ).apply {
                    putExtra(EntityDetailsActivity.PARAM_ID, storedHome.id)
                    putExtra(
                        EntityDetailsActivity.PARAM_SOURCE,
                        EntityCacheActionEvent.SourceType.HOME.name
                    )
                })
        }
    }

    private fun fillWorkInfo() {
        val prefs =
            requireContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val storedWork = Gson().fromJson(
            prefs.getString(getString(R.string.saved_work_address_key), ""),
            Entity::class.java
        )

        if (storedWork == null) {
            binding?.workAddressConfigure?.text = requireContext().getString(R.string.setup)
            return
        } else {
            binding?.workAddressConfigure?.text = requireContext().getString(R.string.edit)
        }

        val name =
            if (storedWork.type == EntityType.ADDRESS) storedWork.address.formattedAddress else storedWork.place.name

        binding?.workAddress?.text = name
        binding?.workAddress?.setOnClickListener {
            dataCollectorClient.entityCachedClick(storedWork.id, EntityCacheActionEvent.SourceType.WORK)
            startActivity(
                Intent(
                    requireContext(),
                    EntityDetailsActivity::class.java
                ).apply {
                    putExtra(EntityDetailsActivity.PARAM_ID, storedWork.id)
                    putExtra(
                        EntityDetailsActivity.PARAM_SOURCE,
                        EntityCacheActionEvent.SourceType.WORK.name
                    )
                })
        }
    }

    private fun showHomeAreaActivity() {
        startActivity(Intent(requireContext(), HomeAreaActivity::class.java))
    }

    companion object {
        fun newInstance() =
            PersonalInfoFragment()
    }

}