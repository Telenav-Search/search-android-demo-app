package telenav.demo.app.homepage

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.entity.model.prediction.Suggestion
import telenav.demo.app.R

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
            Log.w("test", "Open suggestion ${item.formattedLabel}")
            return@setOnClickListener
        }
        holder.vDistanceTo.text =
            if (item.entity != null) String.format("%.1f km", item.entity.distance / 1000) else ""
    }

    override fun getItemCount(): Int {
        return list.size
    }

}

class SuggestionHolder(view: View) : RecyclerView.ViewHolder(view) {
    val vName = view.findViewById<TextView>(R.id.suggestion_name)
    val vDistanceTo = view.findViewById<TextView>(R.id.suggestion_distance)
}