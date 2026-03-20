import requests
from bs4 import BeautifulSoup
import json
from urllib.parse import urljoin
from datetime import datetime, timezone
import re
import os

BASE_URL = "https://www.macpokhara.edu.np/"

URLS = {
    "notices": BASE_URL,
    "student_downloads": BASE_URL + "Downloads/ShowStudentDownloadDetails",
    "general_downloads": BASE_URL + "Downloads/ShowOtherDownloadDetails",
    "gallery_routine": BASE_URL + "Gallery/ShowGalleryGeneral?id=4affa6d7-c154-4b8f-a2b6-d8efff49f659",
    "gallery_semester": BASE_URL + "Gallery/ShowGalleryGeneral?id=eb7b0844-c6ef-4ff3-84e2-a8078d0f68d9",
}

OUTPUT_DIR = "data"

headers = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64)"
}

# ---------------------------
# Helpers
# ---------------------------

def clean_text(text):
    return " ".join(text.split())

def extract_batch(title):
    match = re.search(r'20\d{2}', title)
    return match.group(0) if match else None

def classify(title):
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

# ---------------------------
# Notices Scraper
# ---------------------------

def scrape_notices():
    """Scrape notices from homepage"""
    print("[*] Scraping notices...")
    
    html = requests.get(URLS["notices"], headers=headers, timeout=10).text
    soup = BeautifulSoup(html, "html.parser")
    
    container = soup.select_one(".col-lg-8")
    if not container:
        print("[!] Notice container not found")
        return []
    
    links = container.find_all("a")
    notices = []
    
    KEYWORDS = ["exam", "result", "routine", "batch", "semester", "notice", "project"]
    JUNK = ["about", "program", "mail", "team", "iost", "login", "contact"]
    
    for a in links:
        title = clean_text(a.get_text())
        href = a.get("href")
        
        if not title or not href:
            continue
        
        t = title.lower()
        if len(t) < 20:
            continue
        if not any(k in t for k in KEYWORDS):
            continue
        if any(j in t for j in JUNK):
            continue
        
        notices.append({
            "title": title,
            "url": urljoin(BASE_URL, href),
            "category": classify(title),
            "batch": extract_batch(title),
            "scraped_at": datetime.now(timezone.utc).isoformat()
        })
    
    # Deduplicate
    seen = set()
    unique = []
    for n in notices:
        if n["url"] not in seen:
            seen.add(n["url"])
            unique.append(n)
    
    print(f"[+] Found {len(unique)} notices")
    return unique[::-1]

# ---------------------------
# Downloads Scraper (FIXED)
# ---------------------------

