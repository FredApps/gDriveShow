package com.fredapp.gdriveshow.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveFileMapperTest {
    @Test
    fun `maps image metadata with thumbnail and media URL`() {
        val item = DriveFileMetadata(
            id = "image-1",
            name = "Kitchen Display.jpg",
            mimeType = "image/jpeg",
            modifiedLabel = "Updated May 19, 2026",
            mediaUrl = "https://example.test/image",
            thumbnailUrl = "https://example.test/thumb",
        ).toDriveItem()

        assertEquals(DriveMediaType.Image, item.type)
        assertEquals("Kitchen Display.jpg", item.title)
        assertEquals("Google Drive image.", item.description)
        assertEquals("https://example.test/image", item.mediaUrl)
        assertEquals("https://example.test/thumb", item.thumbnailUrl)
        assertTrue(item.isPlayable)
    }

    @Test
    fun `maps folder metadata as non playable folder`() {
        val item = DriveFileMetadata(
            id = "folder-1",
            name = "Lobby",
            mimeType = GoogleDriveMimeTypes.Folder,
            modifiedLabel = "Updated May 19, 2026",
            itemCount = 12,
        ).toDriveItem()

        assertEquals(DriveMediaType.Folder, item.type)
        assertEquals("12 items", item.subtitle)
        assertEquals("Google Drive folder.", item.description)
        assertEquals(null, item.mediaUrl)
        assertEquals(false, item.isPlayable)
    }
}
