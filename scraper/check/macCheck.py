import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse
import json

BASE_URL = "https://www.macpokhara.edu.np/"

headers = {
    "User-Agent": "Mozilla/5.0"
}

def discover_site_structure():
    """Find all major sections and download pages"""
    
    res = requests.get(BASE_URL, headers=headers, timeout=10)
    soup = BeautifulSoup(res.text, "html.parser")
    
    print("=" * 60)
    print("MAC POKHARA SITE STRUCTURE DISCOVERY")
    print("=" * 60)
    
    # Find navigation menu
    print("\n📋 MAIN NAVIGATION LINKS:")
    nav_links = soup.select("nav a, .navbar a, .menu a")
    nav_pages = {}
    
    for link in nav_links:
        text = link.get_text(strip=True)
        href = link.get("href")
        
        if href and len(text) > 2:
            full_url = urljoin(BASE_URL, href)
            # Only internal links
            if "macpokhara.edu.np" in full_url or href.startswith("/"):
                nav_pages[text] = full_url
                print(f"  - {text:30} → {full_url}")
    
    # Find download sections
    print("\n📥 DOWNLOAD SECTIONS:")
    download_keywords = ["download", "downloads", "resources", "files", "documents"]
    
    all_links = soup.find_all("a")
    download_links = []
    
    for link in all_links:
        text = link.get_text(strip=True).lower()
        href = link.get("href", "")
        
        if any(keyword in text for keyword in download_keywords):
            full_url = urljoin(BASE_URL, href)
            download_links.append({
                "text": link.get_text(strip=True),
                "url": full_url
            })
            print(f"  - {link.get_text(strip=True):30} → {full_url}")
    
    # Find PDF/file links
    print("\n📄 DIRECT FILE LINKS (PDFs, Docs):")
    file_links = []
    
    for link in all_links:
        href = link.get("href", "")
        if any(ext in href.lower() for ext in ['.pdf', '.doc', '.docx', '.xls', '.xlsx']):
            full_url = urljoin(BASE_URL, href)
            file_links.append({
                "text": link.get_text(strip=True),
                "url": full_url,
                "type": href.split('.')[-1].upper()
            })
            print(f"  [{href.split('.')[-1].upper()}] {link.get_text(strip=True)}")
    
    # Find potential student portal/downloads page
    print("\n🎓 STUDENT-SPECIFIC SECTIONS:")
    student_keywords = ["student", "portal", "resources", "materials"]
    
    for link in all_links:
        text = link.get_text(strip=True).lower()
        href = link.get("href", "")
        
        if any(keyword in text for keyword in student_keywords) and href:
            full_url = urljoin(BASE_URL, href)
            print(f"  - {link.get_text(strip=True):30} → {full_url}")
    
    # Check for paginated content
    print("\n📄 PAGINATION DETECTED:")
    pagination = soup.select(".pagination a, .pager a, a[rel='next']")
    if pagination:
        print(f"  Found {len(pagination)} pagination links")
        for p in pagination[:5]:
            print(f"  - {p.get_text(strip=True)} → {p.get('href')}")
    else:
        print("  No pagination found on homepage")
    
    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY:")
    print(f"  Navigation pages: {len(nav_pages)}")
    print(f"  Download sections: {len(download_links)}")
    print(f"  Direct files: {len(file_links)}")
    print("=" * 60)
    
    # Save to JSON for reference
    discovery = {
        "navigation": nav_pages,
        "downloads": download_links,
        "files": file_links,
        "timestamp": str(datetime.now())
    }
    
    with open("macCheck.json", "w") as f:
        json.dump(discovery, f, indent=2)
    
    print("\n💾 Full data saved to: macCheck.json")

if __name__ == "__main__":
    from datetime import datetime
    discover_site_structure()
