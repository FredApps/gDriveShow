package com.fredapps.gdriveshow.drive

import com.fredapps.gdriveshow.drive.auth.AccessTokenResult
import com.fredapps.gdriveshow.drive.auth.DriveAccessTokenProvider
import com.fredapps.gdriveshow.drive.auth.DriveTokenStore
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class GoogleDriveRepository(
    private val tokenStore: DriveTokenStore,
    private val accessTokenProvider: DriveAccessTokenProvider,
) : DriveRepository {
    override fun connectionState(): DriveConnectionState {
        return if (tokenStore.read() == null) {
            DriveConnectionState.Disconnected
        } else {
            DriveConnectionState.Connected("Google Drive connected")
        }
    }

    override fun content(folderId: String): DriveContentState {
        return when (val tokenResult = accessTokenProvider.accessToken()) {
            AccessTokenResult.MissingTokens -> DriveContentState.Empty
            is AccessTokenResult.Failed -> DriveContentState.Failed(tokenResult.message)
            is AccessTokenResult.Ready -> listFiles(accessToken = tokenResult.accessToken, folderId = folderId)
        }
    }

    override fun slideshowCandidates(items: List<DriveItem>): List<DriveItem> = items.filter { it.isPlayable }

    private fun listFiles(accessToken: String, folderId: String): DriveContentState {
        return try {
            val files = mutableListOf<DriveItem>()
            var pageToken: String? = null

            do {
                val response = get(
                    accessToken = accessToken,
                    values = buildMap {
                        put("pageSize", "100")
                        put("q", "'$folderId' in parents and trashed = false")
                        put("orderBy", "folder,modifiedTime desc,name")
                        put("fields", "nextPageToken,files(id,name,mimeType,modifiedTime,description,videoMediaMetadata)")
                        pageToken?.let { put("pageToken", it) }
                    },
                )

                if (!response.isSuccess) {
                    return DriveContentState.Failed(response.errorMessage())
                }

                val json = JSONObject(response.body)
                val jsonFiles = json.optJSONArray("files")
                if (jsonFiles != null) {
                    for (index in 0 until jsonFiles.length()) {
                        val item = jsonFiles.getJSONObject(index).toDriveFileMetadata()
                        if (item.mimeType.isSupportedDriveMedia()) {
                            files += item.toDriveItem()
                        }
                    }
                }
                pageToken = json.optString("nextPageToken").ifBlank { null }
            } while (pageToken != null)

            if (files.isEmpty()) DriveContentState.Empty else DriveContentState.Ready(files)
        } catch (exception: IOException) {
            DriveContentState.Failed("Could not reach Google Drive: ${exception.message}")
        } catch (exception: Exception) {
            DriveContentState.Failed("Could not list Google Drive files: ${exception.message}")
        }
    }

    private fun get(accessToken: String, values: Map<String, String>): DriveHttpResponse {
        val query = values.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
        val connection = (URL("$FilesUrl?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TimeoutMillis
            readTimeout = TimeoutMillis
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            DriveHttpResponse(
                code = code,
                body = stream?.bufferedReader()?.use { it.readText() }.orEmpty(),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toDriveFileMetadata(): DriveFileMetadata {
        val mimeType = getString("mimeType")
        val durationLabel = optJSONObject("videoMediaMetadata")
            ?.optString("durationMillis")
            ?.takeIf { it.isNotBlank() }
            ?.toLongOrNull()
            ?.toDurationLabel()

        return DriveFileMetadata(
            id = getString("id"),
            name = getString("name"),
            mimeType = mimeType,
            modifiedLabel = optString("modifiedTime").toModifiedLabel(),
            description = optString("description").ifBlank { null },
            durationLabel = durationLabel,
            mediaUrl = if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
                "$FilesUrl/${getString("id")}?alt=media"
            } else {
                null
            },
        )
    }

    private fun String.isSupportedDriveMedia(): Boolean =
        this == GoogleDriveMimeTypes.Folder || startsWith("image/") || startsWith("video/")

    private fun String.toModifiedLabel(): String {
        if (isBlank()) return "Modified date unknown"
        return try {
            "Updated ${OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
        } catch (_: DateTimeParseException) {
            "Updated $this"
        }
    }

    private fun Long.toDurationLabel(): String {
        val totalSeconds = this / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "${minutes}:${seconds.toString().padStart(2, '0')}"
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private data class DriveHttpResponse(
        val code: Int,
        val body: String,
    ) {
        val isSuccess: Boolean = code in 200..299

        fun errorMessage(): String {
            val json = runCatching { JSONObject(body) }.getOrNull()
            val error = json?.optJSONObject("error")
            val message = error?.optString("message")?.takeIf { it.isNotBlank() }
            return message ?: "Google Drive returned HTTP $code."
        }
    }

    private companion object {
        const val FilesUrl = "https://www.googleapis.com/drive/v3/files"
        const val TimeoutMillis = 15_000
    }
}
