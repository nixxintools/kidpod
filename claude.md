# KidPod - Technical Documentation

## Project Overview

**Project Name**: KidPod

**Purpose**: Transform old Android phones into locked-down, distraction-free music and audiobook players for children through pure software (no hardware purchase required).

**Problem Solved**: Parents want kids to enjoy music/audiobooks without smartphone distractions (social media, games, browsers, YouTube). Buying dedicated hardware is expensive. KidPod is a software-only solution.

**Target Users**: 
- Primary: Parents with kids ages 4-14
- Secondary: Kids using the locked-down device
- Device: Old/unused Android phones (Android 8.0+)

---

## Technical Stack

### Core Technologies
- **Language**: Kotlin (100%)
- **Build System**: Gradle 8.1.0+ with Kotlin DSL
- **Target SDK**: Android 13 (API 33)
- **Minimum SDK**: Android 8.0 (API 26)
- **Java Version**: Java 17

### UI Framework
- **UI Toolkit**: Jetpack Compose (declarative UI)
- **Material Design**: Material 3 (Material You)
- **Navigation**: Compose Navigation
- **Theme**: Custom kid-friendly theme with large touch targets

### Architecture
- **Pattern**: MVVM (Model-View-ViewModel) + Repository
- **Dependency Injection**: Manual (keep MVP simple, add Hilt later if needed)
- **Reactive Programming**: Kotlin Coroutines + Flow
- **State Management**: Compose State + ViewModels

### Key Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.compose:compose-bom` | 2023.08.00 | Compose UI framework |
| `androidx.media3:media3-exoplayer` | 1.1.1 | Media playback engine |
| `androidx.media3:media3-ui` | 1.1.1 | Media playback UI components |
| `androidx.room:room-runtime` | 2.5.2 | Local database (SQLite) |
| `androidx.room:room-ktx` | 2.5.2 | Coroutines support for Room |
| `androidx.security:security-crypto` | 1.1.0-alpha06 | EncryptedSharedPreferences |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.6.1 | Lifecycle-aware components |
| `androidx.activity:activity-compose` | 1.7.2 | Compose integration with Activity |

### Android System APIs
- **DevicePolicyManager**: Enforce Device Admin policies (lockdown)
- **ActivityManager**: Lock Task Mode (kiosk mode)
- **MediaMetadataRetriever**: Extract metadata from audio files
- **ContentResolver**: Scan file system for media files
- **MediaSession**: Lock screen media controls
- **PackageManager**: Detect installed apps (for whitelist)

---

## Architecture & Design Decisions

### 1. MVVM + Repository Pattern

**Why**:
- Clear separation of concerns (UI, business logic, data)
- Testable (mock repositories in tests)
- Reactive with Kotlin Flow (data flows from DB → UI automatically)
- Standard Android architecture (well-documented)

**Structure**:
```
UI (Compose) → ViewModel → UseCase → Repository → Data Source
```

**Example**:
```kotlin
// UI Layer (Compose)
@Composable
fun MusicScreen(viewModel: MusicViewModel = viewModel()) {
    val songs by viewModel.songs.collectAsState()
    LazyColumn {
        items(songs) { song -> SongItem(song) }
    }
}

// ViewModel
class MusicViewModel(private val repository: MediaRepository) : ViewModel() {
    val songs: StateFlow<List<Song>> = repository.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

// Repository
class MediaRepository(private val mediaDao: MediaDao) {
    fun getAllSongs(): Flow<List<Song>> = mediaDao.getAllSongs()
}

// Data Source (Room DAO)
@Dao
interface MediaDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>
}
```

### 2. Lockdown Approach: Launcher + Device Admin (No Root)

**Why**:
- **Accessibility**: Works on any Android phone without rooting
- **No Warranty Void**: Parents don't need technical knowledge
- **Good Enough Security**: Blocks 95%+ of casual bypass attempts
- **Acceptable Trade-off**: Not unbreakable, but realistic for the use case

