package com.example.data.model

data class Album(
    val id: String,
    val name: String,
    val coverMedia: MediaItem? = null,
    val itemCount: Int = 0,
    val isSystem: Boolean = false,
    val subtitle: String = ""
)
