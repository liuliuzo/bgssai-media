# MEDIA hosts

Status: **UNDEPLOYED**

| Key | Value in this repository |
| --- | --- |
| `MEDIA_USER_HOST` | empty |
| `MEDIA_ADMIN_HOST` | empty |

The hosts inventory is a Jenkins secret file (`bgssai-<env>-hosts`), not a file in this repo. `deploy/README.md` already records that both keys are empty and that `dev|prod-media-deploy|stop` are not laid down.

This change does not assign an IP, a hostname, or a fake CDN. Local contract smoke uses `http://127.0.0.1:8081` only. That address is the user process on the machine running the jar, not a MEDIA host.

The format matrix records the same fact at `deployment.status = UNDEPLOYED` with both host fields blank. `FormatMatrix` refuses to load a matrix that fills those fields.

Public read:

`GET /bgssai/user/media/formats` → `result.deployment.status` is `UNDEPLOYED`.