**What This Enables**:
- Custom launcher replaces home screen
- Device Admin prevents uninstallation
- Lock Task Mode blocks escape routes (home button, recent apps, status bar)
- App whitelist enforcement

**What This CANNOT Do** (Accepted Limitations):
- Block Safe Mode boot (hardware limitation)
- Block Recovery Mode access (physical security needed)
- Prevent factory reset from recovery (need physical device supervision)
- 100% guarantee no bypass (tech-savvy teens might find ways)

**Technical Implementation**:
```kotlin
// MainActivity as Launcher
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>

// Start Lock Task Mode
startLockTask()  // Blocks home/recent/status bar

// Device Admin prevents uninstallation
DevicePolicyManager.setUninstallBlocked(componentName, packageName, true)
```

### 3. ExoPlayer Over MediaPlayer

**Why**:
- **Better Format Support**: MP3, FLAC, M4A, M4B, OGG (MediaPlayer limited)
- **Seamless Background Playback**: Built-in foreground service support
- **MediaSession Integration**: Lock screen controls work out-of-box
- **Active Maintenance**: Google-maintained, regular updates
- **Better Error Handling**: Graceful handling of corrupted files

**Trade-off**: 
- Larger APK size (~2-3 MB added)
- More complex API than MediaPlayer

**Rationale**: Better UX and fewer bugs worth the extra size.

### 4. Room Database for Metadata

**Why Store Metadata in DB**:
- **Performance**: Fast queries (filter by artist, album, search)
- **Offline-First**: No need to re-scan 1000+ files every launch
- **Stateful**: Track audiobook progress, playlists, listening history

**Why Not Just Scan Files Every Time**:
- Scanning 1000 MP3s = 10-30 seconds (bad UX)
- No way to track playback position without DB
- No fast filtering/sorting without indexed queries

**Schema**:
```kotlin
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,  // milliseconds
    val dateAdded: Long
)

@Entity(tableName = "audiobooks")
data class AudiobookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val title: String,
    val author: String,
    val duration: Long,
    val currentPosition: Long = 0L,  // Resume position
    val lastPlayed: Long? = null
)
```

### 5. USB-Only Content Transfer (MVP)

**Why USB-Only for MVP**:
- **Simplest Parent UX**: Drag-and-drop files like a USB drive
- **No Network Complexity**: Works completely offline
- **No Cloud Dependencies**: No accounts, no sync servers
- **Universal**: Every computer has USB, every Android has MTP

**Why Not WiFi Sync (Yet)**:
- Adds complexity (local network discovery, file transfer protocol)
- Requires internet permission (conflicts with Offline mode)
- V2 feature after MVP validation

**How It Works**:
1. Parent connects phone via USB to computer
2. Phone appears as MTP device (standard Android)
3. Parent navigates to `/KidPod/Music/` or `/KidPod/Audiobooks/`
4. Drag-and-drop MP3/M4A/FLAC files
5. Disconnect USB
6. KidPod auto-scans folders on next launch
7. New files appear in library

### 6. Jetpack Compose for UI

**Why Compose Over XML**:
- **Faster Development**: Declarative UI = less boilerplate
- **Better Performance**: Smart recomposition (only updates changed UI)
- **Modern Tooling**: Live previews, better IDE support
- **Easier State Management**: State hoisting, remember, derivedStateOf

