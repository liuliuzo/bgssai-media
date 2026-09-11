# bgssai-short implement note (could not push)

This Cloud Agent run (`bc-15d9593e-1222-589c-8b7a-9bfaf28b15ac`) could not clone or open a PR on
https://github.com/liuliuzo/bgssai-short (GitHub 404 / no App access). The publish **contract**
and packager live in **bgssai-media**. Apply the following on short `develop` when access exists.

## Goal

When a short episode is finished (metadata + media asset refs), expose a publish API or job that
POSTs the shared contract to media. short does not become a distribution channel.

## Suggested API

`POST /api/publish/media`

- Auth: user JWT `Jwttoken`, `@NeedAop(roles=USER)`
- Body (snake_case): `{ "source_work_id", "source_episode_id", "source_film_id" }`
- Load finished title / cover / video_url / duration from short's own tables
- Package with the same rules as `ShortDramaPublishJob.packageFinished`
- POST `{bgssai.short.media.base-url}/bgssai/user/media/ingest/short-drama`
- Header `X-Bgssai-Ingest-Token: {bgssai.short.media.ingest-token}`

## Properties (literals, no `${}`, no real secrets)

```
bgssai.short.media.base-url=http://127.0.0.1:8081
bgssai.short.media.ingest-token=local-ingest-token-change-me
```

## Idempotency

`short:{source_work_id}:{source_episode_id}:{source_film_id}`

## Do not

- Host a public play/share channel inside short
- Commit production ingest tokens
- Treat FAILED ingest as published
- Wire admin of short to Chat user login

## Cross-link

After the short PR exists, comment both PRs with each other's URL.
Media PR documents this gap until then.
