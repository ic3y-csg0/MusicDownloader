    package com.iseenoev1l.ytdownloader

    import android.app.DownloadManager
    import android.content.BroadcastReceiver
    import android.content.Context
    import android.content.Intent
    import android.net.Uri
    import android.os.Bundle
    import android.os.Environment
    import androidx.activity.ComponentActivity
    import com.google.gson.Gson
    import com.google.gson.annotations.SerializedName
    import okhttp3.OkHttpClient
    import okhttp3.Request
    import java.net.URLEncoder
    import java.util.concurrent.TimeUnit

    class MainActivity : ComponentActivity() {

        private var downloadManager: DownloadManager? = null
        private var downloadId: Long = -1
        private var downloadReceiver: BroadcastReceiver? = null
        private val apiKeys = listOf("fb4dcf9499mshbfeaf40709d89f9p1a9765jsn38aa05906333", "bed2d871b0mshcfa152644ed2bcep179f87jsn7819ced50c4a", "a3da9eb053msh8dca81dd1fa3ea6p1af12cjsn01c6d17e89dd")
        private var currentApiKeyIndex = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
runOnUiThread { finish() }
            val intent = intent

            if (Intent.ACTION_SEND == intent.action && intent.type != null && intent.type!!.startsWith("text/")) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

                sharedText?.let {
                    executeHttpRequestInBackground(it)
                }

            }
        }

        fun downloadMusicFileToFolder(context: Context, fileUrl: String, fileName: String) {
            downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(fileUrl)
            val request = DownloadManager.Request(uri)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, fileName)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            downloadId = downloadManager?.enqueue(request) ?: -1

            // Больше нет вызова setupDownloadReceiver()
        }

        private fun executeHttpRequestInBackground(sharedText: String) {
            val modifiedUrl = sharedText.replace("https://", "https://www.")
            val shortedSharedText = modifiedUrl.substringBefore("&si")
            val encodedSharedText = URLEncoder.encode(shortedSharedText, "UTF-8")
            val requestUrl =
                "https://youtube-mp3-downloader2.p.rapidapi.com/ytmp3/ytmp3/custom/?url=$encodedSharedText&quality=320"

            Thread {
                try {
                    val client = OkHttpClient.Builder()
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                    val request = Request.Builder()
                        .url(requestUrl)
                        .get()
                        .addHeader("X-RapidAPI-Key", apiKeys[currentApiKeyIndex])
                        .addHeader("X-RapidAPI-Host", "youtube-mp3-downloader2.p.rapidapi.com")
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: "Empty response body"
                        val yourResponse: YourResponse = Gson().fromJson(responseBody, YourResponse::class.java)
                        downloadMusicFileToFolder(this, yourResponse.link, "${yourResponse.title}.mp3")

                        // Завершаем активность после успешной загрузки
                        finish()
                    } else {
                        if (response.code == 429) {
                            switchToNextApiKey()
                            executeHttpRequestInBackground(sharedText)
                        } else {
                            println("Request failed with code ${response.code}")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        private fun switchToNextApiKey() {
            currentApiKeyIndex = (currentApiKeyIndex + 1) % apiKeys.size
        }

        override fun onDestroy() {
            super.onDestroy()
            // Отменяем регистрацию BroadcastReceiver при уничтожении активности
            downloadReceiver?.let { unregisterReceiver(it) }
        }
    }

    data class YourResponse(
        @SerializedName("title") val title: String,
        @SerializedName("link") val link: String,
        @SerializedName("length") val length: String,
        @SerializedName("size") val size: String
    )
