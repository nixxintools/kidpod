# KidPod - Engineering PRD

## Executive Summary

Transform old Android phones into locked-down, distraction-free music and audiobook players for children. No new hardware required—pure software solution using Android's Launcher, Device Admin, and Lock Task Mode APIs.

**Problem**: Parents want kids to enjoy music/audiobooks without smartphone distractions (social media, games, browsers).

**Solution**: Software-based lockdown that makes old Android phones function like dedicated media players (think iPod, but software-only).

**Target Users**: Parents with kids ages 4-14 who have unused Android phones.

---

## Core Technical Requirements

### Lockdown Implementation

**Approach**: Launcher + Device Admin (no root required)

**Must Implement**:
1. Custom launcher replaces home screen (becomes default)
2. Device Admin API prevents:
   - App uninstallation
   - Launcher changes
   - Settings access (without parent PIN)
3. Lock Task Mode (kiosk mode) blocks:
   - Home button escape
   - Recent apps button
   - Status bar pull-down
   - Navigation gestures
4. App whitelist enforcement (parent-controlled)
5. Disable: browser, Play Store, developer options, ADB

**Accepted Limitations**:
- Tech-savvy kids can boot Safe Mode (hardware limitation)
- Recovery mode accessible (physical security required)
- NOT attempting system-level bypass prevention (requires root)

### Operating Modes

**Mode 1: Fully Offline (Default)**
- No internet connectivity
- Local content only
- Maximum lockdown

**Mode 2: Whitelisted Apps Allowed**
- Parent enables specific apps: Spotify Kids, Audible, Libby, etc.
- Internet limited to whitelisted apps only
- No browser, social media, or games EVER

### Media Playback

**Local Music**:
- Formats: MP3, FLAC, M4A, OGG
- Browse by: Artist, Album, All Songs
- Playlists (create/edit)
- Shuffle, repeat modes
- Background playback with lock screen controls

**Local Audiobooks**:
- Formats: M4B, MP3
- Chapter navigation (if metadata present)
- Playback speed: 0.75x, 1x, 1.25x, 1.5x, 2x
- Sleep timer: 15/30/45/60 minutes
- Auto-bookmark: save position every 30 seconds
- Resume from last position on next play

### Content Management

**USB Transfer Only (MVP)**:
- Phone appears as MTP device when connected via USB
- Parent creates folders:
  - `/KidPod/Music/` - music files
  - `/KidPod/Audiobooks/` - audiobook files
- App auto-scans on launch
- Metadata extracted from file tags
- Simple drag-and-drop from computer

### Parental Controls

**Access**: Long-press KidPod logo (3 seconds) → Enter PIN

**Parent Capabilities**:
- Set/change 4-6 digit PIN
- Choose operating mode (Offline vs. Whitelisted Apps)
- Add/remove whitelisted apps
- Set maximum volume limit (hearing protection)
- View listening history
- Manage playlists
- Delete content
- Exit lockdown mode temporarily
- Factory reset KidPod settings

---

## Technical Stack

### Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Repository pattern
- **Media Playback**: ExoPlayer (AndroidX Media3)
- **Database**: Room
- **Coroutines**: Kotlin Coroutines + Flow
- **Security**: EncryptedSharedPreferences

### Android APIs
- **DevicePolicyManager**: Device Admin enforcement
- **ActivityManager**: Lock Task Mode
- **MediaMetadataRetriever**: Extract file metadata
- **ContentResolver**: File system scanning
- **MediaSession**: Lock screen controls

### Target Platforms
- **Target SDK**: Android 13 (API 33)
- **Minimum SDK**: Android 8.0 (API 26)
- **Gradle**: 8.1.0+
- **Kotlin**: 1.9.0+

---

## Technical Architecture

### App Components

