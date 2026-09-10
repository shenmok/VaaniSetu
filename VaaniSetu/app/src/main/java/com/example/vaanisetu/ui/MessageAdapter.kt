package com.example.vaanisetu.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vaanisetu.R
import com.example.vaanisetu.viewmodel.IncomingMessage

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<IncomingMessage>()

    fun setMessages(newMessages: List<IncomingMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderText: TextView = itemView.findViewById(R.id.senderNameText)
        private val msgText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(message: IncomingMessage) {
            senderText.text = message.sender
            msgText.text = message.text
            
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            timeText.text = sdf.format(java.util.Date(message.timestamp))
        }
    }
}
