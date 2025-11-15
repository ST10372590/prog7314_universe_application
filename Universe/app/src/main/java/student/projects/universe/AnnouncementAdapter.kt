package student.projects.universe

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class AnnouncementAdapter(
    private val context: Context,
    private val announcements: List<AnnouncementResponse>
) : BaseAdapter() {

    override fun getCount(): Int = announcements.size
    override fun getItem(position: Int): Any = announcements[position]
    override fun getItemId(position: Int): Long = announcements[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.announcement_item, parent, false)

        val title = view.findViewById<TextView>(R.id.tvAnnouncementTitle)
        val content = view.findViewById<TextView>(R.id.tvAnnouncementContent)
        val date = view.findViewById<TextView>(R.id.tvAnnouncementDate)
        val module = view.findViewById<TextView>(R.id.tvAnnouncementModule)

        val announcement = announcements[position]
        title.text = announcement.title
        content.text = announcement.content
        date.text = announcement.date.take(10) // e.g., "2025-11-12"
        module.text = context.getString(R.string.label_module, announcement.moduleTitle ?: "N/A")

        return view
    }
}
