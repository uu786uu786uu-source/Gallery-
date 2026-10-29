package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.R
import com.example.data.model.Album
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GalleryRepository(private val context: Context) {

    private val _photos = MutableStateFlow<List<MediaItem>>(emptyList())
    val photos: StateFlow<List<MediaItem>> = _photos.asStateFlow()

    private val _customAlbums = MutableStateFlow<List<String>>(
        listOf("Nature & Travel", "Architecture", "Portraits")
    )
    val customAlbums: StateFlow<List<String>> = _customAlbums.asStateFlow()

    init {
        loadInitialShowcase()
    }

    private fun loadInitialShowcase() {
        val now = System.currentTimeMillis()
        val dayMillis = 86_400_000L

        val initialItems = listOf(
            MediaItem(
                id = "sample_mountains",
                drawableResId = R.drawable.photo_nature_mountains,
                title = "Alpine Sunset Mirror",
                albumName = "Nature & Travel",
                dateTimestamp = now - 15 * 60 * 1000L,
                dateDisplay = "Today",
                location = "Swiss Alps, Valais",
                isFavorite = true,
                width = 3840,
                height = 2160,
                sizeBytes = 4_820_000L,
                cameraInfo = "Sony α7R V • 24mm • ƒ/2.8 • 1/500s • ISO 100",
                tags = listOf("mountains", "lake", "sunset", "reflection", "nature")
            ),
            MediaItem(
                id = "sample_city",
                drawableResId = R.drawable.photo_city_sunset,
                title = "Metropolis Twilight",
                albumName = "Architecture",
                dateTimestamp = now - 4 * 3600 * 1000L,
                dateDisplay = "Today",
                location = "Shinjuku, Tokyo",
                isFavorite = true,
                width = 3840,
                height = 2560,
                sizeBytes = 5_120_000L,
                cameraInfo = "Pixel 9 Pro • 50 MP • ƒ/1.68 • 1/60s • ISO 200",
                tags = listOf("city", "architecture", "night", "neon", "travel")
            ),
            MediaItem(
                id = "sample_beach",
                drawableResId = R.drawable.photo_travel_beach,
                title = "Lagoon Azure Paradise",
                albumName = "Nature & Travel",
                dateTimestamp = now - dayMillis,
                dateDisplay = "Yesterday",
                location = "Bora Bora, French Polynesia",
                isFavorite = false,
                width = 4032,
                height = 3024,
                sizeBytes = 6_340_000L,
                cameraInfo = "Canon EOS R5 • 35mm • ƒ/4.0 • 1/1000s • ISO 100",
                tags = listOf("beach", "ocean", "tropical", "summer", "island")
            ),
            MediaItem(
                id = "sample_portrait",
                drawableResId = R.drawable.photo_portrait_art,
                title = "Golden Hour Aura",
                albumName = "Portraits",
                dateTimestamp = now - 3 * dayMillis,
                dateDisplay = "This Week",
                location = "San Francisco, CA",
                isFavorite = true,
                width = 2800,
                height = 3733,
                sizeBytes = 3_920_000L,
                cameraInfo = "Fujifilm X-T5 • 56mm • ƒ/1.2 • 1/400s • ISO 160",
                tags = listOf("portrait", "people", "golden hour", "bokeh")
            )
        )

        _photos.value = initialItems
    }

    /**
     * Queries on-device MediaStore images if permission is available.
     */
    suspend fun queryDeviceMedia() = withContext(Dispatchers.IO) {
        val deviceMediaList = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val bucketColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val name = c.getString(nameColumn) ?: "Photo"
                    val dateAddedSeconds = c.getLong(dateColumn)
                    val dateAddedMillis = dateAddedSeconds * 1000L
                    val width = c.getInt(widthColumn).let { if (it > 0) it else 1920 }
                    val height = c.getInt(heightColumn).let { if (it > 0) it else 1080 }
                    val size = c.getLong(sizeColumn)
                    val bucketName = c.getString(bucketColumn) ?: "Camera"

                    deviceMediaList.add(
                        MediaItem(
                            id = "device_$id",
                            uri = contentUri,
                            title = name.substringBeforeLast('.'),
                            albumName = bucketName,
                            dateTimestamp = dateAddedMillis,
                            dateDisplay = dateFormat.format(Date(dateAddedMillis)),
                            location = "Device Storage",
                            isFavorite = false,
                            width = width,
                            height = height,
                            sizeBytes = size,
                            cameraInfo = "Device MediaStore • High Quality",
                            tags = listOf(bucketName.lowercase(), "device", "gallery")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Permission might not be granted yet, fallback gracefully to initial items
        }

        if (deviceMediaList.isNotEmpty()) {
            val current = _photos.value.toMutableList()
            // Keep showcase items that don't conflict
            val combined = (deviceMediaList + current).distinctBy { it.id }
            _photos.value = combined
        }
    }

    /**
     * Adds imported photos chosen via Android Photo Picker.
     */
    fun addImportedPhotos(uris: List<Uri>, targetAlbum: String = "Camera") {
        val now = System.currentTimeMillis()
        val newItems = uris.mapIndexed { index, uri ->
            MediaItem(
                id = "imported_${UUID.randomUUID()}",
                uri = uri,
                title = "Imported Photo ${index + 1}",
                albumName = targetAlbum,
                dateTimestamp = now - (index * 1000L),
                dateDisplay = "Today",
                location = "Photo Picker Import",
                isFavorite = false,
                width = 2400,
                height = 1600,
                sizeBytes = 2_800_000L,
                cameraInfo = "Imported from Android Photo Picker",
                tags = listOf("imported", targetAlbum.lowercase())
            )
        }
        _photos.value = newItems + _photos.value
    }

    fun toggleFavorite(photoId: String) {
        _photos.value = _photos.value.map { item ->
            if (item.id == photoId) {
                item.copy(isFavorite = !item.isFavorite)
            } else {
                item
            }
        }
    }

    fun deletePhotos(photoIds: Set<String>) {
        _photos.value = _photos.value.filterNot { it.id in photoIds }
    }

    fun applyFilterToPhoto(photoId: String, filterName: String?) {
        _photos.value = _photos.value.map { item ->
            if (item.id == photoId) {
                item.copy(filterName = filterName)
            } else {
                item
            }
        }
    }

    fun createAlbum(albumName: String, initialPhotoIds: Set<String> = emptySet()) {
        val trimmed = albumName.trim()
        if (trimmed.isEmpty()) return
        if (!_customAlbums.value.contains(trimmed)) {
            _customAlbums.value = _customAlbums.value + trimmed
        }
        if (initialPhotoIds.isNotEmpty()) {
            _photos.value = _photos.value.map { item ->
                if (item.id in initialPhotoIds) {
                    item.copy(albumName = trimmed)
                } else {
                    item
                }
            }
        }
    }
}
