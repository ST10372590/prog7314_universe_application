package student.projects.universe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentListAdapter(
    private val users: List<UserResponse>,
    private val onMessageClick: (UserResponse) -> Unit
) : RecyclerView.Adapter<StudentListAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val btnSendMessage: Button = itemView.findViewById(R.id.btnSendMessage)
        val tvUnreadBadge: TextView = itemView.findViewById(R.id.tvUnreadBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val user = users[position]
        holder.tvStudentName.text = "${user.firstName} ${user.lastName}"

        // Show unread badge if unreadCount > 0
        if (user.unreadCount > 0) {
            holder.tvUnreadBadge.visibility = View.VISIBLE
            holder.tvUnreadBadge.text = user.unreadCount.toString()
        } else {
            holder.tvUnreadBadge.visibility = View.GONE
        }

        holder.btnSendMessage.setOnClickListener {
            onMessageClick(user)
        }
    }

    override fun getItemCount(): Int = users.size
}




/*
package student.projects.universe

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.database.FirebaseDatabase

/**
 * Adapter for displaying user list with unread message indicators.
 * Integrated with Firebase Realtime Database.
 */
class StudentListAdapter(
    private val users: List<UserResponse>,
    private val onMessageClick: (UserResponse) -> Unit
) : RecyclerView.Adapter<StudentListAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val btnSendMessage: Button = itemView.findViewById(R.id.btnSendMessage)
        val tvUnreadBadge: TextView = itemView.findViewById(R.id.tvUnreadBadge)
    }

    private val firebaseDb = FirebaseDatabase.getInstance().reference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val user = users[position]
        holder.tvStudentName.text = "${user.firstName} ${user.lastName}"

        // Check for unread messages from Firebase
        val chatRef = firebaseDb.child("messages")
        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var hasUnread = false
                for (conversation in snapshot.children) {
                    for (msg in conversation.children) {
                        val senderId = msg.child("senderID").getValue(Int::class.java)
                        val receiverId = msg.child("receiverID").getValue(Int::class.java)
                        val readStatus = msg.child("readStatus").getValue(String::class.java)

                        if (receiverId == ApiClient.currentUserId &&
                            senderId == user.userID &&
                            readStatus == "Unread"
                        ) {
                            hasUnread = true
                            break
                        }
                    }
                }

                if (hasUnread) {
                    holder.tvUnreadBadge.visibility = View.VISIBLE
                    holder.tvUnreadBadge.text = "New"
                    holder.tvUnreadBadge.setTextColor(Color.WHITE)
                    holder.tvUnreadBadge.setBackgroundColor(Color.RED)
                } else {
                    holder.tvUnreadBadge.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StudentListAdapter", "Error fetching messages: ${error.message}")
            }
        })

        holder.btnSendMessage.setOnClickListener {
            onMessageClick(user)
        }
    }

    override fun getItemCount(): Int = users.size
}

/*
package student.projects.universe

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying a list of students in a RecyclerView.
 * Allows sending messages to individual students via a callback.
 */
class StudentListAdapter( // (Patel, 2025)
    private val students: List<UserResponse>,
    private val onMessageClick: (UserResponse) -> Unit
) : RecyclerView.Adapter<StudentListAdapter.StudentViewHolder>() {

    // --- ViewHolder to hold UI elements for each student item // (Patel, 2025)
    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val btnSendMessage: Button = itemView.findViewById(R.id.btnSendMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder { // (Patel, 2025)
        Log.d("StudentListAdapter", "Creating new ViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        // Display student's full name (concatenated) // (Patel, 2025)
        holder.tvStudentName.text = "${student.firstName} ${student.lastName}"
        Log.d("StudentListAdapter", "Binding student at position $position: ${student.firstName} ${student.lastName}")

        // Handle send message button click // (Patel, 2025)
        holder.btnSendMessage.setOnClickListener {
            Log.d("StudentListAdapter", "Send message clicked for student: ${student.firstName} ${student.lastName}")
            onMessageClick(student)
        }
    }

    override fun getItemCount(): Int { // (Patel, 2025)
        Log.d("StudentListAdapter", "Total students: ${students.size}")
        return students.size
    }
}
*/
/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */