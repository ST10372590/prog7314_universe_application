package student.projects.universe

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying chat messages using Firebase in real-time.
 * Differentiates between sender and receiver messages and marks read status.
 */
class MessageAdapter(
    private val messages: MutableList<MessageResponse>,
    private val currentUserId: Int
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessageContent: TextView = itemView.findViewById(R.id.tvMessageContent)
        val ivReadStatus: ImageView? = itemView.findViewById(R.id.ivReadStatus) // only in sender layout
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderID == currentUserId) 1 else 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 1)
            R.layout.item_message_sender
        else
            R.layout.item_message_receiver

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.tvMessageContent.text = message.content

        // Bold unread messages if received by current user
        if (message.readStatus.equals("Unread", ignoreCase = true) &&
            message.senderID != currentUserId
        ) {
            holder.tvMessageContent.setTypeface(null, Typeface.BOLD)
        } else {
            holder.tvMessageContent.setTypeface(null, Typeface.NORMAL)
        }

        // Show read receipt icon only for messages sent by current user
        if (message.senderID == currentUserId) {
            holder.ivReadStatus?.visibility = View.VISIBLE

            // Show double check for read, single check for unread
            if (message.readStatus.equals("Read", ignoreCase = true)) {
                holder.ivReadStatus?.setImageResource(R.drawable.ic_double_check) // your double check drawable
            } else {
                holder.ivReadStatus?.setImageResource(R.drawable.ic_check) // your single check drawable
            }
        } else {
            // Hide read receipt icon for received messages
            holder.ivReadStatus?.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = messages.size
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */