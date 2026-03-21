# macHub - Quick Start Guide

Get the app running in 5 minutes.

## 1. Prerequisites

Install these first:
- **Android Studio** (Hedgehog or later) - [Download](https://developer.android.com/studio)
- **JDK 17** - Usually bundled with Android Studio

## 2. Open Project

```bash
# Method 1: From command line
cd macHub
./gradlew assembleDebug

# Method 2: Open in Android Studio
1. Launch Android Studio
2. Click "Open"
3. Select the macHub folder
4. Wait for Gradle sync (1-5 minutes)
```

## 3. Run on Device

### Option A: Physical Device
```bash
1. Enable USB Debugging on your phone:
   Settings → About Phone → Tap "Build Number" 7 times
   Settings → Developer Options → Enable "USB Debugging"

2. Connect phone via USB

3. Click "Run" (▶️) in Android Studio
   OR
   ./gradlew installDebug
```

### Option B: Emulator
```bash
1. In Android Studio: Tools → Device Manager
2. Create Virtual Device (Pixel 6, API 34)
3. Click "Run" (▶️)
```

## 4. Build Release APK

```bash
# From terminal
./gradlew assembleRelease

# APK location
app/build/outputs/apk/release/app-release-unsigned.apk

# Install on device
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

## 5. Update Data (Optional)

The app comes with pre-loaded data. To update with fresh scraped data:

```bash
# Run scraper (requires Python)
cd scraper
pip install -r requirements.txt
python main.py

# Copy fresh data to assets
cp ../data/*.json ../app/src/main/assets/data/

# Rebuild
cd ..
./gradlew assembleRelease
```

## Troubleshooting

### Gradle sync failed
```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

### "SDK not found"
```bash
# Set ANDROID_HOME environment variable
export ANDROID_HOME=$HOME/Android/Sdk  # Linux/Mac
# OR
set ANDROID_HOME=C:\Users\YourName\AppData\Local\Android\Sdk  # Windows
```

### App crashes on startup
```bash
# Check logcat
adb logcat | grep flanux

# Most common: Data files missing
# Solution: Verify app/src/main/assets/data/ contains:
#   - notices.json
#   - downloads.json
#   - news.json
#   - gallery.json
```

## Project Structure

```
macHub/
├── app/
│   ├── src/main/
│   │   ├── assets/data/          ← JSON data files (REQUIRED)
│   │   ├── java/com/flanux/machub/
│   │   │   ├── features/         ← UI screens
│   │   │   ├── data/             ← Models & repository
│   │   │   └── MainActivity.kt
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── scraper/                      ← Python scraper
├── data/                         ← Scraped output
└── build.gradle.kts
```

## Common Tasks

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Clean build artifacts
./gradlew clean

# Run tests
./gradlew test

# Check for updates
./gradlew --version
```

## Next Steps

1. **Customize colors**: Edit `app/src/main/java/com/flanux/machub/ui/theme/Color.kt`
2. **Add features**: Check `features/` directory for modular code
3. **Update scraper**: Modify `scraper/main.py` for new data sources
4. **Deploy**: Follow GitHub Actions setup in `.github/workflows/`

## Support

- **Issues**: https://github.com/flanux/macHub/issues
- **Discussions**: https://github.com/flanux/macHub/discussions

---

**Pro Tip**: Use `./gradlew tasks` to see all available Gradle tasks.
