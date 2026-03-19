# MAC Dashboard

A clean, student-focused Android app for MAC Pokhara notices. No more scrolling through the repetitive college website.

## 🎯 Features

- **Clean UI**: No intro pages, just your notices
- **Smart Filtering**: Filter by category (exam, result, assignment, etc.) and batch (2079, 2080, etc.)
- **Offline-First**: Cached data works without internet
- **Auto-Updated**: Scraper runs every 6 hours via GitHub Actions
- **Fast**: Loads instantly, no website bloat

## 🏗️ Architecture

```
┌─────────────────┐
│  MAC Website    │
└────────┬────────┘
         │
    ┌────▼─────┐
    │ Scraper  │ (GitHub Actions - runs every 6h)
    └────┬─────┘
         │
    ┌────▼─────┐
    │ JSON API │ (GitHub Pages)
    └────┬─────┘
         │
    ┌────▼─────┐
    │ Android  │
    │   App    │
    └──────────┘
```

## 🚀 Setup

### 1. Fork/Clone this repo

```bash
git clone https://github.com/YOUR_USERNAME/mac-dashboard.git
cd mac-dashboard
```

### 2. Update API endpoint

Edit `app/src/main/java/com/flanux/macdashboard/data/ApiService.kt`:

```kotlin
// Replace with your actual GitHub username and repo name
private const val BASE_URL = "https://YOUR_USERNAME.github.io/mac-dashboard/"
```

### 3. Enable GitHub Pages

- Go to repo **Settings** → **Pages**
- Source: **Deploy from a branch**
- Branch: **main** / **root**
- Save

### 4. Set up signing (for release builds)

Create these GitHub Secrets in **Settings** → **Secrets and variables** → **Actions**:

```bash
# Generate keystore first (one-time setup):
keytool -genkey -v -keystore release.keystore -alias key0 -keyalg RSA -keysize 2048 -validity 10000

# Convert to base64:
base64 release.keystore > keystore.txt

# Then add these secrets:
KEYSTORE_BASE64=<paste contents of keystore.txt>
KEYSTORE_PASSWORD=<your password>
KEY_ALIAS=key0
KEY_PASSWORD=<your key password>
```

**Don't have a keystore?** The workflow will build a debug APK automatically.

### 5. Push to GitHub

```bash
git add .
git commit -m "Initial commit"
git push origin main
```

### 6. Trigger workflows

**Scraper:**
- Go to **Actions** → **Scrape MAC Notices**
- Click **Run workflow**
- Wait ~1 minute
- Check `data/notices.json` is created

**APK Build:**
- Automatically triggers on push
- Or manually: **Actions** → **Build and Release APK** → **Run workflow**
- Download APK from **Releases** or **Artifacts**

## 🔧 Local Development

### Test scraper locally:

```bash
cd scraper
pip install -r requirements.txt
python main.py
```

Check `data/notices.json` to verify output.

### Refine selectors:

The scraper uses generic selectors. You'll need to inspect MAC's HTML and update `scraper/main.py`:

```python
# Example: if notices are in a specific div
for link in soup.select("div.notice-board a"):
    # ...
```

### Android development:

**You don't need Android Studio locally!** Just:

1. Edit code in any text editor
2. Push to GitHub
3. GitHub Actions builds the APK
4. Download and install on your phone

## 📱 Build Manually (if needed)

```bash
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## 🐛 Troubleshooting

### No notices showing in app

1. Check if `data/notices.json` exists in repo
2. Verify GitHub Pages is enabled and live
3. Check API URL in `ApiService.kt` matches your GitHub Pages URL
4. Open the JSON URL in browser to confirm it's accessible

### Scraper not finding notices

1. Run scraper locally and check output
2. Inspect MAC website HTML (right-click → Inspect)
3. Update CSS selectors in `scraper/main.py`
4. Common patterns:
   ```python
   soup.select(".notice-board a")  # class selector
   soup.select("#notices a")        # id selector
   soup.select("div.content a")     # nested selector
   ```

### APK build fails

1. Check if all secrets are set correctly
2. For debug builds, keystore is optional
3. Check GitHub Actions logs for errors

## 📂 Project Structure

```
mac-dashboard/
├── scraper/              # Python scraper
│   ├── main.py
│   └── requirements.txt
├── data/                 # Generated JSON (auto-updated)
│   └── notices.json
├── app/                  # Android app
│   ├── src/main/java/com/flanux/macdashboard/
│   │   ├── MainActivity.kt
│   │   ├── data/         # Models, API, Repository
│   │   └── ui/           # Compose screens, ViewModels
│   └── build.gradle.kts
└── .github/workflows/    # CI/CD
    ├── scrape.yml        # Runs every 6h
    └── build-apk.yml     # Builds on push
```

## 🎨 Customization

### Change scraper frequency

Edit `.github/workflows/scrape.yml`:

```yaml
schedule:
  - cron: "0 */6 * * *"  # Change to your preferred schedule
```

### Add more categories

Edit `scraper/main.py` → `classify_notice()` function

### Change app colors

Edit `app/src/main/java/com/flanux/macdashboard/ui/theme/Theme.kt`

## 📄 License

MIT - Do whatever you want with this

## 🙏 Credits

Built by a student tired of clicking through 10 pages to find exam notices.

---

**Note:** This is a student project. Not affiliated with MAC Pokhara.
