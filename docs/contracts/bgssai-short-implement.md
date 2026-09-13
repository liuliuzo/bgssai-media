# bgssai-short implement note

User report: **short PR #54 (idempotent publish) is already merged**.
This Cloud Agent still cannot clone https://github.com/liuliuzo/bgssai-short
(GitHub 404 / no App access), so the short-side diff was not re-read here.

Media ingest now matches that publish contract:

- `POST /bgssai/user/media/ingest/short-drama`
- UNIQUE `idempotency_key` = `short:{source_work_id}:{source_episode_id}:{source_film_id}`
- Accept READY approved packs (`status=READY|APPROVED` or omitted; `approved` not false)
- Replay returns the existing catalog entry (`replayed=true`), no second catalog row

## Goal

When a short episode is finished (metadata + media asset refs) **and approved READY**,
the short publish job POSTs the shared contract to media. short does not become a
distribution channel. Retry with the same key is safe.

## Suggested API (short PR #54)

`POST /api/publish/media`

- Auth: user JWT `Jwttoken`, `@NeedAop(roles=USER)`
- Body (snake_case): `{ "source_work_id", "source_episode_id", "source_film_id" }`
- Load finished title / cover / video_url / duration from short's own tables
- Package with the same rules as `ShortDramaPublishJob.packageFinished`
- POST `{bgssai.short.media.base-url}/bgssai/user/media/ingest/short-drama`
- Header `X-Bgssai-Ingest-Token: {bgssai.short.media.ingest-token}`
- Header `Idempotency-Key` optional (body key is required and must match the formula)

## Properties (literals, no `${}`, no real secrets)

```
bgssai.short.media.base-url=http://127.0.0.1:8081
bgssai.short.media.ingest-token=local-ingest-token-change-me
```

## Idempotency

`short:{source_work_id}:{source_episode_id}:{source_film_id}`

Media UNIQUE-upserts on that key. Duplicate publish must return the first catalog
`media_id` / `play_url`.

## Do not

- Host a public play/share channel inside short
- Commit production ingest tokens
- Treat FAILED ingest as published
- Wire admin of short to Chat user login
- Send DRAFT / unapproved packs to media

## Cross-link

After both PRs exist, comment both with each other's URL.
Media documents the private-repo gap until this agent can read short.
