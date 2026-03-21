"""
MAC Pokhara scraper
Outputs:
  data/notices.json   — exam/general/iost notices with body + attachments
  data/news.json      — events/workshops/programs with thumbnail
  data/downloads.json — student + general downloads, all pages
  data/gallery.json   — scrapeable albums + static WebView links

Designed for enrolled students only. Skips admission/prospectus content.
Includes change detection (body hash), smart attachment labelling,
full pagination, and a failure log so broken runs are visible.
"""

import hashlib
import json
import os
import re
import time
from datetime import datetime, timezone

import requests
from bs4 import BeautifulSoup

# ── Config ────────────────────────────────────────────────────────────────────
BASE_URL = "https://macpokhara.edu.np"
HEADERS = {"User-Agent": "Mozilla/5.0 (compatible; macHub-scraper/1.0)"}
DATA_DIR = "data"
DETAIL_DELAY = 1.0   # seconds between detail page fetches — be polite
MAX_PAGES = 50       # safety cap on any paginated loop

# ── JS-rendered galleries — can't scrape images, store as WebView links ───────
STATIC_GALLERIES = [
    {
        "title": "Class Routine",
        "type": "webview",
        "url": f"{BASE_URL}/Gallery/ShowGalleryGeneral?id=4affa6d7-c154-4b8f-a2b6-d8efff49f659",
        "thumbnail": None,
        "batches": [],
    },
    {
        "title": "Semester Plan",
        "type": "webview",
        "url": f"{BASE_URL}/Gallery/ShowGalleryGeneral?id=eb7b0844-c6ef-4ff3-84e2-a8078d0f68d9",
        "thumbnail": None,
        "batches": [],
    },
    {
        "title": "B.Sc.CSIT Batch Photos",
        "type": "webview",
        "url": f"{BASE_URL}/Gallery/ShowGalleryGeneral?id=c2c8f553-ba9e-45eb-921c-c3fa29cce85f",
        "thumbnail": None,
        "batches": [],
    },
]

SEM_WORDS = {
    "first": "1", "second": "2", "third": "3", "fourth": "4",
    "fifth": "5", "sixth": "6", "seventh": "7", "eighth": "8",
    "i": "1", "ii": "2", "iii": "3", "iv": "4",
    "v": "5", "vi": "6", "vii": "7", "viii": "8",
}

SKIP_HREF = [
    "facebook.com", "office.com", "outlook.com",
    "Account/login", "Account/Login", "Home/Index", "javascript",
]

ENROLLED_SKIP = [
    "admission procedure", "pre-registration", "admission requirement",
    "brochure", "new admission",
]

# ── Attachment label heuristics — because MAC writes "Click Here" everywhere ──
ATTACHMENT_LABELS = [
    (re.compile(r"attendance", re.I),      "Attendance Sheet"),
    (re.compile(r"result",     re.I),      "Result"),
    (re.compile(r"notice",     re.I),      "Notice"),
    (re.compile(r"routine",    re.I),      "Class Routine"),
    (re.compile(r"syllabus",   re.I),      "Syllabus"),
    (re.compile(r"question",   re.I),      "Question Paper"),
    (re.compile(r"form",       re.I),      "Form"),
    (re.compile(r"schedule",   re.I),      "Schedule"),
    (re.compile(r"plan",       re.I),      "Semester Plan"),
]


# ═══════════════════════════════════════════════════════════════════════════════
# UTILS
# ═══════════════════════════════════════════════════════════════════════════════

def fetch(url, retries=3):
    for attempt in range(retries):
        try:
            r = requests.get(url, headers=HEADERS, timeout=15)
            r.raise_for_status()
            return r
        except Exception as e:
            if attempt == retries - 1:
                print(f"    [FAIL] {url} — {e}")
                return None
            time.sleep(2 * (attempt + 1))


def save_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def load_json(path):
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            try:
                return json.load(f)
            except Exception:
                return {}
    return {}


def parse_date(s):
    for fmt in ("%m/%d/%Y", "%m/%d/%y"):
        try:
            return datetime.strptime(s.strip(), fmt).replace(tzinfo=timezone.utc).isoformat()
        except ValueError:
            continue
    return ""


def body_hash(text):
    return hashlib.md5(text.encode()).hexdigest()


def extract_batches(text):
    return list(dict.fromkeys(re.findall(r"\b(20[6-9]\d)\b", text)))


