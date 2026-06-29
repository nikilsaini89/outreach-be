# Configuration

All properties are in `src/main/resources/application.properties`. Most can be overridden via environment variable.

## Database

| Property | Env var | Default | Notes |
|---|---|---|---|
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/coldemailer` | Local Postgres — not Docker |
| `spring.datasource.username` | `DB_USERNAME` | `coldemailer` | |
| `spring.datasource.password` | `DB_PASSWORD` | `coldemailer` | |
| `spring.jpa.hibernate.ddl-auto` | — | `update` | Hibernate auto-creates/alters tables |

## Logging

| Property | Env var | Default | Notes |
|---|---|---|---|
| `logging.level.io.github.nikilsaini` | `LOG_LEVEL_APP` | `INFO` | Set `DEBUG` for verbose trace |
| `logging.pattern.console` | — | includes `%kvp` | Renders SLF4J `addKeyValue(k,v)` pairs |

## Google OAuth

| Property | Env var | Default | Notes |
|---|---|---|---|
| `google.oauth.client-id` | `GOOGLE_OAUTH_CLIENT_ID` | **required** | From Google Cloud Console |
| `google.oauth.client-secret` | `GOOGLE_OAUTH_CLIENT_SECRET` | **required** | |
| `google.oauth.redirect-uri` | — | `http://localhost:8080/oauth/google/callback` | Must be registered in Google Cloud Console |
| `google.oauth.auth-uri` | — | `https://accounts.google.com/o/oauth2/v2/auth` | |
| `google.oauth.token-uri` | — | `https://oauth2.googleapis.com/token` | |
| `google.oauth.scopes` | — | `openid,email,profile,https://www.googleapis.com/auth/gmail.send` | |

## JWT

| Property | Env var | Default | Notes |
|---|---|---|---|
| `app.jwt.secret` | `JWT_SECRET` | `ChangeThisSecretKeyInProductionItMustBeLongEnoughForHS256!!` | Must be ≥ 32 bytes (256 bits). 64-char default gives HS512. **Change in production.** |
| `app.jwt.auth-expiry-ms` | `JWT_AUTH_EXPIRY_MS` | `86400000` (24 hours) | Auth token lifetime |
| `app.jwt.refresh-expiry-ms` | `JWT_REFRESH_EXPIRY_MS` | `2592000000` (30 days) | Refresh token lifetime |

## Encryption

| Property | Env var | Default | Notes |
|---|---|---|---|
| `app.encryption.secret-key` | `APP_ENCRYPTION_SECRET_KEY` | **required** | Base64-encoded raw AES key (128/192/256-bit). Generate with `openssl rand -base64 32` for AES-256. |

## Gemini AI

| Property | Env var | Default | Notes |
|---|---|---|---|
| `gemini.api-key` | `GEMINI_API_KEY` | **required** | Never log — used as query param in the request URL |
| `gemini.model` | — | `gemini-2.5-flash` | Gemini model name |

## App

| Property | Env var | Default | Notes |
|---|---|---|---|
| `app.frontend-url` | `FRONTEND_URL` | `http://localhost:5173` | Where the OAuth callback redirects after login |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | `local` | |

## Required env vars for local dev

Minimum set needed to start the application:

```bash
export GOOGLE_OAUTH_CLIENT_ID=...
export GOOGLE_OAUTH_CLIENT_SECRET=...
export APP_ENCRYPTION_SECRET_KEY=$(openssl rand -base64 32)
export GEMINI_API_KEY=...
# JWT_SECRET has a dev default; change it if needed
```

## Running with Java 21

Machine default JAVA_HOME is JDK 17. Always prefix Maven commands:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home mvn spring-boot:run
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home mvn compile
```
