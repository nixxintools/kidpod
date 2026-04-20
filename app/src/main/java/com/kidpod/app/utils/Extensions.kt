package com.kidpod.app.utils

import java.util.concurrent.TimeUnit

fun Long.toReadableDuration(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun String.toSafeFileName(): String =
    replace(Regex("[^a-zA-Z0-9._\\- ]"), "_").trim()

fun Int.clamp(min: Int, max: Int): Int = maxOf(min, minOf(max, this))
