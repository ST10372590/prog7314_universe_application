package student.projects.universe

import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying chat messages in a RecyclerView.
 * Differentiates between sender and receiver messages.
 * Highlights unread messages and shows read receipts for sender messages.
 */
class MessageAdapter(
    private val messages: MutableList<MessageResponse>,
    private val currentUserId: Int
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val TAG = "MessageAdapter"

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessageContent: TextView = itemView.findViewById(R.id.tvMessageContent)
        val ivReadStatus: ImageView? = itemView.findViewById(R.id.ivReadStatus) // Only exists in sender layout
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val viewType = if (message.senderID == currentUserId) 1 else 2
        Log.d(TAG, "getItemViewType for position $position: $viewType (senderID=${message.senderID}, currentUserId=$currentUserId)")
        return viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 1) {
            R.layout.item_message_sender
        } else {
            R.layout.item_message_receiver
        }

        Log.d(TAG, "Inflating layout: $layout for viewType: $viewType")
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.tvMessageContent.text = message.content
        Log.d(TAG, "Binding message at position $position: '${message.content.take(30)}...'")

        // Bold unread received messages for emphasis
        if (message.readStatus.equals("Unread", ignoreCase = true) &&
            message.senderID != currentUserId
        ) {
            holder.tvMessageContent.setTypeface(null, Typeface.BOLD)
            Log.d(TAG, "Message at position $position is unread and received: applying bold text")
        } else {
            holder.tvMessageContent.setTypeface(null, Typeface.NORMAL)
        }

        if (message.senderID == currentUserId) {
            holder.ivReadStatus?.visibility = View.VISIBLE

            // Show read receipts with appropriate icon
            if (message.readStatus.equals("Read", ignoreCase = true)) {
                holder.ivReadStatus?.setImageResource(R.drawable.ic_double_check) // Double check icon
                Log.d(TAG, "Message at position $position is read: showing double check icon")
            } else {
                holder.ivReadStatus?.setImageResource(R.drawable.ic_check) // Single check icon
                Log.d(TAG, "Message at position $position is unread: showing single check icon")
            }
        } else {
            // Hide read receipt for received messages
            holder.ivReadStatus?.visibility = View.GONE
            Log.d(TAG, "Message at position $position is received: hiding read receipt icon")
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