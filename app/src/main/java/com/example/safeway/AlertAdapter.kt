package com.example.safeway

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AlertAdapter(
    private val alertList: List<Alert>,
    private val onItemClick: (Alert) -> Unit
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.alert_item, parent, false)
        return AlertViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alertList[position]
        holder.alertTitle.text = alert.title
        holder.alertContent.text = alert.content
        holder.alertTime.text = getRelativeTime(alert.time) // ✅ 상대적 시간 표시

        holder.itemView.setOnClickListener {
            onItemClick(alert)
        }
    }

    override fun getItemCount(): Int = alertList.size

    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alertTitle: TextView = itemView.findViewById(R.id.textViewAlertTitle)
        val alertContent: TextView = itemView.findViewById(R.id.textViewAlertContent)
        val alertTime: TextView = itemView.findViewById(R.id.textViewAlertTime)
    }

    // ✅ 시간을 상대적으로 변환하는 함수
    private fun getRelativeTime(time: Date): String {
        val now = Date()
        val diffInMillis = now.time - time.time

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            hours < 24 -> "${hours}시간 전"
            days < 7 -> "${days}일 전"
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(time) // 일주일 이상이면 날짜로 표시
        }
    }
}
