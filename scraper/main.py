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
# Downloads Scraper
# ---------------------------

def scrape_downloads(url, download_type):
    """Scrape download pages (student/general)"""
    print(f"[*] Scraping {download_type} downloads...")
    
    try:
        html = requests.get(url, headers=headers, timeout=10).text
        soup = BeautifulSoup(html, "html.parser")
        
        downloads = []
        
        # Look for download links (PDFs, docs, etc.)
        links = soup.find_all("a", href=True)
        
        for link in links:
            href = link.get("href")
            title = clean_text(link.get_text())
            
            # Check if it's a file link
            if any(ext in href.lower() for ext in ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.zip']):
                file_type = href.split('.')[-1].upper()
                
                downloads.append({
                    "title": title if title else f"Download {file_type}",
                    "url": urljoin(BASE_URL, href),
                    "type": file_type,
                    "category": download_type,
                    "scraped_at": datetime.now(timezone.utc).isoformat()
                })
        
        # Also check for download cards/sections
        download_cards = soup.select(".download-item, .file-item, .card")
        for card in download_cards:
            title_elem = card.select_one("h3, h4, .title, .name")
            link_elem = card.select_one("a[href]")
            
            if title_elem and link_elem:
                title = clean_text(title_elem.get_text())
                href = link_elem.get("href")
                
                if href and any(ext in href.lower() for ext in ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.zip']):
                    file_type = href.split('.')[-1].upper()
                    
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
# Gallery Scraper
# ---------------------------

def scrape_gallery(url, gallery_name):
    """Scrape gallery images (routines, schedules, etc.)"""
    print(f"[*] Scraping {gallery_name} gallery...")
    
    try:
        html = requests.get(url, headers=headers, timeout=10).text
        soup = BeautifulSoup(html, "html.parser")
        
        images = []
        
        # Find all image elements
        img_tags = soup.find_all("img")
        
        for img in img_tags:
            src = img.get("src")
            alt = img.get("alt", "Image")
            
            if src and not any(skip in src.lower() for skip in ['logo', 'icon', 'banner']):
                images.append({
                    "title": clean_text(alt) if alt else f"{gallery_name} Image",
                    "image_url": urljoin(BASE_URL, src),
                    "category": gallery_name,
                    "scraped_at": datetime.now(timezone.utc).isoformat()
                })
        
        # Also check for gallery items/cards
        gallery_items = soup.select(".gallery-item, .image-item, a[href*='image'], a[href*='.jpg'], a[href*='.png']")
        
        for item in gallery_items:
            href = item.get("href", "")
            title = clean_text(item.get_text())
            
            if any(ext in href.lower() for ext in ['.jpg', '.jpeg', '.png', '.gif']):
                images.append({
                    "title": title if title else f"{gallery_name} Image",
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
    print("MAC POKHARA COMPLETE SCRAPER")
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
