package com.fredapp.gdriveshow.drive

interface DriveRepository {
    fun connectionState(): DriveConnectionState
    fun content(folderId: String = RootFolderId): DriveContentState
    fun slideshowCandidates(items: List<DriveItem>): List<DriveItem>

    companion object {
        const val RootFolderId = "root"
    }
}