def scrape_downloads(url, download_type):
    """Scrape download pages - IMPROVED title extraction"""
    print(f"[*] Scraping {download_type} downloads...")
    
    try:
        html = requests.get(url, headers=headers, timeout=10).text
        soup = BeautifulSoup(html, "html.parser")
        
        downloads = []
        
        # Strategy 1: Look for file links with proper titles
        # Try finding parent containers with both title and link
        containers = soup.select("div, li, tr")
        
        for container in containers:
            link = container.find("a", href=lambda x: x and any(ext in x.lower() for ext in ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.zip']))
            
            if not link:
                continue
            
            href = link.get("href")
            
            # Try multiple strategies to get title
            title = None
            
            # 1. Check if link has meaningful text
            link_text = clean_text(link.get_text())
            if link_text and len(link_text) > 5 and "download" not in link_text.lower():
                title = link_text
            
            # 2. Look for nearby heading or label
            if not title:
                heading = container.find(["h3", "h4", "h5", "strong", "b", "span"])
                if heading:
                    heading_text = clean_text(heading.get_text())
                    if len(heading_text) > 5:
                        title = heading_text
            
            # 3. Extract filename from URL as last resort
            if not title:
                filename = href.split('/')[-1].replace('%20', ' ')
                if '.' in filename:
                    title = filename.rsplit('.', 1)[0]  # Remove extension
            
            # 4. Ultimate fallback
            if not title or len(title) < 3:
                title = f"Download File"
            
            file_type = href.split('.')[-1].upper() if '.' in href else 'FILE'
            
            downloads.append({
                "title": title,
                "url": urljoin(BASE_URL, href),
                "type": file_type,
                "category": download_type,
                "scraped_at": datetime.now(timezone.utc).isoformat()
            })
        
        # Deduplicate
        seen = set()
        unique = []
        for d in downloads:
            if d["url"] not in seen:
                seen.add(d["url"])
                unique.append(d)
        
        print(f"[+] Found {len(unique)} {download_type} downloads")
        return unique
        
    except Exception as e:
        print(f"[!] Error scraping {download_type}: {e}")
        return []

# ---------------------------
# Gallery Scraper (FIXED)
# ---------------------------

def scrape_gallery(url, gallery_name):
    """Scrape gallery images - IMPROVED filtering"""
    print(f"[*] Scraping {gallery_name} gallery...")
    
    try:
        html = requests.get(url, headers=headers, timeout=10).text
        soup = BeautifulSoup(html, "html.parser")
        
        images = []
        
        # Find all image elements
        img_tags = soup.find_all("img")
        
        # STRICT filtering to exclude logos/icons/banners
        EXCLUDE_KEYWORDS = ['logo', 'icon', 'banner', 'header', 'footer', 'nav', 'menu', 
                           'facebook', 'twitter', 'instagram', 'whatsapp', 'symbol']
        
        for img in img_tags:
            src = img.get("src", "")
            alt = img.get("alt", "").lower()
            
            # Skip if no src
            if not src:
                continue
            
            # Skip if contains excluded keywords in URL or alt text
            if any(keyword in src.lower() for keyword in EXCLUDE_KEYWORDS):
                continue
            if any(keyword in alt for keyword in EXCLUDE_KEYWORDS):
                continue
            
            # Skip very small images (likely icons)
            width = img.get("width", "")
            height = img.get("height", "")
            if width and height:
                try:
                    if int(width) < 100 or int(height) < 100:
                        continue
                except:
                    pass
            
            # Get a better title
            title = clean_text(img.get("alt", ""))
            if not title or len(title) < 3:
                # Try to get title from parent link
                parent_link = img.find_parent("a")
                if parent_link:
                    title = clean_text(parent_link.get("title", ""))
            
            if not title or len(title) < 3:
                title = f"{gallery_name.replace('_', ' ').title()} Image"
            
            images.append({
                "title": title,
                "image_url": urljoin(BASE_URL, src),
                "category": gallery_name,
                "scraped_at": datetime.now(timezone.utc).isoformat()
            })
        
        # Also check for gallery items/cards with links to images
        gallery_links = soup.select("a[href*='.jpg'], a[href*='.jpeg'], a[href*='.png'], a[href*='.gif']")
        
        for link in gallery_links:
            href = link.get("href", "")
            
            # Same filtering
            if any(keyword in href.lower() for keyword in EXCLUDE_KEYWORDS):
                continue
            
            title = clean_text(link.get_text())
            if not title or len(title) < 3:
                title = f"{gallery_name.replace('_', ' ').title()} Image"
            
            images.append({
                "title": title,
                "image_url": urljoin(BASE_URL, href),
                "category": gallery_name,
                "scraped_at": datetime.now(timezone.utc).isoformat()
            })
        
        # Deduplicate
        seen = set()
        unique = []
        for i in images:
            if i["image_url"] not in seen:
                seen.add(i["image_url"])
                unique.append(i)
        
        print(f"[+] Found {len(unique)} {gallery_name} images")
        return unique
        
    except Exception as e:
        print(f"[!] Error scraping {gallery_name}: {e}")
        return []

# ---------------------------
# Main Execution
# ---------------------------

def main():
    """Main scraper - scrapes everything"""
    print("=" * 60)
    print("MAC POKHARA COMPLETE SCRAPER v2")
    print("=" * 60 + "\n")
    
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    # Scrape all sections
    notices = scrape_notices()
    student_downloads = scrape_downloads(URLS["student_downloads"], "student")
    general_downloads = scrape_downloads(URLS["general_downloads"], "general")
    routine_images = scrape_gallery(URLS["gallery_routine"], "routine")
    semester_images = scrape_gallery(URLS["gallery_semester"], "semester_plan")
    
    # Combine all downloads
    all_downloads = student_downloads + general_downloads
    all_gallery = routine_images + semester_images
    
    # Create final JSON structure
    data = {
        "notices": {
            "items": notices,
            "count": len(notices)
        },
        "downloads": {
            "items": all_downloads,
            "count": len(all_downloads),
            "student_count": len(student_downloads),
            "general_count": len(general_downloads)
        },
        "gallery": {
            "items": all_gallery,
            "count": len(all_gallery),
            "routine_count": len(routine_images),
            "semester_count": len(semester_images)
        },
        "last_updated": datetime.now(timezone.utc).isoformat(),
        "total_items": len(notices) + len(all_downloads) + len(all_gallery)
    }
    
    # Save individual files for app
    with open(f"{OUTPUT_DIR}/notices.json", "w", encoding="utf-8") as f:
        json.dump({
            "notices": notices,
            "last_updated": data["last_updated"],
            "total_count": len(notices)
        }, f, indent=2, ensure_ascii=False)
    
    with open(f"{OUTPUT_DIR}/downloads.json", "w", encoding="utf-8") as f:
        json.dump({
            "downloads": all_downloads,
            "last_updated": data["last_updated"],
            "total_count": len(all_downloads)
        }, f, indent=2, ensure_ascii=False)
    
    with open(f"{OUTPUT_DIR}/gallery.json", "w", encoding="utf-8") as f:
        json.dump({
            "gallery": all_gallery,
            "last_updated": data["last_updated"],
            "total_count": len(all_gallery)
        }, f, indent=2, ensure_ascii=False)
    
    # Save combined file
    with open(f"{OUTPUT_DIR}/all_data.json", "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    
    # Print summary
    print("\n" + "=" * 60)
    print("SCRAPING COMPLETE!")
    print("=" * 60)
    print(f"Notices:          {len(notices)}")
    print(f"Downloads:        {len(all_downloads)}")
    print(f"  - Student:      {len(student_downloads)}")
    print(f"  - General:      {len(general_downloads)}")
    print(f"Gallery Images:   {len(all_gallery)}")
    print(f"  - Routines:     {len(routine_images)}")
    print(f"  - Semester:     {len(semester_images)}")
    print(f"\nTotal items:      {data['total_items']}")
    print("=" * 60)
    print(f"\n💾 Saved to {OUTPUT_DIR}/")

if __name__ == "__main__":
    main()
