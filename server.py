"""
OTT Channel Server - FastAPI
Serves channel list with genres, programs, and HLS stream URLs.
Run: uvicorn server:app --host 0.0.0.0 --port 8000
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from datetime import datetime, timedelta, timezone

app = FastAPI(title="OTT Channel API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------------------------------------------------------------------
# Helper – generate a rolling program schedule (24 h, repeating)
# ---------------------------------------------------------------------------
def _make_programs(channel_id: str, titles: list[dict]) -> list[dict]:
    """Build a 24-hour program list starting from the previous midnight UTC."""
    now = datetime.now(timezone.utc)
    start = now.replace(hour=0, minute=0, second=0, microsecond=0)
    programs = []
    cursor = start
    idx = 0
    while cursor < start + timedelta(hours=24):
        entry = titles[idx % len(titles)]
        end = cursor + timedelta(minutes=entry["duration_min"])
        programs.append({
            "title":       entry["title"],
            "description": entry.get("description", ""),
            "start_utc":   cursor.strftime("%Y%m%d%H%M%S +0000"),
            "stop_utc":    end.strftime("%Y%m%d%H%M%S +0000"),
            "genre":       entry.get("genre", "UNDEFINED"),
            "icon":        entry.get("icon", ""),
            "channel_id":  channel_id,
        })
        cursor = end
        idx += 1
    return programs


# ---------------------------------------------------------------------------
# Channel definitions – all 5 HLS streams from the provided list
# ---------------------------------------------------------------------------
CHANNELS = [
    {
        "id":     "ch1",
        "number": "1",
        "name":   "LongTail Captions",
        "url":    "http://playertest.longtailvideo.com/adaptive/captions/playlist.m3u8",
        "type":   "HLS",
        "logo":   "",
        "genre":  "TECH_SCIENCE",
        "group":  "Test",
        "description": "HLS adaptive stream with captions demo by LongTail Video.",
    },
    {
        "id":     "ch2",
        "number": "2",
        "name":   "Skate 4K Phantom",
        "url":    "http://sample.vodobox.net/skate_phantom_flex_4k/skate_phantom_flex_4k.m3u8",
        "type":   "HLS",
        "logo":   "",
        "genre":  "SPORTS",
        "group":  "Sports",
        "description": "Skateboarding filmed with a Phantom Flex 4K camera.",
    },
    {
        "id":     "ch3",
        "number": "3",
        "name":   "Uplynk Live",
        "url":    "https://content.uplynk.com/channel/3353345690554b0e8de31bcd6b73fc37.m3u8",
        "type":   "HLS",
        "logo":   "",
        "genre":  "NEWS",
        "group":  "Live",
        "description": "Live channel powered by Uplynk CDN.",
    },
    {
        "id":     "ch4",
        "number": "4",
        "name":   "Kaltura Multi-Bitrate",
        "url":    "http://cdnapi.kaltura.com/p/1878761/sp/187876100/playManifest/entryId/1_2xvajead/flavorIds/1_tl01409m,1_kptb3ez8,1_re3akioy,1_wuylsxwp/format/applehttp/protocol/http/a.m3u8",
        "type":   "HLS",
        "logo":   "",
        "genre":  "MOVIES",
        "group":  "VOD",
        "description": "Multi-bitrate HLS stream from Kaltura platform.",
    },
    {
        "id":     "ch5",
        "number": "5",
        "name":   "Kaltura Sample",
        "url":    "http://cdnbakmi.kaltura.com/p/243342/sp/24334200/playManifest/entryId/0_uka1msg4/flavorIds/1_vqhfu6uy,1_80sohj7p/format/applehttp/protocol/http/a.m3u8",
        "type":   "HLS",
        "logo":   "",
        "genre":  "FAMILY_KIDS",
        "group":  "VOD",
        "description": "Sample HLS stream from Kaltura CDN backup.",
    },
]

# ---------------------------------------------------------------------------
# Program schedules per channel
# ---------------------------------------------------------------------------
PROGRAMS_TEMPLATE = {
    "ch1": [
        {"title": "Tech Captions Demo",      "duration_min": 30,  "genre": "TECH_SCIENCE", "description": "Live captioning technology demonstration."},
        {"title": "Adaptive Streaming 101",  "duration_min": 45,  "genre": "EDUCATION",    "description": "Introduction to HLS adaptive bitrate streaming."},
        {"title": "Web Video Standards",     "duration_min": 60,  "genre": "TECH_SCIENCE", "description": "Overview of modern web video codecs and protocols."},
    ],
    "ch2": [
        {"title": "Skate Phantom 4K",        "duration_min": 60,  "genre": "SPORTS",       "description": "Slow-motion skateboarding at 2500fps."},
        {"title": "Street Skating Highlights","duration_min": 30,  "genre": "SPORTS",       "description": "Best street skating moments."},
        {"title": "Skate Culture",           "duration_min": 45,  "genre": "LIFESTYLE",     "description": "Documentary on modern skateboarding culture."},
    ],
    "ch3": [
        {"title": "Morning News",            "duration_min": 60,  "genre": "NEWS",          "description": "Top stories from around the world."},
        {"title": "Business Report",         "duration_min": 30,  "genre": "NEWS",          "description": "Financial markets and economy updates."},
        {"title": "Weather & Sports",        "duration_min": 30,  "genre": "NEWS",          "description": "Local weather forecast and sports scores."},
        {"title": "Evening News",            "duration_min": 60,  "genre": "NEWS",          "description": "Comprehensive evening news broadcast."},
    ],
    "ch4": [
        {"title": "Feature Film",            "duration_min": 120, "genre": "MOVIES",        "description": "Tonight's feature presentation."},
        {"title": "Short Films Block",       "duration_min": 60,  "genre": "MOVIES",        "description": "Curated independent short films."},
        {"title": "Cinema Classics",         "duration_min": 90,  "genre": "MOVIES",        "description": "Classic cinema from the golden age of Hollywood."},
    ],
    "ch5": [
        {"title": "Kids Cartoons",           "duration_min": 30,  "genre": "FAMILY_KIDS",   "description": "Animated series for children."},
        {"title": "Nature for Kids",         "duration_min": 30,  "genre": "FAMILY_KIDS",   "description": "Educational nature documentary for young viewers."},
        {"title": "Story Time",              "duration_min": 30,  "genre": "FAMILY_KIDS",   "description": "Interactive storytelling for preschoolers."},
        {"title": "Family Movie",            "duration_min": 90,  "genre": "FAMILY_KIDS",   "description": "Family-friendly movie night."},
    ],
}


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@app.get("/channels")
def get_channels():
    """Return full channel list."""
    return {"channels": CHANNELS, "count": len(CHANNELS)}


@app.get("/meta/{channel_id}")
def get_meta(channel_id: str):
    """Return metadata for a single channel."""
    ch = next((c for c in CHANNELS if c["id"] == channel_id), None)
    if not ch:
        return JSONResponse(status_code=404, content={"error": "Channel not found"})
    return ch


@app.get("/stream/{channel_id}")
def stream(channel_id: str):
    """Return raw HLS URL for a channel (redirect-friendly)."""
    ch = next((c for c in CHANNELS if c["id"] == channel_id), None)
    if not ch:
        return JSONResponse(status_code=404, content={"error": "Channel not found"})
    return ch["url"]


@app.get("/programs/{channel_id}")
def get_programs(channel_id: str):
    """Return today's program schedule for a channel."""
    if channel_id not in PROGRAMS_TEMPLATE:
        return JSONResponse(status_code=404, content={"error": "Programs not found"})
    programs = _make_programs(channel_id, PROGRAMS_TEMPLATE[channel_id])
    return {"channel_id": channel_id, "programs": programs, "count": len(programs)}


@app.get("/epg")
def get_full_epg():
    """Return full EPG: all channels with their programs."""
    result = []
    for ch in CHANNELS:
        tmpl = PROGRAMS_TEMPLATE.get(ch["id"], [])
        programs = _make_programs(ch["id"], tmpl) if tmpl else []
        result.append({**ch, "programs": programs})
    return {"epg": result}
