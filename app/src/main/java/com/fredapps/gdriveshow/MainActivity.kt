package com.fredapps.gdriveshow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fredapps.gdriveshow.drive.DriveItem
import com.fredapps.gdriveshow.drive.DriveConnectionState
import com.fredapps.gdriveshow.drive.DriveContentState
import com.fredapps.gdriveshow.drive.DriveMediaType
import com.fredapps.gdriveshow.drive.SampleDriveRepository
import com.fredapps.gdriveshow.drive.isPlayable
import com.fredapps.gdriveshow.drive.label
import com.fredapps.gdriveshow.drive.statusLabel
import com.fredapps.gdriveshow.drive.subtitle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GDriveShowApp()
        }
    }
}

private enum class AppSection(val label: String) {
    Drive("Drive"),
    Slideshows("Slideshows"),
    Settings("Settings"),
}

private enum class MediaFilter(val label: String) {
    All("All"),
    Folders("Folders"),
    Images("Images"),
    Videos("Videos"),
}

private enum class SortMode(val label: String) {
    Recent("Recent"),
    Name("Name"),
}

@Composable
private fun GDriveShowApp(repository: SampleDriveRepository = SampleDriveRepository()) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color(0xFF101214),
            surface = Color(0xFF191D21),
            primary = Color(0xFF72D6C9),
            onPrimary = Color(0xFF101214),
            onSurface = Color.White,
        ),
    ) {
        val connectionState = remember { repository.connectionState() }
        val contentState = remember { repository.rootContent() }
        val driveItems = (contentState as? DriveContentState.Ready)?.items.orEmpty()
        val slideshowItems = remember(contentState) { repository.slideshowCandidates(driveItems) }
        var section by remember { mutableStateOf(AppSection.Drive) }
        var filter by remember { mutableStateOf(MediaFilter.All) }
        var sortMode by remember { mutableStateOf(SortMode.Recent) }
        var selectedItemId by remember { mutableStateOf(driveItems.firstOrNull()?.id) }
        var slideshowIndex by remember { mutableIntStateOf(0) }
        var showingSlideshow by remember { mutableStateOf(false) }

        val visibleItems = remember(filter, sortMode, driveItems) {
            driveItems
                .filter { item ->
                    when (filter) {
                        MediaFilter.All -> true
                        MediaFilter.Folders -> item.type == DriveMediaType.Folder
                        MediaFilter.Images -> item.type == DriveMediaType.Image
                        MediaFilter.Videos -> item.type == DriveMediaType.Video
                    }
                }
                .let { filtered ->
                    when (sortMode) {
                        SortMode.Recent -> filtered
                        SortMode.Name -> filtered.sortedBy { it.title }
                    }
                }
        }
        val selectedItem = visibleItems.firstOrNull { it.id == selectedItemId }
            ?: driveItems.firstOrNull()

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            if (showingSlideshow) {
                SlideshowScreen(
                    items = slideshowItems,
                    currentIndex = slideshowIndex,
                    onPrevious = { slideshowIndex = (slideshowIndex - 1).floorMod(slideshowItems.size) },
                    onNext = { slideshowIndex = (slideshowIndex + 1).floorMod(slideshowItems.size) },
                    onBack = { showingSlideshow = false },
                )
            } else {
                AppShell(
                    section = section,
                    onSectionSelected = { section = it },
                    driveStatus = connectionState.statusLabel,
                ) {
                    when (section) {
                        AppSection.Drive -> DriveContentScreen(
                            contentState = contentState,
                            selectedItem = selectedItem,
                            visibleItems = visibleItems,
                            filter = filter,
                            sortMode = sortMode,
                            onFilterChanged = { filter = it },
                            onSortChanged = { sortMode = it },
                            onItemSelected = { selectedItemId = it.id },
                            onStartSlideshow = {
                                selectedItem?.let { current ->
                                    val index = slideshowItems.indexOfFirst { it.id == current.id }
                                    slideshowIndex = if (index >= 0) index else 0
                                    showingSlideshow = slideshowItems.isNotEmpty()
                                }
                            },
                        )

                        AppSection.Slideshows -> SlideshowLibraryScreen(
                            items = slideshowItems,
                            onStart = { item ->
                                slideshowIndex = slideshowItems.indexOf(item).coerceAtLeast(0)
                                showingSlideshow = true
                            },
                        )

                        AppSection.Settings -> SettingsScreen(connectionState = connectionState)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppShell(
    section: AppSection,
    onSectionSelected: (AppSection) -> Unit,
    driveStatus: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101214))
            .padding(horizontal = 48.dp, vertical = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Sidebar(
            selectedSection = section,
            driveStatus = driveStatus,
            onSectionSelected = onSectionSelected,
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun Sidebar(
    selectedSection: AppSection,
    driveStatus: String,
    onSectionSelected: (AppSection) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(178.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "gDriveShow",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(40.dp))
            AppSection.entries.forEach { section ->
                SidebarItem(
                    label = section.label,
                    selected = selectedSection == section,
                    onClick = { onSectionSelected(section) },
                )
            }
        }
        Text(
            text = driveStatus,
            color = Color(0xFF98A2AD),
            fontSize = 13.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun SidebarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)

    Text(
        text = label,
        color = if (selected || focused) Color.White else Color(0xFF98A2AD),
        fontSize = 18.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color(0xFF222A30) else Color.Transparent)
            .border(
                BorderStroke(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent),
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

@Composable
private fun DriveContentScreen(
    contentState: DriveContentState,
    selectedItem: DriveItem?,
    visibleItems: List<DriveItem>,
    filter: MediaFilter,
    sortMode: SortMode,
    onFilterChanged: (MediaFilter) -> Unit,
    onSortChanged: (SortMode) -> Unit,
    onItemSelected: (DriveItem) -> Unit,
    onStartSlideshow: () -> Unit,
) {
    when (contentState) {
        DriveContentState.Loading -> StatePanel(
            title = "Loading Drive",
            message = "Reading folders and supported media from Google Drive.",
            actionLabel = null,
            onAction = {},
        )

        DriveContentState.Empty -> StatePanel(
            title = "No Media Found",
            message = "The selected Drive folder does not contain folders, images, or videos yet.",
            actionLabel = "Open settings",
            onAction = {},
        )

        is DriveContentState.Failed -> StatePanel(
            title = "Drive Error",
            message = contentState.message,
            actionLabel = "Retry",
            onAction = {},
        )

        is DriveContentState.Ready -> {
            if (selectedItem == null || visibleItems.isEmpty()) {
                StatePanel(
                    title = "Nothing Matches",
                    message = "Change the media filter or sort mode to see more Drive content.",
                    actionLabel = "Show all",
                    onAction = { onFilterChanged(MediaFilter.All) },
                )
            } else {
                BrowseScreen(
                    selectedItem = selectedItem,
                    items = visibleItems,
                    filter = filter,
                    sortMode = sortMode,
                    onFilterChanged = onFilterChanged,
                    onSortChanged = onSortChanged,
                    onItemSelected = onItemSelected,
                    onStartSlideshow = onStartSlideshow,
                )
            }
        }
    }
}

@Composable
private fun BrowseScreen(
    selectedItem: DriveItem,
    items: List<DriveItem>,
    filter: MediaFilter,
    sortMode: SortMode,
    onFilterChanged: (MediaFilter) -> Unit,
    onSortChanged: (SortMode) -> Unit,
    onItemSelected: (DriveItem) -> Unit,
    onStartSlideshow: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp), modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            PageHeader(
                title = "Browse Drive",
                subtitle = "Folders, photos, and videos optimized for TV navigation",
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 18.dp),
            ) {
                MediaFilter.entries.forEach { option ->
                    PillButton(
                        label = option.label,
                        selected = filter == option,
                        onClick = { onFilterChanged(option) },
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                SortMode.entries.forEach { option ->
                    PillButton(
                        label = "Sort: ${option.label}",
                        selected = sortMode == option,
                        onClick = { onSortChanged(option) },
                    )
                }
            }
            ContentGrid(
                items = items,
                selectedItem = selectedItem,
                onItemSelected = onItemSelected,
            )
        }
        DetailPanel(
            selectedItem = selectedItem,
            onStartSlideshow = onStartSlideshow,
            modifier = Modifier.width(392.dp),
        )
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = subtitle,
        color = Color(0xFFB0BAC5),
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
    )
}

@Composable
private fun PillButton(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val background = when {
        selected -> Color(0xFF72D6C9)
        focused -> Color(0xFF2B333A)
        else -> Color(0xFF191D21)
    }
    val foreground = if (selected) Color(0xFF101214) else Color.White

    Text(
        text = label,
        color = foreground,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF303941)), shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun ContentGrid(
    items: List<DriveItem>,
    selectedItem: DriveItem,
    onItemSelected: (DriveItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxHeight(),
    ) {
        items(items, key = { it.id }) { item ->
            DriveCard(
                item = item,
                selected = selectedItem.id == item.id,
                onSelected = { onItemSelected(item) },
            )
        }
    }
}

@Composable
private fun DriveCard(item: DriveItem, selected: Boolean, onSelected: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val accent = Color(item.accentColor)
    val borderColor = when {
        focused -> Color.White
        selected -> accent
        else -> Color(0xFF2E353B)
    }

    Column(
        modifier = Modifier
            .clip(shape)
            .background(Color(0xFF191D21))
            .border(BorderStroke(if (focused || selected) 3.dp else 1.dp, borderColor), shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onSelected)
            .padding(14.dp),
    ) {
        MediaArtwork(item = item, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            color = Color(0xFFB0BAC5),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MediaArtwork(item: DriveItem, modifier: Modifier = Modifier) {
    val accent = Color(item.accentColor)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.linearGradient(colors = listOf(accent, Color(0xFF273039)))),
    ) {
        Text(
            text = item.type.label,
            color = Color(0xFF101214),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun DetailPanel(
    selectedItem: DriveItem,
    onStartSlideshow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF191D21))
            .padding(24.dp),
    ) {
        MediaArtwork(
            item = selectedItem,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 11f),
        )
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = selectedItem.title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp,
        )
        Text(
            text = selectedItem.modifiedLabel,
            color = Color(0xFFB0BAC5),
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = selectedItem.description,
            color = Color(0xFFCFD7DE),
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 28.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onStartSlideshow,
            enabled = selectedItem.isPlayable,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF72D6C9),
                contentColor = Color(0xFF101214),
                disabledContainerColor = Color(0xFF303941),
                disabledContentColor = Color(0xFF8C98A3),
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (selectedItem.isPlayable) "Start slideshow" else "Open folder",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatePanel(
    title: String,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF191D21))
            .padding(36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = message,
            color = Color(0xFFB0BAC5),
            fontSize = 17.sp,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 26.dp),
        )
        if (actionLabel != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF72D6C9),
                    contentColor = Color(0xFF101214),
                ),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(actionLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SlideshowLibraryScreen(items: List<DriveItem>, onStart: (DriveItem) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = "Slideshows",
            subtitle = "Playable image and video sets discovered in the selected Drive folder",
        )
        if (items.isEmpty()) {
            StatePanel(
                title = "No Playable Media",
                message = "Images and videos will appear here after Drive content is loaded.",
                actionLabel = null,
                onAction = {},
                modifier = Modifier.weight(1f),
            )
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { item ->
                DriveCard(item = item, selected = false, onSelected = { onStart(item) })
            }
        }
    }
}

@Composable
private fun SettingsScreen(connectionState: DriveConnectionState) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = "Settings",
            subtitle = "TV-safe controls that will back Google Drive, playback, and cache behavior",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
            SettingsGroup(
                title = "Drive",
                rows = listOf(
                    "Account" to connectionState.settingsLabel,
                    "Starting folder" to "Root folder",
                    "Shared drives" to "Later milestone",
                ),
                modifier = Modifier.weight(1f),
            )
            SettingsGroup(
                title = "Slideshow",
                rows = listOf(
                    "Interval" to "8 seconds",
                    "Image fit" to "Contain",
                    "Video behavior" to "Continue to next",
                ),
                modifier = Modifier.weight(1f),
            )
            SettingsGroup(
                title = "Cache",
                rows = listOf(
                    "Thumbnails" to "Enabled",
                    "Offline metadata" to "Later milestone",
                    "Cache size" to "512 MB",
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val DriveConnectionState.settingsLabel: String
    get() = when (this) {
        DriveConnectionState.Disconnected -> "Not connected"
        DriveConnectionState.Connecting -> "Connecting"
        is DriveConnectionState.Connected -> accountLabel
        is DriveConnectionState.Failed -> "Error"
    }

@Composable
private fun SettingsGroup(
    title: String,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF191D21))
            .padding(20.dp),
    ) {
        Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = label, color = Color(0xFFCFD7DE), fontSize = 15.sp)
                Text(text = value, color = Color(0xFF98A2AD), fontSize = 15.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SlideshowScreen(
    items: List<DriveItem>,
    currentIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val item = items.getOrNull(currentIndex) ?: return
    val accent = Color(item.accentColor)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(accent, Color.Black))),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = item.title, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${currentIndex + 1} of ${items.size} - ${item.type.label}",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(onClick = onPrevious, shape = RoundedCornerShape(6.dp)) {
                Text("Previous")
            }
            Button(onClick = onNext, shape = RoundedCornerShape(6.dp)) {
                Text("Next")
            }
            Button(onClick = onBack, shape = RoundedCornerShape(6.dp)) {
                Text("Back")
            }
        }
    }
}

private fun Int.floorMod(size: Int): Int = if (size == 0) 0 else ((this % size) + size) % size
