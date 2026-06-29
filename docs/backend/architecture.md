# Architecture

## Package layout

Root: `io.github.nikilsaini.outreach`

```
outreach/
├── ColdEmailerApplication.java          @SpringBootApplication + @EnableScheduling
│                                        + @EnableJpaAuditing + @ConfigurationPropertiesScan
│
├── auth/                                Everything authentication-related
│   ├── config/
│   │   └── SecurityConfig               Spring Security filter chain (stateless JWT)
│   ├── controller/
│   │   └── AuthController               POST /auth/refresh
│   ├── jwt/
│   │   ├── config/JwtProperties         @ConfigurationProperties("app.jwt")
│   │   ├── dto/RefreshRequest           { refreshToken }
│   │   ├── dto/TokenResponse            { authToken }
│   │   ├── filter/JwtAuthFilter         OncePerRequestFilter — reads Bearer token
│   │   └── service/JwtService           generate / validate / parse JWTs
│   └── oauth/
│       ├── config/GoogleOAuthProperties @ConfigurationProperties("google.oauth")
│       ├── config/RestClientConfig       @Bean RestClient googleRestClient
│       ├── constant/GoogleOAuthConstants static String constants for OAuth params
│       ├── controller/GoogleOAuthController  GET /oauth/google/login + /callback
│       ├── dto/request/                 TokenExchangeRequest, RefreshTokenRequest
│       ├── dto/response/                GoogleTokenResponse, GoogleIdTokenClaims
│       ├── exception/InvalidIdTokenException
│       ├── service/GoogleOAuthService   build auth URI, exchange code, refresh token, decode id_token
│       └── util/GoogleOAuthUriBuilder   static URI builder
│
└── coldemailer/                         Core domain
    ├── config/
    │   ├── EncryptionProperties         @ConfigurationProperties("app.encryption")
    │   └── GeminiProperties             @ConfigurationProperties("gemini")
    ├── controller/
    │   └── CampaignController           /campaigns CRUD + pause/resume
    ├── dto/
    │   ├── mapper/                      CampaignMapper, FollowupMapper, UserMapper (static methods)
    │   ├── request/                     CreateCampaignRequest, CreateUserRequest, GeminiRequest
    │   └── response/                    CampaignResponse, FollowupResponse, GmailSendResponse, GeminiResponse
    ├── entity/                          BaseEntity, User, Campaign, Followup
    ├── enums/                           CampaignStatus, FollowupStatus
    ├── exception/                       GlobalExceptionHandler + domain exceptions
    ├── repository/                      CampaignRepository, FollowupRepository, UserRepository
    ├── scheduler/
    │   └── FollowupScheduler            @Scheduled(fixedDelay=60s) — sends due follow-ups
    └── service/
        ├── CampaignService
        ├── EncryptionService            AES/GCM/NoPadding
        ├── FollowupService
        ├── GeminiService                calls Gemini REST API
        ├── GmailService                 calls Gmail REST API
        └── UserService
```

## Request lifecycle

```
HTTP request
  → JwtAuthFilter (reads Authorization: Bearer, sets UUID principal in SecurityContext)
  → Spring Security authz check (permitAll for /oauth/**, /auth/**)
  → Controller (extracts UUID from Authentication.getPrincipal())
  → Service (business logic + DB via Repository)
  → Mapper (Entity → Response DTO)
  → JSON response
```

## Key conventions

- **DTOs are Java records** — immutable, field names map directly to JSON via Jackson.
- **ConfigurationProperties are records** — auto-picked up by `@ConfigurationPropertiesScan`.
- **Mappers are static utility classes** — no Spring bean, no state.
- **JPA auditing** — `BaseEntity` supplies `id` (UUID, auto-generated), `createdAt`, `updatedAt` via `@EnableJpaAuditing`.
- **Dirty checking** — pause/resume use `campaign.setStatus(...)` inside a `@Transactional` method; no explicit `save()` needed.
- **Logging** — SLF4J 2.x fluent API: `log.atInfo().setMessage("…").addKeyValue("k", v).log()`. Never log secrets.
- **Error handling** — `GlobalExceptionHandler` (`@RestControllerAdvice`) maps domain exceptions to structured JSON: `{status, error, message, timestamp}`.
- **Scheduler** — `FollowupScheduler.sendDueFollowups()` runs every 60 s (fixedDelay). It queries `followupRepository.findDueFollowups(PENDING, now, ACTIVE)` — only follows up on ACTIVE campaigns with past `scheduledAt`.
