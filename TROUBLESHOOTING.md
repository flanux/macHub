# 🐛 Troubleshooting Guide

## Scraper Issues

### ❌ "No notices found" in JSON

**Cause:** CSS selectors don't match MAC website structure

**Fix:**
1. Run scraper locally to see output
2. Open MAC website in browser
3. Right-click → Inspect Element
4. Find where notices actually are
5. Update `scraper/main.py`:

```python
# Example fixes:
# If notices are in a specific div:
for link in soup.select("div.notice-list a"):

# If they have a specific class:
for link in soup.select("a.notice-link"):

# If in a table:
for row in soup.select("table.notices tr"):
    link = row.select_one("a")
```

### ❌ Scraper finds too many irrelevant links

**Fix:** Add filtering in `scraper/main.py`:

```python
# Skip short titles
if len(title) < 10:
    continue

# Skip navigation items
skip_terms = ["home", "about", "contact", "login", "admission", "gallery"]
if any(term in title.lower() for term in skip_terms):
    continue

# Only keep items with certain keywords
if not any(word in title.lower() for word in ["notice", "exam", "result", "routine"]):
    continue
```

### ❌ GitHub Actions scraper fails

**Check:**
1. Actions tab → Click failed run → View logs
2. Common causes:
   - Website timeout: increase timeout in `requests.get(timeout=30)`
   - Python dependency issue: check requirements.txt
   - Commit permission: Actions needs write access

## Android App Issues

### ❌ App shows "Error" screen

**Possible causes:**

1. **API URL wrong**
   - Open `app/src/main/java/com/flanux/macdashboard/data/ApiService.kt`
   - Verify BASE_URL matches your GitHub Pages URL
   - Format: `https://USERNAME.github.io/REPO/`

2. **GitHub Pages not enabled**
   - Go to repo Settings → Pages
   - Enable Pages from main branch

3. **JSON file doesn't exist**
   - Check if `data/notices.json` is in repo
   - Run scraper workflow manually

4. **JSON not accessible**
   - Open URL in browser: `https://USERNAME.github.io/REPO/data/notices.json`
   - If 404: GitHub Pages might still be deploying (wait 2-3 min)

### ❌ App crashes on startup

**Check ProGuard rules:**
- If building release APK, ProGuard might strip needed classes
- Review `app/proguard-rules.pro`
- Common fix: add `-keep class com.flanux.macdashboard.** { *; }`

### ❌ Filtering doesn't work

**Cause:** Scraper categorization is basic keyword-matching

**Fix:** Improve `classify_notice()` in `scraper/main.py`:

```python
def classify_notice(title):
    t = title.lower()
    
    # More specific patterns
    if "exam routine" in t or "examination schedule" in t:
        return "exam"
    
    if "result" in t or "marks" in t:
        return "result"
    
    # Add your own patterns
    if "assignment" in t or "homework" in t:
        return "assignment"
    
    return "general"
```

## Build Issues

### ❌ GitHub Actions build fails

**Check workflow logs:**

1. **"gradlew not found"**
   - Ensure `gradlew` is executable: `chmod +x gradlew`
   - Commit and push

2. **"Keystore not found"**
   - This is OK for debug builds
   - For release: add GitHub secrets (see README)

3. **Gradle cache issues**
   - Sometimes caching causes problems
   - Try: Delete `.gradle` folder and push

### ❌ APK not signed

**For release builds:**
1. Generate keystore locally:
   ```bash
   keytool -genkey -v -keystore release.keystore -alias key0 -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Convert to base64:
   ```bash
   base64 release.keystore > keystore.txt
   ```

3. Add to GitHub Secrets:
   - KEYSTORE_BASE64: (paste content of keystore.txt)
   - KEYSTORE_PASSWORD: (your password)
   - KEY_ALIAS: key0
   - KEY_PASSWORD: (your key password)

**For testing:** Just use debug build (no signing needed)

### ❌ Build takes forever

**GitHub Actions quota:**
- Free accounts: 2000 minutes/month
- Each build: ~3-5 minutes

**Optimization:**
- Workflow already has Gradle caching enabled
- Don't push tiny changes repeatedly
- Test locally when possible

## Local Development Issues

### ❌ "Android SDK not found"

**You don't need local builds!** Use GitHub Actions.

But if you want local development:
1. Install Android Studio
2. Let it install SDK automatically
3. Set ANDROID_HOME environment variable

### ❌ Gradle daemon issues

```bash
# Kill all gradle processes:
pkill -f gradle

# Clear cache:
rm -rf ~/.gradle/caches
```

### ❌ Out of memory (8GB RAM)

**Don't build locally.** That's the whole point of this setup!

But if needed:
```bash
# In gradle.properties, reduce:
org.gradle.jvmargs=-Xmx1536m
```

## Data Issues

### ❌ Old notices showing

**Cause:** App caching

**Fix:**
- Pull to refresh in app (if implemented)
- Or clear app data in phone settings

### ❌ Dates showing wrong format

**Nepali calendar (BS) issue:**

The scraper assumes AD dates. For BS dates:

```python
# In scraper, you'd need to parse BS dates
# Example: "2080/12/15" → needs conversion
# Or just store as-is and show raw
```

### ❌ Batch detection not working

**Fix in `scraper/main.py`:**

```python
def extract_batch(title):
    # Try multiple patterns
    
    # Pattern 1: 20XX format
    match = re.search(r'20\d{2}', title)
    if match:
        return match.group(0)
    
    # Pattern 2: "Batch 2080"
    match = re.search(r'Batch\s+(\d{4})', title, re.IGNORECASE)
    if match:
        return match.group(1)
    
    # Pattern 3: "BCA 2080"
    match = re.search(r'BCA\s+(\d{4})', title, re.IGNORECASE)
    if match:
        return match.group(1)
    
    return None
```

## Still Stuck?

1. **Check repo structure:**
   ```
   mac-dashboard/
   ├── data/notices.json  ← Should exist after scraper runs
   ├── scraper/main.py    ← Your scraping logic
   ├── app/               ← Android app
   └── .github/workflows/ ← CI/CD
   ```

2. **Verify GitHub Pages:**
   - Open: `https://USERNAME.github.io/REPO/data/notices.json`
   - Should show JSON (not 404)

3. **Check Actions:**
   - Both workflows should have green checkmarks
   - If red: click and read logs

4. **Test scraper independently:**
   ```bash
   cd scraper
   python main.py
   cat ../data/notices.json  # Should show data
   ```

5. **Common Nepali university site patterns:**
   - Many use WordPress
   - Notices often in: `.post-content`, `.entry-content`, `#content`
   - Try inspecting with browser DevTools

## 💡 Pro Tips

- Start with **very generic selectors**, see what you get, then narrow down
- Use `print(soup.prettify())` to see full HTML structure
- Test scraper locally before pushing
- For major changes, create a test branch
- Keep commits small and focused

## Contact

This is a student project. If you're stuck:
- Check the README again
- Review your setup step-by-step
- Try the scraper locally first
- Check GitHub Actions logs carefully

Remember: The first version won't be perfect. Iterate and improve! 🚀