def extract_semester(text):
    t = text.lower()
    m = re.search(r"sem[- ]?([ivx\d]+)", t)
    if m:
        v = m.group(1)
        return SEM_WORDS.get(v, v if v.isdigit() else "")
    m = re.search(r"(first|second|third|fourth|fifth|sixth|seventh|eighth)\s+semester", t)
    if m:
        return SEM_WORDS.get(m.group(1), "")
    m = re.search(r"(\d+)\s*(?:st|nd|rd|th)?\s+semester", t)
    if m:
        return m.group(1)
    m = re.search(r"semester\s+(i{1,3}|iv|v|vi{0,3}|viii?)\b", t)
    if m:
        return SEM_WORDS.get(m.group(1), "")
    return ""


def extract_year(text):
    m = re.search(r"(first|second|third|fourth)\s+year", text.lower())
    return m.group(1) if m else ""


def is_skip_link(href):
    if not href or href == "#":
        return True
    if href.startswith("/"):
        return True
    return any(s in href for s in SKIP_HREF)


def is_enrolled_relevant(title):
    t = title.lower()
    return not any(k in t for k in ENROLLED_SKIP)


def smart_attachment_label(raw_label, url, notice_title, index):
    """
    MAC's notices almost always say 'Click Here' or just 'Here'.
    Try to infer a useful label from the notice title or URL instead.
    """
    # If the raw label is actually descriptive, keep it
    if len(raw_label) > 6 and raw_label.lower() not in ("click here", "here", "here.", "click"):
        return raw_label

    # Try to match keywords from the notice title
    for pattern, label in ATTACHMENT_LABELS:
        if pattern.search(notice_title):
            suffix = f" {index + 1}" if index > 0 else ""
            return label + suffix

    # Fall back to URL-based typing
    if ".pdf" in url.lower():
        return f"PDF Document {index + 1}"
    if "sharepoint.com" in url:
        return f"SharePoint Folder {index + 1}"
    if "drive.google.com" in url:
        return f"Google Drive {index + 1}"

    return f"Attachment {index + 1}"


def att_type(href):
    if ".pdf" in href.lower():
        return "pdf"
    if "sharepoint.com" in href:
        return "sharepoint"
    if "drive.google.com" in href:
        return "gdrive"
    return "link"


# ═══════════════════════════════════════════════════════════════════════════════
# NOTICES
# ═══════════════════════════════════════════════════════════════════════════════

def classify_notice(title, cat_label):
    cat = (cat_label or "").lower()
    t = title.lower()
    if cat == "examination" or any(
        k in t for k in ["exam", "result", "preboard", "terminal", "attendance", "form fill", "marksheet"]
    ):
        return "examination"
    if cat == "iost" or "iost" in t:
        return "iost"
    if any(k in t for k in ["admission", "registration"]):
        return "admission"
    return "general"


def scrape_notice_list():
    """
    /Notice/readmorenotice has all notices on one page (no pagination found).
    The page renders each notice twice (tab duplication) — dedup by URL.
    """
    r = fetch(f"{BASE_URL}/Notice/readmorenotice")
    if not r:
        return []

    soup = BeautifulSoup(r.text, "html.parser")
    notices = {}

    for a in soup.find_all("a", href=re.compile(r"/Notice/noticedetail/")):
        href = a["href"]
        url = BASE_URL + href if href.startswith("/") else href
        title = a.get_text(strip=True)

        if not title or url in notices or not is_enrolled_relevant(title):
            continue

        parent_text = a.parent.get_text(" ", strip=True)
        date_m = re.search(r"(\d{1,2}/\d{1,2}/\d{4})", parent_text)
        cat_m = re.search(r"\((\w+)\)", parent_text)

        date_str = date_m.group(1) if date_m else ""
        category = classify_notice(title, cat_m.group(1) if cat_m else "")

        notices[url] = {
            "id": url.split("/")[-1],
            "title": title,
            "url": url,
            "date_str": date_str,
            "date_iso": parse_date(date_str),
            "category": category,
            "batches": extract_batches(title),
            "semester": extract_semester(title),
            "year": extract_year(title),
        }

    return list(notices.values())


def scrape_notice_detail(notice):
    r = fetch(notice["url"])
    if not r:
        notice.update(body="", body_hash="", attachments=[])
        return notice, "fetch_failed"

    soup = BeautifulSoup(r.text, "html.parser")

    title_heading = next(
        (tag for tag in soup.find_all(["h2", "h3", "h4"])
         if notice["title"][:25] in tag.get_text()),
        None
    )

    body_text = ""
    attachments = []

    if title_heading:
        container = title_heading.find_parent("div") or title_heading.parent
        parts = []
        for el in container.children:
            text = el.get_text(" ", strip=True) if hasattr(el, "get_text") else str(el).strip()
            if "Notice Board" in text:
                break
            if text:
                parts.append(text)
        body_text = "\n".join(parts).strip()

        seen = set()
        for idx, a in enumerate(container.find_all("a", href=True)):
            href = a["href"]
            if is_skip_link(href) or href in seen:
                continue
            seen.add(href)
            raw_label = a.get_text(strip=True)
            attachments.append({
                "label": smart_attachment_label(raw_label, href, notice["title"], len(attachments)),
                "url": href,
                "type": att_type(href),
            })

    notice["body"] = body_text
    notice["body_hash"] = body_hash(body_text)
    notice["attachments"] = attachments

    if not notice["batches"]:
        notice["batches"] = extract_batches(body_text)
    if not notice["semester"]:
        notice["semester"] = extract_semester(body_text)
    if not notice["year"]:
        notice["year"] = extract_year(body_text)

    return notice, "ok"


