package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FilterPreset
import com.example.data.model.MediaItem
import com.example.ui.components.PhotoFilterBottomSheet
import com.example.ui.components.PhotoInfoBottomSheet
import com.example.ui.components.ZoomablePhotoBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenViewerScreen(
    photos: List<MediaItem>,
    initialIndex: Int,
    isSlideshowActive: Boolean,
    isInfoSheetOpen: Boolean,
    isFilterSheetOpen: Boolean,
    onIndexChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeletePhoto: (String) -> Unit,
    onToggleSlideshow: () -> Unit,
    onSetInfoSheetOpen: (Boolean) -> Unit,
    onSetFilterSheetOpen: (Boolean) -> Unit,
    onApplyFilter: (FilterPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val safeInitialIndex = initialIndex.coerceIn(0, photos.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { photos.size }
    )

    // Sync pager position to viewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onIndexChanged(page)
        }
    }

    // React to external index change (e.g. slideshow auto progression)
    LaunchedEffect(initialIndex) {
        if (pagerState.currentPage != initialIndex && initialIndex in photos.indices) {
            pagerState.animateScrollToPage(initialIndex)
        }
    }

    var showControls by remember { mutableStateOf(true) }
    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.first()

    val infoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("full_screen_viewer")
    ) {
        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]
            ZoomablePhotoBox(
                photo = photo,
                onTap = { showControls = !showControls }
            )
        }

        // Top Chrome Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("fullscreen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = currentPhoto.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} of ${photos.size} • ${currentPhoto.dateDisplay}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    // Favorite toggle
                    IconButton(
                        onClick = { onToggleFavorite(currentPhoto.id) },
                        modifier = Modifier.testTag("fullscreen_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (currentPhoto.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentPhoto.isFavorite) Color(0xFFFF4D6D) else Color.White
                        )
                    }

                    // EXIF Info button
                    IconButton(
                        onClick = { onSetInfoSheetOpen(true) },
                        modifier = Modifier.testTag("fullscreen_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Photo Details",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Chrome Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    BottomActionItem(
                        icon = Icons.Default.Share,
                        label = "Share",
                        tag = "fullscreen_share_button",
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                currentPhoto.uri?.let { putExtra(Intent.EXTRA_STREAM, it) }
                                putExtra(Intent.EXTRA_TEXT, "Shared from Lumina Gallery: ${currentPhoto.title}")
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Photo")
                            context.startActivity(shareIntent)
                        }
                    )

                    // Edit / Filter
                    BottomActionItem(
                        icon = Icons.Default.AutoFixHigh,
                        label = "Filter",
                        tag = "fullscreen_filter_button",
                        onClick = { onSetFilterSheetOpen(true) }
                    )

                    // Slideshow
                    BottomActionItem(
                        icon = if (isSlideshowActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        label = if (isSlideshowActive) "Pause" else "Slideshow",
                        tag = "fullscreen_slideshow_button",
                        onClick = onToggleSlideshow
                    )

                    // Delete
                    BottomActionItem(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tag = "fullscreen_delete_button",
                        onClick = { onDeletePhoto(currentPhoto.id) }
                    )
                }
            }
        }

        // Modals
        if (isInfoSheetOpen) {
            PhotoInfoBottomSheet(
                photo = currentPhoto,
                sheetState = infoSheetState,
                onDismiss = { onSetInfoSheetOpen(false) }
            )
        }

        if (isFilterSheetOpen) {
            PhotoFilterBottomSheet(
                photo = currentPhoto,
                sheetState = filterSheetState,
                onApplyFilter = onApplyFilter,
                onDismiss = { onSetFilterSheetOpen(false) }
            )
        }
    }
}

@Composable
private fun BottomActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.testTag(tag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}
