package com.fredapps.gdriveshow.drive

interface DriveRepository {
    fun rootItems(): List<DriveItem>
    fun slideshowCandidates(): List<DriveItem>
}

