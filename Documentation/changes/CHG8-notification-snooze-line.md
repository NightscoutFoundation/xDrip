# CHG8 — Snooze line in the ongoing BG notification

| | |
|---|---|
| **Change ID** | CHG8 |
| **Date** | 2026-07-10 |
| **Status** | Implemented (approved 2026-07-10) |
| **Module** | app (phone) |
| **Source** | Comparison review of [PR #4510](https://github.com/NightscoutFoundation/xDrip/pull/4510) against CHG1–CHG6 (2026-07-08); item A4 of that report. Items A1/A2/A3/A5 are covered by [CHG7](CHG7-pr4510-adoptions.md). |
| **Related** | [CHG5](CHG5-statusline-snoozed-alert.md) — snoozed-alert segment in the Extra Status Line |

## Request / summary

When a glucose alert is snoozed, the **ongoing BG notification** (the persistent
notification with the glucose graph and delta) should show an extra line such as:

```
Delta: -0.6 mmol/l
Alert low snoozed until 14:35
```

This complements CHG5: the Extra Status Line reaches the user on the Home screen, widget
and watch, while the ongoing notification reaches the user **outside xDrip** (notification
shade, always visible), without requiring the Extra Status Line feature to be enabled.

## Design (per PR #4510, to be adapted to this tree)

- New helper `buildSnoozeLine()` in `Notifications.java`, appended to the delta line in
  all notification styles (standard content text, collapsed and expanded custom views,
  and the Android 16+ BigText/chip style).
- Shown while `ActiveBgAlert` exists and `is_snoozed`, with **smart suppression**:
  - hidden when the latest BG reading no longer violates the alert threshold (high alert:
    BG back below threshold; low alert: BG back above threshold **including the
    vehicle-mode offset** of 18 mg/dL when vehicle mode is active — mirroring
    `AlertType.get_highest_active_alert_helper()`);
  - when the latest reading is older than 15 minutes the threshold check is skipped and
    the line is always shown (never hide safety information based on stale data);
  - hidden when the `AlertType` cannot be found (orphaned record).
- Appears for every snooze route (buttons, volume keys, notification, watch, remote,
  `start_snoozed`, `PreSnooze`), and disappears when the snooze expires or the alert ends.
- **Prompt refresh:**
  - `SnoozeActivity.recheckAlerts()` switches from `Notifications.start()` (10-second rate
    limit; up to 5 minutes delay) to `Notifications.staticUpdateNotification()`;
  - `notificationSetter()` captures the snooze state before evaluating alerts and rebuilds
    the ongoing notification a second time when the state flipped during evaluation;
  - a `currentlySnoozed()` helper is added to `ActiveBgAlert`.
- New string `notification_alert_snoozed_until` ("Alert %1$s snoozed until %2$s").

## Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/Notifications.java` | `buildSnoozeLine()` + span-preserving `withSnoozeLine()` applied to all styles (content text, collapsed/expanded custom views, chip/BigText) + snooze-state change detection in `notificationSetter()` |
| `app/src/main/java/com/eveningoutpost/dexdrip/models/ActiveBgAlert.java` | `currentlySnoozed()` helper |
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` | `recheckAlerts()` uses `staticUpdateNotification()` |
| `app/src/main/res/values/strings.xml` | New string `notification_alert_snoozed_until` |

All code insertion points are marked with a `CHG8` comment. The Wear module is untouched.

## Open points from the proposal — all resolved at implementation (2026-07-10)

1. `Notifications.staticUpdateNotification()` exists in this tree and performs the exact
   same service run as `Notifications.start()`, only without the 10-second rate limit —
   the swap in `recheckAlerts()` therefore loses no behaviour.
2. CHG5's Extra Status Line segment keeps its own (simpler) always-while-snoozed rule as
   explicitly specified there; adopting this smart suppression on that surface too can be
   raised as a future change if desired.
3. Improvement over the PR: the snooze line is appended with `TextUtils.concat`, so the
   styling (spans) of the delta text is preserved instead of flattened to plain text.
4. Addendum per [CHG12](CHG12-consistent-snooze-text-and-prompt-refresh.md) (2026-07-10):
   the core `AlertPlayer.Snooze(...)` and `PreSnooze()` now trigger
   `staticUpdateNotification()` directly, so the line appears immediately after snoozing
   through any channel; the CHG5 status line uses the same string resource since CHG12.

## Test plan (after implementation)

1. Snooze a low/high alert (any route) → the ongoing notification shows
   "Alert &lt;name&gt; snoozed until HH:mm" within a few seconds, in collapsed, expanded and
   chip styles.
2. Snooze expires or the alert is stopped/removed → the line disappears.
3. High alert snoozed, BG drops below the threshold → line disappears while still snoozed.
4. Low alert snoozed with vehicle mode active → the threshold check includes the
   +18 mg/dL offset.
5. Latest reading older than 15 minutes → the line is always shown.
6. `start_snoozed` and `PreSnooze` alerts also show the line.
7. CHG5 status line and this notification line stay consistent with each other.
