# Open Decisions

Known gaps and deferred behaviour. Read this before touching the scheduler or campaign state machine.

---

## OPEN-DECISION-1: Resume burst

**Problem:** When a campaign is resumed after being paused for several days, all overdue follow-ups (with `scheduledAt` in the past) fire immediately on the next scheduler tick — potentially sending multiple emails within 60 seconds.

**Current behaviour:** `CampaignService.resume()` just sets `status = ACTIVE`. The scheduler query finds all `PENDING` follow-ups with `scheduledAt <= now` and dispatches them all in one tick.

**What's needed to fix it:**
- Add `gapDays` and `preferredHour` columns to the `campaigns` table (not currently stored)
- On resume, update `scheduledAt` for all PENDING follow-ups: `new base = now + (seq * gapDays) days, at preferredHour`
- This reschedules from today, preserving the original cadence

**Where to look:** `CampaignService.resume()`, `FollowupService.save()`, `FollowupRepository`

---

## OPEN-DECISION-2: Retry for failed sends

**Problem:** If a Gmail send fails (e.g., expired access token, network error), the follow-up is immediately marked `FAILED` with no retry. The campaign eventually transitions to `FAILED` status.

**Current behaviour:** `FollowupScheduler.sendFollowup()` catches any exception, calls `followupService.updateStatus(followup, FAILED)`, and moves on.

**What's needed to fix it:**
- Add `attempt_count INT`, `last_error TEXT`, `next_retry_at TIMESTAMP` columns to `followups` table
- On failure: increment `attempt_count`, set `last_error`, compute `next_retry_at` (exponential backoff), set status back to `PENDING`
- Cap retries at some max (e.g., 3 attempts), then permanently mark `FAILED`

**Where to look:** `FollowupScheduler.sendFollowup()`, `Followup` entity, `FollowupRepository`

---

## OPEN-DECISION-3: Cancel remaining follow-ups

**Problem:** There is no endpoint to cancel (delete or permanently skip) remaining follow-ups on a campaign. The frontend has a "Cancel remaining follow-ups" button that is wired up but disabled.

**What's needed:**
- New endpoint: `DELETE /campaigns/{id}/followups` or `POST /campaigns/{id}/cancel`
- Set all PENDING follow-ups to FAILED (or a new CANCELLED status)
- Set campaign status to FAILED or a new CANCELLED status
- Wire up the frontend button

**Where to look:** `CampaignController`, `CampaignService`, `FollowupRepository`

---

## OPEN-DECISION-4: No sentAt on Followup

**Problem:** The `Followup` entity has no `sentAt` column. The frontend Timeline component uses `scheduledAt` as a proxy for when SENT follow-ups were actually delivered, which may differ if there was a delay.

**What's needed:**
- Add `sent_at TIMESTAMP` column to `followups` (nullable)
- Set it in `FollowupScheduler.sendFollowup()` after a successful Gmail send
- Expose it in `FollowupResponse`
- Update frontend Timeline to show `sentAt` for SENT follow-ups
