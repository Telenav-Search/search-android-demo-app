package telenav.demo.app.searchlist

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType
import telenav.demo.app.R
import telenav.demo.app.convertNumberToDistance
import telenav.demo.app.entitydetails.EntityDetailsActivity

class SearchListRecyclerAdapter(entities: List<Entity>, val categoryIcon: Int) :
    RecyclerView.Adapter<EntityHolder>() {
    var list: List<Entity> = entities

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityHolder {
        val view = LayoutInflater.from((parent.context))
            .inflate(R.layout.entity_list_item, parent, false)

        return EntityHolder(view)
    }

    override fun onBindViewHolder(holder: EntityHolder, position: Int) {
        val entity = list[position];
        val name =
            if (entity.type == EntityType.ADDRESS) entity.address.formattedAddress else entity.place.name

        holder.vName.text = name
        holder.itemView.setOnClickListener {
            holder.itemView.context.startActivity(
                Intent(
                    holder.itemView.context,
                    EntityDetailsActivity::class.java
                ).apply {
                    putExtra(EntityDetailsActivity.PARAM_ID, entity.id)
                    if (categoryIcon != 0)
                        putExtra(EntityDetailsActivity.PARAM_ICON, categoryIcon)
                })
            return@setOnClickListener
        }
        holder.vDistanceTo.text =
            holder.vDistanceTo.context.convertNumberToDistance(entity.distance)
        holder.vNumber.text = "${position + 1}."
        if (entity.type == EntityType.ADDRESS) {
            holder.vAddress.visibility = View.GONE
        } else {
            holder.vAddress.text = entity.place.address.formattedAddress
            holder.vAddress.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}

class EntityHolder(view: View) : RecyclerView.ViewHolder(view) {
    val vName = view.findViewById<TextView>(R.id.entity_name)
    val vDistanceTo = view.findViewById<TextView>(R.id.entity_distance)
    val vNumber = view.findViewById<TextView>(R.id.entity_number)
    val vAddress = view.findViewById<TextView>(R.id.entity_address)
}