**Trade-off**:
- Requires Android 8.0+ (acceptable in 2024)
- Team must learn Compose (but it's now Android standard)

**Compose Principles We Follow**:
- **Unidirectional Data Flow**: State flows down, events flow up
- **Single Source of Truth**: State in ViewModel, UI observes
- **Stateless Composables**: Pass state as parameters, hoist state up

### 7. EncryptedSharedPreferences for PIN Storage

**Why Encrypted**:
- Parent PIN must be secure (kids shouldn't read plaintext)
- Uses Android Keystore (hardware-backed encryption)
- Simple API (same as regular SharedPreferences)

**Implementation**:
```kotlin
val prefs = EncryptedSharedPreferences.create(
    "parent_prefs",
    MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build(),
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// Store hashed PIN (double security)
fun setPin(pin: String) {
    val hashedPin = MessageDigest.getInstance("SHA-256")
        .digest(pin.toByteArray())
        .joinToString("") { "%02x".format(it) }
    prefs.edit().putString("parent_pin", hashedPin).apply()
}
```

---

## File Structure & Organization

### Standard Android Gradle Project Structure

```
KidPod/
├── app/                                  # Main application module
│   ├── build.gradle.kts                  # App-level build configuration
│   ├── proguard-rules.pro                # ProGuard rules (code obfuscation)
│   └── src/
│       ├── main/
│       │   ├── java/com/kidpod/app/      # Kotlin source code
│       │   │   ├── KidPodApplication.kt  # Application class
│       │   │   ├── MainActivity.kt       # Main launcher activity
│       │   │   │
│       │   │   ├── ui/                   # Presentation layer
│       │   │   │   ├── screens/          # Full-screen Composables
│       │   │   │   │   ├── KidModeScreen.kt
│       │   │   │   │   ├── ParentModeScreen.kt
│       │   │   │   │   ├── MusicScreen.kt
│       │   │   │   │   ├── AudiobookScreen.kt
│       │   │   │   │   └── PlaylistScreen.kt
│       │   │   │   ├── components/       # Reusable UI components
│       │   │   │   │   ├── PlayerControls.kt
│       │   │   │   │   ├── VolumeControl.kt
│       │   │   │   │   ├── PinDialog.kt
│       │   │   │   │   └── MediaItem.kt
│       │   │   │   ├── theme/            # Material Theme
│       │   │   │   │   ├── Color.kt
│       │   │   │   │   ├── Theme.kt
│       │   │   │   │   └── Type.kt
│       │   │   │   └── viewmodels/       # ViewModels (state management)
│       │   │   │       ├── MusicViewModel.kt
│       │   │   │       ├── AudiobookViewModel.kt
│       │   │   │       └── ParentViewModel.kt
│       │   │   │
│       │   │   ├── domain/               # Business logic layer
│       │   │   │   ├── usecases/         # Use cases (single responsibility)
│       │   │   │   │   ├── PlayMediaUseCase.kt
│       │   │   │   │   ├── ScanContentUseCase.kt
│       │   │   │   │   ├── ManageWhitelistUseCase.kt
│       │   │   │   │   └── AuthenticateParentUseCase.kt
│       │   │   │   └── models/           # Domain models (clean, simple data classes)
│       │   │   │       ├── MediaItem.kt
│       │   │   │       ├── Playlist.kt
│       │   │   │       ├── ParentSettings.kt
│       │   │   │       └── OperatingMode.kt
│       │   │   │
│       │   │   ├── data/                 # Data layer
│       │   │   │   ├── repository/       # Repositories (data abstraction)
│       │   │   │   │   ├── MediaRepository.kt
│       │   │   │   │   ├── SettingsRepository.kt
│       │   │   │   │   └── PlaybackRepository.kt
│       │   │   │   ├── database/         # Room database
│       │   │   │   │   ├── KidPodDatabase.kt
│       │   │   │   │   ├── dao/
│       │   │   │   │   │   ├── MediaDao.kt
│       │   │   │   │   │   ├── PlaylistDao.kt
│       │   │   │   │   │   └── HistoryDao.kt
│       │   │   │   │   └── entities/     # Room entities (DB tables)
│       │   │   │   │       ├── SongEntity.kt
│       │   │   │   │       ├── AudiobookEntity.kt
│       │   │   │   │       ├── PlaylistEntity.kt
│       │   │   │   │       └── PlaylistSongCrossRef.kt
│       │   │   │   └── storage/          # File system operations
│       │   │   │       ├── ContentScanner.kt
│       │   │   │       └── MetadataExtractor.kt
│       │   │   │
│       │   │   ├── service/              # Android Services
│       │   │   │   ├── MediaPlaybackService.kt  # Foreground service for playback
│       │   │   │   ├── LockdownService.kt       # Maintains kiosk mode
│       │   │   │   └── ContentSyncService.kt    # Background content scanning
│       │   │   │
│       │   │   ├── receiver/             # Broadcast Receivers
│       │   │   │   ├── DeviceAdminReceiver.kt   # Device Admin callbacks
│       │   │   │   └── BootReceiver.kt          # Auto-start on boot
│       │   │   │
│       │   │   └── utils/                # Utility classes
│       │   │       ├── Constants.kt      # App constants
│       │   │       ├── PermissionHelper.kt
│       │   │       ├── Logger.kt
│       │   │       └── Extensions.kt     # Kotlin extensions
│       │   │
│       │   ├── res/                      # Resources
│       │   │   ├── drawable/             # Icons, images (vector XML preferred)
│       │   │   ├── values/
│       │   │   │   ├── strings.xml       # All user-facing text (for i18n)
│       │   │   │   ├── colors.xml        # Color palette
│       │   │   │   └── themes.xml        # Material themes
│       │   │   └── xml/
│       │   │       └── device_admin.xml  # Device Admin configuration
│       │   │
│       │   └── AndroidManifest.xml       # App manifest (permissions, components)
│       │
│       ├── androidTest/                  # Instrumented tests (run on device)
│       │   └── java/com/kidpod/app/
│       │       ├── DatabaseTest.kt
│       │       ├── ContentScannerTest.kt
│       │       └── LockdownTest.kt
│       │
│       └── test/                         # Unit tests (run on JVM)
│           └── java/com/kidpod/app/
│               ├── ViewModelTest.kt
│               ├── RepositoryTest.kt
│               └── UseCaseTest.kt
│
├── build.gradle.kts                      # Project-level Gradle config
├── settings.gradle.kts                   # Gradle settings
├── gradle.properties                     # Gradle properties
├── gradlew                               # Gradle wrapper (Unix/Mac)
├── gradlew.bat                           # Gradle wrapper (Windows)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── kidpod.md                             # Product Requirements Document
├── claude.md                             # This file (technical docs)
└── README.md                             # User-facing documentation
```

### File Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Kotlin Files | PascalCase | `MusicViewModel.kt` |
| Composables | PascalCase | `PlayerControls.kt` |
| Activities | `*Activity.kt` | `MainActivity.kt` |
| Fragments | `*Fragment.kt` | (Not using fragments) |
| Services | `*Service.kt` | `MediaPlaybackService.kt` |
| Receivers | `*Receiver.kt` | `DeviceAdminReceiver.kt` |
| ViewModels | `*ViewModel.kt` | `MusicViewModel.kt` |
| Repositories | `*Repository.kt` | `MediaRepository.kt` |
| Use Cases | `*UseCase.kt` | `PlayMediaUseCase.kt` |
| DAOs | `*Dao.kt` | `MediaDao.kt` |
| Entities | `*Entity.kt` | `SongEntity.kt` |
| XML Resources | snake_case | `device_admin.xml` |

---

## Coding Conventions & Style

### Kotlin Style Guide

**Follow Official Kotlin Coding Conventions**: https://kotlinlang.org/docs/coding-conventions.html

**Key Rules**:
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: 120 characters max
- **Braces**: Same line for `if`, `for`, `while`, etc.
- **Naming**:
  - Classes: `PascalCase`
  - Functions: `camelCase`
  - Constants: `SCREAMING_SNAKE_CASE`
  - Private vars: `_camelCase` (if backing property)

**Example**:
```kotlin
class MediaRepository(
    private val mediaDao: MediaDao,
    private val contentScanner: ContentScanner
) {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    
    suspend fun refreshContent() {
        val newSongs = contentScanner.scanForSongs()
        mediaDao.insertAll(newSongs)
        _songs.value = newSongs
    }
    
    companion object {
        private const val TAG = "MediaRepository"
        const val MAX_SONGS = 10_000
    }
}
```

### Compose Best Practices

**1. Stateless Composables**
```kotlin
// ❌ BAD: Stateful composable (hard to test, reuse)
@Composable
fun PlayerControls() {
    var isPlaying by remember { mutableStateOf(false) }
    Button(onClick = { isPlaying = !isPlaying }) {
        Text(if (isPlaying) "Pause" else "Play")
    }
}

// ✅ GOOD: Stateless composable (easy to test, reuse)
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onPlayPause, modifier = modifier) {
        Text(if (isPlaying) "Pause" else "Play")
    }
}
```

**2. Unidirectional Data Flow**
```kotlin
// ViewModel holds state, UI observes
class MusicViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()
    
    fun onSongClick(song: Song) {
        // Handle event, update state
    }
}

// UI observes state, sends events
@Composable
fun MusicScreen(viewModel: MusicViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn {
        items(uiState.songs) { song ->
            SongItem(
                song = song,
                onClick = { viewModel.onSongClick(song) }  // Event up
            )
        }
    }
}
```

**3. Use `remember` for Expensive Computations**
```kotlin
@Composable
fun SongList(songs: List<Song>) {
    val sortedSongs = remember(songs) {
        songs.sortedBy { it.title }  // Only recompute when songs change
    }
}
```

### Coroutines Best Practices

**1. Use Appropriate Dispatchers**
```kotlin
// ❌ BAD: Blocking main thread
fun loadSongs() {
    val songs = mediaDao.getAllSongs()  // Blocks UI
}

// ✅ GOOD: Use IO dispatcher for DB/file operations
suspend fun loadSongs() = withContext(Dispatchers.IO) {
    val songs = mediaDao.getAllSongs()
}
```

**2. Handle Errors with Try-Catch**
```kotlin
suspend fun scanContent() {
    try {
        val files = contentScanner.scan()
        mediaDao.insertAll(files)
    } catch (e: IOException) {
        Log.e(TAG, "Failed to scan content", e)
        _error.value = "Could not scan files"
    }
}
```

**3. Use `viewModelScope` for ViewModel Coroutines**
```kotlin
class MusicViewModel : ViewModel() {
    fun loadSongs() {
        viewModelScope.launch {  // Automatically cancelled when ViewModel cleared
            repository.getAllSongs().collect { songs ->
                _uiState.value = _uiState.value.copy(songs = songs)
            }
        }
    }
}
```

### Room Database Conventions

**1. Use Suspend Functions for Write Operations**
```kotlin
@Dao
interface MediaDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<SongEntity>>  // Flow for reactive reads
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)  // Suspend for writes
    
    @Delete
    suspend fun deleteSong(song: SongEntity)
}
```

**2. Use Entities for Database, Models for UI**
```kotlin
// Database layer (Room Entity)
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val title: String,
    val artist: String
)

// Domain layer (UI Model)
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val filePath: String
)

// Map in Repository
fun SongEntity.toSong() = Song(id, title, artist, filePath)
```

---

## Design Patterns Used

### 1. Repository Pattern

**Purpose**: Abstract data sources (DB, file system, network) from ViewModels.

**Structure**:
```
ViewModel → Repository → [Data Source 1, Data Source 2, ...]
```

**Example**:
```kotlin
class MediaRepository(
    private val mediaDao: MediaDao,
    private val contentScanner: ContentScanner
) {
    // Expose data as Flow (reactive)
    fun getAllSongs(): Flow<List<Song>> = 
        mediaDao.getAllSongs().map { entities ->
            entities.map { it.toSong() }
        }
    
    // Encapsulate complex logic
    suspend fun refreshContent() {
        val newFiles = contentScanner.scan()
        val songs = newFiles.map { it.toEntity() }
        mediaDao.insertAll(songs)
    }
}
```

### 2. Use Case Pattern

**Purpose**: Encapsulate single business logic operations (Single Responsibility Principle).

**Structure**:
```kotlin
class PlayMediaUseCase(
    private val repository: MediaRepository,
    private val playbackService: MediaPlaybackService
) {
    suspend operator fun invoke(mediaItem: MediaItem) {
        // Business logic here
        repository.markAsPlayed(mediaItem)
        playbackService.play(mediaItem)
        
        if (mediaItem is Audiobook) {
            repository.savePosition(mediaItem.id, 0L)
        }
    }
}
```

### 3. Observer Pattern (via Kotlin Flow)

**Purpose**: UI automatically updates when data changes.

**Example**:
```kotlin
// Repository exposes Flow
class MediaRepository {
    fun getAllSongs(): Flow<List<Song>> = mediaDao.getAllSongs()
}

// ViewModel observes Flow
class MusicViewModel(repo: MediaRepository) : ViewModel() {
    val songs: StateFlow<List<Song>> = repo.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

// UI observes StateFlow
@Composable
fun MusicScreen(viewModel: MusicViewModel) {
    val songs by viewModel.songs.collectAsState()
    // UI automatically updates when songs change
}
```

### 4. Factory Pattern

**Purpose**: Create complex objects (e.g., ExoPlayer, Room database).

**Example**:
```kotlin
object ExoPlayerFactory {
    fun create(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
```

---

## Common Workflows & Tasks

### Workflow 1: Add a New Screen

1. Create Composable in `ui/screens/`:
   ```kotlin
   @Composable
   fun NewScreen(viewModel: NewViewModel = viewModel()) {
       // UI code
   }
   ```

2. Create ViewModel in `ui/viewmodels/`:
   ```kotlin
   class NewViewModel : ViewModel() {
       private val _uiState = MutableStateFlow(NewUiState())
       val uiState = _uiState.asStateFlow()
   }
   ```

3. Add navigation route in `MainActivity.kt`:
   ```kotlin
   composable("new_screen") { NewScreen() }
   ```

### Workflow 2: Add Database Table

1. Create Entity in `data/database/entities/`:
   ```kotlin
   @Entity(tableName = "table_name")
   data class NewEntity(
       @PrimaryKey(autoGenerate = true) val id: Long = 0,
       val field: String
   )
   ```

2. Create DAO in `data/database/dao/`:
   ```kotlin
   @Dao
   interface NewDao {
       @Query("SELECT * FROM table_name")
       fun getAll(): Flow<List<NewEntity>>
   }
   ```

3. Add to `KidPodDatabase.kt`:
   ```kotlin
   @Database(entities = [SongEntity::class, NewEntity::class], version = 2)
   abstract class KidPodDatabase : RoomDatabase() {
       abstract fun newDao(): NewDao
   }
   ```

4. Increment database version and add migration.

### Workflow 3: Build & Test

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep "KidPod"
```

### Workflow 4: Add New Dependency

1. Add to `app/build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation("androidx.work:work-runtime-ktx:2.8.1")
   }
   ```

2. Sync Gradle: `./gradlew build --refresh-dependencies`

3. Use in code.

---

## Things to Avoid

### ❌ Don't Use These Patterns

**1. Don't Put Business Logic in Composables**
```kotlin
// ❌ BAD
@Composable
fun MusicScreen() {
    LaunchedEffect(Unit) {
        val songs = contentScanner.scan()  // Business logic in UI
        mediaDao.insertAll(songs)
    }
}

// ✅ GOOD: Business logic in ViewModel/UseCase
@Composable
fun MusicScreen(viewModel: MusicViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadSongs()  // Delegate to ViewModel
    }
}
```

**2. Don't Block Main Thread**
```kotlin
// ❌ BAD
fun loadSongs() {
    val songs = File("/sdcard/Music").listFiles()  // Blocks UI
}

