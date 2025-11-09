package student.projects.universe

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Custom ArrayAdapter for displaying a simple menu with string items.
 *
 * @param context The context in which the adapter is used
 * @param items The list of menu item strings
 */
class MenuAdapter(context: Context, private val items: List<String>) : // (Patel, 2025)
    ArrayAdapter<String>(context, 0, items) {

    /**
     * Returns a view for each menu item in the ListView or GridView.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Reuse existing view if available, otherwise inflate a new one // (Patel, 2025)
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_menu, parent, false)

        // Bind TextView and set the title for the current menu item
        val tvTitle = view.findViewById<TextView>(R.id.tvMenuTitle)
        tvTitle.text = items[position]

        // Log the menu item being displayed for debugging // (Patel, 2025)
        Log.d("MenuAdapter", "Displaying menu item at position $position: ${items[position]}")

        return view
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */
