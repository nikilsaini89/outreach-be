# Knowledge Base

Agent guide: navigate to the sub-folder for the repo you are working in, then open the file that matches your task.

## Repos

| Folder | Repo | Stack |
|---|---|---|
| [backend/](backend/README.md) | `nikilsaini89/outreach-be` | Spring Boot 4.1 · JDK 21 · PostgreSQL · JJWT |
| `frontend/` | `nikilsaini89/outreach-fe` | React 19 · TypeScript 5.7 · Vite 6 · Tailwind v4 |

## Cross-cutting rules

- **Never log secrets:** access tokens, refresh tokens, AES encryption key, id_token payload, Gemini API key (appears as URL query param), email bodies.
- **JDK 21 required:** machine default is JDK 17 — prefix Maven with `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`.
- **Local Postgres:** port 5432, role + db both named `coldemailer` (not Docker).
- **Feature branches:** backend changes go on `feat/react-app`; frontend changes go on `feat/jwt-auth`.
