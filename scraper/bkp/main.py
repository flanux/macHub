import requests
from bs4 import BeautifulSoup
import json
from urllib.parse import urljoin
from datetime import datetime, timezone
import re
import os

BASE_URL = "https://www.macpokhara.edu.np/"
OUTPUT_FILE = "data/notices.json"

headers = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64)"
}

KEYWORDS = ["exam", "result", "routine", "batch", "semester", "notice", "project"]
JUNK = ["about", "program", "mail", "team", "iost", "login", "contact"]

# ---------------------------
# helpers
# ---------------------------

def clean_text(text):
    """Clean and normalize text"""
    return " ".join(text.split())

def extract_batch(title):
    """Extract batch year from title (e.g., 2079, 2080)"""
    match = re.search(r'20\d{2}', title)
    return match.group(0) if match else None

def classify(title):
    """Classify notice by category"""
    t = title.lower()
    if "result" in t:
        return "result"
    if "exam" in t:
        return "exam"
    if "notice" in t:
        return "notice"
    if "project" in t:
        return "project"
    return "general"

def is_real_notice(title):
    """Filter out navigation and junk links"""
    t = title.lower()
    if len(t) < 20:
        return False
    if not any(k in t for k in KEYWORDS):
        return False
    if any(j in t for j in JUNK):
        return False
    return True

def fetch_html():
    """Fetch HTML from MAC website"""
    res = requests.get(BASE_URL, headers=headers, timeout=10)
    res.raise_for_status()
    return res.text

# ---------------------------
# main scraper
# ---------------------------

def scrape():
    """Scrape notices from MAC website"""
    html = fetch_html()
    soup = BeautifulSoup(html, "html.parser")
    
    # Target the main content container
    container = soup.select_one(".col-lg-8")
    if not container:
        raise Exception("Notice container (.col-lg-8) not found")
    
    links = container.find_all("a")
    
    notices = []
    for a in links:
        title = clean_text(a.get_text())
        href = a.get("href")
        
        if not title or not href:
            continue
        
        if not is_real_notice(title):
            continue
        
        notice = {
            "title": title,
            "url": urljoin(BASE_URL, href),
            "category": classify(title),
            "batch": extract_batch(title),
            "scraped_at": datetime.now(timezone.utc).isoformat()
        }
        
        notices.append(notice)
    
    # Deduplicate
    seen = set()
    unique = []
    for n in notices:
        if n["url"] not in seen:
            seen.add(n["url"])
            unique.append(n)
    
    # Site already lists newest first, so reverse to maintain order
    unique = unique[::-1]
    
    return unique

# ---------------------------
# entry point
# ---------------------------

def main():
    """Main scraper function"""
    try:
        notices = scrape()
        
        print(f"[+] Final notice count: {len(notices)}")
        for n in notices[:10]:
            print("  -", n["title"])
        
        # Ensure folder exists
        os.makedirs("data", exist_ok=True)
        
        # Save to JSON
        with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
            json.dump(notices, f, indent=2, ensure_ascii=False)
        
        print(f"[+] Saved to {OUTPUT_FILE}")
        
    except Exception as e:
        print("[ERROR]", e)
        raise

if __name__ == "__main__":
    main()
