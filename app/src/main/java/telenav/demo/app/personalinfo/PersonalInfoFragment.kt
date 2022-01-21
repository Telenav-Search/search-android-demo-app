package telenav.demo.app.personalinfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.entity.model.base.Entity
import telenav.demo.app.R
import telenav.demo.app.databinding.FragmentPersonalInfoBottomBinding
import telenav.demo.app.utils.deleteFavorite
import java.lang.reflect.Type
import androidx.recyclerview.widget.ItemTouchHelper
import telenav.demo.app.map.MapActivity
import telenav.demo.app.utils.SwipeToDeleteCallback
import telenav.demo.app.widgets.RoundedBottomSheetLayout

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

        binding?.personalInfoAreaBack?.setOnClickListener {
            dismiss()
            (activity!! as MapActivity).showBottomSheet()
        }
        binding?.personalInfoOta?.setOnClickListener { showHomeAreaActivity() }

        childFragmentManager.beginTransaction().replace(R.id.user_address,
            UserAddressFragment.newInstance()).commit()

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

    private fun fillFavoriteList(favoriteEntities: List<Entity>?) {
        if (favoriteEntities.isNullOrEmpty()) {
            binding?.personalFavoriteList?.visibility = View.GONE
            binding?.favorites?.visibility = View.GONE
            return
        }

        binding?.favorites?.visibility = View.VISIBLE
        binding?.personalFavoriteList?.layoutManager = LinearLayoutManager(requireContext())
        binding?.personalFavoriteList?.adapter = FavoriteResultsListRecyclerAdapterNew(favoriteEntities)

        val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val entity = favoriteEntities[viewHolder.adapterPosition]
                deleteFavoriteEntity(entity)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding?.personalFavoriteList)
    }

    private fun deleteFavoriteEntity(entity: Entity) {
        dataCollectorClient.deleteFavorite(requireContext(), entity)
        getFavoriteData()
    }

    private fun showHomeAreaActivity() {
        startActivity(Intent(requireContext(), HomeAreaActivity::class.java))
    }

    companion object {
        fun newInstance() =
            PersonalInfoFragment()
    }
}