```
KidPod/
├── Presentation Layer (Jetpack Compose UI)
│   ├── screens/
│   │   ├── KidModeScreen.kt         # Main player UI (kid view)
│   │   ├── ParentModeScreen.kt      # Settings (PIN-locked)
│   │   ├── MusicScreen.kt           # Music library
│   │   ├── AudiobookScreen.kt       # Audiobook library
│   │   └── PlaylistScreen.kt        # Playlist management
│   └── components/
│       ├── PlayerControls.kt        # Playback UI component
│       ├── VolumeControl.kt         # Volume slider
│       └── PinDialog.kt             # PIN entry dialog
│
├── Domain Layer (Business Logic)
│   ├── usecases/
│   │   ├── PlayMediaUseCase.kt
│   │   ├── ScanContentUseCase.kt
│   │   ├── ManageWhitelistUseCase.kt
│   │   └── AuthenticateParentUseCase.kt
│   └── models/
│       ├── MediaItem.kt             # Song/audiobook model
│       ├── Playlist.kt
│       └── ParentSettings.kt
│
├── Data Layer
│   ├── repository/
│   │   ├── MediaRepository.kt       # Content management
│   │   ├── SettingsRepository.kt    # Parent settings
│   │   └── PlaybackRepository.kt    # Playback state
│   ├── database/
│   │   ├── KidPodDatabase.kt        # Room database
│   │   ├── MediaDao.kt              # Media queries
│   │   └── entities/
│   │       ├── SongEntity.kt
│   │       └── AudiobookEntity.kt
│   └── storage/
│       ├── ContentScanner.kt        # USB folder scanner
│       └── MetadataExtractor.kt     # File tag reader
│
├── Services
│   ├── MediaPlaybackService.kt      # Foreground service
│   ├── LockdownService.kt           # Maintains kiosk mode
│   └── ContentSyncService.kt        # Background scanning
│
├── Receivers
│   ├── DeviceAdminReceiver.kt       # Device Admin callbacks
│   └── BootReceiver.kt              # Auto-start on boot
│
└── MainActivity.kt                   # Launcher activity
```

### Data Flow

```
User Interaction (Compose UI)
    ↓
ViewModel (state management)
    ↓
UseCase (business logic)
    ↓
Repository (data abstraction)
    ↓
Data Sources (Room DB / File System / ExoPlayer)
```

---

## Key Technical Decisions

### 1. Launcher + Device Admin (No Root)
**Why**: 
- Works on any Android device without voiding warranty
- No technical expertise required from parents
- Acceptable security trade-off (not unbreakable, but "good enough")

**Trade-offs**:
- Can be bypassed by booting Safe Mode (hardware limitation)
- Relies on Android APIs that manufacturers might customize
- Less secure than custom ROM, but vastly more accessible

### 2. Jetpack Compose for UI
**Why**:
- Modern, declarative UI (faster development)
- Better performance than XML layouts
- Easier to create dynamic, reactive UIs
- Native support for material design

**Trade-offs**:
- Requires Android 8.0+ (acceptable for 2024)
- Slightly larger APK size vs. XML

### 3. ExoPlayer over MediaPlayer
**Why**:
- Better format support (FLAC, M4B, etc.)
- Seamless background playback
- Built-in MediaSession integration
- Active maintenance by Google

**Trade-offs**:
- Larger dependency size
- More complex API

### 4. USB-Only Content Transfer (MVP)
**Why**:
- Simplest parent UX (drag-and-drop files)
- No network complexity for MVP
- Works completely offline
- No cloud storage dependencies

**Trade-offs**:
- Requires USB cable + computer
- No remote content management
- Future: Add WiFi sync in V2

### 5. Room Database for Metadata
**Why**:
- Fast queries (filter by artist, album, etc.)
- Offline-first by design
- No need to re-scan files on every launch

**Trade-offs**:
- Must keep DB in sync with file system
- Adds complexity vs. scanning files every time

