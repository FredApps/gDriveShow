package com.fredapps.gdriveshow.drive

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DriveMetadataCache(context: Context) {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    fun read(folderId: String): List<DriveItem> {
        val raw = preferences.getString(folderId.cacheKey(), null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    add(json.getJSONObject(index).toDriveItem())
                }
            }
        }.getOrDefault(emptyList())
    }

    fun write(folderId: String, items: List<DriveItem>) {
        val json = JSONArray()
        items.forEach { item -> json.put(item.toJson()) }
        preferences.edit()
            .putString(folderId.cacheKey(), json.toString())
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun DriveItem.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("title", title)
            .put("description", description)
            .put("type", type.name)
            .put("itemCount", itemCount)
            .put("durationLabel", durationLabel)
            .put("modifiedLabel", modifiedLabel)
            .put("accentColor", accentColor)
            .put("mediaUrl", mediaUrl)
            .put("thumbnailUrl", thumbnailUrl)

    private fun JSONObject.toDriveItem(): DriveItem {
        val type = runCatching { DriveMediaType.valueOf(getString("type")) }
            .getOrDefault(DriveMediaType.Folder)
        return DriveItem(
            id = getString("id"),
            title = getString("title"),
            description = getString("description"),
            type = type,
            itemCount = optNullableInt("itemCount"),
            durationLabel = optNullableString("durationLabel"),
            modifiedLabel = optString("modifiedLabel", "Modified date unknown"),
            accentColor = optLong("accentColor", type.fallbackAccentColor),
            mediaUrl = optNullableString("mediaUrl"),
            thumbnailUrl = optNullableString("thumbnailUrl"),
        )
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (isNull(name)) null else optInt(name)

    private fun String.cacheKey(): String = "folder:$this"

    private companion object {
        const val PreferencesName = "gdrive_show_metadata_cache"
    }
}

private val DriveMediaType.fallbackAccentColor: Long
    get() = when (this) {
        DriveMediaType.Folder -> 0xFF72D6C9
        DriveMediaType.Image -> 0xFFFFD166
        DriveMediaType.Video -> 0xFFFF8A65
    }
