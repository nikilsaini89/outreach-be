# Backend Knowledge Base

Agent guide: read the file that matches your task. You rarely need more than two files.

| File | Read when you need to… |
|---|---|
| [architecture.md](architecture.md) | Understand package layout, layers, or how components wire together |
| [api.md](api.md) | Look up endpoint paths, request/response shapes, or auth requirements |
| [data-model.md](data-model.md) | Check entity fields, DB columns, enums, or table relationships |
| [auth.md](auth.md) | Trace the OAuth→JWT flow or understand how security filters work |
| [services.md](services.md) | Find the right service method signature or understand business logic |
| [configuration.md](configuration.md) | Find a property name, its env var override, or its default value |
| [open-decisions.md](open-decisions.md) | Understand known gaps or deferred behaviour before modifying the scheduler |

## Stack at a glance

- **Spring Boot 4.1.0** / Spring Framework 7 / Spring Security 7
- **JDK 21** (machine default is 17 — run Maven with `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`)
- **PostgreSQL** (local, port 5432, role+db both named `coldemailer`)
- **JJWT 0.12.6** for JWT generation/validation (HS-HMAC, key from `app.jwt.secret`)
- **Google OAuth 2.0** — `gmail.send` + `openid email profile` scopes
- **Gemini AI** (`gemini-2.5-flash`) for follow-up body generation
- **AES/GCM/NoPadding** for encrypting Google refresh tokens at rest

## Secret logging rule (never violate)

Never log: access tokens, refresh tokens, `app.encryption.secret-key`, id_token payload, Gemini API key (it appears in the URL as a query param — never log the URL), or email bodies.
