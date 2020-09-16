package telenav.demo.app.homepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.entity.model.prediction.Suggestion
import telenav.demo.app.R
import telenav.demo.app.convertNumberToDistance

class SuggestionRecyclerAdapter(
    suggestions: List<Suggestion>,
    val clickListener: (Suggestion) -> Unit
) :
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
        holder.itemView.setOnClickListener { clickListener(item) }
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