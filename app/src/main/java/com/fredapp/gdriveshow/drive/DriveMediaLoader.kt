package com.fredapp.gdriveshow.drive

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.fredapp.gdriveshow.drive.auth.AccessTokenResult
import com.fredapp.gdriveshow.drive.auth.DriveAccessTokenProvider
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class DriveMediaLoader(
    private val accessTokenProvider: DriveAccessTokenProvider,
    private val thumbnailCache: DriveThumbnailCache? = null,
) {
    fun loadImage(item: DriveItem): ImageLoadResult {
        val mediaUrl = item.mediaUrl ?: return ImageLoadResult.Unavailable
        return loadBitmap(mediaUrl)
    }

    fun loadThumbnail(item: DriveItem): ImageLoadResult {
        val thumbnailUrl = item.thumbnailUrl ?: return ImageLoadResult.Unavailable
        thumbnailCache?.read(item)?.let { return ImageLoadResult.Ready(it) }
        val result = loadBitmap(thumbnailUrl)
        if (result is ImageLoadResult.Ready) {
            thumbnailCache?.write(item, result.bitmap)
        }
        return result
    }

    private fun loadBitmap(url: String): ImageLoadResult {
        val accessToken = when (val token = accessTokenProvider.accessToken()) {
            is AccessTokenResult.Ready -> token.accessToken
            AccessTokenResult.MissingTokens -> return ImageLoadResult.Failed("Google Drive is not connected.")
            is AccessTokenResult.Failed -> return ImageLoadResult.Failed(token.message)
        }

        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TimeoutMillis
                readTimeout = TimeoutMillis
                setRequestProperty("Authorization", "Bearer $accessToken")
            }

            try {
                if (connection.responseCode !in 200..299) {
                    return ImageLoadResult.Failed("Google Drive returned HTTP ${connection.responseCode}.")
                }
                val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                    ?: return ImageLoadResult.Failed("Could not decode image.")
                ImageLoadResult.Ready(bitmap)
            } finally {
                connection.disconnect()
            }
        } catch (exception: IOException) {
            ImageLoadResult.Failed("Could not load image: ${exception.message}")
        }
    }

    fun videoRequest(item: DriveItem): VideoRequest? {
        val mediaUrl = item.mediaUrl ?: return null
        val accessToken = (accessTokenProvider.accessToken() as? AccessTokenResult.Ready)?.accessToken ?: return null
        return VideoRequest(url = mediaUrl, accessToken = accessToken)
    }

    private companion object {
        const val TimeoutMillis = 20_000
    }
}

sealed interface ImageLoadResult {
    data class Ready(val bitmap: Bitmap) : ImageLoadResult
    data object Unavailable : ImageLoadResult
    data class Failed(val message: String) : ImageLoadResult
}

data class VideoRequest(
    val url: String,
    val accessToken: String,
)
