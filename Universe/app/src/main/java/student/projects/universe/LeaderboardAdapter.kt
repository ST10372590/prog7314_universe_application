package student.projects.universe

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(private val items: List<LeaderboardEntryResponse>) : // (Patel, 2025)
    RecyclerView.Adapter<LeaderboardAdapter.VH>() {

    // ViewHolder class to hold references to each leaderboard item's UI elements
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvLbName)
        val tvPoints: TextView = v.findViewById(R.id.tvLbPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH { // (Patel, 2025)
        // Inflate the layout for each leaderboard entry
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)

        Log.d("LeaderboardAdapter", "Created new ViewHolder for leaderboard item")
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        // Get the leaderboard entry for the given position // (Patel, 2025)
        val it = items[position]

        // Set user name and position
        holder.tvName.text = "${position + 1}. ${it.username}"

        // Display the points with a suffix // (Patel, 2025)
        holder.tvPoints.text = "${it.points} pts"

        Log.d(
            "LeaderboardAdapter",
            "Binding leaderboard entry #${position + 1} | Username: ${it.username}, Points: ${it.points}"
        )
    }

    override fun getItemCount(): Int {
        // Return total leaderboard entries and log count // (Patel, 2025)
        Log.d("LeaderboardAdapter", "Total leaderboard entries: ${items.size}")
        return items.size
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */