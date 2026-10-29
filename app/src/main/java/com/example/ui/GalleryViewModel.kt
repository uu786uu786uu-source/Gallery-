package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GalleryRepository
import com.example.data.model.Album
import com.example.data.model.FilterPreset
import com.example.data.model.MediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GalleryTab {
    PHOTOS,
    ALBUMS,
    EXPLORE,
    FAVORITES
}

data class GalleryUiState(
    val allPhotos: List<MediaItem> = emptyList(),
    val albums: List<Album> = emptyList(),
    val currentTab: GalleryTab = GalleryTab.PHOTOS,
    val selectedAlbumName: String? = null,
    val fullScreenIndex: Int? = null,
    val fullScreenList: List<MediaItem> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedPhotoIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedExploreTag: String = "All",
    val gridSpanCount: Int = 3,
    val isInfoSheetOpen: Boolean = false,
    val isFilterSheetOpen: Boolean = false,
    val isSlideshowActive: Boolean = false,
    val isNewAlbumDialogOpen: Boolean = false,
    val pendingAlbumName: String = "",
    val userNotice: String? = null
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GalleryRepository(application)

    private val _currentTab = MutableStateFlow(GalleryTab.PHOTOS)
    private val _selectedAlbumName = MutableStateFlow<String?>(null)
    private val _fullScreenIndex = MutableStateFlow<Int?>(null)
    private val _fullScreenList = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedExploreTag = MutableStateFlow("All")
    private val _gridSpanCount = MutableStateFlow(3)
    private val _isInfoSheetOpen = MutableStateFlow(false)
    private val _isFilterSheetOpen = MutableStateFlow(false)
    private val _isSlideshowActive = MutableStateFlow(false)
    private val _isNewAlbumDialogOpen = MutableStateFlow(false)
    private val _pendingAlbumName = MutableStateFlow("")
    private val _userNotice = MutableStateFlow<String?>(null)

    private var slideshowJob: Job? = null

    val uiState: StateFlow<GalleryUiState> = combine(
        repository.photos,
        repository.customAlbums,
        _currentTab,
        _selectedAlbumName,
        _fullScreenIndex,
        _fullScreenList,
        _isSelectionMode,
        _selectedPhotoIds,
        _searchQuery,
        _selectedExploreTag,
        _gridSpanCount,
        _isInfoSheetOpen,
        _isFilterSheetOpen,
        _isSlideshowActive,
        _isNewAlbumDialogOpen,
        _pendingAlbumName,
        _userNotice
    ) { params ->
        val photos = params[0] as List<MediaItem>
        val customAlbums = params[1] as List<String>
        val currentTab = params[2] as GalleryTab
        val selectedAlbumName = params[3] as String?
        val fullScreenIndex = params[4] as Int?
        val fullScreenList = params[5] as List<MediaItem>
        val isSelectionMode = params[6] as Boolean
        val selectedPhotoIds = params[7] as Set<String>
        val searchQuery = params[8] as String
        val selectedExploreTag = params[9] as String
        val gridSpanCount = params[10] as Int
        val isInfoSheetOpen = params[11] as Boolean
        val isFilterSheetOpen = params[12] as Boolean
        val isSlideshowActive = params[13] as Boolean
        val isNewAlbumDialogOpen = params[14] as Boolean
        val pendingAlbumName = params[15] as String
        val userNotice = params[16] as String?

        // Compute albums
        val albumsList = mutableListOf<Album>()

        // System albums
        albumsList.add(
            Album(
                id = "system_all",
                name = "All Photos",
                coverMedia = photos.firstOrNull(),
                itemCount = photos.size,
                isSystem = true,
                subtitle = "Complete library"
            )
        )

        val favorites = photos.filter { it.isFavorite }
        albumsList.add(
            Album(
                id = "system_favorites",
                name = "Favorites",
                coverMedia = favorites.firstOrNull(),
                itemCount = favorites.size,
                isSystem = true,
                subtitle = "Starred memories"
            )
        )

        val cameraPhotos = photos.filter { it.albumName.equals("Camera", ignoreCase = true) }
        albumsList.add(
            Album(
                id = "system_camera",
                name = "Camera",
                coverMedia = cameraPhotos.firstOrNull() ?: photos.firstOrNull(),
                itemCount = cameraPhotos.size,
                isSystem = true,
                subtitle = "Captured photos"
            )
        )

        // Custom & detected albums
        val allAlbumNames = (customAlbums + photos.map { it.albumName })
            .distinct()
            .filterNot { it.equals("Camera", ignoreCase = true) }

        allAlbumNames.forEach { albName ->
            val albumPhotos = photos.filter { it.albumName.equals(albName, ignoreCase = true) }
            albumsList.add(
                Album(
                    id = "custom_${albName.lowercase()}",
                    name = albName,
                    coverMedia = albumPhotos.firstOrNull(),
                    itemCount = albumPhotos.size,
                    isSystem = false,
                    subtitle = "Album collection"
                )
            )
        }

        GalleryUiState(
            allPhotos = photos,
            albums = albumsList,
            currentTab = currentTab,
            selectedAlbumName = selectedAlbumName,
            fullScreenIndex = fullScreenIndex,
            fullScreenList = fullScreenList,
            isSelectionMode = isSelectionMode,
            selectedPhotoIds = selectedPhotoIds,
            searchQuery = searchQuery,
            selectedExploreTag = selectedExploreTag,
            gridSpanCount = gridSpanCount,
            isInfoSheetOpen = isInfoSheetOpen,
            isFilterSheetOpen = isFilterSheetOpen,
            isSlideshowActive = isSlideshowActive,
            isNewAlbumDialogOpen = isNewAlbumDialogOpen,
            pendingAlbumName = pendingAlbumName,
            userNotice = userNotice
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GalleryUiState()
    )

    init {
        // Attempt reading device media if permitted
        viewModelScope.launch {
            repository.queryDeviceMedia()
        }
    }

    fun selectTab(tab: GalleryTab) {
        _currentTab.value = tab
        _selectedAlbumName.value = null
        clearSelection()
    }

    fun selectAlbum(albumName: String) {
        _selectedAlbumName.value = albumName
        clearSelection()
    }

    fun backFromAlbum() {
        _selectedAlbumName.value = null
    }

    fun openFullScreen(photo: MediaItem, contextList: List<MediaItem>) {
        val index = contextList.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
        _fullScreenList.value = contextList
        _fullScreenIndex.value = index
    }

    fun setFullScreenIndex(index: Int) {
        val list = _fullScreenList.value
        if (index in list.indices) {
            _fullScreenIndex.value = index
        }
    }

    fun closeFullScreen() {
        stopSlideshow()
        _fullScreenIndex.value = null
        _isInfoSheetOpen.value = false
        _isFilterSheetOpen.value = false
    }

    fun toggleFavorite(photoId: String) {
        repository.toggleFavorite(photoId)
    }

    fun toggleSelectionMode(initialId: String? = null) {
        if (_isSelectionMode.value) {
            clearSelection()
        } else {
            _isSelectionMode.value = true
            _selectedPhotoIds.value = if (initialId != null) setOf(initialId) else emptySet()
        }
    }

    fun togglePhotoSelection(photoId: String) {
        val current = _selectedPhotoIds.value.toMutableSet()
        if (current.contains(photoId)) {
            current.remove(photoId)
        } else {
            current.add(photoId)
        }
        _selectedPhotoIds.value = current
        if (current.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun selectAll(photos: List<MediaItem>) {
        _isSelectionMode.value = true
        _selectedPhotoIds.value = photos.map { it.id }.toSet()
    }

    fun clearSelection() {
        _isSelectionMode.value = false
        _selectedPhotoIds.value = emptySet()
    }

    fun deleteSelectedPhotos() {
        val toDelete = _selectedPhotoIds.value
        if (toDelete.isNotEmpty()) {
            repository.deletePhotos(toDelete)
            _userNotice.value = "Deleted ${toDelete.size} photo(s)"
            clearSelection()
        }
    }

    fun deleteSinglePhoto(photoId: String) {
        repository.deletePhotos(setOf(photoId))
        val currentIdx = _fullScreenIndex.value
        val list = _fullScreenList.value
        if (list.size <= 1) {
            closeFullScreen()
        } else if (currentIdx != null) {
            val nextList = list.filterNot { it.id == photoId }
            _fullScreenList.value = nextList
            _fullScreenIndex.value = (currentIdx).coerceIn(0, (nextList.size - 1).coerceAtLeast(0))
        }
        _userNotice.value = "Photo deleted"
    }

    fun addImportedPhotos(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            val target = _selectedAlbumName.value ?: "Camera"
            repository.addImportedPhotos(uris, target)
            _userNotice.value = "Added ${uris.size} photo(s) to $target"
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setExploreTag(tag: String) {
        _selectedExploreTag.value = tag
    }

    fun cycleGridSpan() {
        _gridSpanCount.value = when (_gridSpanCount.value) {
            2 -> 3
            3 -> 4
            4 -> 2
            else -> 3
        }
    }

    fun setInfoSheetOpen(open: Boolean) {
        _isInfoSheetOpen.value = open
    }

    fun setFilterSheetOpen(open: Boolean) {
        _isFilterSheetOpen.value = open
    }

    fun applyFilterToCurrentPhoto(preset: FilterPreset) {
        val idx = _fullScreenIndex.value ?: return
        val list = _fullScreenList.value
        if (idx in list.indices) {
            val item = list[idx]
            val filterName = if (preset == FilterPreset.ORIGINAL) null else preset.name
            repository.applyFilterToPhoto(item.id, filterName)
            _userNotice.value = "Applied ${preset.displayName} filter"
        }
    }

    fun openNewAlbumDialog() {
        _pendingAlbumName.value = ""
        _isNewAlbumDialogOpen.value = true
    }

    fun closeNewAlbumDialog() {
        _isNewAlbumDialogOpen.value = false
    }

    fun setPendingAlbumName(name: String) {
        _pendingAlbumName.value = name
    }

    fun createNewAlbum() {
        val name = _pendingAlbumName.value.trim()
        if (name.isNotEmpty()) {
            val photosToAdd = _selectedPhotoIds.value
            repository.createAlbum(name, photosToAdd)
            _isNewAlbumDialogOpen.value = false
            _pendingAlbumName.value = ""
            clearSelection()
            _userNotice.value = "Album \"$name\" created"
        }
    }

    fun toggleSlideshow() {
        if (_isSlideshowActive.value) {
            stopSlideshow()
        } else {
            startSlideshow()
        }
    }

    private fun startSlideshow() {
        _isSlideshowActive.value = true
        slideshowJob?.cancel()
        slideshowJob = viewModelScope.launch {
            while (_isSlideshowActive.value) {
                delay(3000)
                val list = _fullScreenList.value
                val current = _fullScreenIndex.value ?: 0
                if (list.isNotEmpty()) {
                    _fullScreenIndex.value = (current + 1) % list.size
                }
            }
        }
    }

    private fun stopSlideshow() {
        _isSlideshowActive.value = false
        slideshowJob?.cancel()
        slideshowJob = null
    }

    fun clearNotice() {
        _userNotice.value = null
    }

    fun onBackPressHandled(): Boolean {
        if (_fullScreenIndex.value != null) {
            closeFullScreen()
            return true
        }
        if (_isInfoSheetOpen.value) {
            _isInfoSheetOpen.value = false
            return true
        }
        if (_isFilterSheetOpen.value) {
            _isFilterSheetOpen.value = false
            return true
        }
        if (_isSelectionMode.value) {
            clearSelection()
            return true
        }
        if (_selectedAlbumName.value != null) {
            backFromAlbum()
            return true
        }
        if (_currentTab.value != GalleryTab.PHOTOS) {
            selectTab(GalleryTab.PHOTOS)
            return true
        }
        return false
    }
}
