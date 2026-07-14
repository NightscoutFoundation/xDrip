# CHG7 — Adoptions from upstream PR #4510 (robustness and volume-snooze details)

| | |
|---|---|
| **Change ID** | CHG7 |
| **Date** | 2026-07-10 |
| **Status** | Implemented (approved 2026-07-10) |
| **Module** | app (phone) |
| **Source** | Comparison review of [PR #4510](https://github.com/NightscoutFoundation/xDrip/pull/4510) against CHG1–CHG6 (2026-07-08); items A1, A2, A3 and A5 of that report. Item A4 is registered separately as [CHG8](CHG8-notification-snooze-line.md). |
| **Related** | [CHG2](CHG2-fullscreen-snooze-on-lockscreen.md), [CHG3](CHG3-volume-key-snooze.md), [CHG4](CHG4-volume-key-double-press.md), [CHG6](CHG6-review-chg1-chg5.md) |

## A1 — Orphaned-alert / NPE robustness cluster

Pre-existing upstream bugs, fixed in PR #4510 and now adopted:

- **`SnoozeActivity.snoozeForType()`** dereferenced `activeBgAlert.above` without a null
  check — a reproducible `NullPointerException` when disabling high/low alerts while the
  active alert's `AlertType` is missing (test alerts, removed alert types). Inline null
  guards added; *disable all alerts* still clears the record regardless.
- **`ActiveBgAlert.alertTypegetOnly()`** destructively deleted the active alert record
  whenever the `AlertType` lookup failed. That made test alerts unsnoozable (their
  temporary `AlertType` is never saved) and hid orphaned state instead of handling it.
  The destructive `ClearData()` is removed; the lookup is now side-effect free.
- **`EditAlertActivity` REMOVE ALERT** deleted the `AlertType` but left the
  `ActiveBgAlert` record behind (and any playing sound running). The remove handler now
  stops and clears the active alert when its UUID matches the removed alert type
  (`stopAlert(ClearData)` — which via the CHG6 hook also closes any open overlay/lock
  snooze screen) and calls `recheckAlerts()`.
- **Orphan cleanup in the volume-key handler** (`SnoozeActivity.volumeKeySnooze()`): a
  snoozed record whose `AlertType` no longer exists is cleared on the first volume-key
  press, after which the keys return to normal volume behaviour.
- **`displayStatus()`** now shows *"Alert type not found"* (new string
  `alert_type_not_found`) instead of only logging an error when the record is orphaned.

## A2 — Double press bound to the same alert (UUID tracking)

The second volume-key press only snoozes when it is for the **same alert** as the first
press: the first press stores `alert_uuid`, the second press must match it (in addition to
the same key code and the 1.5 s window of CHG4). This prevents a stale first press from one
alert carrying over to a different alert that appears within the window. The pending UUID
is cleared on snooze and by `resetVolumeKeyConfirmation()` (CHG6 hook).

## A3 — Notification channel importance for the full-screen intent

On Android 8+, a full-screen intent (the CHG2 fallback path) is silently ignored when the
notification channel importance is below `IMPORTANCE_HIGH`. With xDrip's opt-in dynamic
channels (`use_notification_channels`), all channels were created with
`IMPORTANCE_DEFAULT`. Because Android never allows raising the importance of an existing
channel (the PR's runtime-upgrade approach is silently ignored), this is solved at
creation time:

- In `NotificationChannels.getChan(...)` (both overloads), channels derived from
  `BG_ALERT_CHANNEL` are created with `IMPORTANCE_HIGH` and carry a `!` marker in their
  channel id; existing installs therefore migrate to a fresh high-importance channel and
  the old default-importance channel is deleted. The sound-change probe in
  `my_text_hash()` includes the same marker so the channel-rotation machinery keeps
  working.
- In `AlertPlayer`, when a full-screen intent is set, the importance of the channel the
  notification actually resolved to is checked; if it is below HIGH (and not the legacy
  `IMPORTANCE_UNSPECIFIED` of the pre-Oreo compatibility channel, where the notification
  priority still applies), a rate-limited event-log warning explains that the lock-screen
  snooze screen may not appear. This also covers users who manually demote the channel.

## A5 — Volume-snooze detail improvements

- **Per-button toasts:** first press now says which button to press again (*"Press the
  volume DOWN/UP button again to snooze"*; mute keeps the generic text), the snoozing
  toast names the button, and pressing a *different* volume button within the window shows
  *"Double press the same volume button to snooze"*.
- **Seconds display:** the snooze screen status shows *"75 seconds left"* instead of
  *"0 minutes left"* when fewer than 120 seconds remain until the alert re-rises.
- **xDrip in the foreground:** when xDrip itself is the foreground app, the snooze screen
  now pops up on alert without requiring the CHG1 preference or the overlay permission
  (no permission is needed to start an activity from the foreground). This also covers
  testing alerts from within the app.
- **Snooze-time default:** `AlertPlayer.GuessDefaultSnoozeTime()` falls back to the
  type-based default (`getDefaultSnooze(above)`: 120 min high / 35 min low) when the
  alert's `default_snooze` is 0, matching the notification-dismiss logic.

## Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/eveningoutpost/dexdrip/models/ActiveBgAlert.java` | A1: `alertTypegetOnly()` no longer destructively clears the record |
| `app/src/main/java/com/eveningoutpost/dexdrip/EditAlertActivity.java` | A1: REMOVE ALERT stops/clears a matching active alert + `recheckAlerts()` |
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` | A1: `snoozeForType()` null guards, orphan cleanup in `volumeKeySnooze()`, "Alert type not found" status; A2: UUID tracking; A5: per-button/wrong-button toasts, seconds display |
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeOverlayActivity.java` | A5: `isXdripForeground()` — overlay shown without preference/permission when xDrip itself is foreground |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java` | A3: channel-importance diagnostic at notify time; A5: type-based snooze default in `GuessDefaultSnoozeTime()` |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/NotificationChannels.java` | A3: BG alert channels created with `IMPORTANCE_HIGH` under a new id; old channel deleted; hash probe updated |
| `app/src/main/res/values/strings.xml` | New strings: `alert_type_not_found`, per-button confirm/snoozing texts, wrong-button text |

All code insertion points are marked with a `CHG7 A<n>` comment. The Wear module is
untouched.

## Fix summary

| Item | Severity | Outcome | Files changed |
|---|---|---|---|
| A1 — orphaned-alert / NPE robustness cluster | medium | Fixed | `SnoozeActivity.java` (`snoozeForType()` null guards, orphan cleanup in `volumeKeySnooze()`, "Alert type not found" status), `ActiveBgAlert.java` (non-destructive `alertTypegetOnly()`), `EditAlertActivity.java` (REMOVE ALERT stops/clears matching active alert), `strings.xml` (`alert_type_not_found`) |
| A2 — double press bound to the same alert (UUID) | low | Fixed | `SnoozeActivity.java` (`pendingVolumeKeyUuid` tracking, cleared on snooze and by `resetVolumeKeyConfirmation()`) |
| A3 — channel importance for the full-screen intent | medium | Fixed — *refined per [CHG13-final-review.md](CHG13-final-review.md) ER2 (2026-07-12); the `NotificationChannels` part was re-implemented per [CHG14](CHG14-adopt-upstream-notificationchannels.md) (2026-07-13) on the reworked upstream channel code* | `NotificationChannels.java` (since CHG14: upstream base + gated `!`-migration port), `AlertPlayer.java` (rate-limited importance diagnostic at notify time — unchanged) |
| A5 — volume-snooze detail improvements | low | Fixed | `SnoozeActivity.java` (per-button/wrong-button toasts, seconds display), `SnoozeOverlayActivity.java` (`isXdripForeground()` bypass), `AlertPlayer.java` (type-based snooze default in `GuessDefaultSnoozeTime()`), `strings.xml` (per-button texts) |

Item A4 of the same comparison review is registered and implemented separately as
[CHG8](CHG8-notification-snooze-line.md).

## Deliberately not adopted from the PR (differences kept)

- Held-button auto-repeats still do not count as a double press (CHG4 decision).
- Volume keys stay inactive while an alert is silently snoozed (CHG4 decision; the PR
  re-snoozes/extends in that state).
- The duplicated per-screen key handling of the PR is not taken over; everything remains
  in the single shared handler.
- The PR's runtime channel-importance upgrade is replaced by the creation-time approach
  described under A3, because Android ignores upward importance changes on existing
  channels.

## Test plan

1. A1: create + test-fire an alert via *Glucose Level Alerts List → Test alert* → the
   snooze screen shows the alert and volume double press snoozes it; afterwards the status
   shows "Alert type not found" if opened again, and one volume press clears the orphan
   (keys then adjust volume normally).
2. A1: while an alert is actively sounding, REMOVE the alert type → sound stops, record
   cleared, open snooze screens close (CHG6 hook), no snooze remnants.
3. A1: disable high/low/all alerts while a test alert is active → no crash.
4. A2: press volume-down during alert A; make alert A end and a new alert B fire within
   1.5 s (lab scenario) → the next press is a *first* press for B, no instant snooze.
5. A3: with *use notification channels* enabled, fire an alert → a new "Glucose alerts"
   channel (id with `!`) exists with importance High; the old channel is gone; with a
   manually demoted channel the event log contains the importance warning.
6. A5: first press volume-down → toast names DOWN; press volume-up within 1.5 s → "Double
   press the same volume button" toast; double volume-up → snoozing toast names UP.
7. A5: snooze screen with < 2 minutes until re-rise shows seconds.
8. A5: alert fired while xDrip itself is open (CHG1 preference off, no overlay permission)
   → the snooze screen still pops up; snoozing returns to the previous xDrip screen.
9. A5: alert with `default_snooze` 0 snoozed via volume keys → snoozed for 120 min (high)
   c.q. 35 min (low) instead of the per-alert value.
10. Regression: CHG1–CHG6 behaviour otherwise unchanged (overlay/lock screen, double
    press, volume blocking, auto-close, status line).
