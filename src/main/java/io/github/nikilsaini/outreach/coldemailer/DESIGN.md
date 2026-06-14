# Cold Email SaaS (Gmail Only) - LLD/HLD

## Objective

Build a Gmail-only cold email platform where:

* User signs in using Google OAuth
* User sends an initial cold email
* User configures followups
* AI generates followup content
* Followups are sent automatically
* All followups remain in the same Gmail thread

---

# Scope (V1)

## Supported

* Google OAuth Login
* Gmail API Integration
* Send Initial Email
* Generate Followups using AI
* Schedule Followups
* Automatic Followup Sending
* Pause Campaign
* Resume Campaign

## Not Supported (Future)

* Reply Detection
* Skip Weekends
* Open Tracking
* Outlook Support
* Campaign Analytics
* Team Accounts

---

# High Level Design

```text
                    +----------------+
                    | React / NextJS |
                    +-------+--------+
                            |
                            |
                            v
                    +----------------+
                    | Spring Boot API|
                    +-------+--------+
                            |
      +---------------------+---------------------+
      |                     |                     |
      v                     v                     v

+-------------+   +----------------+   +----------------+
| PostgreSQL  |   | OpenAI Service |   | Gmail API      |
+-------------+   +----------------+   +----------------+

                            |
                            |
                            v
                  +-------------------+
                  | Scheduler (Cron)  |
                  +-------------------+
```

---

# Authentication Flow

```text
User
  |
  v
Google OAuth Login
  |
  v
Google Callback API
  |
  v
Exchange Auth Code
  |
  v
Access Token + Refresh Token
  |
  v
Store Refresh Token
```

Only Refresh Token is stored.

Access Tokens are generated whenever required.

---

# Campaign Creation Flow

```text
Create Campaign Request
        |
        |
        +--> Send Initial Email
        |
        +--> Receive:
        |       - Gmail Thread ID
        |       - Gmail Message ID
        |
        +--> Generate AI Followups
        |
        +--> Save Campaign
        |
        +--> Save Followups
        |
        +--> Return Success
```

---

# Scheduler Flow

Runs every minute.

```text
Cron
  |
  +--> Fetch Due Followups
  |
  +--> Lock Followup
  |
  +--> Send Email
  |
  +--> Update Status
  |
  +--> Update Campaign Stats
```

---

# Database Design

## users

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    gmail_email VARCHAR(255) NOT NULL UNIQUE,

    encrypted_refresh_token TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

---

## campaigns

One campaign represents one email conversation/thread.

```sql
CREATE TABLE campaigns (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    recipient_email VARCHAR(255) NOT NULL,

    subject TEXT NOT NULL,

    initial_body TEXT NOT NULL,

    gmail_thread_id VARCHAR(255) NOT NULL,

    root_message_id VARCHAR(255) NOT NULL,

    total_followups INTEGER NOT NULL,

    followups_sent INTEGER NOT NULL DEFAULT 0,

    status VARCHAR(50) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Campaign Status

```text
ACTIVE
PAUSED
COMPLETED
FAILED
```

---

## followups

Each followup is stored as an independent row.

```sql
CREATE TABLE followups (
    id BIGSERIAL PRIMARY KEY,

    campaign_id BIGINT NOT NULL,

    sequence_number INTEGER NOT NULL,

    body TEXT NOT NULL,

    scheduled_at TIMESTAMP NOT NULL,

    sent_at TIMESTAMP,

    gmail_message_id VARCHAR(255),

    status VARCHAR(50) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Followup Status

```text
PENDING
PROCESSING
SENT
FAILED
```

---

# Indexing Strategy

This is critical for scalability.

Scheduler query:

```sql
SELECT *
FROM followups
WHERE status = 'PENDING'
AND scheduled_at <= NOW()
LIMIT 100;
```

Without indexes:

* Full table scan
* Does not scale

Create composite index:

```sql
CREATE INDEX idx_followups_status_schedule
ON followups(status, scheduled_at);
```

Benefits:

* Direct lookup of pending followups
* No full table scan
* Scales to millions of followup rows

---

# Due Followup Query

```sql
SELECT *
FROM followups
WHERE status = 'PENDING'
AND scheduled_at <= NOW()
ORDER BY scheduled_at
LIMIT 100;
```

This query is executed by the scheduler every minute.

---

# Prevent Duplicate Sends

Multiple scheduler instances may run simultaneously.

Use row locking.

Example:

```sql
SELECT *
FROM followups
WHERE status = 'PENDING'
AND scheduled_at <= NOW()
FOR UPDATE SKIP LOCKED
LIMIT 100;
```

Or:

```text
PENDING
   ->
PROCESSING
```

inside a transaction.

This guarantees a followup is sent exactly once.

---

# Gmail Threading

When first email is sent:

Store:

```text
gmail_thread_id
root_message_id
```

For followups:

Use:

```text
threadId
```

returned by Gmail.

Also include:

```text
In-Reply-To
References
```

headers.

Result:

```text
Initial Email
      |
Followup 1
      |
Followup 2
      |
Followup 3
```

appears as a single Gmail conversation.

---

# Followup Scheduling

Example:

```text
Today = 1 Jan 10:00 AM

Followups = 3

Gap = 3 Days
```

Generated Schedule:

```text
Followup 1 -> 4 Jan 10:00 AM
Followup 2 -> 7 Jan 10:00 AM
Followup 3 -> 10 Jan 10:00 AM
```

Store exact execution times in database.

No runtime calculations needed.

---

# APIs

## Generate Google Login URL

```http
GET /auth/google/url
```

---

## OAuth Callback

```http
GET /auth/google/callback
```

---

## Create Campaign

```http
POST /campaigns
```

Request:

```json
{
  "recipientEmail": "john@example.com",
  "subject": "Software Engineer Opportunity",
  "body": "Hi John...",
  "followups": 3,
  "gapDays": 3,
  "preferredHour": 10
}
```

---

## List Campaigns

```http
GET /campaigns
```

---

## Campaign Details

```http
GET /campaigns/{id}
```

---

## Pause Campaign

```http
POST /campaigns/{id}/pause
```

---

## Resume Campaign

```http
POST /campaigns/{id}/resume
```

---

# AI Integration

At campaign creation:

```text
Initial Email
      |
      v
OpenAI API
      |
      v
Generate N Followups
      |
      v
Store Followups
```

AI is never called during scheduler execution.

Scheduler should only:

```text
Fetch
Send
Update
```

---

# Scaling Strategy

## MVP

```text
Postgres
+
Cron
+
Composite Index
```

Suitable for:

* Thousands of users
* Millions of followup rows

---

## Future Scale

Replace:

```text
Cron
  +
Database Polling
```

With:

```text
Delayed Queue

SQS
RabbitMQ
Kafka
BullMQ
```

Flow:

```text
Campaign Creation
       |
       v
Schedule Delayed Jobs
       |
       v
Worker Executes Automatically
```

No database polling required.

---

# Future Enhancements

## Reply Detection

```text
users.threads.get(threadId)
```

If recipient replied:

```text
campaign.status = COMPLETED
```

Stop future followups.

---

## Skip Weekends

```text
Saturday -> Monday
Sunday   -> Monday
```

---

## Working Hours

Restrict sends to:

```text
9 AM - 5 PM
```

---

## Analytics

Track:

* Sent
* Replied
* Opened
* Clicked

---

# Technology Stack

Backend:

* Spring Boot

Database:

* PostgreSQL

ORM:

* Spring Data JPA

Scheduler:

* Spring @Scheduled

Email Provider:

* Gmail API

AI:

* OpenAI API

Authentication:

* Google OAuth 2.0