### 6. Lock Task Mode over Screen Pinning
**Why**:
- Programmatic control (don't rely on user enabling)
- Blocks more system actions
- Persistent across reboots

**Trade-offs**:
- Requires device owner or whitelisting
- Some devices may not support it

---

## Project Structure

```
KidPod/
├── app/
│   ├── build.gradle.kts              # App-level Gradle config
│   ├── proguard-rules.pro            # Code obfuscation rules
│   └── src/
│       ├── main/
│       │   ├── java/com/kidpod/app/
│       │   │   ├── MainActivity.kt
│       │   │   ├── KidPodApplication.kt
│       │   │   ├── ui/
│       │   │   │   ├── screens/      # Compose screens
│       │   │   │   ├── components/   # Reusable UI components
│       │   │   │   ├── theme/        # Material theme
│       │   │   │   └── viewmodels/   # ViewModels
│       │   │   ├── domain/
│       │   │   │   ├── usecases/
│       │   │   │   └── models/
│       │   │   ├── data/
│       │   │   │   ├── repository/
│       │   │   │   ├── database/
│       │   │   │   └── storage/
│       │   │   ├── service/
│       │   │   │   ├── MediaPlaybackService.kt
│       │   │   │   └── LockdownService.kt
│       │   │   ├── receiver/
│       │   │   │   ├── DeviceAdminReceiver.kt
│       │   │   │   └── BootReceiver.kt
│       │   │   └── utils/
│       │   │       ├── Constants.kt
│       │   │       ├── PermissionHelper.kt
│       │   │       └── Logger.kt
│       │   ├── res/
│       │   │   ├── drawable/         # Icons, images
│       │   │   ├── values/
│       │   │   │   ├── strings.xml
│       │   │   │   ├── colors.xml
│       │   │   │   └── themes.xml
│       │   │   └── xml/
│       │   │       └── device_admin.xml  # Device Admin config
│       │   └── AndroidManifest.xml
│       ├── androidTest/              # Instrumented tests
│       │   └── java/com/kidpod/app/
│       └── test/                     # Unit tests
│           └── java/com/kidpod/app/
├── build.gradle.kts                  # Project-level Gradle
├── settings.gradle.kts               # Gradle settings
├── gradle.properties                 # Gradle properties
├── gradlew                           # Gradle wrapper (Unix)
├── gradlew.bat                       # Gradle wrapper (Windows)
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── kidpod.md                         # This PRD
├── claude.md                         # Technical documentation
└── README.md                         # User-facing docs
```

---

## MVP Feature List

### Phase 1: Core Lockdown (Must Have)
- [ ] Custom launcher implementation
- [ ] Device Admin activation flow
- [ ] Lock Task Mode enforcement
- [ ] Parent PIN authentication (4-6 digits)
- [ ] Prevent app uninstallation
- [ ] Disable browser access
- [ ] Disable Play Store access
- [ ] Disable settings access (without PIN)
- [ ] Basic UI shell (Compose)

### Phase 2: Media Playback (Must Have)
- [ ] USB folder scanning (`/KidPod/Music/`, `/KidPod/Audiobooks/`)
- [ ] Metadata extraction (MediaMetadataRetriever)
- [ ] Room database setup (songs, audiobooks tables)
- [ ] ExoPlayer integration
- [ ] Music playback (play, pause, skip, shuffle, repeat)
- [ ] Audiobook playback with position saving
- [ ] Background playback (foreground service)
- [ ] Lock screen controls (MediaSession)
- [ ] Playback speed control (0.75x - 2x)
- [ ] Sleep timer (15/30/45/60 min)

### Phase 3: Parental Controls (Must Have)
- [ ] Operating mode selector (Offline vs. Whitelisted Apps)
- [ ] App whitelist manager
- [ ] Installed app scanner (PackageManager)
- [ ] Volume limit enforcement
- [ ] Listening history tracking
- [ ] Content rescan button
- [ ] Factory reset KidPod settings

### Phase 4: Kid UI (Must Have)
- [ ] Music screen (list of songs by artist/album)
- [ ] Audiobook screen (list with progress %)
- [ ] Now Playing card (large playback controls)
- [ ] Bottom navigation (Music | Audiobooks | Playlists)
- [ ] Large touch targets (60dp minimum)
- [ ] Kid-friendly theme (bright colors, simple icons)

### Phase 5: Polish & Testing (Must Have)
- [ ] Handle edge cases (no content, corrupted files, no permissions)
- [ ] Error messages (user-friendly)
- [ ] Loading states (scanning content, etc.)
- [ ] Multi-device testing (Android 8, 10, 12, 13)
- [ ] Bypass attempt testing (Safe Mode, Recovery Mode)
- [ ] Battery optimization (prevent background kills)
- [ ] Unit tests (critical business logic)
- [ ] UI tests (key user flows)

---

## Out of Scope (Not Building)

- ❌ Video playback
- ❌ Games or educational apps
- ❌ Messaging/texting
- ❌ Camera functionality
- ❌ Social features
- ❌ Web browser in any form
- ❌ WiFi content sync (future: V2)
- ❌ Cloud storage integration
- ❌ iOS version
- ❌ Custom ROM/rooting
- ❌ Advanced analytics dashboard

---

## User Stories with Acceptance Criteria

### Parent Stories

**Story 1**: Transform old phone into kid's music player
- **AC1**: App installs on Android 8.0+ device
- **AC2**: Device becomes fully locked down after setup
- **AC3**: Child cannot access browser, Play Store, or settings
- **AC4**: Parent can unlock with PIN

**Story 2**: Add music via USB
- **AC1**: Connect phone via USB → appears as MTP device
- **AC2**: Create `/KidPod/Music/` folder on computer
- **AC3**: Drag MP3 files into folder
- **AC4**: Disconnect USB → KidPod auto-scans → music appears in app

**Story 3**: Set volume limit
- **AC1**: Parent enters PIN → opens settings
- **AC2**: Slider sets max volume (0-100%)
- **AC3**: Child cannot exceed max volume
- **AC4**: Volume limit persists across app restarts

**Story 4**: Enable whitelisted apps
- **AC1**: Parent enters PIN → opens settings
- **AC2**: Select "Whitelisted Apps Mode"
- **AC3**: List shows installed apps (Spotify Kids, Audible, etc.)
- **AC4**: Check apps to allow → Save
- **AC5**: Child can launch only checked apps

### Child Stories

**Story 5**: Play favorite song
- **AC1**: Open app → see Music tab
- **AC2**: Scroll list of songs
- **AC3**: Tap song → starts playing immediately
- **AC4**: See playback controls (pause, skip, volume)

**Story 6**: Resume audiobook
- **AC1**: Open app → see Audiobooks tab
- **AC2**: Tap book title
- **AC3**: Playback resumes from last saved position
- **AC4**: Can adjust speed (0.75x - 2x)

**Story 7**: Use sleep timer
- **AC1**: Tap sleep timer icon
- **AC2**: Select duration (15/30/45/60 min)
- **AC3**: Playback stops after timer expires
- **AC4**: Book position is saved before stopping

---

## Technical Implementation Details

### Lockdown Implementation

**Step 1: Custom Launcher**

`AndroidManifest.xml`:
```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

**Step 2: Device Admin**

`res/xml/device_admin.xml`:
```xml
<device-admin xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-policies>
        <limit-password />
        <watch-login />
        <reset-password />
        <force-lock />
    </uses-policies>
</device-admin>
```

`DeviceAdminReceiver.kt`:
```kotlin
class KidPodDeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        // Device Admin enabled
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        // Prevent disabling without parent PIN
    }
}
```

**Step 3: Lock Task Mode**

`MainActivity.kt`:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Start Lock Task Mode
    val dpm = getSystemService(DevicePolicyManager::class.java)
    if (dpm.isLockTaskPermitted(packageName)) {
        startLockTask()
    }
}

// Exit Lock Task (parent mode only)
fun exitKioskMode() {
    stopLockTask()
}
```

