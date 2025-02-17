package com.example.safeway

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LocationAdapter(private val locationList: MutableList<Location>) :
    RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_item, parent, false)
        return LocationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locationList[position]
        holder.locationName.text = location.name
        holder.locationAddress.text = location.address
        // 아이콘이나 다른 이미지를 설정하려면 아래와 같이 설정
        // holder.imageViewLocation.setImageResource(location.imageResId)
    }

    override fun getItemCount(): Int = locationList.size

    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val locationName: TextView = itemView.findViewById(R.id.textViewLocationName)
        val locationAddress: TextView = itemView.findViewById(R.id.textViewLocationAddress)
        val imageViewLocation: ImageView = itemView.findViewById(R.id.imageViewLocation)
    }
}