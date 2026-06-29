# API Reference

Base URL: `http://localhost:8080`

All `/campaigns/**` endpoints require `Authorization: Bearer <authToken>` header.
`/oauth/**` and `/auth/**` are public.

## Auth

### GET /oauth/google/login
Returns the Google authorization URL as a JSON-encoded URI string.

**Auth:** none  
**Response:** `200 "https://accounts.google.com/o/oauth2/v2/auth?..."`  (a bare JSON string)

### GET /oauth/google/callback?code={code}
Called by Google after user consent. Upserts the user, issues JWTs, redirects to the frontend.

**Auth:** none  
**Query params:** `code` — authorization code from Google  
**Response:** `302` redirect to `{app.frontend-url}/?authToken={jwt}&refreshToken={jwt}`

### POST /auth/refresh
Exchange a valid refresh JWT for a new auth JWT.

**Auth:** none  
**Request body:**
```json
{ "refreshToken": "<refresh_jwt>" }
```
**Response `200`:**
```json
{ "authToken": "<new_auth_jwt>" }
```
**Response `401`:** if token is invalid, expired, or is not a refresh token (body empty).

---

## Campaigns

### POST /campaigns
Create a campaign: sends the initial email immediately via Gmail, generates AI follow-up bodies via Gemini, schedules follow-ups.

**Auth:** Bearer  
**Request body:**
```json
{
  "recipientEmail": "jane@example.com",
  "subject":        "Quick intro",
  "initialBody":    "Hi Jane, …",
  "followupCount":  3,
  "gapDays":        3,
  "preferredHour":  10
}
```
- `followupCount` — number of follow-up emails to generate (1–n)
- `gapDays` — days between consecutive follow-ups
- `preferredHour` — hour of day (0–23) follow-ups are scheduled at (minute/second = 0)

**Response `201`:** `CampaignResponse` (see shapes below)  
**Error `404`:** user not found (shouldn't happen if JWT is valid)  
**Error `500`:** Gmail send failed or Gemini call failed (campaign not created)

### GET /campaigns
List all campaigns for the authenticated user, newest first.

**Auth:** Bearer  
**Response `200`:** `CampaignResponse[]`

### GET /campaigns/{id}
Get a single campaign with all follow-ups.

**Auth:** Bearer  
**Response `200`:** `CampaignResponse`  
**Error `404`:** campaign not found

### POST /campaigns/{id}/pause
Pause an ACTIVE campaign. All pending follow-ups are held (scheduler skips PAUSED campaigns).

**Auth:** Bearer  
**Response `200`:** `CampaignResponse`  
**Error `404`:** campaign not found  
**Error `409`:** campaign is not ACTIVE

### POST /campaigns/{id}/resume
Resume a PAUSED campaign. Status reverts to ACTIVE; scheduler picks up overdue follow-ups on the next tick (see [open-decisions.md](open-decisions.md) OPEN-DECISION-1).

**Auth:** Bearer  
**Response `200`:** `CampaignResponse`  
**Error `404`:** campaign not found  
**Error `409`:** campaign is not PAUSED

---

## Response shapes

### CampaignResponse
```json
{
  "id":             "uuid",
  "recipientEmail": "jane@example.com",
  "subject":        "Quick intro",
  "initialBody":    "Hi Jane, …",
  "status":         "ACTIVE | PAUSED | COMPLETED | FAILED",
  "createdAt":      "2026-06-29T10:00:00",
  "followups":      [ FollowupResponse, … ]
}
```

### FollowupResponse
```json
{
  "id":             "uuid",
  "sequenceNumber": 1,
  "body":           "Just following up…",
  "status":         "PENDING | PROCESSING | SENT | FAILED",
  "scheduledAt":    "2026-07-02T10:00:00"
}
```
Note: there is no `sentAt` field — the `Followup` entity has no such column. Use `scheduledAt` as the approximate send time for SENT follow-ups.

### Error response (all 4xx/5xx)
```json
{
  "status":    404,
  "error":     "Not Found",
  "message":   "Campaign not found with id: …",
  "timestamp": "2026-06-29T10:00:00.000Z"
}
```

---

## HTTP status summary

| Situation | Status |
|---|---|
| Campaign created | 201 |
| Successful read / action | 200 |
| Missing or invalid JWT | 401 |
| Campaign not found | 404 |
| User not found | 404 |
| Invalid campaign state transition | 409 |
| Encryption / Gmail / Gemini failure | 500 |
