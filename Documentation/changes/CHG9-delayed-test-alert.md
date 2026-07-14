# CHG9 — Test alert fires after a 5-second delay

| | |
|---|---|
| **Change ID** | CHG9 |
| **Date** | 2026-07-10 |
| **Status** | Implemented (requested with immediate implementation, 2026-07-10) |
| **Module** | app (phone) |
| **Related** | [CHG1](CHG1-snooze-over-other-apps.md), [CHG2](CHG2-fullscreen-snooze-on-lockscreen.md) — the presentations this makes testable; [CHG7](CHG7-pr4510-adoptions.md) A1 — made test alerts snoozable |

## Request

On the **Edit Alert** screen, pressing *Test alert* should fire the alert **after
5 seconds** instead of immediately. This gives the user time to switch to another app or
turn the screen off first, so the CHG1 overlay (snooze screen over the app in use) and the
CHG2 lock-screen presentation can actually be exercised with a test alert.

## Implementation

In `EditAlertActivity.testAlert()`:

- Input **validation and the existing informational toasts remain immediate** (invalid
  input still fails at the press, before the delay).
- The actual firing — the `AlertType.testAlert(...)` call — is scheduled after
  `TEST_ALERT_DELAY_MS` (5000 ms) via `Inevitable.task("chg9-delayed-test-alert", ...)`:
  - the values are captured at press time (final locals), so later edits on the screen do
    not change the already scheduled test;
  - `Inevitable` dedupes by task id: pressing *Test alert* again within the delay
    reschedules the same task (one test fires, with the latest values);
  - the schedule survives leaving the screen, switching apps and turning the screen off.
- A toast informs the user: *"Test alert will fire in 5 seconds - switch apps or turn the
  screen off to test"* (new string `test_alert_fires_in_5_seconds`).

## Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/eveningoutpost/dexdrip/EditAlertActivity.java` | Delayed firing via `Inevitable.task` + capture of form values + informational toast; constant `TEST_ALERT_DELAY_MS` |
| `app/src/main/res/values/strings.xml` | New string `test_alert_fires_in_5_seconds` |

All code insertion points are marked with a `CHG9` comment. The Wear module is untouched.

## Test plan

1. Edit an alert → *Test alert* → toast appears immediately; the alert fires ~5 seconds
   later.
2. Press *Test alert*, then turn the screen off → the CHG2 lock-screen snooze screen
   appears when the alert fires (with *Override silent mode* or *Wake Screen* enabled).
3. Press *Test alert*, then switch to another app → the CHG1 overlay appears when the
   alert fires (with the CHG1 preference and permission active, or xDrip itself in the
   foreground per CHG7/A5).
4. Press *Test alert* twice within 5 seconds → only one test alert fires.
5. Invalid threshold or equal start/end time → immediate error, nothing is scheduled.
6. Test alert remains snoozable (CHG7/A1) and shows "Alert type not found" on the snooze
   screen as expected.
