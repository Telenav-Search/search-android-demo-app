package telenav.demo.app.searchlist

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType
import com.telenav.sdk.entity.model.base.Rating
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
        Log.w("test", "Entity ${Gson().toJson(entity)}")
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

        if (entity.facets?.rating != null && entity.facets?.rating!!.size > 0 && entity.facets?.rating!![0].source == "YELP")
            showStars(entity.facets?.rating!![0], holder)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private fun showStars(rating: Rating, holder: EntityHolder) {
        holder.vEntityStars.visibility = View.VISIBLE
        for (i in 0..5) {
            if (rating.averageRating >= i + 1) {
                holder.vEntityStar[i].setImageResource(R.drawable.ic_star_full)
            } else if (rating.averageRating > i) {
                holder.vEntityStar[i].setImageResource(R.drawable.ic_start_half)
            }
        }

        holder.vEntityRating.text = "${rating.totalCount} Yelp reviews"
    }

}

class EntityHolder(view: View) : RecyclerView.ViewHolder(view) {
    val vName = view.findViewById<TextView>(R.id.entity_name)
    val vDistanceTo = view.findViewById<TextView>(R.id.entity_distance)
    val vNumber = view.findViewById<TextView>(R.id.entity_number)
    val vAddress = view.findViewById<TextView>(R.id.entity_address)
    val vEntityStars = view.findViewById<View>(R.id.entity_stars)
    val vEntityRating = view.findViewById<TextView>(R.id.entity_rating)
    val vEntityStar = ArrayList<ImageView>().apply {
        add(view.findViewById(R.id.entity_star1))
        add(view.findViewById(R.id.entity_star2))
        add(view.findViewById(R.id.entity_star3))
        add(view.findViewById(R.id.entity_star4))
        add(view.findViewById(R.id.entity_star5))
    }

}