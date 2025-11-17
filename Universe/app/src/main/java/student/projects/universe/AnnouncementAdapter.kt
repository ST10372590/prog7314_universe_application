package student.projects.universe

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class AnnouncementAdapter(
    private val context: Context,
    private val announcements: List<AnnouncementResponse>
) : BaseAdapter() {

    // Returns the total number of announcements
    override fun getCount(): Int = announcements.size
    override fun getItem(position: Int): Any = announcements[position]
    override fun getItemId(position: Int): Long = announcements[position].id.toLong()

    // Returns the View for the announcement at the specified position
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.announcement_item, parent, false)

        // Find the TextViews inside the list item layout
        val title = view.findViewById<TextView>(R.id.tvAnnouncementTitle)
        val content = view.findViewById<TextView>(R.id.tvAnnouncementContent)
        val date = view.findViewById<TextView>(R.id.tvAnnouncementDate)
        val module = view.findViewById<TextView>(R.id.tvAnnouncementModule)

        // Get the announcement data for the current position
        val announcement = announcements[position]

        // Bind data to the TextViews
        title.text = announcement.title
        content.text = announcement.content
        date.text = announcement.date.take(10)
        module.text = context.getString(R.string.label_module, announcement.moduleTitle ?: "N/A")

        // Log the binding operation for debugging
        Log.d("AnnouncementAdapter", "Bound announcement at position $position: ${announcement.title}")

        return view
    }
}