**Step 4: Whitelist Enforcement**

```kotlin
// Override app launch attempts
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    
    if (intent?.action == Intent.ACTION_MAIN) {
        val targetPackage = intent.component?.packageName
        
        if (!isWhitelisted(targetPackage)) {
            Toast.makeText(this, "This app is not allowed", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Allow whitelisted app launch
        startActivity(intent)
    }
}

private fun isWhitelisted(packageName: String?): Boolean {
    val whitelistMode = settingsRepository.getOperatingMode()
    if (whitelistMode == OperatingMode.OFFLINE) return false
    
    val whitelist = settingsRepository.getWhitelistedApps()
    return whitelist.contains(packageName)
}
```

### Content Scanning

```kotlin
class ContentScanner(
    private val context: Context,
    private val mediaDao: MediaDao
) {
    suspend fun scanForNewContent() = withContext(Dispatchers.IO) {
        val musicDir = File(Environment.getExternalStorageDirectory(), "KidPod/Music")
        val audiobookDir = File(Environment.getExternalStorageDirectory(), "KidPod/Audiobooks")
        
        // Scan music
        val musicFiles = musicDir.walkTopDown()
            .filter { it.isFile && it.extension in listOf("mp3", "m4a", "flac", "ogg") }
            .toList()
        
        musicFiles.forEach { file ->
            val metadata = extractMetadata(file)
            val song = SongEntity(
                filePath = file.absolutePath,
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                duration = metadata.duration
            )
            mediaDao.insertSong(song)
        }
        
        // Similar for audiobooks...
    }
    
    private fun extractMetadata(file: File): MediaMetadata {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        
        return MediaMetadata(
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.nameWithoutExtension,
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown",
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown",
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        )
    }
}
```

