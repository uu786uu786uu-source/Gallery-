package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaItem
import com.example.ui.GalleryUiState
import com.example.ui.components.PhotoThumbnail

@Composable
fun ExploreScreen(
    uiState: GalleryUiState,
    onPhotoClick: (MediaItem, List<MediaItem>) -> Unit,
    onPhotoLongClick: (MediaItem) -> Unit,
    onSearchChange: (String) -> Unit,
    onTagSelect: (String) -> Unit,
    onTogglePhotoSelection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val query = uiState.searchQuery.trim().lowercase()
    val activeTag = uiState.selectedExploreTag

    val tags = listOf("All", "Favorites", "Nature", "Architecture", "Portraits", "Sunsets", "Beach")

    val filteredPhotos = remember(uiState.allPhotos, query, activeTag) {
        uiState.allPhotos.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.title.lowercase().contains(query) ||
                item.albumName.lowercase().contains(query) ||
                item.location.lowercase().contains(query) ||
                item.dateDisplay.lowercase().contains(query) ||
                item.tags.any { it.lowercase().contains(query) }

            val matchesTag = when (activeTag) {
                "All" -> true
                "Favorites" -> item.isFavorite
                "Nature" -> item.albumName.contains("Nature", ignoreCase = true) || item.tags.any { it.contains("nature") || it.contains("mountain") }
                "Architecture" -> item.albumName.contains("Architecture", ignoreCase = true) || item.tags.any { it.contains("city") }
                "Portraits" -> item.albumName.contains("Portraits", ignoreCase = true) || item.tags.any { it.contains("portrait") }
                "Sunsets" -> item.tags.any { it.contains("sunset") || it.contains("twilight") }
                "Beach" -> item.tags.any { it.contains("beach") || it.contains("ocean") || it.contains("island") }
                else -> true
            }

            matchesQuery && matchesTag
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search photos, places, dates…") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("explore_search_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tag Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tags) { tag ->
                val selected = tag == activeTag
                FilterChip(
                    selected = selected,
                    onClick = { onTagSelect(tag) },
                    label = { Text(tag) },
                    modifier = Modifier.testTag("tag_chip_$tag")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid Content
        LazyVerticalGrid(
            columns = GridCells.Fixed(uiState.gridSpanCount),
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("explore_photos_grid")
        ) {
            // Memory spotlight banner if no active search
            if (query.isEmpty() && activeTag == "All" && uiState.allPhotos.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val memoryPhoto = uiState.allPhotos.firstOrNull { it.isFavorite } ?: uiState.allPhotos.first()
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { onPhotoClick(memoryPhoto, uiState.allPhotos) }
                            .testTag("memory_spotlight_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            val model = memoryPhoto.uri ?: memoryPhoto.drawableResId
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(model)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Memory spotlight",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.3f),
                                                Color.Black.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = "FEATURED MEMORY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD54F)
                                    )
                                }
                                Text(
                                    text = memoryPhoto.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${memoryPhoto.location} • ${memoryPhoto.dateDisplay}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            if (filteredPhotos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No photos match \"${if (query.isNotEmpty()) query else activeTag}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = filteredPhotos,
                    key = { it.id }
                ) { photo ->
                    val isSelected = uiState.selectedPhotoIds.contains(photo.id)
                    PhotoThumbnail(
                        photo = photo,
                        isSelected = isSelected,
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                onTogglePhotoSelection(photo.id)
                            } else {
                                onPhotoClick(photo, filteredPhotos)
                            }
                        },
                        onLongClick = { onPhotoLongClick(photo) }
                    )
                }
            }
        }
    }
}
