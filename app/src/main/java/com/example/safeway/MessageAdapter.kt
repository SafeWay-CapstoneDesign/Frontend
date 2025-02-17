package com.example.safeway

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    // 뷰홀더 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    // 뷰홀더에 데이터 바인딩
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    // 아이템 개수 반환
    override fun getItemCount(): Int {
        return messages.size
    }

    // 메시지 항목을 나타내는 뷰홀더 클래스
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)

        fun bind(message: ChatMessage) {
            messageTextView.text = message.text
            if (message.isSentByMe) {
                itemView.setBackgroundColor(Color.parseColor("#C1FFC1")) // 발신 메시지 배경
            } else {
                itemView.setBackgroundColor(Color.parseColor("#D0E8FF")) // 수신 메시지 배경
            }
        }
    }
}
