package dilipsuthar.saathi.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dilipsuthar.saathi.R
import dilipsuthar.saathi.model.ItemRideHistory

class AdapterRideHistory constructor(private val items: ArrayList<ItemRideHistory>) : RecyclerView.Adapter<AdapterRideHistory.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ride_history, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var item = items[position]

        holder.bikeId.text = item.bike_id.toString()
        holder.distance.text = item.travel_distance.toString()
        holder.time.text = item.travel_time.toString()
        holder.amount.text = item.travel_amount.toString()
        holder.rideDateTime.text = item.ride_date_time.toString()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var bikeId: TextView = itemView.findViewById(R.id.textBikeId)
        var distance: TextView = itemView.findViewById(R.id.textTravelDistance)
        var time: TextView = itemView.findViewById(R.id.textTravelTime)
        var amount: TextView = itemView.findViewById(R.id.textAmount)
        var rideDateTime: TextView = itemView.findViewById(R.id.textRideDateTime)

    }

}