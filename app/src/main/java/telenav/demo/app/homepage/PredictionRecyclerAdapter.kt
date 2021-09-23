package telenav.demo.app.homepage

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.telenav.sdk.entity.model.prediction.Destination
import telenav.demo.app.R
import telenav.demo.app.convertNumberToDistance

class PredictionRecyclerAdapter(
    destinations: List<Destination>,
    val clickListener: (Destination) -> Unit
) :
    RecyclerView.Adapter<PredictionHolder>() {
    var list: List<Destination> = destinations

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionHolder {
        val view = LayoutInflater.from((parent.context))
            .inflate(R.layout.destination_list_item, parent, false)

        return PredictionHolder(view)
    }

    override fun onBindViewHolder(holder: PredictionHolder, position: Int) {
        val item = list[position]
        holder.vName.text = item.label
        holder.itemView.setOnClickListener { clickListener(item) }
        holder.vDistanceTo.text = item.entity.type.name

    }

    override fun getItemCount(): Int {
        return list.size
    }

}

class PredictionHolder(view: View) : RecyclerView.ViewHolder(view) {
    val vName = view.findViewById<TextView>(R.id.destination_name)
    val vDistanceTo = view.findViewById<TextView>(R.id.destination_distance)
}