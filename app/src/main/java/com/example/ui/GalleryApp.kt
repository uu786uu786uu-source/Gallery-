package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FullScreenViewerScreen
import com.example.ui.screens.PhotosScreen

@Composable
fun GalleryApp(
    viewModel: GalleryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // System BackHandler
    BackHandler(
        enabled = uiState.fullScreenIndex != null ||
            uiState.selectedAlbumName != null ||
            uiState.isSelectionMode ||
            uiState.currentTab != GalleryTab.PHOTOS
    ) {
        viewModel.onBackPressHandled()
    }

    // Android Photo Picker Launcher (works without permissions on modern Android!)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImportedPhotos(uris)
        }
    }

    // React to user notice snackbars
    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotice()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Show navigation bar and AdMob banner only when not in full-screen viewer
                if (uiState.fullScreenIndex == null) {
                    androidx.compose.foundation.layout.Column {
                        com.example.ui.components.AdMobBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.testTag("gallery_bottom_nav")
                        ) {
                        NavigationBarItem(
                            selected = uiState.currentTab == GalleryTab.PHOTOS && uiState.selectedAlbumName == null,
                            onClick = { viewModel.selectTab(GalleryTab.PHOTOS) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.currentTab == GalleryTab.PHOTOS) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                                    contentDescription = "Photos"
                                )
                            },
                            label = { Text("Photos") },
                            modifier = Modifier.testTag("nav_tab_photos")
                        )

                        NavigationBarItem(
                            selected = uiState.currentTab == GalleryTab.ALBUMS || uiState.selectedAlbumName != null,
                            onClick = { viewModel.selectTab(GalleryTab.ALBUMS) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.currentTab == GalleryTab.ALBUMS) Icons.Filled.Folder else Icons.Outlined.Folder,
                                    contentDescription = "Albums"
                                )
                            },
                            label = { Text("Albums") },
                            modifier = Modifier.testTag("nav_tab_albums")
                        )

                        NavigationBarItem(
                            selected = uiState.currentTab == GalleryTab.EXPLORE && uiState.selectedAlbumName == null,
                            onClick = { viewModel.selectTab(GalleryTab.EXPLORE) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.currentTab == GalleryTab.EXPLORE) Icons.Filled.Search else Icons.Outlined.Search,
                                    contentDescription = "Explore"
                                )
                            },
                            label = { Text("Explore") },
                            modifier = Modifier.testTag("nav_tab_explore")
                        )

                        NavigationBarItem(
                            selected = uiState.currentTab == GalleryTab.FAVORITES && uiState.selectedAlbumName == null,
                            onClick = { viewModel.selectTab(GalleryTab.FAVORITES) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.currentTab == GalleryTab.FAVORITES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorites"
                                )
                            },
                            label = { Text("Favorites") },
                            modifier = Modifier.testTag("nav_tab_favorites")
                        )
                    }
                }
            }
        }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // If a specific album is chosen
                if (uiState.selectedAlbumName != null) {
                    AlbumDetailScreen(
                        albumName = uiState.selectedAlbumName!!,
                        uiState = uiState,
                        onBack = { viewModel.backFromAlbum() },
                        onPhotoClick = { photo, list -> viewModel.openFullScreen(photo, list) },
                        onPhotoLongClick = { photo -> viewModel.toggleSelectionMode(photo.id) },
                        onAddPhotosClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onTogglePhotoSelection = { photoId -> viewModel.togglePhotoSelection(photoId) }
                    )
                } else {
                    when (uiState.currentTab) {
                        GalleryTab.PHOTOS -> {
                            PhotosScreen(
                                uiState = uiState,
                                onPhotoClick = { photo ->
                                    viewModel.openFullScreen(photo, uiState.allPhotos)
                                },
                                onPhotoLongClick = { photo ->
                                    viewModel.toggleSelectionMode(photo.id)
                                },
                                onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                                onTogglePhotoSelection = { photoId ->
                                    viewModel.togglePhotoSelection(photoId)
                                },
                                onSelectAll = { viewModel.selectAll(uiState.allPhotos) },
                                onClearSelection = { viewModel.clearSelection() },
                                onDeleteSelected = { viewModel.deleteSelectedPhotos() },
                                onCycleGridSpan = { viewModel.cycleGridSpan() },
                                onAddPhotosClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onCreateAlbumWithSelected = { viewModel.openNewAlbumDialog() }
                            )
                        }

                        GalleryTab.ALBUMS -> {
                            AlbumsScreen(
                                uiState = uiState,
                                onSelectAlbum = { albumName -> viewModel.selectAlbum(albumName) },
                                onCreateAlbumClick = { viewModel.openNewAlbumDialog() }
                            )
                        }

                        GalleryTab.EXPLORE -> {
                            ExploreScreen(
                                uiState = uiState,
                                onPhotoClick = { photo, list -> viewModel.openFullScreen(photo, list) },
                                onPhotoLongClick = { photo -> viewModel.toggleSelectionMode(photo.id) },
                                onSearchChange = { q -> viewModel.setSearchQuery(q) },
                                onTagSelect = { tag -> viewModel.setExploreTag(tag) },
                                onTogglePhotoSelection = { photoId -> viewModel.togglePhotoSelection(photoId) }
                            )
                        }

                        GalleryTab.FAVORITES -> {
                            val favoritePhotos = uiState.allPhotos.filter { it.isFavorite }
                            PhotosScreen(
                                uiState = uiState.copy(allPhotos = favoritePhotos),
                                onPhotoClick = { photo ->
                                    viewModel.openFullScreen(photo, favoritePhotos)
                                },
                                onPhotoLongClick = { photo ->
                                    viewModel.toggleSelectionMode(photo.id)
                                },
                                onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                                onTogglePhotoSelection = { photoId ->
                                    viewModel.togglePhotoSelection(photoId)
                                },
                                onSelectAll = { viewModel.selectAll(favoritePhotos) },
                                onClearSelection = { viewModel.clearSelection() },
                                onDeleteSelected = { viewModel.deleteSelectedPhotos() },
                                onCycleGridSpan = { viewModel.cycleGridSpan() },
                                onAddPhotosClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onCreateAlbumWithSelected = { viewModel.openNewAlbumDialog() }
                            )
                        }
                    }
                }
            }
        }

        // Full-screen Viewer Layer
        AnimatedVisibility(
            visible = uiState.fullScreenIndex != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.fullScreenIndex?.let { index ->
                FullScreenViewerScreen(
                    photos = uiState.fullScreenList,
                    initialIndex = index,
                    isSlideshowActive = uiState.isSlideshowActive,
                    isInfoSheetOpen = uiState.isInfoSheetOpen,
                    isFilterSheetOpen = uiState.isFilterSheetOpen,
                    onIndexChanged = { newIdx -> viewModel.setFullScreenIndex(newIdx) },
                    onBack = { viewModel.closeFullScreen() },
                    onToggleFavorite = { photoId -> viewModel.toggleFavorite(photoId) },
                    onDeletePhoto = { photoId -> viewModel.deleteSinglePhoto(photoId) },
                    onToggleSlideshow = { viewModel.toggleSlideshow() },
                    onSetInfoSheetOpen = { open -> viewModel.setInfoSheetOpen(open) },
                    onSetFilterSheetOpen = { open -> viewModel.setFilterSheetOpen(open) },
                    onApplyFilter = { preset -> viewModel.applyFilterToCurrentPhoto(preset) }
                )
            }
        }

        // Dialog: Create New Album
        if (uiState.isNewAlbumDialogOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.closeNewAlbumDialog() },
                title = { Text("New Album") },
                text = {
                    OutlinedTextField(
                        value = uiState.pendingAlbumName,
                        onValueChange = { viewModel.setPendingAlbumName(it) },
                        placeholder = { Text("Album name (e.g. Summer Roadtrip)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_album_name_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.createNewAlbum() },
                        enabled = uiState.pendingAlbumName.isNotBlank(),
                        modifier = Modifier.testTag("confirm_create_album_button")
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeNewAlbumDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