### Playback Service

```kotlin
class MediaPlaybackService : Service() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    
    override fun onCreate() {
        super.onCreate()
        
        player = ExoPlayer.Builder(this).build()
        
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(PlaybackCallback())
            .build()
        
        // Foreground notification
        startForeground(NOTIFICATION_ID, buildNotification())
    }
    
    fun playMedia(mediaItem: MediaItem) {
        val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this))
            .createMediaSource(mediaItem)
        
        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()
    }
    
    // Auto-save audiobook position
    private fun startPositionSaver() {
        lifecycleScope.launch {
            while (player.isPlaying) {
                delay(30_000) // Every 30 seconds
                saveCurrentPosition()
            }
        }
    }
}
```

### Parent Authentication

```kotlin
class ParentAuthManager(private val context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        "parent_prefs",
        "kidpod_master_key",
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun setPin(pin: String) {
        val hashedPin = hashPin(pin)
        prefs.edit().putString("parent_pin", hashedPin).apply()
    }
    
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString("parent_pin", null) ?: return false
        return hashPin(pin) == storedHash
    }
    
    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
```

---

## Build & Deployment

### Prerequisites
- Android SDK (API 26-33)
- Gradle 8.1.0+
- Java 17+
- Kotlin 1.9.0+

### Environment Setup

```bash
# Set ANDROID_HOME
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Install SDK components
sdkmanager "platform-tools" "platforms;android-33" "build-tools;33.0.0"
```

### Build Commands

```bash
# Debug build (for testing)
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release build (for distribution)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk

# Run tests
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests

# Clean build
./gradlew clean build
```

### Signing Release APK

```bash
# Generate keystore (one-time)
keytool -genkey -v -keystore kidpod-release.keystore \
  -alias kidpod -keyalg RSA -keysize 2048 -validity 10000

# Sign APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore kidpod-release.keystore \
  app/build/outputs/apk/release/app-release-unsigned.apk kidpod

# Align APK
zipalign -v 4 app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/app-release.apk
```

### Installation

```bash
# Install via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Uninstall
adb uninstall com.kidpod.app
```

---

## Testing Strategy

### Unit Tests
- Business logic (UseCases, ViewModels)
- Data layer (Repositories, DAOs)
- Utility functions

```kotlin
@Test
fun `verify PIN authentication success`() {
    val authManager = ParentAuthManager(context)
    authManager.setPin("1234")
    
    assertTrue(authManager.verifyPin("1234"))
    assertFalse(authManager.verifyPin("0000"))
}
```

