package telenav.demo.app.homepage

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.entity.model.base.Category
import telenav.demo.app.R
import java.util.*

class CategoriesRecyclerAdapter(
    categories: List<Category>,
    val clickListener: (category: Category) -> Unit
) :
    RecyclerView.Adapter<CategoryHolder>() {
    var list: List<CategoryNode> =
        sortCategories(categories).map { cat: Category -> CategoryNode(0, cat) }

    private fun sortCategories(categories: List<Category>): List<Category> {
        Collections.sort(categories, object : Comparator<Category> {
            override fun compare(c1: Category, c2: Category): Int {
                if (c2.childNodes != null && c1.childNodes == null)
                    return 1
                if (c2.childNodes == null && c1.childNodes != null)
                    return -1
                return c1.name.compareTo(c2.name)
            }
        })
        categories.forEach { cat ->
            if (cat.childNodes != null)
                sortCategories(cat.childNodes)
        }
        return categories
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].level
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryHolder {
        val view = LayoutInflater.from((parent.context))
            .inflate(R.layout.category_list_item, parent, false)
        view.setPadding(60 * viewType, 0, 0, 0)

        return CategoryHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryHolder, position: Int) {
        val node = list[position]
        holder.vName.text = node.category.name
        holder.vExpand.visibility = if (node.expandable) View.VISIBLE else View.GONE
        holder.vSearch.visibility = if (!node.expandable) View.VISIBLE else View.GONE
        holder.vExpand.rotation = if (node.expanded) 90f else 0f
        holder.vName.setTypeface(null, if (node.expandable) Typeface.BOLD else Typeface.NORMAL)
        holder.vItem.setOnClickListener {
            if (!node.expandable) {
                clickListener(node.category)
                return@setOnClickListener
            }
            node.expanded = !node.expanded
            if (node.expanded) {
                list =
                    list.subList(0, position + 1) + node.category.childNodes.map { cat: Category ->
                        CategoryNode(node.level + 1, cat)
                    } + list.subList(position + 1, list.size)
            } else {
                var index = -1
                for (i in position + 1 until list.size)
                    if (list[i].level <= node.level) {
                        index = i
                        break
                    }
                if (index == -1)
                    list = list.subList(0, position + 1)
                else
                    list = list.subList(0, position + 1) + list.subList(index, list.size)
            }

            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}

class CategoryHolder(view: View) : RecyclerView.ViewHolder(view) {
    val vItem = view.findViewById<View>(R.id.category_item)
    val vExpand = view.findViewById<TextView>(R.id.category_item_expand)
    val vName = view.findViewById<TextView>(R.id.category_item_name)
    val vSearch = view.findViewById<ImageView>(R.id.category_item_search)
    val vIcon = view.findViewById<ImageView>(R.id.category_item_icon)
}

class CategoryNode(val level: Int, val category: Category) {
    val expandable = category.childNodes != null && category.childNodes.isNotEmpty()
    var expanded = false
}