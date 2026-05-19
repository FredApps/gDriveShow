package com.fredapps.gdriveshow

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GDriveShowApp(
                onExit = { finish() },
            )
        }
    }
}

private enum class DriveMediaType {
    Folder,
    Image,
    Video,
}

private data class DriveItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: DriveMediaType,
    val accent: Color,
)

private val sampleItems = listOf(
    DriveItem("1", "Family Photos", "42 images", DriveMediaType.Folder, Color(0xFF72D6C9)),
    DriveItem("2", "Summer Trip", "18 images", DriveMediaType.Image, Color(0xFFFFD166)),
    DriveItem("3", "Cabin Weekend", "12 videos", DriveMediaType.Video, Color(0xFFFF8A65)),
    DriveItem("4", "Receipts Archive", "Drive folder", DriveMediaType.Folder, Color(0xFF9FA8DA)),
    DriveItem("5", "Living Room Loop", "Slideshow ready", DriveMediaType.Image, Color(0xFFA5D6A7)),
    DriveItem("6", "Drone Clips", "4K video", DriveMediaType.Video, Color(0xFF90CAF9)),
)

@Composable
private fun GDriveShowApp(onExit: () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color(0xFF101214),
            surface = Color(0xFF191D21),
            primary = Color(0xFF72D6C9),
            onPrimary = Color(0xFF101214),
            onSurface = Color.White,
        ),
    ) {
        var selectedItem by remember { mutableStateOf(sampleItems.first()) }
        var isSlideshow by remember { mutableStateOf(false) }

        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isSlideshow) {
                SlideshowScreen(
                    item = selectedItem,
                    onBack = { isSlideshow = false },
                    onExit = onExit,
                )
            } else {
                BrowseScreen(
                    selectedItem = selectedItem,
                    items = sampleItems,
                    onItemSelected = { selectedItem = it },
                    onStartSlideshow = { isSlideshow = true },
                )
            }
        }
    }
}

@Composable
private fun BrowseScreen(
    selectedItem: DriveItem,
    items: List<DriveItem>,
    onItemSelected: (DriveItem) -> Unit,
    onStartSlideshow: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101214))
            .padding(horizontal = 48.dp, vertical = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Sidebar()
        ContentGrid(
            items = items,
            selectedItem = selectedItem,
            onItemSelected = onItemSelected,
            modifier = Modifier.weight(1f),
        )
        DetailPanel(
            selectedItem = selectedItem,
            onStartSlideshow = onStartSlideshow,
            modifier = Modifier.width(380.dp),
        )
    }
}

@Composable
private fun Sidebar() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(168.dp),
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
            SidebarItem("Drive", true)
            SidebarItem("Slideshows", false)
            SidebarItem("Settings", false)
        }
        Text(
            text = "Google Drive not connected",
            color = Color(0xFF98A2AD),
            fontSize = 13.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun SidebarItem(label: String, selected: Boolean) {
    Text(
        text = label,
        color = if (selected) Color.White else Color(0xFF98A2AD),
        fontSize = 18.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContentGrid(
    items: List<DriveItem>,
    selectedItem: DriveItem,
    onItemSelected: (DriveItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Text(
            text = "Browse Drive",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Folders, photos, and videos optimized for TV navigation",
            color = Color(0xFFB0BAC5),
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
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
}

@Composable
private fun DriveCard(
    item: DriveItem,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val borderColor = when {
        focused -> Color.White
        selected -> item.accent
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(item.accent, Color(0xFF273039)),
                    ),
                ),
        ) {
            Text(
                text = item.type.label,
                color = Color(0xFF101214),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 11f)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(selectedItem.accent, Color(0xFF11161B)))),
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
            text = selectedItem.subtitle,
            color = Color(0xFFB0BAC5),
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Preview, slideshow, and video playback controls will attach here once Drive media loading is wired in.",
            color = Color(0xFFCFD7DE),
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 28.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onStartSlideshow,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF72D6C9),
                contentColor = Color(0xFF101214),
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start slideshow", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SlideshowScreen(
    item: DriveItem,
    onBack: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(item.accent, Color.Black))),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Slideshow preview mode",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text("Back")
            }
            Button(
                onClick = onExit,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text("Exit")
            }
        }
    }
}

private val DriveMediaType.label: String
    get() = when (this) {
        DriveMediaType.Folder -> "Folder"
        DriveMediaType.Image -> "Image"
        DriveMediaType.Video -> "Video"
    }

