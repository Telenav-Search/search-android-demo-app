package telenav.demo.app.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.entity.model.base.Entity
import kotlinx.android.synthetic.main.activity_search_list.*
import telenav.demo.app.R
import telenav.demo.app.map.MapActivity
import telenav.demo.app.searchlist.SearchListRecyclerAdapter
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.widgets.RoundedBottomSheetLayout

class SearchHotCategoriesFragment : RoundedBottomSheetLayout() {

    private lateinit var vSearchTitle: TextView
    private lateinit var vSearchEmpty: TextView
    private lateinit var vSearchError: TextView
    private lateinit var vSearchLoading: ContentLoadingProgressBar
    private lateinit var vSearchList: RecyclerView
    private lateinit var vSearchToggle: ImageView

    private var searchRes: List<Entity> = arrayListOf()
    private var categoryId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vSearchTitle = view.findViewById(R.id.search_title)
        vSearchLoading = view.findViewById(R.id.search_loading)
        vSearchEmpty = view.findViewById(R.id.search_empty)
        vSearchError = view.findViewById(R.id.search_error)
        vSearchList = view.findViewById(R.id.search_list)
        vSearchToggle = view.findViewById(R.id.search_toggle)
        vSearchLoading.show()


        var categoryName = ""
        for (eachCategory in CategoryAndFiltersUtil.hotCategoriesList) {
            if ((eachCategory.id) == categoryId) {
                categoryName = eachCategory.name
                break
            }
        }

        displaySearchResults(searchRes, categoryId)

        vSearchList.layoutManager = LinearLayoutManager(activity)
        view.findViewById<View>(R.id.search_close)
            .setOnClickListener { dismiss() }

        view.findViewById<TextView>(R.id.search_title).text = categoryName
    }

    private fun displaySearchResults(response: List<Entity>, currentSearchHotCategory: String?) {
        vSearchLoading.hide()

        if (response.isNotEmpty()) {
            vSearchList.adapter = SearchListRecyclerAdapter(
                response,
                R.drawable.ic_coffee_color,
                object : SearchListRecyclerAdapter.OnEntityClickListener {
                    override fun onEntityClick(entity: Entity) {
                        dismiss()
                        (activity as MapActivity).displayEntityClicked(entity, currentSearchHotCategory)
                    }
                }
            )
            setToggler(true)
        } else
            vSearchEmpty.visibility = View.VISIBLE
    }

    private fun setToggler(opened: Boolean) {
        vSearchToggle.visibility = View.VISIBLE
        if (opened) {
            vSearchToggle.animate().rotationBy(180f).start()
            vSearchToggle.setOnClickListener {
                search_list.visibility = View.GONE
                setToggler(false)
            }
        } else {
            vSearchToggle.animate().rotationBy(180f).start()
            vSearchToggle.setOnClickListener {
                search_list.visibility = View.VISIBLE
                setToggler(true)
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(searchResults: List<Entity>, categoryId: String?) =
            SearchHotCategoriesFragment().apply {
                searchRes = searchResults
                this.categoryId = categoryId
            }
    }
}

