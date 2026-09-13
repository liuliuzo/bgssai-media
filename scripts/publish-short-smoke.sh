#!/usr/bin/env bash
# Smoke the short → media ingest contract. Does not prove live studio E2E.
set -euo pipefail

BASE="${MEDIA_BASE_URL:-http://127.0.0.1:8081}"
TOKEN="${MEDIA_INGEST_TOKEN:-local-ingest-token-change-me}"
WORK_ID="${1:-smoke-work}"
EP_ID="${2:-1}"
FILM_ID="${3:-smoke-film}"

PAYLOAD=$(cat <<JSON
{
  "source_system": "bgssai-short",
  "source_work_id": "${WORK_ID}",
  "source_episode_id": "${EP_ID}",
  "source_film_id": "${FILM_ID}",
  "title": "Short smoke ${WORK_ID}",
  "cover_url": "https://picsum.photos/seed/bgssai-media/720/1280",
  "video_url": "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
  "duration_sec": 15,
  "aspect_ratio": "9:16",
  "language": "zh-CN",
  "tags": ["smoke"],
  "status": "READY",
  "approved": true,
  "idempotency_key": "short:${WORK_ID}:${EP_ID}:${FILM_ID}"
}
JSON
)

echo "POST ${BASE}/bgssai/user/media/ingest/short-drama"
FIRST=$(curl -sS -X POST "${BASE}/bgssai/user/media/ingest/short-drama" \
  -H "Content-Type: application/json" \
  -H "X-Bgssai-Ingest-Token: ${TOKEN}" \
  -H "Idempotency-Key: short:${WORK_ID}:${EP_ID}:${FILM_ID}" \
  -d "${PAYLOAD}")
echo "${FIRST}"
echo "POST replay (same idempotency_key; must return existing catalog, replayed=true)"
curl -sS -X POST "${BASE}/bgssai/user/media/ingest/short-drama" \
  -H "Content-Type: application/json" \
  -H "X-Bgssai-Ingest-Token: ${TOKEN}" \
  -H "Idempotency-Key: short:${WORK_ID}:${EP_ID}:${FILM_ID}" \
  -d "${PAYLOAD}"
echo
echo "GET ${BASE}/bgssai/user/media/shorts?q=Short"
curl -sS "${BASE}/bgssai/user/media/shorts?q=Short"
echo
