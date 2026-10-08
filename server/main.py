from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import httpx, re, time
from bs4 import BeautifulSoup
from urllib.parse import quote

app = FastAPI(title="RodeArena API", version="1.0.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

OPGG = "https://op.gg/tr/lol/modes/arena/{slug}/build?patch={patch}&region=global"
CDRAGON = "https://raw.communitydragon.org/latest/cdragon/arena/tr_tr.json"
cache = {"meta": None, "ts": 0}


def norm(s: str) -> str:
    s = s.lower().strip()
    s = s.replace("ı","i").replace("ğ","g").replace("ü","u").replace("ş","s").replace("ö","o").replace("ç","c")
    return re.sub(r"[^a-z0-9]+", "", s)

async def get_arena_meta():
    if cache["meta"] and time.time() - cache["ts"] < 3600:
        return cache["meta"]
    async with httpx.AsyncClient(timeout=30, follow_redirects=True) as c:
        r = await c.get(CDRAGON)
        r.raise_for_status()
        data = r.json()
    augments = {}
    for a in data.get("augments", []):
        augments[norm(a.get("name", ""))] = {
            "name": a.get("name"), "rarity": a.get("rarity"),
            "icon": "https://raw.communitydragon.org/latest/game/" + a.get("iconLarge", "").replace("assets/", "assets/")
        }
    # CommunityDragon's Arena JSON is the authoritative metadata map for Arena augment icons/names.
    items = {}
    for item in data.get("items", []):
        name = item.get("name", "")
        if name:
            items[norm(name)] = {
                "name": name,
                "id": item.get("id"),
                "rarity": item.get("rarity"),
                "icon": "https://raw.communitydragon.org/latest/game/" + item.get("iconPath", "").replace("/", "/") if item.get("iconPath") else None
            }
    meta = {"augments": augments, "items": items}
    cache["meta"], cache["ts"] = meta, time.time()
    return meta


def percent_games(text):
    m = re.search(r"(\d+(?:\.\d+)?)%\s*(?:([\d,]+)\s*(?:Games|Oyun|Spellen))?", text, re.I)
    if not m: return None, None
    pct = float(m.group(1)); games = int(m.group(2).replace(',', '')) if m.group(2) else None
    return pct, games


def extract_tables(html):
    soup = BeautifulSoup(html, "html.parser")
    out = []
    for table in soup.find_all("table"):
        rows = []
        for tr in table.find_all("tr"):
            cells = [c.get_text(" ", strip=True) for c in tr.find_all(["th","td"])]
            if cells: rows.append(cells)
        if rows: out.append(rows)
    return out

async def fetch_page(slug: str, patch: str):
    url = OPGG.format(slug=quote(slug), patch=patch, region="global")
    async with httpx.AsyncClient(timeout=30, follow_redirects=True, headers={"User-Agent":"RodeArena/1.0"}) as c:
        r = await c.get(url)
        r.raise_for_status()
        return r.text

@app.get("/health")
async def health(): return {"ok": True}

@app.get("/api/champion/{slug}")
async def champion(slug: str, patch: str = "16.20"):
    try:
        html = await fetch_page(slug, patch)
        tables = extract_tables(html)
        meta = await get_arena_meta()
    except Exception as e:
        raise HTTPException(502, f"Veri kaynağına ulaşılamadı: {e}")

    # OP.GG may alter HTML classes. We identify rows by visible section/table content,
    # then normalize names against Riot/CDragon metadata. Unknown rows are retained.
    aug = {"silver": [], "gold": [], "prismatic": []}
    prism_items, normal_items = [], []
    for rows in tables:
        for row in rows[1:]:
            if not row: continue
            name = row[0]
            pct, games = percent_games(" ".join(row))
            if pct is None: continue
            n = norm(name)
            if n in meta["augments"]:
                a = meta["augments"][n]
                rarity = a.get("rarity")
                bucket = "silver" if rarity == 0 else "gold" if rarity == 1 else "prismatic" if rarity == 4 else None
                if bucket:
                    aug[bucket].append({"name": a["name"], "pickRate": pct, "games": games, "iconUrl": a.get("icon")})
            else:
                # Classification of OP.GG final-item rows is resolved with Arena item metadata when available.
                item = meta["items"].get(n)
                obj = {"name": item["name"] if item else name, "pickRate": pct, "games": games,
                       "iconUrl": item.get("icon") if item else None, "rarity": item.get("rarity") if item else None}
                if item and item.get("rarity") is not None and item.get("rarity") >= 4:
                    prism_items.append(obj)
                else:
                    normal_items.append(obj)
    for v in aug.values(): v.sort(key=lambda x: x["pickRate"], reverse=True)
    prism_items.sort(key=lambda x: x["pickRate"], reverse=True)
    normal_items.sort(key=lambda x: x["pickRate"], reverse=True)
    return {"champion": slug, "patch": patch, "source": "OP.GG", "augments": {k:v[:10] for k,v in aug.items()},
            "prismaticItems": prism_items[:10], "normalItems": normal_items[:10]}