// ✅ GOOD
suspend fun loadSongs() = withContext(Dispatchers.IO) {
    val songs = File("/sdcard/Music").listFiles()
}
```

**3. Don't Hardcode Strings**
```kotlin
// ❌ BAD
Text("Play Music")

// ✅ GOOD
Text(stringResource(R.string.play_music))
```

**4. Don't Ignore Errors**
```kotlin
// ❌ BAD
try {
    mediaDao.insertSong(song)
} catch (e: Exception) {
    // Silent failure
}

// ✅ GOOD
try {
    mediaDao.insertSong(song)
} catch (e: Exception) {
    Log.e(TAG, "Failed to insert song", e)
    _error.value = "Could not add song"
}
```

**5. Don't Use `GlobalScope` for Coroutines**
```kotlin
// ❌ BAD
GlobalScope.launch {  // Never cancelled, memory leak
    loadSongs()
}

// ✅ GOOD
viewModelScope.launch {  // Auto-cancelled
    loadSongs()
}
```

---

## Constraints & Requirements

### Performance Constraints
- **App Launch**: <2 seconds to show UI
- **Content Scan**: <10 seconds for 1000 files
- **Playback Start**: <1 second after tap
- **Battery Drain**: <5% per hour of playback
- **APK Size**: <20 MB
- **RAM Usage**: <150 MB during playback

### Offline-First Design
- **Must Work Without Internet**: Core functionality (music/audiobook playback) requires zero connectivity
- **Optional Internet**: Only for whitelisted apps mode (Spotify Kids, etc.)
- **No Cloud Dependencies**: No accounts, no sync servers, no API calls

### Platform-Specific Constraints
- **Android 8.0+ Only**: Minimum SDK 26 (Oreo)
- **No Root Required**: Must work on unrooted devices
- **Lock Task Mode**: Requires device owner or admin whitelisting (guide users)
- **Safe Mode Bypass**: Accepted limitation (document in parent guide)

### Security Requirements
- **PIN Encryption**: Use EncryptedSharedPreferences + SHA-256 hashing
- **No Plaintext Storage**: Never store sensitive data in plaintext
- **File Access**: Restrict to `/KidPod/` directories only
- **Network**: Completely disabled in Offline mode

### Accessibility Requirements
- **Large Touch Targets**: Minimum 60dp for kid UI
- **High Contrast**: Text must meet WCAG AA contrast ratios
- **Screen Reader**: Label all UI elements with `contentDescription`

---

## Build & Deployment

### Prerequisites Checklist
- [ ] Android SDK installed (API 26-33)
- [ ] Gradle 8.1.0+ installed
- [ ] Java 17+ installed
- [ ] `ANDROID_HOME` environment variable set
- [ ] USB debugging enabled on test device

### Build Commands

```bash
# Clean build
./gradlew clean

