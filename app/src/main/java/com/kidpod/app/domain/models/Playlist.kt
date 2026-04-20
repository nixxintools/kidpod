package com.kidpod.app.domain.models

data class Playlist(
    val id: Long,
    val name: String,
    val songs: List<MediaItem.Song> = emptyList()
)
