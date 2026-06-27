# Open Decisions / Deferred Work

Decisions intentionally parked so they aren't lost. Each item notes the current
behaviour (what ships today) and the deferred option to revisit.

---

## OPEN-DECISION-1 — Resume catch-up: overdue follow-ups after a long pause

**Status:** Deferred (parked 2026-06-27).

**Context.** When a campaign is paused, its PENDING follow-ups keep their original
`scheduled_at`. If the campaign stays paused past one or more of those times, those
follow-ups become *overdue*. On resume the campaign flips back to `ACTIVE`.

**Current behaviour (shipped).** `POST /campaigns/{id}/resume` only sets status to
`ACTIVE`. The scheduler (`FollowupScheduler.sendDueFollowups`, runs every minute) then
picks up every follow-up whose `scheduled_at <= now`, so all overdue follow-ups fire on
the next tick — potentially several within the same minute, in `sequence_number` order.

**The problem — burst sending.** Because every overdue follow-up is due the instant the
campaign resumes, the next tick dispatches them all at once. A campaign paused for weeks
could blast ~10 follow-ups within a single second. That is bad on multiple fronts:
- The recipient receives the entire remaining sequence back-to-back — spammy, and the
  whole point of spacing follow-ups is lost.
- It can trip Gmail API rate limits / per-user send quotas, causing some to FAIL
  (which today is terminal — see OPEN-DECISION-2).
- One slow `sendDueFollowups` tick now does many synchronous Gmail calls in one
  transaction.
This is the core thing the resume path must handle, not just a cosmetic spacing nicety.

**Deferred option — "reschedule from today".** On resume, recompute `scheduled_at` for
the remaining PENDING follow-ups using the campaign's original cadence
(`gapDays` / `preferredHour`) starting from the resume date, so the sequence is re-spread
into the future instead of all firing in one burst. (Alternative/companion mitigations if
we don't reschedule: cap how many follow-ups one tick sends per campaign, or throttle
sends with a small delay between them.)

**Why deferred.** Needs cadence to be persisted on the campaign (today `gapDays` and
`preferredHour` are only request inputs at creation — they are not stored on the
`campaigns` table), plus a decision on whether sequence 1 sends immediately or after one
full gap. Revisit when we add the campaign cadence columns.

**Touch points when we pick this up:**
- `campaigns` table: persist `gap_days`, `preferred_hour` (new columns).
- `CampaignService.resume(...)`: recompute `scheduled_at` for remaining PENDING follow-ups.
- Frontend `campaign-detail.html`: the Actions panel copy already says
  "Resuming reschedules them from today" — align copy with whichever behaviour ships.

---

## OPEN-DECISION-2 — Retry for failed follow-up / email send

**Status:** Deferred (parked 2026-06-27).

**Context.** A follow-up send can fail transiently — expired/revoked Google token,
Gmail API 5xx or rate-limit, network blip. Today any failure is terminal for that
follow-up.

**Current behaviour (shipped).** In `FollowupScheduler.sendFollowup(...)`, a thrown
exception sets the follow-up to `FAILED` with no retry. The scheduler's due query only
picks up `PENDING` follow-ups, so a `FAILED` one is never reattempted. If it was the
last outstanding follow-up, `maybeCompleteCampaign(...)` marks the whole campaign
`FAILED`. The initial email send (`CampaignService.createWithFollowups`) has no retry
either — a failure there propagates out of the create request.

**Deferred option — bounded retry with backoff.** Reattempt a failed send a limited
number of times (e.g. 3) with exponential backoff before giving up, ideally
distinguishing retryable errors (5xx, rate-limit, timeout) from permanent ones
(revoked consent, invalid recipient) so we don't burn retries on hopeless cases.

**Why deferred.** Needs a few schema/design choices:
- Track attempts: add `attempt_count` (and maybe `last_error`, `next_retry_at`) to
  `followups`, so retries survive restarts and stay idempotent.
- Decide the trigger: reuse the existing 1-minute scheduler (re-query rows whose
  `next_retry_at <= now`) vs. Spring Retry / `@Retryable` around the Gmail call.
- Possibly a distinct status (e.g. `RETRYING`) vs. reusing `PENDING` with a future
  `next_retry_at` — affects the dashboard badge set and `FollowupStatus`.
- Define the terminal hand-off: after max attempts → `FAILED` (current behaviour), and
  how that rolls up to campaign status.

**Touch points when we pick this up:**
- `FollowupStatus` enum + frontend badges if a `RETRYING` state is added.
- `followups` table: `attempt_count`, `last_error`, `next_retry_at`.
- `FollowupScheduler.sendFollowup(...)`: increment attempts, classify the error,
  schedule the next retry or mark `FAILED`.
- `FollowupRepository.findDueFollowups(...)`: include rows due for retry.
- Initial-email send path in `CampaignService.createWithFollowups(...)`: decide whether
  the first email retries inline or the failure is surfaced to the user to resend.
