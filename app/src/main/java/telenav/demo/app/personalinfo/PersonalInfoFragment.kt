package telenav.demo.app.personalinfo

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
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
import telenav.demo.app.search.SearchResultsListRecyclerAdapter
import telenav.demo.app.utils.SwipeToDeleteCallback
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import telenav.demo.app.utils.Converter.convertDpToPixel

private const val TAG = "PersonalInfoFragment"
private const val USER_INFO_TAG = "UserAddressFragment"

class PersonalInfoFragment : BottomSheetDialogFragment() {

    private var binding : FragmentPersonalInfoBottomBinding? = null
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }
    private var shouldExtend: Boolean = false
    private var entities: List<Entity>? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPersonalInfoBottomBinding.inflate(inflater, container, false)

        return binding?.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.personalInfoAreaBack?.setOnClickListener {
            onBack()
        }

        binding?.settings?.setOnClickListener {
            dismiss()
            (activity!! as MapActivity).showSettingsActivity()
        }

        binding?.personalInfoOta?.setOnClickListener { showHomeAreaActivity() }
        binding?.resize?.setOnClickListener {
            updateUserInfoVisibility()
            shouldExtend = !shouldExtend
        }

        childFragmentManager.beginTransaction().replace(R.id.user_address,
            UserAddressFragment.newInstance(), USER_INFO_TAG).commit()

        getFavoriteData()
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            override fun onBackPressed() {
                onBack()
            }
        }.apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
        }
    }

    private fun onBack() {
        dismiss()
        (activity!! as MapActivity).updateBottomSheetState()
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
        this.entities = favoriteEntities
        fillFavoriteList(favoriteEntities)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun fillFavoriteList(favoriteEntities: List<Entity>?) {
        if (favoriteEntities.isNullOrEmpty()) {
            binding?.personalFavoriteList?.visibility = View.GONE
            binding?.favorites?.visibility = View.GONE
            binding?.resize?.visibility = View.GONE
            shouldExtend = true
            updateUserInfoVisibility()
            return
        }

        binding?.favorites?.visibility = View.VISIBLE
        binding?.resize?.visibility = View.VISIBLE
        binding?.personalFavoriteList?.layoutManager = LinearLayoutManager(requireContext())
        binding?.personalFavoriteList?.adapter = SearchResultsListRecyclerAdapter(favoriteEntities, requireContext(),
            object : SearchResultsListRecyclerAdapter.OnEntityClickListener {
                override fun onEntityClick(entity: Entity) {
                    (activity as MapActivity).collapseBottomSheet()
                    (activity as MapActivity).displayEntityClicked(entity, "",
                        navigationFromSearchInfo = false, navigationFromPersonalInfo = true)
                    dismiss()
                }
            }
        )

        val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                entities?.let {
                    val entity = it[viewHolder.adapterPosition]
                    deleteFavoriteEntity(entity)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding?.personalFavoriteList)
    }

    private fun deleteFavoriteEntity(entity: Entity) {
        dataCollectorClient.deleteFavorite(requireContext(), entity)
        getFavoriteData()
    }

    private fun updateUserInfoVisibility() {
        val params = binding?.favorites?.layoutParams as MarginLayoutParams
        val marginStart =  convertDpToPixel(requireContext(), 25f)

        if (shouldExtend) {
            childFragmentManager.beginTransaction().replace(R.id.user_address,
                UserAddressFragment.newInstance(), USER_INFO_TAG).commit()
            binding?.myPlace?.visibility = View.VISIBLE
            binding?.separateLine1?.visibility = View.VISIBLE
            params.setMargins(marginStart, convertDpToPixel(requireContext(), 192f), 0, 0)
            binding?.resize?.setText(R.string.extend)
        } else {
            val fragment: Fragment? = childFragmentManager.findFragmentByTag(USER_INFO_TAG)
            if (fragment != null) {
                childFragmentManager.beginTransaction().remove(fragment).commit()
            }
            binding?.myPlace?.visibility = View.GONE
            binding?.separateLine1?.visibility = View.GONE
            params.setMargins(marginStart, convertDpToPixel(requireContext(), 20f), 0, 0)
            binding?.resize?.setText(R.string.collapse)
        }
        binding?.favorites?.layoutParams = params
    }

    private fun showHomeAreaActivity() {
        startActivity(Intent(requireContext(), HomeAreaActivity::class.java))
    }

    companion object {
        fun newInstance() = PersonalInfoFragment()
    }
}