package com.example.reggelirutin

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Calendar

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val browser_download_url: String,
    val name: String
)

sealed class UpdateResult {
    data class NewVersionAvailable(val version: String, val downloadUrl: String) : UpdateResult()
    object NoUpdate : UpdateResult()
    object Error : UpdateResult()
}

class UpdateManager(private val context: Context) {
    private val client = OkHttpClient()
    private val gson = Gson()

    private val LAST_CHECK_KEY = longPreferencesKey("last_update_check")
    private val AUTO_CHECK_ENABLED = booleanPreferencesKey("auto_update_enabled")
    private val LAST_APP_VERSION = longPreferencesKey("last_app_version")
    private val LAST_SHOWCASE_VERSION = longPreferencesKey("last_showcase_version")

    val lastAppVersion: Flow<Long> = context.dataStore.data
        .map { it[LAST_APP_VERSION] ?: 0L }

    val lastShowcaseVersion: Flow<Long> = context.dataStore.data
        .map { it[LAST_SHOWCASE_VERSION] ?: 0L }

    suspend fun updateLastShowcaseVersion(version: Long) {
        context.dataStore.edit { it[LAST_SHOWCASE_VERSION] = version }
    }

    suspend fun updateLastAppVersion(version: Long) {
        context.dataStore.edit { it[LAST_APP_VERSION] = version }
    }

    val isAutoCheckEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_CHECK_ENABLED] ?: true }

    suspend fun setAutoCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CHECK_ENABLED] = enabled
        }
    }

    private suspend fun shouldCheckForUpdate(): Boolean {
        if (!isAutoCheckEnabled.first()) return false
        
        val lastCheck = context.dataStore.data.map { it[LAST_CHECK_KEY] ?: 0L }.first()
        val now = System.currentTimeMillis()
        
        // Check once per day (24 hours)
        return now - lastCheck > 24 * 60 * 60 * 1000
    }

    suspend fun checkForUpdate(currentVersion: String, force: Boolean = false): UpdateResult = withContext(Dispatchers.IO) {
        if (!force && !shouldCheckForUpdate()) return@withContext UpdateResult.NoUpdate
        if (!isNetworkAvailable()) return@withContext UpdateResult.Error

        try {
            val request = Request.Builder().url(AppConfig.GITHUB_API_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext UpdateResult.Error
                
                val release = gson.fromJson(response.body?.string(), GitHubRelease::class.java)
                val latestVersion = release.tag_name.removePrefix("v")
                val current = currentVersion.removePrefix("v")

                // Update last check time
                context.dataStore.edit { it[LAST_CHECK_KEY] = System.currentTimeMillis() }

                if (isNewerVersion(current, latestVersion)) {
                    val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                    val downloadUrl = apkAsset?.browser_download_url ?: release.html_url
                    UpdateResult.NewVersionAvailable(latestVersion, downloadUrl)
                } else {
                    UpdateResult.NoUpdate
                }
            }
        } catch (e: Exception) {
            UpdateResult.Error
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        
        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val c = if (i < currentParts.size) currentParts[i] else 0
            val l = if (i < latestParts.size) latestParts[i] else 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun downloadAndInstall(url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(context.getString(R.string.app_name))
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, AppConfig.APK_NAME)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    installApk(context)
                    context.unregisterReceiver(this)
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), AppConfig.APK_NAME)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        }
    }
}
