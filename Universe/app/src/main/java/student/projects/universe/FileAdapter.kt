package student.projects.universe

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(private val files: List<ClassFileResponse>) : // (Patel, 2025)
    RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvFileType: TextView = itemView.findViewById(R.id.tvFileType)
        val tvUploadDate: TextView = itemView.findViewById(R.id.tvUploadDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder { // (Patel, 2025)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) { // (Patel, 2025)
        val file = files[position]
        holder.tvFileName.text = file.fileName
        holder.tvFileType.text = "Type: ${file.fileType}"
        holder.tvUploadDate.text = "Uploaded: ${file.uploadDate}"

        holder.itemView.setOnClickListener { // (Patel, 2025)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(file.filePath), file.fileType)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = files.size
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */