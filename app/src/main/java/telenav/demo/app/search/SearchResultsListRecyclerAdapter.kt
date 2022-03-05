package telenav.demo.app.search

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

class SearchResultsListRecyclerAdapter(
    entities: List<Entity>,
    val onClickListener: OnEntityClickListener
) :
    RecyclerView.Adapter<EntityFavoriteHolder>() {

    interface OnEntityClickListener {
        fun onEntityClick(entity: Entity)
    }

    private val dataCollectorClient by lazy { DataCollectorService.getClient() }

    var list: List<Entity> = entities

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityFavoriteHolder {
        val view = LayoutInflater.from((parent.context))
            .inflate(R.layout.favorite_list_item_new, parent, false)

        return EntityFavoriteHolder(view)
    }

    override fun onBindViewHolder(holder: EntityFavoriteHolder, position: Int) {
        val entity = list[position]
        val name =
            if (entity.type == EntityType.ADDRESS) entity.address.formattedAddress else entity.place.name

        holder.vName.text = name

        if (entity.type == EntityType.ADDRESS) {
            holder.vAddress.visibility = View.GONE
        } else {
            holder.vAddress.text = entity.place.address.formattedAddress
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

    override fun getItemCount(): Int {
        return list.size
    }
}

class EntityFavoriteHolder(view: View) : RecyclerView.ViewHolder(view) {
    val vName = view.findViewById<TextView>(R.id.entity_name)
    val vAddress = view.findViewById<TextView>(R.id.entity_address)
}