def run_notices():
    print("\n── NOTICES ──────────────────────────────────────────")
    out_path = f"{DATA_DIR}/notices.json"
    old = load_json(out_path)
    existing = {n["url"]: n for n in old.get("notices", [])}

    notices = scrape_notice_list()
    print(f"  Found {len(notices)} notices")

    enriched, new_count, updated_count, failed = [], 0, 0, []

    for i, notice in enumerate(notices):
        url = notice["url"]
        cached = existing.get(url)

        if cached and cached.get("body") is not None:
            # Re-scrape only if we suspect the content changed.
            # Heuristic: notices older than 30 days are unlikely to change.
            date_iso = notice.get("date_iso", "")
            is_recent = date_iso > "2024-01-01"  # rough cutoff

            if cached.get("body_hash") and not is_recent:
                # Old notice, trust cache
                merged = {**notice, "body": cached["body"], "body_hash": cached["body_hash"], "attachments": cached["attachments"]}
                enriched.append(merged)
                print(f"  [{i+1}/{len(notices)}] CACHED   {notice['title'][:65]}")
                continue

            # Recent or unhashed — re-fetch to detect changes
            print(f"  [{i+1}/{len(notices)}] CHECKING {notice['title'][:65]}")
            updated, status = scrape_notice_detail(notice)
            if status == "fetch_failed":
                # Keep old data if fetch fails
                enriched.append({**notice, "body": cached.get("body", ""), "body_hash": cached.get("body_hash", ""), "attachments": cached.get("attachments", [])})
                failed.append(url)
            else:
                if updated["body_hash"] != cached.get("body_hash"):
                    updated_count += 1
                    print(f"    → content changed, updated")
                enriched.append(updated)
            time.sleep(DETAIL_DELAY)
        else:
            print(f"  [{i+1}/{len(notices)}] FETCHING {notice['title'][:65]}")
            result, status = scrape_notice_detail(notice)
            enriched.append(result)
            if status == "fetch_failed":
                failed.append(url)
            else:
                new_count += 1
            time.sleep(DETAIL_DELAY)

    enriched.sort(key=lambda n: n.get("date_iso", ""), reverse=True)

    save_json(out_path, {
        "scraped_at": datetime.now(timezone.utc).isoformat(),
        "count": len(enriched),
        "new_this_run": new_count,
        "updated_this_run": updated_count,
        "failed_urls": failed,
        "notices": enriched,
    })
    print(f"  Saved {len(enriched)} ({new_count} new, {updated_count} updated, {len(failed)} failed)")


# ═══════════════════════════════════════════════════════════════════════════════
# NEWS / EVENTS
# ═══════════════════════════════════════════════════════════════════════════════

def scrape_news_list():
    r = fetch(f"{BASE_URL}/NewsEvent/readmorenews")
    if not r:
        return []

    soup = BeautifulSoup(r.text, "html.parser")
    items = {}

    for a in soup.find_all("a", href=re.compile(r"/NewsEvent/noticedetail/")):
        href = a["href"]
        url = BASE_URL + href if href.startswith("/") else href
        if url in items:
            continue

        title = a.get_text(strip=True)
        img = a.find("img")
        thumb = ""
        if img and img.get("src"):
            src = img["src"]
            thumb = BASE_URL + src if src.startswith("/") else src

        parent_text = a.parent.get_text(" ", strip=True)
        date_m = re.search(r"(\d{1,2}/\d{1,2}/\d{4})", parent_text)
        date_str = date_m.group(1) if date_m else ""

        if not title:
            continue

        items[url] = {
            "id": url.split("/")[-1],
            "title": title,
            "url": url,
            "thumbnail": thumb,
            "date_str": date_str,
            "date_iso": parse_date(date_str),
            "batches": extract_batches(title),
        }

    return list(items.values())


