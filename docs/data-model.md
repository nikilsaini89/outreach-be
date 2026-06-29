# Data Model

Database: PostgreSQL. Schema is managed by `spring.jpa.hibernate.ddl-auto=update` — Hibernate auto-creates/alters tables on startup.

## BaseEntity (all tables inherit)

| Java field | DB column | Type | Notes |
|---|---|---|---|
| `id` | `id` | UUID | PK, auto-generated (GenerationType.UUID) |
| `createdAt` | `created_at` | TIMESTAMP | set on insert, never updated |
| `updatedAt` | `updated_at` | TIMESTAMP | set on insert and every update |

Set by `@EnableJpaAuditing` + `AuditingEntityListener`.

---

## Table: `users`

| Java field | DB column | Type | Constraints |
|---|---|---|---|
| `firstName` | `first_name` | VARCHAR | NOT NULL |
| `lastName` | `last_name` | VARCHAR | NOT NULL |
| `email` | `email` | VARCHAR | NOT NULL, UNIQUE |
| `encryptedRefreshToken` | `encrypted_refresh_token` | TEXT | NOT NULL — AES/GCM encrypted Google refresh token |

**Upsert logic:** `UserService.upsertFromGoogle` finds by `email`; if found, updates `encryptedRefreshToken`; if not, creates a new row.

---

## Table: `campaigns`

| Java field | DB column | Type | Constraints |
|---|---|---|---|
| `user` | `user_id` | UUID FK → users | NOT NULL, LAZY |
| `recipientEmail` | `recipient_email` | VARCHAR | NOT NULL |
| `subject` | `subject` | TEXT | NOT NULL |
| `initialBody` | `initial_body` | TEXT | NOT NULL |
| `gmailThreadId` | `gmail_thread_id` | VARCHAR | NOT NULL — Gmail thread ID for the email chain |
| `rootMessageId` | `root_message_id` | VARCHAR | NOT NULL — RFC 2822 Message-ID of the initial email, used in In-Reply-To header |
| `status` | `status` | VARCHAR (enum) | NOT NULL — `ACTIVE | PAUSED | COMPLETED | FAILED` |

**Query:** `CampaignRepository.findByUserIdOrderByCreatedAtDesc(UUID)` — all campaigns for a user.

---

## Table: `followups`

| Java field | DB column | Type | Constraints |
|---|---|---|---|
| `campaign` | `campaign_id` | UUID FK → campaigns | NOT NULL, LAZY |
| `status` | `status` | VARCHAR (enum) | NOT NULL — `PENDING | PROCESSING | SENT | FAILED` |
| `sequenceNumber` | `sequence_number` | INT | NOT NULL — 1-based ordering within the campaign |
| `body` | `body` | TEXT | NOT NULL — AI-generated email body |
| `scheduledAt` | `scheduled_at` | TIMESTAMP | NOT NULL — when the scheduler should send this follow-up |

**No `sentAt` column** — there is no timestamp for when a follow-up was actually sent.

**Scheduler query:** `FollowupRepository.findDueFollowups(PENDING, now, ACTIVE)` — fetches follow-ups with `status = PENDING AND scheduled_at <= now AND campaign.status = ACTIVE`, with FETCH JOIN on campaign and user.

---

## Enums

### CampaignStatus
| Value | Meaning |
|---|---|
| `ACTIVE` | Follow-ups are being sent on schedule |
| `PAUSED` | Scheduler skips this campaign; follow-ups remain PENDING |
| `COMPLETED` | All follow-ups sent successfully |
| `FAILED` | All follow-ups processed but at least one FAILED |

Terminal transition: after the last follow-up is processed, `FollowupScheduler.updateCampaignStatus()` sets the campaign to COMPLETED (no FAILEDs) or FAILED (any FAILEDs).

### FollowupStatus
| Value | Meaning |
|---|---|
| `PENDING` | Waiting to be sent |
| `PROCESSING` | Currently being sent (set before the Gmail call, guards against double-send) |
| `SENT` | Sent successfully |
| `FAILED` | Send attempt failed; no retry (see [open-decisions.md](open-decisions.md) OPEN-DECISION-2) |

---

## Relationships

```
User  1 ──< Campaign  1 ──< Followup
```

- One user has many campaigns (`campaign.user_id FK → users.id`)
- One campaign has many follow-ups (`followup.campaign_id FK → campaigns.id`)
- Both associations are `FetchType.LAZY`
