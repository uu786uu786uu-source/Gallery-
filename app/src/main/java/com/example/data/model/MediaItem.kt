package com.example.data.model

import android.net.Uri
import androidx.annotation.DrawableRes

data class MediaItem(
    val id: String,
    val uri: Uri? = null,
    @DrawableRes val drawableResId: Int? = null,
    val title: String,
    val albumName: String = "Camera",
    val dateTimestamp: Long = System.currentTimeMillis(),
    val dateDisplay: String = "Today",
    val location: String = "Local Device",
    val isFavorite: Boolean = false,
    val width: Int = 1920,
    val height: Int = 1080,
    val sizeBytes: Long = 2_450_000L,
    val cameraInfo: String = "Pixel 9 Pro • ƒ/1.68 • 1/250s • ISO 50",
    val tags: List<String> = emptyList(),
    val filterName: String? = null
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return String.format("%.1f MB", mb)
        }

    val dimensionsDisplay: String
        get() = "${width} × ${height}"
}
