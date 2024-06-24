package telenav.demo.app.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.telenav.sdk.entity.model.base.Entity
import telenav.demo.app.databinding.RecentSearchListFragmentBinding
import telenav.demo.app.map.MapActivity

private const val TAG = "RecentSearchListFragment"

class RecentSearchListFragment : Fragment() {

    private var binding: RecentSearchListFragmentBinding? = null
    private var entities: List<Entity>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = RecentSearchListFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayRecentSearchInfo()
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
                    }
                }
            )
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(entities: List<Entity>?) =
            RecentSearchListFragment().apply {
                this.entities = entities
            }
    }
}