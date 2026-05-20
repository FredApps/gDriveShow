package com.fredapp.gdriveshow.drive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest

class DriveThumbnailCache(context: Context) {
    private val cacheDirectory = File(context.cacheDir, DirectoryName).apply { mkdirs() }

    fun read(item: DriveItem): Bitmap? {
        val key = item.thumbnailUrl ?: return null
        val file = File(cacheDirectory, key.cacheFileName())
        if (!file.isFile) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun write(item: DriveItem, bitmap: Bitmap) {
        val key = item.thumbnailUrl ?: return
        val file = File(cacheDirectory, key.cacheFileName())
        runCatching {
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }

    fun clear() {
        cacheDirectory.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }
    }

    private fun String.cacheFileName(): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return "$digest.png"
    }

    private companion object {
        const val DirectoryName = "drive_thumbnails"
    }
}
