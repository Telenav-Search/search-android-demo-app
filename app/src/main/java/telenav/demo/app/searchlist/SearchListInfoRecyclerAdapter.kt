package telenav.demo.app.searchlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType
import telenav.demo.app.R
import telenav.demo.app.convertNumberToDistance

class SearchListInfoRecyclerAdapter(
    entities: List<Entity>,
    val onClickListener: OnEntityClickListener?
) :
    RecyclerView.Adapter<SearchEntityHolder>() {
    var list: List<Entity> = entities

    interface OnEntityClickListener {
        fun onEntityClick(entity: Entity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchEntityHolder {
        val view = LayoutInflater.from((parent.context))
            .inflate(R.layout.entity_search_list_item, parent, false)

        return SearchEntityHolder(view)
    }

    override fun onBindViewHolder(holder: SearchEntityHolder, position: Int) {
        val entity = list[position]
        val name =
            if (entity.type == EntityType.ADDRESS) "" else entity.place.name

        holder.vName.text = name
        holder.itemView.setOnClickListener {
            onClickListener?.onEntityClick(entity)

        }
        holder.vDistanceTo.text =
            holder.vDistanceTo.context.convertNumberToDistance(entity.distance)

        if (entity.type != EntityType.ADDRESS) {
            holder.vAddress.text = entity.place.address.formattedAddress
            holder.vAddress.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}

class SearchEntityHolder(view: View) : RecyclerView.ViewHolder(view) {
    val vName = view.findViewById<TextView>(R.id.entity_name)
    val vAddress = view.findViewById<TextView>(R.id.entity_address)
    val vDistanceTo = view.findViewById<TextView>(R.id.entity_distance)


}