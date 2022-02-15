package telenav.demo.app.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.telenav.sdk.entity.model.base.Entity
import kotlinx.android.synthetic.main.search_info_bottom_fragment_layout.*
import kotlinx.android.synthetic.main.search_info_bottom_fragment_layout.search
import telenav.demo.app.databinding.SearchInfoBottomFragmentLayoutBinding
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.map.MapActivity
import telenav.demo.app.searchlist.SearchListInfoRecyclerAdapter
import telenav.demo.app.widgets.RoundedBottomSheetLayoutNew

private const val TAG = "SearchInfoBottomFragment"

class SearchInfoBottomFragment : RoundedBottomSheetLayoutNew() {

    private val viewModel: SearchInfoViewModel by viewModels()
    private var currentSearchHotCategoryId: String? = null
    private var currentSearchHotCategoryName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = SearchInfoBottomFragmentLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        val location = (activity!! as MapActivity).lastKnownLocation
        activity?.getUIExecutor()?.let { executor ->
            currentSearchHotCategoryId?.let {
                //(activity!! as MapActivity).setLastSearch(it)
                //(activity!! as MapActivity).redoButtonLogic()
                //viewModel.search(null, it, location, executor)
                //viewModel.requestSubcategories(it, location, executor)
            }
        }

        search.setText(currentSearchHotCategoryName)
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                val loc = (activity!! as MapActivity).lastKnownLocation
                (activity!! as MapActivity).setLastSearch(search.text.toString())
                (activity!! as MapActivity).redoButtonLogic()
                activity?.getUIExecutor()?.let {
                    currentSearchHotCategoryId = search.text.toString()
                    viewModel.search(search.text.toString(), null, loc, it)
                }
            }
            false
        }

        searchList.layoutManager = LinearLayoutManager(activity)

        viewModel.searchResults.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "result count ->  ${it.size} ")
            if (it.isNotEmpty()) {
                searchList.visibility = View.VISIBLE
                resultsEmpty.visibility = View.GONE
            } else {
                searchList.visibility = View.GONE
                resultsEmpty.visibility = View.VISIBLE
            }

            (activity as MapActivity).displaySearchResults(
                it as List<Entity>?, currentSearchHotCategoryId)


            searchList.adapter = SearchListInfoRecyclerAdapter(it,
                object : SearchListInfoRecyclerAdapter.OnEntityClickListener {
                    override fun onEntityClick(entity: Entity) {
                        (activity as MapActivity).displayEntityClicked(entity, currentSearchHotCategoryId)
                    }
                }
            )
        })

        viewModel.searchError.observe(viewLifecycleOwner, Observer {
            if (it) {
                searchError.visibility = View.VISIBLE
                resultsEmpty.visibility = View.GONE
            } else {
                searchError.visibility = View.GONE
            }
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer {
            if (it) {
                searchLoading.visibility = View.VISIBLE
            } else {
                searchLoading.visibility = View.GONE
            }
        })

    }

    companion object {
        @JvmStatic
        fun newInstance(categoryId: String?, categoryName: String?) =
            SearchInfoBottomFragment().apply {
                this.currentSearchHotCategoryId = categoryId
                this.currentSearchHotCategoryName = categoryName
            }
    }
}