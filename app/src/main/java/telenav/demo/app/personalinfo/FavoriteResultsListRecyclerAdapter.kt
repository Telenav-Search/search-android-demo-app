package telenav.demo.app.personalinfo

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datacollector.model.event.EntityCacheActionEvent
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType
import com.telenav.sdk.entity.model.base.Rating
import telenav.demo.app.R
import telenav.demo.app.entitydetails.EntityDetailsActivity
import telenav.demo.app.utils.entityCachedClick

class FavoriteResultsListRecyclerAdapter(
    entities: List<Entity>,
    onDeleteListener: OnDeleteFavoriteResultListener
) :
    RecyclerView.Adapter<EntityHolder>() {
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }

    var list: List<Entity> = entities
    var deleteListener: OnDeleteFavoriteResultListener = onDeleteListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityHolder {
        val view = LayoutInflater.from((parent.context))
            .inflate(R.layout.favorite_list_item, parent, false)

        return EntityHolder(view)
    }

    override fun onBindViewHolder(holder: EntityHolder, position: Int) {
        val entity = list[position]
        Log.w("test", "Entity ${Gson().toJson(entity)}")
        val name =
            if (entity.type == EntityType.ADDRESS) entity.address.formattedAddress else entity.place.name

        holder.vName.text = name
        holder.itemView.setOnClickListener {
            dataCollectorClient.entityCachedClick(
                entity.id,
                EntityCacheActionEvent.SourceType.FAVORITE
            )
            holder.itemView.context.startActivity(
                Intent(
                    holder.itemView.context,
                    EntityDetailsActivity::class.java
                ).apply {
                    putExtra(
                        EntityDetailsActivity.PARAM_SOURCE,
                        EntityCacheActionEvent.SourceType.FAVORITE.name
                    )
                    putExtra(EntityDetailsActivity.PARAM_ID, entity.id)
                })
            return@setOnClickListener
        }
        holder.vNumber.text = "${position + 1}."
        holder.vDelete.setOnClickListener {
            deleteListener.onDelete(entity)
            return@setOnClickListener
        }
        if (entity.type == EntityType.ADDRESS) {
            holder.vAddress.visibility = View.GONE
        } else {
            holder.vAddress.text = entity.place.address.formattedAddress
            holder.vAddress.visibility = View.VISIBLE
        }

        if (entity.facets?.rating != null && entity.facets?.rating!!.size > 0)//&&
            showStars(entity.facets?.rating!![0], holder)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private fun showStars(rating: Rating, holder: EntityHolder) {
        holder.vEntityStars.visibility = View.VISIBLE
        holder.vEntityYelpSign.visibility = if (rating.source == "YELP") View.VISIBLE else View.GONE
        for (i in 0..5) {
            if (rating.averageRating >= i + 1) {
                holder.vEntityStar[i].setImageResource(R.drawable.ic_star_full)
            } else if (rating.averageRating > i) {
                holder.vEntityStar[i].setImageResource(R.drawable.ic_start_half)
            }
        }

        holder.vEntityRating.text =
            if (rating.source == "YELP")
                "${rating.totalCount} Yelp reviews"
            else
                "${rating.totalCount} reviews"
    }

}

class EntityHolder(view: View) : RecyclerView.ViewHolder(view) {
    val vNumber = view.findViewById<TextView>(R.id.entity_number)
    val vName = view.findViewById<TextView>(R.id.entity_name)
    val vDelete = view.findViewById<ImageView>(R.id.entity_delete)
    val vAddress = view.findViewById<TextView>(R.id.entity_address)
    val vEntityStars = view.findViewById<View>(R.id.entity_stars)
    val vEntityRating = view.findViewById<TextView>(R.id.entity_rating)
    val vEntityYelpSign = view.findViewById<View>(R.id.entity_yelp_sign)
    val vEntityStar = ArrayList<ImageView>().apply {
        add(view.findViewById(R.id.entity_star1))
        add(view.findViewById(R.id.entity_star2))
        add(view.findViewById(R.id.entity_star3))
        add(view.findViewById(R.id.entity_star4))
        add(view.findViewById(R.id.entity_star5))
    }

}

interface OnDeleteFavoriteResultListener {
    fun onDelete(entity: Entity)
}