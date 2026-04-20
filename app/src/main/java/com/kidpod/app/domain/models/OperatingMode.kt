package com.kidpod.app.domain.models

enum class OperatingMode {
    OFFLINE,      // No internet — local content only (default, most restrictive)
    WHITELIST     // Specific apps allowed (Spotify Kids, Audible, etc.)
}
