package com.example.safeway

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class EmergencyAdapter(
    private val context: Context, // ← 추가
    private val contactList: List<EmergencyContact>,
    private val onItemClick: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyAdapter.ContactViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.contactName.text = contact.name
        holder.contactPhone.text = contact.phoneNumber

        holder.itemView.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("연락처 삭제")
                .setMessage("${contact.name} 연락처를 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    onItemClick(contact)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    override fun getItemCount(): Int = contactList.size

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.textViewContactName)
        val contactPhone: TextView = itemView.findViewById(R.id.textViewContactPhone)
    }
}