### Instrumented Tests
- Database operations
- Content scanning
- Device Admin activation
- Lock Task Mode

```kotlin
@Test
fun scanContentFindsAllMusicFiles() {
    // Create test files
    val testFile = File(musicDir, "test_song.mp3")
    testFile.writeBytes(byteArrayOf())
    
    // Run scanner
    val scanner = ContentScanner(context, mediaDao)
    scanner.scanForNewContent()
    
    // Verify
    val songs = mediaDao.getAllSongs()
    assertEquals(1, songs.size)
}
```

### Manual Testing Checklist
- [ ] Device Admin activation flow
- [ ] Lock Task Mode prevents escape
- [ ] Safe Mode boot (expected bypass)
- [ ] USB content transfer & scanning
- [ ] Music playback (play/pause/skip)
- [ ] Audiobook position saving
- [ ] Volume limit enforcement
- [ ] Whitelist mode (allow Spotify Kids)
- [ ] Parent PIN authentication
- [ ] Battery life during playback

---

## Known Issues & Workarounds

| Issue | Root Cause | Workaround |
|-------|------------|------------|
| Safe Mode disables lockdown | Android OS design | Physical device supervision |
| Recovery mode accessible | Hardware limitation | Keep device in common area |
| Battery drain on old devices | Hardware age | Suggest battery replacement |
| Some manufacturers customize Lock Task | OEM modifications | Test on multiple brands |
| MediaMetadataRetriever fails on corrupted files | File corruption | Catch exceptions, skip file |

---

## Performance Requirements

- **App launch**: <2 seconds to show UI
- **Content scan**: <10 seconds for 1000 files
- **Playback start**: <1 second after tap
- **Battery drain**: <5% per hour of playback
- **APK size**: <20 MB
- **RAM usage**: <150 MB during playback

---

## Security Considerations

1. **PIN Storage**: Use EncryptedSharedPreferences with AES-256
2. **Device Admin**: Cannot be disabled without parent PIN
3. **Lock Task**: Prevents system UI access
4. **File Access**: Only read from `/KidPod/` directories
5. **Network**: Disable completely in Offline mode

---

## Success Metrics

**Technical**:
- [ ] Builds successfully on Android 8.0-13
- [ ] <1% crash rate
- [ ] 95%+ lockdown effectiveness (parent surveys)
- [ ] Works on 5+ different phone brands

**User**:
- [ ] <15 min parent setup time
- [ ] 4.5+ star rating (if distributed)
- [ ] Average 45+ min daily child usage

---

## Future Enhancements (Post-MVP)

### V2 Features
- WiFi content sync (local network file transfer)
- Listening statistics dashboard
- Multiple user profiles (for multiple kids)
- Playlist UI editor
- Dark mode

### V3 Features
- Companion Android app (remote management)
- Integration with Libby/OverDrive APIs
- Bluetooth auto-pause on disconnect
- Export usage reports

---

## Questions for Implementation

1. **Device Owner Setup**: How to handle devices where Lock Task requires device owner? Auto-detect or manual guide?
2. **Content Folder Creation**: Auto-create `/KidPod/` folders on first launch or wait for parent?
3. **Metadata Fallback**: If file has no tags, use filename or "Unknown"?
4. **Playlist Format**: Store in Room DB or export as M3U files?
5. **Error Handling**: Show detailed errors or simple "Something went wrong"?
6. **Logging**: Enable crash reporting (Firebase Crashlytics) or fully offline?

---

## Getting Started (For Developers)

1. Clone/create project: `mkdir KidPod && cd KidPod`
2. Initialize Gradle: Follow standard Android Gradle structure
3. Set up build files (see `claude.md` for full configs)
4. Create package structure: `com.kidpod.app`
5. Implement in order: Lockdown → Media → Parental Controls → Polish
6. Test on real devices (emulators don't support Device Admin well)
7. Build APK: `./gradlew assembleDebug`

---

**Ready for implementation. Refer to `claude.md` for detailed technical configurations and coding standards.**
