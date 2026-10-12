#!/usr/bin/env bash
# Local short + long idempotent ingest smoke.
# Talks to a user jar on this machine. Does not invent or call a MEDIA host.
set -euo pipefail

BASE="${MEDIA_BASE_URL:-http://127.0.0.1:8081}"
TOKEN="${MEDIA_INGEST_TOKEN:-local-ingest-token-change-me}"
WORK_ID="${1:-p05-smoke}"
STAMP="${WORK_ID}"

python3 - "$BASE" "$TOKEN" "$STAMP" <<'PY'
import json, sys, urllib.error, urllib.request

base, token, stamp = sys.argv[1:]

def call(method, path, body=None, headers=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for key, value in (headers or {}).items():
        req.add_header(key, value)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            raw = resp.read().decode()
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode()
        raise SystemExit(f"HTTP {exc.code} {path}: {raw}")
    return json.loads(raw)

formats = call("GET", "/bgssai/user/media/formats")
deployment = formats["result"]["deployment"]
if deployment.get("status") != "UNDEPLOYED":
    raise SystemExit(f"expected UNDEPLOYED, got {deployment}")
if deployment.get("media_user_host") or deployment.get("media_admin_host"):
    raise SystemExit(f"host fields must stay empty: {deployment}")
print("formats deployment=UNDEPLOYED hosts empty")

def publish(path, body):
    headers = {
        "X-Bgssai-Ingest-Token": token,
        "Idempotency-Key": body["idempotency_key"],
    }
    return call("POST", path, body, headers)

short_body = {
    "source_system": "bgssai-short",
    "source_work_id": stamp,
    "source_episode_id": "1",
    "source_film_id": "film",
    "title": f"Short smoke {stamp}",
    "video_url": "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
    "duration_sec": 15,
    "aspect_ratio": "9:16",
    "language": "zh-CN",
    "status": "READY",
    "approved": True,
    "idempotency_key": f"short:{stamp}:1:film",
}
long_body = {
    "source_system": "bgssai-long",
    "source_work_id": stamp,
    "source_episode_id": "1",
    "source_film_id": "film",
    "source_version": "1",
    "title": f"Long smoke {stamp}",
    "video_url": "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
    "duration_sec": 30,
    "aspect_ratio": "16:9",
    "language": "zh-CN",
    "status": "READY",
    "approved": True,
    "idempotency_key": f"long:{stamp}:1:film:v1",
}

def assert_pair(label, path, body):
    first = publish(path, body)
    second = publish(path, body)
    if not first.get("success"):
        raise SystemExit(f"{label} first failed: {first}")
    if not second.get("success"):
        raise SystemExit(f"{label} replay failed: {second}")
    a = first["result"]
    b = second["result"]
    if a.get("status") != "READY" or a.get("media_id") in (None, ""):
        raise SystemExit(f"{label} did not become READY: {a}")
    if b.get("replayed") is not True or b.get("media_id") != a.get("media_id"):
        raise SystemExit(f"{label} replay was not idempotent: {a} vs {b}")
    hook = a.get("format_hook") or {}
    if hook.get("web_decision") != "PLAY" or hook.get("verification") != "NOT_VERIFIED":
        raise SystemExit(f"{label} format_hook unexpected: {hook}")
    print(f"{label} media_id={a['media_id']} replayed=true")
    return a["media_id"]

assert_pair("short", "/bgssai/user/media/ingest/short", short_body)
assert_pair("long", "/bgssai/user/media/ingest/long", long_body)

library = call("GET", f"/bgssai/user/media/library?q={stamp}")
rows = library["result"]["list"]
titles = {row.get("title") for row in rows}
catalogs = {row.get("catalog") for row in rows}
if f"Short smoke {stamp}" not in titles or f"Long smoke {stamp}" not in titles:
    raise SystemExit(f"library missing smoke titles: {rows}")
if "short" not in catalogs or "long" not in catalogs:
    raise SystemExit(f"library catalogs: {catalogs}")
print(f"library rows={len(rows)} catalogs={sorted(catalogs)}")
PY
