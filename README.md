# Jellyspot (Kotlin)

A native Android music player with support for local music, Jellyfin media servers, and YouTube Music.

## Features

- 🎵 **Local Music** - Play music stored on your device
- 🖥️ **Jellyfin Integration** - Stream from your Jellyfin media server
- 🎧 **YouTube Music** - Search and stream from YouTube Music (coming soon)
- 🎨 **Material 3 UI** - Modern, dynamic theming with adaptive backgrounds
- ⬇️ **Downloads** - Download tracks for offline playback
- 📝 **Playlists** - Create and manage local playlists
- ❤️ **Favorites** - Mark tracks as favorites
- 📊 **Lyrics** - View synced lyrics

## Tech Stack

- **Kotlin** with Coroutines & Flow
- **Jetpack Compose** with Material 3
- **Hilt** for dependency injection
- **Media3/ExoPlayer** for audio playback
- **Room** for local database
- **DataStore** for preferences
- **Ktor** for networking
- **Coil** for image loading

## Building

### Prerequisites
- JDK 17+
- Android SDK

### Build Debug APK
```bash
cd kotlin-app
./gradlew assembleDebug
```

### Build Release APK
```bash
cd kotlin-app
./gradlew assembleRelease
```

## Project Structure

```
kotlin-app/
├── app/src/main/
│   ├── java/com/jellyspot/
│   │   ├── data/           # Data layer (Room, DataStore)
│   │   ├── di/             # Hilt modules
│   │   ├── player/         # Media playback service
│   │   └── ui/             # Compose UI
│   └── res/                # Android resources
└── build.gradle.kts        # Gradle configuration
```

## License

MIT License
