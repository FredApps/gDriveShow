package com.fredapps.gdriveshow.drive

class SampleDriveRepository : DriveRepository {
    private val items = listOf(
        DriveItem(
            id = "family",
            title = "Family Photos",
            description = "Pinned folder for living-room photo browsing.",
            type = DriveMediaType.Folder,
            itemCount = 42,
            modifiedLabel = "Updated today",
            accentColor = 0xFF72D6C9,
        ),
        DriveItem(
            id = "summer-trip",
            title = "Summer Trip",
            description = "A bright image set ready for slideshow playback.",
            type = DriveMediaType.Image,
            itemCount = 18,
            modifiedLabel = "Updated yesterday",
            accentColor = 0xFFFFD166,
        ),
        DriveItem(
            id = "cabin-weekend",
            title = "Cabin Weekend",
            description = "Short clips mixed with stills from a shared Drive folder.",
            type = DriveMediaType.Video,
            durationLabel = "12 videos",
            modifiedLabel = "Updated May 12",
            accentColor = 0xFFFF8A65,
        ),
        DriveItem(
            id = "archive",
            title = "Receipts Archive",
            description = "A non-media folder shown to validate filtering behavior.",
            type = DriveMediaType.Folder,
            itemCount = 116,
            modifiedLabel = "Updated Apr 29",
            accentColor = 0xFF9FA8DA,
        ),
        DriveItem(
            id = "living-room",
            title = "Living Room Loop",
            description = "The default ambient loop for an Android TV display.",
            type = DriveMediaType.Image,
            itemCount = 64,
            modifiedLabel = "Updated May 18",
            accentColor = 0xFFA5D6A7,
        ),
        DriveItem(
            id = "drone",
            title = "Drone Clips",
            description = "High-resolution video collection for playback testing.",
            type = DriveMediaType.Video,
            durationLabel = "4K video",
            modifiedLabel = "Updated May 9",
            accentColor = 0xFF90CAF9,
        ),
        DriveItem(
            id = "favorites",
            title = "Favorites",
            description = "Curated images that should be one click away.",
            type = DriveMediaType.Image,
            itemCount = 23,
            modifiedLabel = "Updated May 16",
            accentColor = 0xFFF48FB1,
        ),
        DriveItem(
            id = "shared-drive",
            title = "Office Display",
            description = "Placeholder for later shared-drive support.",
            type = DriveMediaType.Folder,
            itemCount = 9,
            modifiedLabel = "Updated May 2",
            accentColor = 0xFFFFCC80,
        ),
    )

    override fun connectionState(): DriveConnectionState = DriveConnectionState.Connected("Sample Drive data")

    override fun rootContent(): DriveContentState = DriveContentState.Ready(items)

    override fun slideshowCandidates(items: List<DriveItem>): List<DriveItem> = items.filter { it.isPlayable }
}
