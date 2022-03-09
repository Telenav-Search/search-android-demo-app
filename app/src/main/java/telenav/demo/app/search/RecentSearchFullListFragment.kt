package telenav.demo.app.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.telenav.sdk.entity.model.base.Entity
import kotlinx.android.synthetic.main.search_info_bottom_fragment_layout.search
import kotlinx.android.synthetic.main.view_header_search.*
import telenav.demo.app.databinding.RecentSearchFullListFragmentBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.widgets.RoundedBottomSheetLayout

private const val TAG = "RecentSearchFullListFragment"

class RecentSearchFullListFragment : RoundedBottomSheetLayout() {

    private var binding: RecentSearchFullListFragmentBinding? = null
    private var entities: List<Entity>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = RecentSearchFullListFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayRecentSearchInfo()

        search.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                (activity!! as MapActivity).showSearchListBottomFragment()
                dismiss()
            }
        }
        search.setOnClickListener {
            (activity!! as MapActivity).showSearchListBottomFragment()
            dismiss()
        }

        user_icon.setOnClickListener {
            (activity!! as MapActivity).collapseBottomSheet()
            (activity!! as MapActivity).showPersonalInfoFragment()
        }
    }

    private fun displayRecentSearchInfo() {
        if (entities.isNullOrEmpty()) {
            binding?.searchList?.visibility = View.GONE
        } else {
            binding?.searchList?.visibility = View.VISIBLE
            binding?.searchList?.layoutManager = LinearLayoutManager(requireContext())
            binding?.searchList?.adapter = SearchResultsListRecyclerAdapter(
                entities!!, requireContext(),
                object : SearchResultsListRecyclerAdapter.OnEntityClickListener {
                    override fun onEntityClick(entity: Entity) {
                        (activity!! as MapActivity).collapseBottomSheet()
                        (activity!! as MapActivity).displayEntityClicked(entity, "")
                        dismiss()
                    }
                }
            )
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(entities: List<Entity>?) =
            RecentSearchFullListFragment().apply {
                this.entities = entities
            }
    }
}