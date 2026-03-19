# 🚀 QUICK START

Follow these steps in order:

## 1️⃣ Update the API URL (CRITICAL!)

Open `app/src/main/java/com/flanux/macdashboard/data/ApiService.kt`

Change this line:
```kotlin
private const val BASE_URL = "https://USERNAME.github.io/REPO/"
```

To your actual GitHub Pages URL:
```kotlin
private const val BASE_URL = "https://YOUR_USERNAME.github.io/macHub/"
```

## 2️⃣ Enable GitHub Pages

1. Push this repo to GitHub
2. Go to **Settings** → **Pages**
3. Set Source to: **Deploy from a branch**
4. Branch: **main** / root
5. Save

Your data will be available at:
`https://YOUR_USERNAME.github.io/mac-dashboard/data/notices.json`

## 3️⃣ Run the Scraper

**Option A - Via GitHub Actions (recommended):**
- Go to **Actions** tab
- Click **Scrape MAC Notices**
- Click **Run workflow**
- Wait ~1 minute
- Check if `data/notices.json` appears in your repo

**Option B - Locally:**
```bash
cd scraper
pip install -r requirements.txt
python main.py
```

## 4️⃣ Build the APK

**Push to GitHub:**
```bash
git add .
git commit -m "Initial commit"
git push origin main
```

GitHub Actions will automatically build your APK.

**Download APK:**
- Go to **Actions** tab
- Click the latest workflow run
- Scroll to **Artifacts**
- Download **MAC-Dashboard-APK**

OR

- Go to **Releases** (if successful)
- Download the APK from latest release

## 5️⃣ Install on Phone

- Transfer APK to your phone
- Allow installation from unknown sources
- Install and open

## ⚠️ Common Issues

### "No notices found"
- Check if `data/notices.json` exists in repo
- Open `https://YOUR_USERNAME.github.io/mac-dashboard/data/notices.json` in browser
- If it's not accessible, GitHub Pages might still be deploying (wait 2-3 minutes)

### Scraper finds too much garbage
Edit `scraper/main.py` and refine the CSS selectors:
```python
# Instead of generic:
for link in soup.select("a"):

# Use specific selectors:
for link in soup.select(".notice-board a"):  # class
for link in soup.select("#notices a"):        # id  
for link in soup.select("div.content a"):     # nested
```

### APK build fails
- Check **Actions** logs for errors
- For debug builds, you don't need keystore secrets
- Ensure all files are committed

## 🎯 Next Steps

Once basic version works:

1. **Refine scraper** - Update selectors for better accuracy
2. **Add categories** - Improve classification in `classify_notice()`
3. **Test filtering** - Try different batches and categories in app
4. **Share with classmates** - Get feedback and iterate

## 📱 Testing Locally (without phone)

You can use Android Studio emulator OR just build and check logs:

```bash
./gradlew assembleDebug
# Check: app/build/outputs/apk/debug/app-debug.apk
```

## 🔧 For Development

The beauty of this setup: **you don't need Android Studio locally!**

1. Edit code in VS Code / any editor
2. Push to GitHub
3. GitHub Actions builds everything
4. Download APK
5. Test on phone

Your i3/8GB machine stays light and fast. 🔥
