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
    private val metadataCache: DriveMetadataCache? = null,
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
                    url = FilesUrl,
                    accessToken = accessToken,
                    values = buildMap {
                        put("pageSize", "100")
                        put("q", "'$folderId' in parents and trashed = false")
                        put("orderBy", "folder,modifiedTime desc,name")
                        put("includeItemsFromAllDrives", "true")
                        put("supportsAllDrives", "true")
                        put("fields", FileListFields)
                        pageToken?.let { put("pageToken", it) }
                    },
                )

                if (!response.isSuccess) {
                    return cachedOrFailed(folderId, response.errorMessage())
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

            if (folderId == DriveRepository.RootFolderId) {
                files += listSharedDrives(accessToken)
                    .filterNot { sharedDrive -> files.any { it.id == sharedDrive.id } }
            }

            metadataCache?.write(folderId, files)
            if (files.isEmpty()) DriveContentState.Empty else DriveContentState.Ready(files)
        } catch (exception: IOException) {
            cachedOrFailed(folderId, "Could not reach Google Drive: ${exception.message}")
        } catch (exception: Exception) {
            cachedOrFailed(folderId, "Could not list Google Drive files: ${exception.message}")
        }
    }

    private fun listSharedDrives(accessToken: String): List<DriveItem> {
        val drives = mutableListOf<DriveItem>()
        var pageToken: String? = null

        do {
            val response = get(
                url = DrivesUrl,
                accessToken = accessToken,
                values = buildMap {
                    put("pageSize", "100")
                    put("fields", "nextPageToken,drives(id,name)")
                    pageToken?.let { put("pageToken", it) }
                },
            )
            if (!response.isSuccess) return drives

            val json = JSONObject(response.body)
            val jsonDrives = json.optJSONArray("drives") ?: return drives
            for (index in 0 until jsonDrives.length()) {
                val drive = jsonDrives.getJSONObject(index)
                drives += DriveItem(
                    id = drive.getString("id"),
                    title = drive.getString("name"),
                    description = "Shared Drive available to this Google account.",
                    type = DriveMediaType.Folder,
                    modifiedLabel = "Shared Drive",
                    accentColor = 0xFFFFCC80,
                )
            }
            pageToken = json.optString("nextPageToken").ifBlank { null }
        } while (pageToken != null)

        return drives
    }

    private fun cachedOrFailed(folderId: String, message: String): DriveContentState {
        val cached = metadataCache?.read(folderId).orEmpty()
        return if (cached.isNotEmpty()) {
            DriveContentState.Ready(
                items = cached,
                isStale = true,
                sourceMessage = "$message Showing cached folder metadata.",
            )
        } else {
            DriveContentState.Failed(message)
        }
    }

    private fun get(url: String, accessToken: String, values: Map<String, String>): DriveHttpResponse {
        val query = values.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
        val connection = (URL("$url?$query").openConnection() as HttpURLConnection).apply {
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
            thumbnailUrl = optString("thumbnailLink").ifBlank { null },
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
        const val DrivesUrl = "https://www.googleapis.com/drive/v3/drives"
        const val FileListFields =
            "nextPageToken,files(id,name,mimeType,modifiedTime,description,thumbnailLink,videoMediaMetadata)"
        const val TimeoutMillis = 15_000
    }
}
