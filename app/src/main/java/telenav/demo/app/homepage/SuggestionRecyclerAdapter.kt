package telenav.demo.app.homepage

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.telenav.sdk.entity.model.prediction.Suggestion
import com.telenav.sdk.entity.model.prediction.SuggestionType
import telenav.demo.app.R
import telenav.demo.app.convertNumberToDistance
import telenav.demo.app.entitydetails.EntityDetailsActivity
import telenav.demo.app.searchlist.SearchListActivity

class SuggestionRecyclerAdapter(suggestions: List<Suggestion>) :
    RecyclerView.Adapter<SuggestionHolder>() {
    var list: List<Suggestion> = suggestions

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionHolder {
        val view = LayoutInflater.from((parent.context))
            .inflate(R.layout.suggestion_list_item, parent, false)

        return SuggestionHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionHolder, position: Int) {
        val item = list[position];
        holder.vName.text = item.formattedLabel
        holder.itemView.setOnClickListener {
            Log.e("test", "click suggestion ${Gson().toJson(item)}")
            if (item.type == SuggestionType.ENTITY)
                holder.itemView.context.startActivity(
                    Intent(
                        holder.itemView.context,
                        EntityDetailsActivity::class.java
                    ).apply {
                        putExtra(EntityDetailsActivity.PARAM_ID, item.id)
                    })
            else
                holder.itemView.context.startActivity(
                    Intent(
                        holder.itemView.context,
                        SearchListActivity::class.java
                    ).apply {
                        putExtra(SearchListActivity.PARAM_TITLE, item.formattedLabel)
                        putExtra(SearchListActivity.PARAM_QUERY, item.query)
                    })
        }
        holder.vDistanceTo.text =
            if (item.entity != null) holder.vDistanceTo.context.convertNumberToDistance(item.entity.distance) else ""
    }

    override fun getItemCount(): Int {
        return list.size
    }

}

class SuggestionHolder(view: View) : RecyclerView.ViewHolder(view) {
    val vName = view.findViewById<TextView>(R.id.suggestion_name)
    val vDistanceTo = view.findViewById<TextView>(R.id.suggestion_distance)
}