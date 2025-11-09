package student.projects.universe

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:5086/"

    // Token storage
    private var token: String? = null
    var currentUserId: Int = 0 // Store user ID after login

    /** Sets the JWT token for future requests */
    fun setToken(newToken: String) {
        token = newToken
    }

    /** Returns current JWT token */
    fun getToken(): String? = token

    /** Clears the token and user ID on logout */
    fun clearToken() {
        token = null
        currentUserId = 0
    }

    // Interceptor to add Authorization header
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder: Request.Builder = originalRequest.newBuilder()

        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }


    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor) // Uncomment for logging
        .build()

   // (Aram, 2023)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // API interfaces (Aram, 2023)
    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val userApi: UserApi by lazy { retrofit.create(UserApi::class.java) }
    val courseApi: CourseApi by lazy { retrofit.create(CourseApi::class.java) }
    val moduleApi: ModuleApi by lazy { retrofit.create(ModuleApi::class.java) }

    val assessmentApi: AssessmentApi by lazy { retrofit.create(AssessmentApi::class.java) }
    val studentApi: StudentApi by lazy { retrofit.create(StudentApi::class.java) }
    val lecturerApi: LecturerApi by lazy { retrofit.create(LecturerApi::class.java) }
    val announcementApi: AnnouncementApi = retrofit.create(AnnouncementApi::class.java)
    val fileApi: FileApi by lazy { retrofit.create(FileApi::class.java) }
    val notificationApi: NotificationApi by lazy { retrofit.create(NotificationApi::class.java) }
    // (Aram, 2023)
    val settingsApi: SettingsApi by lazy { retrofit.create(SettingsApi::class.java) }
    val gamificationApi: GamificationApi = retrofit.create(GamificationApi::class.java)

    // Messages
    val messageApi: MessageApi by lazy { retrofit.create(MessageApi::class.java) } // (Patel, 2025)

}



/*

Reference List

Aram. 2023. Refit – The Retrofit of .NET, 27 September 2023, Codingsonata.
[Blog]. Available at:  https://codingsonata.com/refit-the-retrofit-of-net/ [Accessed 22 October 2025]

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */