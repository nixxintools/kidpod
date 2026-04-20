package com.kidpod.app.domain.models

sealed class MediaItem {
    abstract val id: Long
    abstract val title: String
    abstract val filePath: String
    abstract val duration: Long

    data class Song(
        override val id: Long,
        override val title: String,
        override val filePath: String,
        override val duration: Long,
        val artist: String,
        val album: String
    ) : MediaItem()

    data class Audiobook(
        override val id: Long,
        override val title: String,
        override val filePath: String,
        override val duration: Long,
        val author: String,
        val currentPosition: Long = 0L,
        val lastPlayed: Long? = null
    ) : MediaItem()
}
