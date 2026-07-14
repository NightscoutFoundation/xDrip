# CHG12 — Identical snooze text on all surfaces + immediate notification refresh

| | |
|---|---|
| **Change ID** | CHG12 |
| **Date** | 2026-07-10 |
| **Status** | Implemented (requested with immediate implementation, 2026-07-10) |
| **Module** | app (phone) |
| **Related** | [CHG5](CHG5-statusline-snoozed-alert.md) (text superseded), [CHG8](CHG8-notification-snooze-line.md) (extra refresh trigger) |

## Reported issues

1. During a snoozed glucose alert the xDrip home screen showed *"Alert high snoozed until
   23:30"* (CHG5, generic high/low) while the ongoing notification showed *"Alert h2
   snoozed until 23:30"* (CHG8, the alert's name). The text must be identical everywhere
   and show the **alert name**; the home-screen text is adjusted to match.
2. The snooze line in the ongoing notification appeared only a few seconds after snoozing.
   It must appear (almost) immediately.

## Implementation

1. **Identical text (alert name everywhere).** The CHG5 Extra-Status-Line segment in
   `StatusLine.extraStatusLineReal()` now formats through the **same string resource** as
   the notification line (`notification_alert_snoozed_until`, "Alert %1$s snoozed until
   %2$s"), with the same `HH:mm` formatting — the two surfaces can no longer diverge. The
   settings subtitle `summary_inform_on_snoozed_alert` was updated accordingly
   ("Like 'Alert [name] snoozed until HH:mm'"). Note: the CHG5 lifecycle rules are
   unchanged (the CHG8 threshold suppression remains notification-only, as recorded in
   CHG8's resolved open points).
2. **Immediate refresh.** The core snooze funnel `AlertPlayer.Snooze(Context, int,
   boolean)` — reached by every snooze channel (snooze button, volume keys, notification
   button/swipe, watches, remote, Tasker, broadcast) — and `PreSnooze()` now call
   `Notifications.staticUpdateNotification()` directly after snoozing. Previously the
   rebuild waited for the next service trigger (readings cycle or an indirect path),
   which caused the observed delay. The remaining latency is only the service start plus
   rebuilding the notification graph (typically well under a second up to ~1 s).

## Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/StatusLine.java` | CHG5 segment formats via `notification_alert_snoozed_until` (alert name) |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java` | `staticUpdateNotification()` after `snooze()` in the `Snooze(...)` core and in `PreSnooze()` |
| `app/src/main/res/values/strings.xml` | `summary_inform_on_snoozed_alert` updated to "[name]" |

All code insertion points are marked with a `CHG12` comment. The Wear module is untouched.

## Test plan

1. Snooze a high alert named e.g. "h2" → home screen (Extra Status Line), widget, watch
   and the ongoing notification all show exactly *"Alert h2 snoozed until HH:mm"*.
2. Snooze via each channel (snooze button, volume keys, notification, watch, remote) →
   the notification snooze line is visible within about a second.
3. `PreSnooze` from the alert edit screen → line appears promptly as well.
4. Settings: the *Inform on snoozed alert* subtitle reads "Like 'Alert [name] snoozed
   until HH:mm'".
5. Regression: suppression rules unchanged (CHG8 threshold/vehicle/stale rules on the
   notification; CHG5 segment shows while snoozed).
