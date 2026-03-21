# macHub - MAC Pokhara Student Hub

Unofficial Android app for Mount Annapurna Campus (MAC) Pokhara students. Get instant access to notices, downloads, news, and gallery - all scraped from the official MAC website.

## Features

✅ **Notices** - Exam notices, IOST updates, general announcements with filtering by category, batch, and semester  
✅ **Downloads** - E-Books, Notes, Syllabus, Question Collections organized by semester  
✅ **News & Events** - Latest workshops, programs, and college events with images  
✅ **Gallery** - Photo albums and WebView links for routines/semester plans  
✅ **Smart Filtering** - Filter by batch (2077, 2078, 2079, 2080), semester, category  
✅ **Attachment Support** - Direct links to PDFs, Google Drive, SharePoint folders  
✅ **Offline-First** - All data bundled in the app, no internet required for browsing

## Project Structure

```
macHub/
├── app/
│   ├── src/main/
│   │   ├── java/com/flanux/machub/
│   │   │   ├── data/              # Data models & repository
│   │   │   ├── features/          # Feature modules (notices, downloads, etc.)
│   │   │   ├── navigation/        # Bottom nav & routing
│   │   │   ├── ui/theme/          # Material 3 theming
│   │   │   └── MainActivity.kt
│   │   ├── assets/data/           # Scraped JSON files
│   │   └── res/                   # Resources
│   └── build.gradle.kts
├── scraper/                       # Python scraper (separate)
└── data/                          # Scraped JSON output
```

## Setup & Build

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Gradle 8.2
- Kotlin 1.9.10

### Build Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/flanux/macHub.git
   cd macHub
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open" and navigate to the project directory
   - Wait for Gradle sync to complete

3. **Build the APK**
   - **Debug build**: `./gradlew assembleDebug`
   - **Release build**: `./gradlew assembleRelease`
   - Output: `app/build/outputs/apk/`

4. **Run on device/emulator**
   - Connect device or start emulator
   - Click "Run" in Android Studio or:
   ```bash
   ./gradlew installDebug
   ```

### Updating Data

The app reads JSON files from `app/src/main/assets/data/`. To update:

1. **Run the scraper** (in `scraper/` directory):
   ```bash
   cd scraper
   pip install -r requirements.txt
   python main.py
   ```

2. **Copy fresh data**:
   ```bash
   cp data/*.json app/src/main/assets/data/
   ```

3. **Rebuild the app**:
   ```bash
   ./gradlew assembleRelease
   ```

## Data Files

The app uses these JSON files (scraped from macpokhara.edu.np):

- `notices.json` - Notice details with attachments, body content, batch/semester metadata
- `downloads.json` - Student resources (E-Books, Notes, Syllabus, Questions)
- `news.json` - Events, workshops, programs with thumbnails
- `gallery.json` - Photo albums + WebView links

## Architecture

- **Pattern**: MVVM (Model-View-ViewModel)
- **UI**: Jetpack Compose with Material 3
- **Data**: JSON loaded from assets via Gson
- **Navigation**: Bottom navigation with 5 tabs
- **State**: Kotlin Flows + ViewModel

## Key Dependencies

```kotlin
// Compose & Material 3
androidx.compose.material3
androidx.lifecycle:lifecycle-viewmodel-compose

// JSON parsing
com.google.code.gson:gson

// Image loading
io.coil-kt:coil-compose

// Network (for future API support)
com.squareup.retrofit2:retrofit
com.squareup.okhttp3:okhttp
```

## ProGuard Configuration

Release builds use R8 minification. ProGuard rules are configured in `app/proguard-rules.pro` to preserve:
- Gson annotations & reflection
- Data classes
- Retrofit interfaces
- OkHttp components

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Scraper

The scraper is a separate Python project that fetches data from the official MAC website. See `scraper/main.py` for implementation details.

**Scraper features:**
- Change detection (MD5 body hash)
- Smart attachment labeling
- Full pagination support
- Batch/semester extraction
- Failure logging

## License

MIT License - see LICENSE file for details.

## Disclaimer

This is an **unofficial** student project and is not affiliated with Mount Annapurna Campus. All data is publicly available from macpokhara.edu.np.

## Contact

- GitHub: [@flanux](https://github.com/flanux)
- Project: [macHub](https://github.com/flanux/macHub)

---

Built with ❤️ by students, for students.
