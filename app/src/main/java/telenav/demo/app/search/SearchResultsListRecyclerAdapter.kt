package telenav.demo.app.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datacollector.model.event.EntityCacheActionEvent
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType
import telenav.demo.app.R
import telenav.demo.app.utils.entityCachedClick
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.marginStart
import telenav.demo.app.utils.Converter

class SearchResultsListRecyclerAdapter(
    val entities: List<Entity>,
    val context: Context,
    private val onClickListener: OnEntityClickListener,
    private val pattern: String = "",
) :
    RecyclerView.Adapter<EntityFavoriteHolder>() {

    interface OnEntityClickListener {
        fun onEntityClick(entity: Entity)
    }

    private val dataCollectorClient by lazy { DataCollectorService.getClient() }
    private var list: List<Entity> = entities

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityFavoriteHolder {
        val view = LayoutInflater.from((parent.context))
            .inflate(R.layout.favorite_list_item, parent, false)

        return EntityFavoriteHolder(view)
    }

    override fun onBindViewHolder(holder: EntityFavoriteHolder, position: Int) {
        val entity = list[position]
        val name =
            if (entity.type == EntityType.ADDRESS) entity.address.formattedAddress else entity.place.name

        holder.vName.text = getSpannableNameText(name)

        holder.vName.post {
            val totalWidth = holder.vName.width + holder.vName.marginStart + 20
            val params = holder.vAddress.layoutParams as ConstraintLayout.LayoutParams

            params.setMargins(totalWidth, 0, Converter.convertDpToPixel(context, 20f), 0)
            holder.vAddress.layoutParams = params
        }

        if (entity.type == EntityType.ADDRESS) {
            holder.vAddress.visibility = View.GONE
        } else {
            val address = entity.place.address.formattedAddress
            holder.vAddress.text = getSpannableNameText(address)
            holder.vAddress.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            dataCollectorClient.entityCachedClick(
                entity.id,
                EntityCacheActionEvent.SourceType.FAVORITE
            )
            onClickListener.onEntityClick(entity)
            return@setOnClickListener
        }
    }

    private fun getSpannableNameText(text: String): SpannableString {
        val spannableNameString = SpannableString(text)
        val color = context.getColor(R.color.blue_c1)
        val blueColor = ForegroundColorSpan(color)
        val index = text.lowercase().indexOf(pattern.lowercase())
        if (index != -1) {
            spannableNameString.setSpan(blueColor, index, index + pattern.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannableNameString
    }

    override fun getItemCount(): Int {
        return list.size
    }
}

class EntityFavoriteHolder(view: View) : RecyclerView.ViewHolder(view) {
    val vName = view.findViewById<TextView>(R.id.entity_name)
    val vAddress = view.findViewById<TextView>(R.id.entity_address)
}