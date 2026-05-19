package com.fredapps.gdriveshow.drive

interface DriveRepository {
    fun connectionState(): DriveConnectionState
    fun rootContent(): DriveContentState
    fun slideshowCandidates(items: List<DriveItem>): List<DriveItem>
}
