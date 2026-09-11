# Chat third-party login (user PREP)

Date: 2026-09-11. Scope: bgssai-media user app only.

## Rule

Center accounts live in bgssai-chat. Each product may use Chat as a third-party login **on the user
app**. Admin never accepts Chat or user OAuth. This PR only prepares the user path; it does not
complete a live Chat authorization.

## User endpoints

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/api/auth/chat/prepare` | Returns `{ provider: CHAT, status: PREP\|READY, admin_supported: false, ... }`. Never includes client secret. |
| POST | `/api/auth/chat/callback` | Body `{ code, state }`. Missing code → 400. Otherwise 503 until live exchange is wired. **No JWT is issued.** |

Frontend: login button “Chat 第三方登录（预留）” and `/login/chat/callback`.

## Admin

`GET /api/auth/policy` → `{ chat_oauth: false, user_oauth: false, password_login: true }`.
Admin login UI states that Chat is not accepted. No Chat controller in the admin module.

## Properties (literals)

```
bgssai.media.chat.oauth.enabled=false
bgssai.media.chat.oauth.authorize-url=https://chat.bgssai.com/oauth/authorize
bgssai.media.chat.oauth.token-url=https://chat.bgssai.com/oauth/token
bgssai.media.chat.oauth.userinfo-url=https://chat.bgssai.com/oauth/userinfo
bgssai.media.chat.oauth.client-id=bgssai-media-user
bgssai.media.chat.oauth.client-secret=
bgssai.media.chat.oauth.redirect-uri=http://127.0.0.1:3002/login/chat/callback
bgssai.media.chat.oauth.scope=openid profile
```

`client-secret` stays blank in git. READY (authorize URL assembled) requires enabled + URLs +
non-blank secret. Even then callback still returns 503 until a real Chat token exchange is implemented.

## Honest status

PREP. Domestic WECHAT/DOUYIN/BAIDU/ALIPAY buttons remain demo stubs. Chat is documented and fail-closed.
