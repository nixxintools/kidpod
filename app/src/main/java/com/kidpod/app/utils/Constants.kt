package com.kidpod.app.utils

object Constants {
    const val TAG = "KidPod"

    // Folders (relative to external storage root)
    const val KIDPOD_DIR = "KidPod"
    const val MUSIC_DIR = "KidPod/Music"
    const val AUDIOBOOKS_DIR = "KidPod/Audiobooks"

    // Supported audio formats
    val MUSIC_EXTENSIONS = setOf("mp3", "m4a", "flac", "ogg", "wav")
    val AUDIOBOOK_EXTENSIONS = setOf("m4b", "mp3", "m4a")

    // Prefs
    const val PREFS_NAME = "kidpod_secure_prefs"
    const val PREF_KEY_PIN = "parent_pin_hash"
    const val PREF_KEY_PIN_SET = "parent_pin_is_set"
    const val PREF_KEY_OPERATING_MODE = "operating_mode"
    const val PREF_KEY_VOLUME_LIMIT = "max_volume_percent"
    const val PREF_KEY_SETUP_COMPLETE = "setup_complete"
    const val PREF_KEY_WHITELISTED_APPS = "whitelisted_apps"

    // Default values
    const val DEFAULT_MAX_VOLUME = 80
    const val MIN_PIN_LENGTH = 4
    const val MAX_PIN_LENGTH = 6
    const val MAX_PIN_ATTEMPTS = 5
    const val PIN_LOCKOUT_MILLIS = 60_000L

    // Notifications
    const val NOTIF_CHANNEL_LOCKDOWN = "kidpod_lockdown"
    const val NOTIF_CHANNEL_PLAYBACK = "kidpod_playback"
    const val NOTIF_ID_LOCKDOWN = 1001
    const val NOTIF_ID_PLAYBACK = 1002

    // Position save interval for audiobooks (ms)
    const val AUDIOBOOK_SAVE_INTERVAL_MS = 30_000L
}
