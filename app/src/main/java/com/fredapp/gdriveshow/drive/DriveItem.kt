package com.fredapp.gdriveshow.drive

enum class DriveMediaType {
    Folder,
    Image,
    Video,
}

data class DriveItem(
    val id: String,
    val title: String,
    val description: String,
    val type: DriveMediaType,
    val itemCount: Int? = null,
    val durationLabel: String? = null,
    val modifiedLabel: String,
    val accentColor: Long,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
)

val DriveMediaType.label: String
    get() = when (this) {
        DriveMediaType.Folder -> "Folder"
        DriveMediaType.Image -> "Image"
        DriveMediaType.Video -> "Video"
    }

val DriveItem.subtitle: String
    get() = when {
        itemCount != null -> "$itemCount items"
        durationLabel != null -> durationLabel
        else -> modifiedLabel
    }

val DriveItem.isPlayable: Boolean
    get() = type == DriveMediaType.Image || type == DriveMediaType.Video
