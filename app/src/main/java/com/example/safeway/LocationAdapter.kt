package com.example.safeway

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LocationAdapter(
    private val locationList: MutableList<Location>,
    private val onItemClick: (Location) -> Unit // 클릭 이벤트 콜백 추가
) : RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_item, parent, false)
        return LocationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locationList[position]
        holder.locationName.text = location.name
        holder.locationAddress.text = location.address

        // 검색된 장소 클릭 시 onItemClick 호출
        holder.itemView.setOnClickListener {
            onItemClick(location)
        }
    }

    override fun getItemCount(): Int = locationList.size

    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val locationName: TextView = itemView.findViewById(R.id.textViewLocationName)
        val locationAddress: TextView = itemView.findViewById(R.id.textViewLocationAddress)
        val imageViewLocation: ImageView = itemView.findViewById(R.id.imageViewLocation)
    }
}
