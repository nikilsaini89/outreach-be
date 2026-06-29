# Authentication

Two separate auth mechanisms work together: Google OAuth 2.0 (to get Gmail access) and our own JWTs (to secure the API).

---

## 1. Login flow (OAuth → JWT)

```
Browser                Backend                 Google
  |                       |                       |
  |-- GET /oauth/google/login                      |
  |                       |-- build auth URI       |
  |<-- 200 "https://accounts.google.com/..."       |
  |                       |                       |
  |-- (redirect to Google consent screen) -------->|
  |                       |                       |
  |<----- GET /oauth/google/callback?code=xxx -----|
  |                       |                       |
  |        exchangeCodeForTokens(code)             |
  |                       |--POST /token--------->|
  |                       |<-- accessToken        |
  |                       |    refreshToken       |
  |                       |    idToken            |
  |                       |                       |
  |        decodeIdToken(idToken) → email, name   |
  |        userService.upsertFromGoogle(...)       |
  |        store encrypted Google refreshToken in DB
  |                       |                       |
  |        jwtService.generateAuthToken(userId, email)
  |        jwtService.generateRefreshToken(userId)
  |                       |                       |
  |<-- 302 {frontendUrl}/?authToken=...&refreshToken=...
```

**Key point:** the Google access token is never stored. Only the Google refresh token is stored (AES/GCM encrypted in `users.encrypted_refresh_token`). Whenever Gmail needs to be called, the backend decrypts it and calls `googleOAuthService.refreshAccessToken(refreshToken)` on the fly.

---

## 2. JWT structure

**Auth token** (expiry: 24h default, controlled by `app.jwt.auth-expiry-ms`):
```json
{
  "sub":   "<userId UUID>",
  "email": "user@gmail.com",
  "type":  "auth",
  "iat":   1234567890,
  "exp":   1234654290
}
```

**Refresh token** (expiry: 30d default, controlled by `app.jwt.refresh-expiry-ms`):
```json
{
  "sub":  "<userId UUID>",
  "type": "refresh",
  "iat":  1234567890,
  "exp":  1236986490
}
```

Both are signed with **HS-HMAC** using the key derived from `app.jwt.secret`. Algorithm (HS256/384/512) is chosen automatically by JJWT based on key length — the 64-char default gives HS512.

---

## 3. Request authentication (JwtAuthFilter)

`JwtAuthFilter` extends `OncePerRequestFilter` and runs before `UsernamePasswordAuthenticationFilter`.

Flow for each request:
1. Read `Authorization` header.
2. If missing or doesn't start with `Bearer `, skip (unauthenticated — Spring Security will reject unless path is permitAll).
3. Extract token string, call `jwtService.parseClaims(token)`.
4. If `claims.type == "auth"`, extract `sub` as UUID, create `UsernamePasswordAuthenticationToken(userId, null, [])` and set it in `SecurityContextHolder`.
5. On any `JwtException` (expired, bad sig, malformed): log at DEBUG, do NOT set authentication (request will be rejected by Spring Security as 401).

---

## 4. Security rules (SecurityConfig)

```
/oauth/**    → permitAll
/auth/**     → permitAll
/**          → authenticated (valid auth JWT required)
```

- CSRF: disabled (stateless REST API)
- Session: STATELESS (no HttpSession)
- Unauthorized response: `401 { "status": 401, "error": "Unauthorized" }` (not a redirect to /login)
- No `UserDetailsService` is used — the dummy bean just suppresses Spring Boot's auto-generated password log.

---

## 5. Accessing the authenticated userId in controllers

```java
// Spring injects Authentication from SecurityContextHolder
public CampaignResponse createCampaign(..., Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    ...
}
```

The principal is the raw `UUID` set by `JwtAuthFilter`. Cast directly — do not call `.getName()`.

---

## 6. Token refresh flow

```
Frontend                Backend
  |                         |
  |-- POST /auth/refresh     |
  |   { refreshToken: "..." }|
  |                         |
  |     parseClaims(token)   |
  |     verify type==refresh |
  |     userService.getById(userId)
  |     generateAuthToken(userId, user.email)
  |                         |
  |<-- 200 { authToken: "..." }
```

**Failure cases → 401:**
- Token is expired
- Token signature invalid
- Token type is not "refresh"
- userId in token doesn't match any user

---

## 7. Encryption of Google refresh tokens

Algorithm: `AES/GCM/NoPadding`, 128-bit GCM auth tag, 12-byte random IV.

Storage format: `Base64(IV || ciphertext)` in `users.encrypted_refresh_token`.

Key: `Base64.getDecoder().decode(app.encryption.secret-key)` — must be a base64-encoded 128, 192, or 256-bit AES key.
