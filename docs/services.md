# Services

## CampaignService

**File:** `coldemailer/service/CampaignService.java`  
**Deps:** CampaignRepository, UserService, GoogleOAuthService, GmailService, FollowupService

| Method | Signature | What it does |
|---|---|---|
| `createWithFollowups` | `(UUID userId, CreateCampaignRequest) → CampaignResponse` | Looks up user, gets + decrypts Google refresh token, calls Google to get access token, generates follow-up bodies via Gemini, sends initial email via Gmail, saves Campaign + Followups. All in one `@Transactional`. Gmail failure aborts everything. |
| `listForUser` | `(UUID userId) → List<CampaignResponse>` | Validates user exists, returns campaigns ordered newest-first with their follow-ups. `@Transactional(readOnly=true)`. |
| `getById` | `(UUID campaignId) → CampaignResponse` | Fetches campaign + follow-ups or throws `CampaignNotFoundException`. `@Transactional(readOnly=true)`. |
| `pause` | `(UUID campaignId) → CampaignResponse` | Requires status = ACTIVE; sets to PAUSED via JPA dirty-checking (no explicit save). Throws `IllegalCampaignStateException` if wrong state. |
| `resume` | `(UUID campaignId) → CampaignResponse` | Requires status = PAUSED; sets to ACTIVE. Overdue follow-ups send on next scheduler tick. |

---

## FollowupService

**File:** `coldemailer/service/FollowupService.java`  
**Deps:** FollowupRepository, GeminiService

| Method | Signature | What it does |
|---|---|---|
| `generateBodies` | `(String subject, String initialBody, int count) → List<String>` | Delegates to GeminiService. Returns `count` AI-written follow-up email bodies. |
| `save` | `(Campaign, List<String> bodies, int gapDays, int preferredHour) → List<FollowupResponse>` | Creates and saves Followup entities. Sequence starts at 1. `scheduledAt` = now + (seq * gapDays) days, at preferredHour:00:00. |
| `getFollowUpsForCampaign` | `(UUID campaignId) → List<FollowupResponse>` | Returns follow-ups ordered by sequenceNumber ASC. |
| `findDue` | `() → List<Followup>` | `status=PENDING`, `scheduledAt <= now`, campaign `status=ACTIVE`. JOIN FETCHes campaign and user (avoids N+1 in scheduler). |
| `updateStatus` | `(Followup, FollowupStatus) → void` | Sets status and saves explicitly (called from scheduler, which manages its own transaction). |
| `hasOutstanding` | `(UUID campaignId) → boolean` | True if any PENDING or PROCESSING follow-ups remain. |
| `hasFailures` | `(UUID campaignId) → boolean` | True if any FAILED follow-ups exist. |

---

## UserService

**File:** `coldemailer/service/UserService.java`  
**Deps:** UserRepository, EncryptionService

| Method | Signature | What it does |
|---|---|---|
| `upsertFromGoogle` | `(CreateUserRequest) → User` | Find by email; if found, re-encrypt and save new refresh token; if not, create new user. |
| `getById` | `(UUID) → User` | Throws `UserNotFoundException` if missing. |
| `existsById` | `(UUID) → boolean` | Used in `CampaignService.listForUser` to validate userId. |
| `getDecryptedRefreshToken` | `(UUID userId) → String` | Fetches user, decrypts `encrypted_refresh_token`. Used before any Gmail call. |

---

## JwtService

**File:** `auth/jwt/service/JwtService.java`  
**Deps:** JwtProperties

| Method | Signature | What it does |
|---|---|---|
| `generateAuthToken` | `(UUID userId, String email) → String` | Signs JWT with claims: `sub=userId`, `email`, `type="auth"`, `exp=now+authExpiryMs`. |
| `generateRefreshToken` | `(UUID userId) → String` | Signs JWT with claims: `sub=userId`, `type="refresh"`, `exp=now+refreshExpiryMs`. |
| `parseClaims` | `(String token) → Claims` | Validates signature + expiry. Throws `JwtException` on any failure. |
| `isAuthToken` | `(Claims) → boolean` | Checks `claims.get("type") == "auth"`. |
| `isRefreshToken` | `(Claims) → boolean` | Checks `claims.get("type") == "refresh"`. |

Secret key: `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`. The key object is created fresh per call (not cached) — if performance matters, cache it as a field.

---

## GoogleOAuthService

**File:** `auth/oauth/service/GoogleOAuthService.java`  
**Deps:** GoogleOAuthProperties, RestClient, ObjectMapper

| Method | Signature | What it does |
|---|---|---|
| `buildAuthorizationUri` | `() → URI` | Builds Google consent URL with client_id, redirect_uri, scopes (`openid email profile gmail.send`), `access_type=offline`, `prompt=consent`. |
| `exchangeCodeForTokens` | `(String code) → GoogleTokenResponse` | POSTs to `google.oauth.token-uri` with `grant_type=authorization_code`. Returns access + refresh + id token. |
| `refreshAccessToken` | `(String refreshToken) → GoogleTokenResponse` | POSTs to token URI with `grant_type=refresh_token`. Returns new access token. Google refresh tokens are long-lived; no re-storage needed. |
| `decodeIdToken` | `(String idToken) → GoogleIdTokenClaims` | Base64-decodes the JWT payload segment (no signature verification — relies on HTTPS transport). Extracts `email`, `given_name`, `family_name`. |

---

## GmailService

**File:** `coldemailer/service/GmailService.java`  
**Deps:** RestClient

| Method | Signature | What it does |
|---|---|---|
| `sendEmail` | `(accessToken, from, to, subject, body) → GmailSendResponse` | Builds RFC 2822 email, base64url-encodes it, POSTs `{"raw":"..."}` to Gmail API. Returns `{id, threadId}`. |
| `sendFollowup` | `(accessToken, from, to, subject, body, threadId, rootMessageId) → GmailSendResponse` | Builds RFC 2822 reply (prefixes `Re: ` to subject, adds `In-Reply-To` + `References` headers using `rootMessageId`), POSTs with `{"raw":"...", "threadId":"..."}` to keep the reply in the same Gmail thread. |

---

## GeminiService

**File:** `coldemailer/service/GeminiService.java`  
**Deps:** GeminiProperties, RestClient, ObjectMapper

| Method | Signature | What it does |
|---|---|---|
| `generateFollowups` | `(String subject, String initialBody, int count) → List<String>` | Sends a prompt to Gemini asking for `count` follow-up email bodies. Gemini returns a JSON array of strings. `responseMimeType="application/json"` enforces structured output. **Never log the request URL** — it contains the API key as a query param. |

---

## EncryptionService

**File:** `coldemailer/service/EncryptionService.java`  
**Deps:** EncryptionProperties

Algorithm: `AES/GCM/NoPadding`, 12-byte IV, 128-bit GCM tag.

| Method | What it does |
|---|---|
| `encrypt(String plaintext) → String` | Generates random 12-byte IV, encrypts, returns `Base64(IV \|\| ciphertext)`. |
| `decrypt(String encrypted) → String` | Decodes base64, splits IV (first 12 bytes) from ciphertext, decrypts. |

Key derivation: `Base64.getDecoder().decode(properties.secretKey())` → must be 128/192/256-bit raw AES key encoded in standard Base64.