# Build debug APK (for testing)
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build release APK (for distribution)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk

# Run unit tests
./gradlew test
./gradlew test --tests "com.kidpod.app.*"  # Specific package

# Run instrumented tests (requires connected device)
./gradlew connectedAndroidTest

# Check for lint errors
./gradlew lint

# Generate test coverage report
./gradlew jacocoTestReport
```

### Signing Release APK

```bash
# One-time: Generate keystore
keytool -genkey -v -keystore kidpod-release.keystore \
  -alias kidpod -keyalg RSA -keysize 2048 -validity 10000

# Sign APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore kidpod-release.keystore \
  app/build/outputs/apk/release/app-release-unsigned.apk kidpod

# Align APK (optimize for Android)
zipalign -v 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/app-release.apk
```

### Installation & Testing

```bash
# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# Install with replacing existing app
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Uninstall
adb uninstall com.kidpod.app

# View logs
adb logcat | grep "KidPod"

# Clear app data (for testing setup flow)
adb shell pm clear com.kidpod.app
```

---

## Testing Strategy

### Unit Tests (`test/` directory)
**What to Test**:
- ViewModels (state management, business logic)
- Use Cases (business logic)
- Repositories (data transformations, error handling)
- Utility functions

**Example**:
```kotlin
@Test
fun `verify parent PIN authentication`() {
    val authManager = ParentAuthManager(context)
    authManager.setPin("1234")
    
    assertTrue(authManager.verifyPin("1234"))
    assertFalse(authManager.verifyPin("0000"))
}
```

### Instrumented Tests (`androidTest/` directory)
**What to Test**:
- Database operations (Room)
- Content scanning (file system access)
- Device Admin activation
- Lock Task Mode
- UI interactions (Compose UI tests)

**Example**:
```kotlin
@Test
fun insertSongAndRead() = runBlocking {
    val dao = database.mediaDao()
    val song = SongEntity(title = "Test Song", artist = "Artist")
    
    dao.insertSong(song)
    
    val songs = dao.getAllSongs().first()
    assertEquals(1, songs.size)
    assertEquals("Test Song", songs[0].title)
}
```

### Manual Testing Checklist
- [ ] Device Admin activation flow (first launch)
- [ ] Lock Task Mode prevents escape (home, recent, status bar)
- [ ] USB content transfer & auto-scan
- [ ] Music playback (play, pause, skip, shuffle)
- [ ] Audiobook position saving & resume
- [ ] Volume limit enforcement
- [ ] Parent PIN authentication (correct & incorrect)
- [ ] Whitelist mode (allow Spotify Kids, block Chrome)
- [ ] Safe Mode boot (expected bypass)
- [ ] Battery life during 1-hour playback
- [ ] Test on multiple devices (Samsung, Xiaomi, OnePlus)

---

## Troubleshooting Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| **Build fails: SDK not found** | `ANDROID_HOME` not set | `export ANDROID_HOME=$HOME/Android/Sdk` |
| **Device Admin won't activate** | Device already has another Device Admin | Remove other admins first |
| **Lock Task Mode fails** | Not device owner | Guide user through setup or use Launcher-only mode |
| **Content scanner finds no files** | Wrong folder path | Ensure `/sdcard/KidPod/Music/` exists |
| **ExoPlayer crashes on corrupted file** | Bad MP3 file | Catch exception, skip file, log error |
| **Battery drains fast** | Wake lock not released | Review foreground service implementation |
| **Safe Mode bypasses lockdown** | Hardware limitation | Document as expected, advise physical supervision |

---

## Future Enhancements (Post-MVP)

### V2 Features
- WiFi content sync (local network file transfer)
- Listening statistics dashboard (hours listened, favorite songs)
- Multiple user profiles (for multiple kids on same device)
- Dark mode theme
- Playlist UI editor (drag-and-drop reordering)

### V3 Features
- Companion Android app (remote management from parent's phone)
- Integration with Libby/OverDrive APIs (library audiobooks)
- Bluetooth auto-pause on disconnect
- Export usage reports (CSV)
- Text-to-speech for ebooks (EPUB support)

---

## Key Principles

**1. Offline-First**
- Core functionality must work without internet
- Optional internet for whitelisted apps only

**2. Security is Pragmatic**
- Not attempting 100% bypass prevention (requires root)
- Goal: "Good enough" for 95%+ of use cases
- Be transparent about limitations

**3. Parent Experience is Priority**
- Setup must be <15 minutes
- Content management must be simple (USB drag-and-drop)
- PIN protection must be secure but not annoying

**4. Kid Experience is Delightful**
- Large buttons, simple UI
- Fast playback start (<1 second)
- No frustrating errors ("File not found" → "Let's add some music!")

**5. Performance Matters**
- Old phones have limited resources
- Optimize for battery life
- Fast content scanning

---

**This document should remain stable across development. Update only when making architectural decisions or adding permanent project knowledge.**
