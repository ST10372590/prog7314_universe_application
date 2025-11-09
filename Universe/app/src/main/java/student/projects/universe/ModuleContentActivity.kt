package student.projects.universe

import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity to display module content such as PDF, video, or web links // (Patel, 2025)
 */
class ModuleContentActivity : AppCompatActivity() {

    private lateinit var tvModuleTitle: TextView
    private lateinit var tvContentType: TextView
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module_content)
        Log.d("ModuleContentActivity", "ModuleContentActivity started") // (Patel, 2025)

        // Bind views // (Patel, 2025)
        tvModuleTitle = findViewById(R.id.tvModuleTitle)
        tvContentType = findViewById(R.id.tvContentType)
        webView = findViewById(R.id.webViewContent)
        Log.d("ModuleContentActivity", "Views initialized: tvModuleTitle, tvContentType, webView")

        // Retrieve module data from intent // (Patel, 2025)
        val moduleTitle = intent.getStringExtra("MODULE_TITLE")
        val contentType = intent.getStringExtra("CONTENT_TYPE")
        val contentLink = intent.getStringExtra("CONTENT_LINK")

        // Set module title and content type
        tvModuleTitle.text = moduleTitle ?: "Module Content"
        tvContentType.text = "Type: ${contentType ?: "Unknown"}"
        Log.d("ModuleContentActivity", "Module Title: ${tvModuleTitle.text}, Content Type: ${tvContentType.text}")

        // Load content if link is available // (Patel, 2025)
        if (!contentLink.isNullOrBlank()) {
            Log.d("ModuleContentActivity", "Content link available: $contentLink")

            webView.webViewClient = WebViewClient()
            val webSettings: WebSettings = webView.settings
            webSettings.javaScriptEnabled = true
            Log.d("ModuleContentActivity", "WebView settings initialized with JavaScript enabled")

            when (contentType?.lowercase()) {
                "pdf" -> {
                    // Open PDF via Google Docs Viewer // (Patel, 2025)
                    Log.d("ModuleContentActivity", "Loading PDF content via Google Docs Viewer")
                    webView.loadUrl("https://docs.google.com/gview?embedded=true&url=$contentLink")
                }
                "video" -> {
                    // Open video link directly
                    Log.d("ModuleContentActivity", "Loading video content")
                    webView.loadUrl(contentLink)
                }
                "link", "url" -> {
                    // Open any generic link // (Patel, 2025)
                    Log.d("ModuleContentActivity", "Loading generic web link")
                    webView.loadUrl(contentLink)
                }
                else -> {
                    Log.w("ModuleContentActivity", "Unknown content type: $contentType")
                    Toast.makeText(this, "Unknown content type", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.w("ModuleContentActivity", "No content link provided for this module") // (Patel, 2025)
            Toast.makeText(this, "No content available for this module", Toast.LENGTH_SHORT).show()
        }
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */