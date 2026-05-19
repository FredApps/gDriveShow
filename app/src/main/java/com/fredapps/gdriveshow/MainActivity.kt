package com.fredapps.gdriveshow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.fredapps.gdriveshow.drive.DriveItem
import com.fredapps.gdriveshow.drive.DriveConnectionState
import com.fredapps.gdriveshow.drive.DriveContentState
import com.fredapps.gdriveshow.drive.DriveMediaType
import com.fredapps.gdriveshow.drive.DriveRepository
import com.fredapps.gdriveshow.drive.GoogleDriveRepository
import com.fredapps.gdriveshow.drive.AppPreferences
import com.fredapps.gdriveshow.drive.DriveMediaLoader
import com.fredapps.gdriveshow.drive.DriveMetadataCache
import com.fredapps.gdriveshow.drive.DriveThumbnailCache
import com.fredapps.gdriveshow.drive.ImageLoadResult
import com.fredapps.gdriveshow.drive.SampleDriveRepository
import com.fredapps.gdriveshow.drive.StartupFolder
import com.fredapps.gdriveshow.drive.VideoRequest
import com.fredapps.gdriveshow.drive.isPlayable
import com.fredapps.gdriveshow.drive.label
import com.fredapps.gdriveshow.drive.statusLabel
import com.fredapps.gdriveshow.drive.subtitle
import com.fredapps.gdriveshow.drive.auth.DeviceAuthorizationPrompt
import com.fredapps.gdriveshow.drive.auth.DeviceAuthorizationResult
import com.fredapps.gdriveshow.drive.auth.DeviceAuthorizationStartResult
import com.fredapps.gdriveshow.drive.auth.DriveAccessTokenProvider
import com.fredapps.gdriveshow.drive.auth.DriveAuthConfig
import com.fredapps.gdriveshow.drive.auth.EncryptedDriveTokenStore
import com.fredapps.gdriveshow.drive.auth.GoogleDeviceCodeAuthClient
import kotlinx.coroutines.delay

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

private data class FolderLocation(
    val id: String,
    val title: String,
)

private sealed interface AuthUiState {
    data object Idle : AuthUiState
    data class Working(val message: String) : AuthUiState
    data class Prompt(
        val prompt: DeviceAuthorizationPrompt,
        val message: String = "Open the URL on your phone or computer and enter the code.",
    ) : AuthUiState

    data class Authorized(val message: String) : AuthUiState
    data class SignedOut(val message: String) : AuthUiState
    data class Failed(val message: String) : AuthUiState
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
        val context = LocalContext.current
        val tokenStore = remember(context) {
            EncryptedDriveTokenStore(context.applicationContext)
        }
        val appPreferences = remember(context) {
            AppPreferences(context.applicationContext)
        }
        val metadataCache = remember(context) {
            DriveMetadataCache(context.applicationContext)
        }
        val thumbnailCache = remember(context) {
            DriveThumbnailCache(context.applicationContext)
        }
        val authConfig = remember(context) {
            DriveAuthConfig(clientId = context.getString(R.string.google_oauth_tv_client_id))
        }
        val authClient = remember(authConfig, tokenStore) {
            GoogleDeviceCodeAuthClient(
                config = authConfig,
                tokenStore = tokenStore,
            )
        }
        val googleDriveRepository = remember(authConfig, tokenStore, metadataCache) {
            val accessTokenProvider = DriveAccessTokenProvider(
                config = authConfig,
                tokenStore = tokenStore,
            )
            GoogleDriveRepository(
                tokenStore = tokenStore,
                accessTokenProvider = accessTokenProvider,
                metadataCache = metadataCache,
            )
        }
        val mediaLoader = remember(authConfig, tokenStore, thumbnailCache) {
            DriveMediaLoader(
                DriveAccessTokenProvider(
                    config = authConfig,
                    tokenStore = tokenStore,
                ),
                thumbnailCache = thumbnailCache,
            )
        }
        var connectionState by remember { mutableStateOf(repository.connectionState()) }
        var startupFolder by remember {
            mutableStateOf(appPreferences.startupFolder())
        }
        var folderStack by remember {
            mutableStateOf(listOf(FolderLocation(startupFolder.id, startupFolder.title)))
        }
        var contentState by remember { mutableStateOf<DriveContentState>(repository.content(startupFolder.id)) }
        val driveItems = (contentState as? DriveContentState.Ready)?.items.orEmpty()
        val slideshowItems = remember(contentState) { repository.slideshowCandidates(driveItems) }
        var section by remember { mutableStateOf(AppSection.Drive) }
        var filter by remember { mutableStateOf(MediaFilter.All) }
        var sortMode by remember { mutableStateOf(SortMode.Recent) }
        var selectedItemId by remember { mutableStateOf(driveItems.firstOrNull()?.id) }
        var slideshowIndex by remember { mutableIntStateOf(0) }
        var mediaViewerIndex by remember { mutableIntStateOf(0) }
        var showingSlideshow by remember { mutableStateOf(false) }
        var showingMediaViewer by remember { mutableStateOf(false) }
        var authUiState by remember { mutableStateOf<AuthUiState>(AuthUiState.Idle) }

        fun loadDriveContent(folder: FolderLocation = folderStack.last()) {
            if (tokenStore.read() == null) {
                connectionState = repository.connectionState()
                contentState = repository.content(folder.id)
                selectedItemId = (contentState as? DriveContentState.Ready)?.items?.firstOrNull()?.id
                return
            }

            connectionState = googleDriveRepository.connectionState()
            contentState = DriveContentState.Loading
            runBackground(
                request = { googleDriveRepository.content(folder.id) },
                onResult = { result ->
                    connectionState = googleDriveRepository.connectionState()
                    contentState = result
                    selectedItemId = (result as? DriveContentState.Ready)?.items?.firstOrNull()?.id
                },
            )
        }

        LaunchedEffect(Unit) {
            loadDriveContent()
        }

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
            if (showingMediaViewer) {
                MediaViewerScreen(
                    items = slideshowItems,
                    currentIndex = mediaViewerIndex,
                    mediaLoader = mediaLoader,
                    onPrevious = { mediaViewerIndex = (mediaViewerIndex - 1).floorMod(slideshowItems.size) },
                    onNext = { mediaViewerIndex = (mediaViewerIndex + 1).floorMod(slideshowItems.size) },
                    onBack = { showingMediaViewer = false },
                )
            } else if (showingSlideshow) {
                SlideshowScreen(
                    items = slideshowItems,
                    currentIndex = slideshowIndex,
                    mediaLoader = mediaLoader,
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
                            currentFolderId = folderStack.last().id,
                            currentFolderTitle = folderStack.last().title,
                            startupFolder = startupFolder,
                            canGoBack = folderStack.size > 1,
                            onBackFolder = {
                                if (folderStack.size > 1) {
                                    val nextStack = folderStack.dropLast(1)
                                    folderStack = nextStack
                                    loadDriveContent(nextStack.last())
                                }
                            },
                            onOpenFolder = { folder ->
                                val nextLocation = FolderLocation(folder.id, folder.title)
                                folderStack = folderStack + nextLocation
                                loadDriveContent(nextLocation)
                            },
                            onSetStartupFolder = {
                                val current = folderStack.last()
                                val nextStartupFolder = StartupFolder(current.id, current.title)
                                appPreferences.setStartupFolder(nextStartupFolder)
                                startupFolder = nextStartupFolder
                            },
                            onOpenMedia = {
                                selectedItem?.let { current ->
                                    val index = slideshowItems.indexOfFirst { it.id == current.id }
                                    mediaViewerIndex = if (index >= 0) index else 0
                                    showingMediaViewer = slideshowItems.isNotEmpty()
                                }
                            },
                            onStartSlideshow = {
                                selectedItem?.let { current ->
                                    val index = slideshowItems.indexOfFirst { it.id == current.id }
                                    slideshowIndex = if (index >= 0) index else 0
                                    showingSlideshow = slideshowItems.isNotEmpty()
                                }
                            },
                            onRetry = { loadDriveContent() },
                            onOpenSettings = { section = AppSection.Settings },
                            mediaLoader = mediaLoader,
                        )

                        AppSection.Slideshows -> SlideshowLibraryScreen(
                            items = slideshowItems,
                            mediaLoader = mediaLoader,
                            onStart = { item ->
                                slideshowIndex = slideshowItems.indexOf(item).coerceAtLeast(0)
                                showingSlideshow = true
                            },
                        )

                        AppSection.Settings -> SettingsScreen(
                            connectionState = connectionState,
                            startupFolder = startupFolder,
                            currentFolder = folderStack.last(),
                            folderOptions = driveItems.filter { it.type == DriveMediaType.Folder },
                            authState = authUiState,
                            onPickStartupFolder = { folder ->
                                val nextStartupFolder = StartupFolder(folder.id, folder.title)
                                appPreferences.setStartupFolder(nextStartupFolder)
                                startupFolder = nextStartupFolder
                            },
                            onConnect = {
                                authUiState = AuthUiState.Working("Requesting a Google Drive sign-in code")
                                runAuthRequest(
                                    request = { authClient.startAuthorization() },
                                    onResult = { result ->
                                        authUiState = when (result) {
                                            is DeviceAuthorizationStartResult.Prompt -> AuthUiState.Prompt(result.prompt)
                                            is DeviceAuthorizationStartResult.Failed -> AuthUiState.Failed(result.message)
                                        }
                                    },
                                )
                            },
                            onCheckAuthorization = { prompt ->
                                authUiState = AuthUiState.Working("Checking Google Drive authorization")
                                runAuthRequest(
                                    request = { authClient.pollAuthorization(prompt) },
                                    onResult = { result ->
                                        authUiState = when (result) {
                                            DeviceAuthorizationResult.AuthorizationPending -> AuthUiState.Prompt(
                                                prompt = prompt,
                                                message = "Still waiting for approval. Finish the Google prompt, then check again.",
                                            )

                                            DeviceAuthorizationResult.SlowDown -> AuthUiState.Prompt(
                                                prompt = prompt,
                                                message = "Google asked us to slow down. Wait ${prompt.pollIntervalSeconds} seconds, then check again.",
                                            )

                                            is DeviceAuthorizationResult.Authorized -> AuthUiState.Authorized(
                                                "Google Drive authorization succeeded. Loading real Drive content now.",
                                            ).also {
                                                folderStack = listOf(FolderLocation(startupFolder.id, startupFolder.title))
                                                loadDriveContent()
                                            }

                                            is DeviceAuthorizationResult.Denied -> AuthUiState.Failed(result.message)
                                            is DeviceAuthorizationResult.Failed -> AuthUiState.Failed(result.message)
                                        }
                                    },
                                )
                            },
                            onCancelAuthorization = { authUiState = AuthUiState.Idle },
                            onSignOut = {
                                runAuthRequest(
                                    request = {
                                        authClient.signOut()
                                        metadataCache.clear()
                                        thumbnailCache.clear()
                                        Unit
                                    },
                                    onResult = {
                                        authUiState = AuthUiState.SignedOut("Stored Google Drive tokens were cleared.")
                                        val rootFolder = StartupFolder(DriveRepository.RootFolderId, "Drive Root")
                                        appPreferences.setStartupFolder(rootFolder)
                                        startupFolder = rootFolder
                                        folderStack = listOf(FolderLocation(rootFolder.id, rootFolder.title))
                                        loadDriveContent()
                                    },
                                )
                            },
                        )
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
    currentFolderId: String,
    currentFolderTitle: String,
    startupFolder: StartupFolder,
    canGoBack: Boolean,
    onBackFolder: () -> Unit,
    onFilterChanged: (MediaFilter) -> Unit,
    onSortChanged: (SortMode) -> Unit,
    onItemSelected: (DriveItem) -> Unit,
    onOpenFolder: (DriveItem) -> Unit,
    onSetStartupFolder: () -> Unit,
    onOpenMedia: () -> Unit,
    onStartSlideshow: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    mediaLoader: DriveMediaLoader,
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
            onAction = onOpenSettings,
        )

        is DriveContentState.Failed -> StatePanel(
            title = "Drive Error",
            message = contentState.message,
            actionLabel = "Retry",
            onAction = onRetry,
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
                    currentFolderId = currentFolderId,
                    currentFolderTitle = currentFolderTitle,
                    startupFolder = startupFolder,
                    canGoBack = canGoBack,
                    onBackFolder = onBackFolder,
                    onFilterChanged = onFilterChanged,
                    onSortChanged = onSortChanged,
                    onItemSelected = onItemSelected,
                    onOpenFolder = onOpenFolder,
                    onSetStartupFolder = onSetStartupFolder,
                    onOpenMedia = onOpenMedia,
                    onStartSlideshow = onStartSlideshow,
                    mediaLoader = mediaLoader,
                    staleMessage = contentState.sourceMessage,
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
    currentFolderId: String,
    currentFolderTitle: String,
    startupFolder: StartupFolder,
    canGoBack: Boolean,
    onBackFolder: () -> Unit,
    onFilterChanged: (MediaFilter) -> Unit,
    onSortChanged: (SortMode) -> Unit,
    onItemSelected: (DriveItem) -> Unit,
    onOpenFolder: (DriveItem) -> Unit,
    onSetStartupFolder: () -> Unit,
    onOpenMedia: () -> Unit,
    onStartSlideshow: () -> Unit,
    mediaLoader: DriveMediaLoader,
    staleMessage: String?,
) {
    val isStartupFolder = startupFolder.id == currentFolderId
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp), modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            PageHeader(
                title = currentFolderTitle,
                subtitle = staleMessage ?: "Folders, photos, and videos optimized for TV navigation",
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 18.dp),
            ) {
                PillButton(
                    label = if (isStartupFolder) "Startup folder" else "Set startup",
                    selected = isStartupFolder,
                    onClick = onSetStartupFolder,
                )
                Spacer(modifier = Modifier.width(12.dp))
                if (canGoBack) {
                    PillButton(
                        label = "Back",
                        selected = false,
                        onClick = onBackFolder,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
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
                mediaLoader = mediaLoader,
            )
        }
        DetailPanel(
            selectedItem = selectedItem,
            onOpenFolder = { onOpenFolder(selectedItem) },
            onOpenMedia = onOpenMedia,
            onStartSlideshow = onStartSlideshow,
            mediaLoader = mediaLoader,
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
    mediaLoader: DriveMediaLoader,
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
                mediaLoader = mediaLoader,
                onSelected = { onItemSelected(item) },
            )
        }
    }
}

@Composable
private fun DriveCard(
    item: DriveItem,
    selected: Boolean,
    mediaLoader: DriveMediaLoader?,
    onSelected: () -> Unit,
) {
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
        MediaArtwork(
            item = item,
            mediaLoader = mediaLoader,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
        )
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
private fun MediaArtwork(
    item: DriveItem,
    mediaLoader: DriveMediaLoader? = null,
    modifier: Modifier = Modifier,
) {
    val accent = Color(item.accentColor)
    var thumbnailResult by remember(item.id, item.thumbnailUrl) { mutableStateOf<ImageLoadResult?>(null) }

    LaunchedEffect(item.id, item.thumbnailUrl, mediaLoader) {
        thumbnailResult = null
        if (mediaLoader != null && item.thumbnailUrl != null) {
            runBackground(
                request = { mediaLoader.loadThumbnail(item) },
                onResult = { thumbnailResult = it },
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.linearGradient(colors = listOf(accent, Color(0xFF273039)))),
    ) {
        val readyThumbnail = thumbnailResult as? ImageLoadResult.Ready
        if (readyThumbnail != null) {
            Image(
                bitmap = readyThumbnail.bitmap.asImageBitmap(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f)),
            )
        }
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
    onOpenFolder: () -> Unit,
    onOpenMedia: () -> Unit,
    onStartSlideshow: () -> Unit,
    mediaLoader: DriveMediaLoader,
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
            mediaLoader = mediaLoader,
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
            onClick = if (selectedItem.type == DriveMediaType.Folder) onOpenFolder else onOpenMedia,
            enabled = selectedItem.type == DriveMediaType.Folder || selectedItem.isPlayable,
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
                text = when (selectedItem.type) {
                    DriveMediaType.Folder -> "Open folder"
                    DriveMediaType.Image -> "View image"
                    DriveMediaType.Video -> "Play video"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (selectedItem.isPlayable) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartSlideshow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF303941),
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start slideshow", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
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
private fun SlideshowLibraryScreen(
    items: List<DriveItem>,
    mediaLoader: DriveMediaLoader,
    onStart: (DriveItem) -> Unit,
) {
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
                DriveCard(
                    item = item,
                    selected = false,
                    mediaLoader = mediaLoader,
                    onSelected = { onStart(item) },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    connectionState: DriveConnectionState,
    startupFolder: StartupFolder,
    currentFolder: FolderLocation,
    folderOptions: List<DriveItem>,
    authState: AuthUiState,
    onPickStartupFolder: (StartupFolder) -> Unit,
    onConnect: () -> Unit,
    onCheckAuthorization: (DeviceAuthorizationPrompt) -> Unit,
    onCancelAuthorization: () -> Unit,
    onSignOut: () -> Unit,
) {
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
                    "Starting folder" to startupFolder.title,
                    "Shared drives" to "Root picker enabled",
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
                    "Thumbnails" to "Loaded from Drive",
                    "Offline metadata" to "Last folder cached",
                    "Cache size" to "512 MB",
                ),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        StartupFolderPicker(
            startupFolder = startupFolder,
            currentFolder = currentFolder,
            folderOptions = folderOptions,
            onPickStartupFolder = onPickStartupFolder,
        )
        Spacer(modifier = Modifier.height(22.dp))
        DriveAuthPanel(
            authState = authState,
            onConnect = onConnect,
            onCheckAuthorization = onCheckAuthorization,
            onCancelAuthorization = onCancelAuthorization,
            onSignOut = onSignOut,
        )
    }
}

@Composable
private fun StartupFolderPicker(
    startupFolder: StartupFolder,
    currentFolder: FolderLocation,
    folderOptions: List<DriveItem>,
    onPickStartupFolder: (StartupFolder) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF191D21))
            .padding(20.dp),
    ) {
        Text(text = "Startup Folder", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Pick the folder this TV opens after launch. Browse Drive first to reveal more choices.",
            color = Color(0xFFB0BAC5),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val currentStartup = StartupFolder(currentFolder.id, currentFolder.title)
            FolderPickButton(
                label = "Current: ${currentFolder.title}",
                selected = startupFolder.id == currentFolder.id,
                onClick = { onPickStartupFolder(currentStartup) },
            )
            folderOptions.take(3).forEach { folder ->
                FolderPickButton(
                    label = folder.title,
                    selected = startupFolder.id == folder.id,
                    onClick = { onPickStartupFolder(StartupFolder(folder.id, folder.title)) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.FolderPickButton(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val background = when {
        selected -> Color(0xFF72D6C9)
        focused -> Color(0xFF303941)
        else -> Color(0xFF242A30)
    }

    Text(
        text = label,
        color = if (selected) Color(0xFF101214) else Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF303941)), shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

private val DriveConnectionState.settingsLabel: String
    get() = when (this) {
        DriveConnectionState.Disconnected -> "Not connected"
        DriveConnectionState.Connecting -> "Connecting"
        is DriveConnectionState.Connected -> accountLabel
        is DriveConnectionState.Failed -> "Error"
    }

@Composable
private fun DriveAuthPanel(
    authState: AuthUiState,
    onConnect: () -> Unit,
    onCheckAuthorization: (DeviceAuthorizationPrompt) -> Unit,
    onCancelAuthorization: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF191D21))
            .padding(22.dp),
    ) {
        Text(text = "Google Drive Connection", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))

        when (authState) {
            AuthUiState.Idle -> {
                Text(
                    text = "Connect with Google's TV sign-in flow. The app will show a code to approve on another device.",
                    color = Color(0xFFCFD7DE),
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
                Spacer(modifier = Modifier.height(18.dp))
                ButtonRow {
                    PrimaryActionButton(label = "Connect", onClick = onConnect)
                    SecondaryActionButton(label = "Clear tokens", onClick = onSignOut)
                }
            }

            is AuthUiState.Working -> {
                Text(text = authState.message, color = Color(0xFFCFD7DE), fontSize = 15.sp)
            }

            is AuthUiState.Prompt -> {
                Text(text = authState.message, color = Color(0xFFCFD7DE), fontSize = 15.sp, lineHeight = 21.sp)
                Spacer(modifier = Modifier.height(16.dp))
                CodeDisplay(label = "Code", value = authState.prompt.userCode)
                CodeDisplay(label = "URL", value = authState.prompt.verificationUrl)
                authState.prompt.verificationUrlComplete?.let {
                    CodeDisplay(label = "Direct URL", value = it)
                }
                Text(
                    text = "Expires in ${authState.prompt.expiresInSeconds / 60} minutes.",
                    color = Color(0xFF98A2AD),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Spacer(modifier = Modifier.height(18.dp))
                ButtonRow {
                    PrimaryActionButton(
                        label = "Check approval",
                        onClick = { onCheckAuthorization(authState.prompt) },
                    )
                    SecondaryActionButton(label = "Cancel", onClick = onCancelAuthorization)
                }
            }

            is AuthUiState.Authorized -> {
                Text(text = authState.message, color = Color(0xFFCFD7DE), fontSize = 15.sp, lineHeight = 21.sp)
                Spacer(modifier = Modifier.height(18.dp))
                ButtonRow {
                    SecondaryActionButton(label = "Clear tokens", onClick = onSignOut)
                }
            }

            is AuthUiState.SignedOut -> {
                Text(text = authState.message, color = Color(0xFFCFD7DE), fontSize = 15.sp, lineHeight = 21.sp)
                Spacer(modifier = Modifier.height(18.dp))
                ButtonRow {
                    PrimaryActionButton(label = "Connect", onClick = onConnect)
                }
            }

            is AuthUiState.Failed -> {
                Text(text = authState.message, color = Color(0xFFFFB4A8), fontSize = 15.sp, lineHeight = 21.sp)
                Spacer(modifier = Modifier.height(18.dp))
                ButtonRow {
                    PrimaryActionButton(label = "Try again", onClick = onConnect)
                    SecondaryActionButton(label = "Cancel", onClick = onCancelAuthorization)
                }
            }
        }
    }
}

@Composable
private fun CodeDisplay(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label, color = Color(0xFF98A2AD), fontSize = 13.sp)
        Text(
            text = value,
            color = Color.White,
            fontSize = if (label == "Code") 30.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = if (label == "Code") 34.sp else 22.sp,
        )
    }
}

@Composable
private fun ButtonRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        content()
    }
}

@Composable
private fun PrimaryActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF72D6C9),
            contentColor = Color(0xFF101214),
        ),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF303941),
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
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
    mediaLoader: DriveMediaLoader,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val item = items.getOrNull(currentIndex) ?: return
    var paused by remember { mutableStateOf(false) }
    var videoStatus by remember(item.id) { mutableStateOf("Preparing video") }
    var imageLoadResult by remember(item.id) { mutableStateOf<ImageLoadResult?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(item.id, item.mediaUrl) {
        imageLoadResult = null
        if (item.type == DriveMediaType.Image && item.mediaUrl != null) {
            runBackground(
                request = { mediaLoader.loadImage(item) },
                onResult = { imageLoadResult = it },
            )
        }
    }

    LaunchedEffect(item.id, paused, items.size) {
        if (!paused && items.size > 1) {
            delay(if (item.type == DriveMediaType.Video) VideoSlideIntervalMillis else ImageSlideIntervalMillis)
            onNext()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onPrevious()
                        true
                    }

                    Key.DirectionRight -> {
                        onNext()
                        true
                    }

                    Key.DirectionCenter, Key.Enter, Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        paused = !paused
                        true
                    }

                    Key.Back, Key.Escape -> {
                        onBack()
                        true
                    }

                    else -> false
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.76f),
        ) {
            when (item.type) {
                DriveMediaType.Image -> ImageViewerContent(
                    item = item,
                    imageLoadResult = imageLoadResult,
                )

                DriveMediaType.Video -> VideoViewerContent(
                    item = item,
                    mediaLoader = mediaLoader,
                    playWhenReady = !paused,
                    onStatusChanged = { videoStatus = it },
                )

                DriveMediaType.Folder -> MediaPlaceholderContent(
                    item = item,
                    status = "Folder",
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 44.dp, top = 32.dp),
        ) {
            Text(text = item.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${currentIndex + 1} of ${items.size} - ${item.type.label} - ${
                    if (paused) "Paused" else if (item.type == DriveMediaType.Video) videoStatus else "Auto"
                }",
                color = Color(0xFFB0BAC5),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 6.dp),
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
            Button(
                onClick = { paused = !paused },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF72D6C9),
                    contentColor = Color(0xFF101214),
                ),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(if (paused) "Resume" else "Pause")
            }
            Button(onClick = onBack, shape = RoundedCornerShape(6.dp)) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun MediaViewerScreen(
    items: List<DriveItem>,
    currentIndex: Int,
    mediaLoader: DriveMediaLoader,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val item = items.getOrNull(currentIndex) ?: return
    var videoPlaying by remember(item.id) { mutableStateOf(false) }
    var videoStatus by remember(item.id) { mutableStateOf("Preparing video") }
    var imageLoadResult by remember(item.id) { mutableStateOf<ImageLoadResult?>(null) }
    val focusRequester = remember { FocusRequester() }
    val accent = Color(item.accentColor)

    LaunchedEffect(item.id, item.mediaUrl) {
        imageLoadResult = null
        if (item.type == DriveMediaType.Image && item.mediaUrl != null) {
            runBackground(
                request = { mediaLoader.loadImage(item) },
                onResult = { imageLoadResult = it },
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onPrevious()
                        true
                    }

                    Key.DirectionRight -> {
                        onNext()
                        true
                    }

                    Key.Back, Key.Escape -> {
                        onBack()
                        true
                    }

                    Key.DirectionCenter, Key.Enter, Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        if (item.type == DriveMediaType.Video) {
                            videoPlaying = !videoPlaying
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.72f)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.radialGradient(listOf(accent, Color(0xFF11161B)))),
        ) {
            when (item.type) {
                DriveMediaType.Image -> ImageViewerContent(
                    item = item,
                    imageLoadResult = imageLoadResult,
                )

                DriveMediaType.Video -> VideoViewerContent(
                    item = item,
                    mediaLoader = mediaLoader,
                    playWhenReady = videoPlaying,
                    onStatusChanged = { videoStatus = it },
                )

                DriveMediaType.Folder -> MediaPlaceholderContent(
                    item = item,
                    status = "Folder",
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 44.dp, top = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "${currentIndex + 1} of ${items.size}", color = Color.White, fontSize = 18.sp)
            Text(text = item.modifiedLabel, color = Color(0xFFB0BAC5), fontSize = 15.sp)
            if (item.type == DriveMediaType.Video) {
                Text(text = videoStatus, color = Color(0xFFB0BAC5), fontSize = 15.sp)
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(onClick = onPrevious, shape = RoundedCornerShape(6.dp)) {
                Text("Previous")
            }
            if (item.type == DriveMediaType.Video) {
                Button(
                    onClick = { videoPlaying = !videoPlaying },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF72D6C9),
                        contentColor = Color(0xFF101214),
                    ),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(if (videoPlaying) "Pause" else "Play")
                }
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

@Composable
private fun ImageViewerContent(
    item: DriveItem,
    imageLoadResult: ImageLoadResult?,
) {
    when (val result = imageLoadResult) {
        is ImageLoadResult.Ready -> Image(
            bitmap = result.bitmap.asImageBitmap(),
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
        )

        is ImageLoadResult.Failed -> MediaPlaceholderContent(item = item, status = result.message)
        ImageLoadResult.Unavailable -> MediaPlaceholderContent(item = item, status = "Image stream unavailable")
        null -> MediaPlaceholderContent(
            item = item,
            status = if (item.mediaUrl == null) "Sample image preview" else "Loading image",
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoViewerContent(
    item: DriveItem,
    mediaLoader: DriveMediaLoader,
    playWhenReady: Boolean,
    onStatusChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    var videoRequest by remember(item.id, item.mediaUrl) { mutableStateOf<VideoRequest?>(null) }
    var videoRequestLoaded by remember(item.id, item.mediaUrl) { mutableStateOf(false) }

    LaunchedEffect(item.id, item.mediaUrl) {
        videoRequest = null
        videoRequestLoaded = false
        onStatusChanged("Preparing video")
        if (item.mediaUrl != null) {
            runBackground(
                request = { mediaLoader.videoRequest(item) },
                onResult = {
                    videoRequest = it
                    videoRequestLoaded = true
                    if (it == null) {
                        onStatusChanged("Stream unavailable")
                    }
                },
            )
        } else {
            videoRequestLoaded = true
            onStatusChanged("Stream unavailable")
        }
    }

    if (videoRequest == null) {
        MediaPlaceholderContent(
            item = item,
            status = if (videoRequestLoaded) "Video stream unavailable" else "Preparing video",
        )
        return
    }

    val request = videoRequest ?: return
    val player = remember(request.url, request.accessToken) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer ${request.accessToken}"))
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(request.url))
                prepare()
            }
    }

    LaunchedEffect(playWhenReady) {
        player.playWhenReady = playWhenReady
        onStatusChanged(if (playWhenReady) "Playing" else "Paused")
    }

    androidx.compose.runtime.DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                onStatusChanged(
                    when (playbackState) {
                        Player.STATE_BUFFERING -> "Buffering"
                        Player.STATE_READY -> if (player.playWhenReady) "Playing" else "Ready"
                        Player.STATE_ENDED -> "Ended"
                        Player.STATE_IDLE -> "Idle"
                        else -> "Preparing video"
                    },
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                onStatusChanged(error.message ?: "Playback error")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onStatusChanged(if (isPlaying) "Playing" else "Paused")
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                this.player = player
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun MediaPlaceholderContent(item: DriveItem, status: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = item.type.label,
            color = Color.White.copy(alpha = 0.74f),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 48.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = status,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

private fun <T> runAuthRequest(request: () -> T, onResult: (T) -> Unit) {
    runBackground(request = request, onResult = onResult)
}

private fun <T> runBackground(request: () -> T, onResult: (T) -> Unit) {
    Thread {
        val result = request()
        Handler(Looper.getMainLooper()).post {
            onResult(result)
        }
    }.start()
}

private fun Int.floorMod(size: Int): Int = if (size == 0) 0 else ((this % size) + size) % size

private const val ImageSlideIntervalMillis = 8_000L
private const val VideoSlideIntervalMillis = 15_000L
