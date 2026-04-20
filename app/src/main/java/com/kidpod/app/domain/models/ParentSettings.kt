package com.kidpod.app.domain.models

data class ParentSettings(
    val operatingMode: OperatingMode = OperatingMode.OFFLINE,
    val maxVolumePercent: Int = 80,
    val whitelistedApps: Set<String> = emptySet(),
    val isPinSet: Boolean = false
)
