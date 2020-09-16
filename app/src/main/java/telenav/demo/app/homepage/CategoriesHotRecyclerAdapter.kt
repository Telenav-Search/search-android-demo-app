package telenav.demo.app.homepage

import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import telenav.demo.app.R
import telenav.demo.app.searchlist.SearchListFragment

class CategoriesHotRecyclerAdapter(categories: List<HotCategory>, val onCategoryClicked: (category:HotCategory) -> Unit) :
    RecyclerView.Adapter<CategoryHolder>() {
    var list: List<HotCategory> = categories

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryHolder {
        val view = LayoutInflater.from((parent.context))
            .inflate(R.layout.category_list_item, parent, false)

        return CategoryHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryHolder, position: Int) {
        val category = list[position];
        holder.vName.text = category.name
        holder.vExpand.visibility = View.GONE
        holder.vSearch.visibility = View.GONE
        holder.vIcon.visibility = View.VISIBLE
        holder.vName.setTypeface(
            null,
            if (category.id.isEmpty()) Typeface.NORMAL else Typeface.BOLD
        )
        holder.vIcon.setImageResource(category.icon)
        holder.vItem.setOnClickListener {
            onCategoryClicked(category)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}