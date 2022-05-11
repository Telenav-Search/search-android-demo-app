package telenav.demo.app.widgets

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.MainThread

import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView

import kotlin.math.min

import telenav.demo.app.R
import telenav.demo.app.utils.CategoryAndFiltersUtil

/**
 * Adapter to control CategoryView display in RecyclerView
 *
 * @param listener onClickListener, call back with position and viewData when Category item clicked
 * @since 2022-05-10
 */
class CategoryAdapter(private val listener: (Int, CategoryAndFiltersUtil.CategoryViewData) -> Unit)
    : RecyclerView.Adapter<CategoryAdapter.CategoryVH>() {
    companion object {
        private const val TAG = "CategoryAdapter"
    }

    // we have two situations: 1 - HotCategory, 2 rows to show, 2 - SubCategory, 1 row to show
    // when it comes to limited row mode, use mLimit to limit the data size, otherwise use -1 for no limit
    private var mLimit = -1

    private val mData = mutableListOf<CategoryAndFiltersUtil.CategoryViewData>()

    /**
     * Set category data to display
     *
     * @param data list of ViewData, use [CategoryAndFiltersUtil.toViewData] to format input
     */
    @MainThread
    fun setData(data: List<CategoryAndFiltersUtil.CategoryViewData>) {
        Log.i(TAG, "setData: data size = ${data.size}")
        mData.clear()
        mData.addAll(data)
        notifyDataSetChanged()
    }

    /**
     * Set row limitation
     *
     * @param rows row counts
     */
    fun setRowLimit(rows: Int) {
        Log.i(TAG, "setRowLimit: rows = $rows")
        mLimit = if (rows >= 0 ) { rows * CategoryAndFiltersUtil.DISPLAY_LIMIT } else { -1 }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
        return CategoryVH(LayoutInflater.from(parent.context).inflate(R.layout.category_holder, parent, false)).apply {
            itemView.setOnClickListener {
                Log.i(TAG, "item $adapterPosition clicked")
                listener.invoke(adapterPosition, mData[adapterPosition])
            }
        }
    }

    override fun onBindViewHolder(holder: CategoryVH, position: Int) {
        val draw = ResourcesCompat.getDrawable(holder.icon.resources, mData[position].icon, null)
        holder.icon.setImageDrawable(draw)
        holder.name.text = mData[position].name
    }

    override fun getItemCount(): Int = if (mLimit < 0) mData.size else min(mLimit, mData.size)

    /**
     * ViewHolder for CategoryAdapter
     *
     * @since 2022-05-10
     */
    class CategoryVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.category_icon)
        val name: TextView = itemView.findViewById(R.id.category_name)
    }
}