def run_news():
    print("\n── NEWS / EVENTS ────────────────────────────────────")
    out_path = f"{DATA_DIR}/news.json"

    items = scrape_news_list()
    print(f"  Found {len(items)} news/events")

    items.sort(key=lambda n: n.get("date_iso", ""), reverse=True)

    save_json(out_path, {
        "scraped_at": datetime.now(timezone.utc).isoformat(),
        "count": len(items),
        "items": items,
    })
    print(f"  Saved {len(items)} news items → {out_path}")


# ═══════════════════════════════════════════════════════════════════════════════
# DOWNLOADS  (student + general, both paginated)
# ═══════════════════════════════════════════════════════════════════════════════

def scrape_download_pages(base_url, section):
    all_rows = []
    for page in range(1, MAX_PAGES):
        url = f"{base_url}?page={page}" if page > 1 else base_url
        r = fetch(url)
        if not r:
            break

        soup = BeautifulSoup(r.text, "html.parser")
        table = soup.find("table")
        if not table:
            break

        rows = []
        for tr in table.find_all("tr")[1:]:
            tds = tr.find_all("td")
            if len(tds) < 5:
                continue
            program = tds[1].get_text(strip=True)
            level = tds[2].get_text(strip=True)
            dl_type = tds[3].get_text(strip=True)
            a = tds[4].find("a")
            if not a or not a.get("href"):
                continue
            href = a["href"]
            rows.append({
                "section": section,
                "program": program,
                "level": level,
                "semester": extract_semester(level),
                "type": dl_type,
                "url": href,
                "att_type": att_type(href),
            })

        all_rows.extend(rows)
        print(f"    page {page}: {len(rows)} rows")

        has_next = bool(soup.find("a", href=re.compile(r"\?page=\d+")))
        if not has_next or not rows:
            break

        time.sleep(1)

    return all_rows


def run_downloads():
    print("\n── DOWNLOADS ────────────────────────────────────────")
    out_path = f"{DATA_DIR}/downloads.json"

    print("  Student downloads:")
    student = scrape_download_pages(f"{BASE_URL}/Downloads/ShowStudentDownloadDetails", "student")

    print("  General downloads:")
    general = scrape_download_pages(f"{BASE_URL}/Downloads/ShowOtherDownloadDetails", "general")

    all_dl = student + general

    def sort_key(d):
        try:
            sem = int(d.get("semester") or 99)
        except ValueError:
            sem = 99
        return (d.get("program", ""), sem, d.get("type", ""))

    all_dl.sort(key=sort_key)

    save_json(out_path, {
        "scraped_at": datetime.now(timezone.utc).isoformat(),
        "count": len(all_dl),
        "student_count": len(student),
        "general_count": len(general),
        "downloads": all_dl,
    })
    print(f"  Saved {len(all_dl)} downloads → {out_path}")


# ═══════════════════════════════════════════════════════════════════════════════
# GALLERY
# ═══════════════════════════════════════════════════════════════════════════════

def scrape_browse_gallery():
    r = fetch(f"{BASE_URL}/Gallery/BrowseGallery")
    if not r:
        return []

    soup = BeautifulSoup(r.text, "html.parser")
    items = []

    for a in soup.find_all("a", href=True):
        href = a["href"]
        if is_skip_link(href) or "macpokhara.edu.np" in href:
            continue
        img = a.find("img")
        if not img:
            continue

        title = a.get_text(strip=True)
        thumb = img.get("src", "")
        if thumb.startswith("/"):
            thumb = BASE_URL + thumb

        items.append({
            "title": title,
            "type": "album",
            "url": href,
            "thumbnail": thumb,
            "batches": extract_batches(title),
        })

    return items


def run_gallery():
    print("\n── GALLERY ──────────────────────────────────────────")
    out_path = f"{DATA_DIR}/gallery.json"

    print("  Scraping BrowseGallery...")
    albums = scrape_browse_gallery()
    print(f"  Found {len(albums)} albums")

    all_items = STATIC_GALLERIES + albums

    save_json(out_path, {
        "scraped_at": datetime.now(timezone.utc).isoformat(),
        "count": len(all_items),
        "note": "type=webview → open in WebView | type=album → open URL directly",
        "items": all_items,
    })
    print(f"  Saved {len(all_items)} gallery items → {out_path}")


# ═══════════════════════════════════════════════════════════════════════════════
# ENTRY POINT
# ═══════════════════════════════════════════════════════════════════════════════

if __name__ == "__main__":
    start = datetime.now(timezone.utc)
    print(f"MAC Hub scraper — {start.strftime('%Y-%m-%d %H:%M UTC')}")

    run_notices()
    run_news()
    run_downloads()
    run_gallery()

    elapsed = (datetime.now(timezone.utc) - start).seconds
    print(f"\nAll done in {elapsed}s.")
