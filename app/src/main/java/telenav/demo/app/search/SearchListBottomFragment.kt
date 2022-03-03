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
import telenav.demo.app.R
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.map.MapActivity
import telenav.demo.app.widgets.RoundedBottomSheetLayout
import android.text.Editable
import android.text.TextWatcher
import telenav.demo.app.databinding.SearchListBottomFragmentLayoutBinding
import telenav.demo.app.personalinfo.FavoriteResultsListRecyclerAdapterNew

private const val TAG = "SearchListBottomFragment"

class SearchListBottomFragment : RoundedBottomSheetLayout() {

    private val viewModel: SearchInfoViewModel by viewModels()
    private var currentSearchHotCategoryTag: String? = null
    private var binding: SearchListBottomFragmentLayoutBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SearchListBottomFragmentLayoutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                val loc = (activity!! as MapActivity).lastKnownLocation
                (activity!! as MapActivity).setLastSearch(search.text.toString())
                (activity!! as MapActivity).hideKeyboard(search)
                searchList.removeAllViewsInLayout()
                activity?.getUIExecutor()?.let {
                    viewModel.search(search.text.toString(), null, loc, it)
                }
            }
            false
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isNotEmpty()) {
                    binding?.clearText?.visibility = View.VISIBLE
                    binding?.recentHeader?.visibility = View.GONE
                } else {
                    binding?.clearText?.visibility = View.GONE
                    binding?.recentHeader?.visibility = View.VISIBLE
                    viewModel.getRecentSearchData(requireContext())
                }
            }
        })

        binding?.clearText?.setOnClickListener {
            search.setText("")
            binding?.recentHeader?.visibility = View.VISIBLE
            viewModel.getRecentSearchData(requireContext())
        }

        searchList.layoutManager = LinearLayoutManager(activity)

        viewModel.searchResults.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "result count ->  ${it.size} ")
            if (it.isNotEmpty()) {
                searchList.visibility = View.VISIBLE
                searchError.visibility = View.GONE
                viewModel.saveRecentSearchData(requireContext())
            } else {
                searchList.visibility = View.GONE
                searchError.visibility = View.GONE
                searchError.text = getString(R.string.no_result)
            }

            (activity as MapActivity).displaySearchResults(
                it as List<Entity>?, currentSearchHotCategoryTag)

            searchList.adapter = FavoriteResultsListRecyclerAdapterNew(it,
                object : FavoriteResultsListRecyclerAdapterNew.OnEntityClickListener {
                    override fun onEntityClick(entity: Entity) {
                        dismiss()
                        (activity as MapActivity).displayEntityClicked(entity, currentSearchHotCategoryTag)
                    }
                }
            )
        })

        viewModel.searchError.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrBlank()) {
                searchError.visibility = View.VISIBLE
                searchError.text = it
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

        binding?.searchListAreaBack?.setOnClickListener {
            dismiss()
            (activity!! as MapActivity).showBottomSheet()
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(categoryTag: String?) =
            SearchListBottomFragment().apply {
                this.currentSearchHotCategoryTag = categoryTag
            }
    }
}