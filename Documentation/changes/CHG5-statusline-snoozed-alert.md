# CHG5 — Extra status line segment: inform on snoozed alert

| | |
|---|---|
| **Change ID** | CHG5 |
| **Date** | 2026-07-08 |
| **Status** | Implemented (plan approved 2026-07-08) |
| **Module** | app (phone) |
| **Related** | [CHG2](CHG2-fullscreen-snooze-on-lockscreen.md), [CHG3](CHG3-volume-key-snooze.md), [CHG4](CHG4-volume-key-double-press.md) — snooze handling |

## Request

When there is an active glucose alert that is snoozed, xDrip should be able to inform the
user about it through an extra status line. Add the option **"Inform on snoozed alert"**
with subtitle **"Like 'Alert [high|low] snoozed until HH:mm'"** to the *Extra Status Line*
menu, plus the functionality that actually shows this information.
*(The request wrote "alaert"; assumed to be a typo for "alert".)*

> **Superseded by [CHG12](CHG12-consistent-snooze-text-and-prompt-refresh.md)
> (2026-07-10):** the segment now shows the **alert name** instead of high/low, using the
> same string resource as the ongoing-notification snooze line, so both surfaces are
> always identical.

## Analysis

- The *Extra Status Line* menu lives in `pref_advanced_settings.xml` (PreferenceScreen
  `xdrip_extra_status_line`, under *Less common settings*): a master switch
  `extra_status_line` plus one `CheckBoxPreference` per segment with key `status_line_*`
  and `android:dependency="extra_status_line"`. The new option follows that exact pattern
  with key **`status_line_snoozed_alert`**.
- The line itself is assembled in `StatusLine.extraStatusLineReal()`
  (`utilitymodels/StatusLine.java`): one `append(...)` block per enabled preference. The
  result is consumed by `Home`, the widget (`WidgetDisplayHelper`, when *Show on widget* is
  on), the Android Wear sync (`WatchUpdaterService`) and BlueJay — a new segment therefore
  automatically appears everywhere the extra status line is shown. Existing segment texts
  are hard-coded English (e.g. `"Carbs:"`, `"slope = "`); the new segment follows suit.
- Data source: `ActiveBgAlert.getOnly()` — the segment is shown only when an active alert
  record exists **and** `is_snoozed` is true. High/low comes from the linked alert type
  (`ActiveBgAlert.alertTypegetOnly().above`), the end time from `next_alert_at`, formatted
  `HH:mm` with the same `SimpleDateFormat` approach as the existing time segment. Output:
  `Alert high snoozed until 14:05` c.q. `Alert low snoozed until 09:30`.
- Behaviour over the alert lifecycle (all automatic, no extra plumbing):
  - alert alerting (not snoozed) → no segment;
  - alert snoozed (including alerts that start snoozed via *Start snoozed*) → segment
    visible;
  - alert re-fires after the snooze period (`is_snoozed` flips back) → segment disappears;
  - alert cleared or alerts *disabled* (those flows delete the `ActiveBgAlert` record) →
    no segment.
- Refresh: `StatusLine` caches for only 5 seconds; the consumers refresh on their own
  cadence (Home on chart/UI updates, widget on its update cycle, watch on sync). The
  segment therefore appears/disappears within seconds after a snooze, at the next redraw.
- Note: the snooze picker allows up to 12 hours, so `HH:mm` (without a date) is
  unambiguous in practice.

## Implementation

| File | Change |
|---|---|
| `app/src/main/res/xml/pref_advanced_settings.xml` | New `CheckBoxPreference` `status_line_snoozed_alert` (default off, dependency `extra_status_line`), placed after the *Time* option |
| `app/src/main/res/values/strings.xml` | New strings `inform_on_snoozed_alert` ("Inform on snoozed alert") and `summary_inform_on_snoozed_alert` ("Like 'Alert [high\|low] snoozed until HH:mm'") |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/StatusLine.java` | New segment in `extraStatusLineReal()`: when the preference is on and a snoozed active alert exists, append `Alert high/low snoozed until HH:mm` (placed just before the time segment) |

All code insertion points are marked with a `CHG5` comment. No manifest changes; the Wear
module is untouched (the watch receives the phone-built status line).

## Decisions (2026-07-08)

1. Plan approved.
2. Option title uses the corrected spelling "Inform on snoozed alert" (request wrote
   "alaert"): **yes**.

## Test plan

1. *Extra Status Line* on + *Inform on snoozed alert* on: snooze a high alert → status
   line on Home shows `Alert high snoozed until HH:mm` with the correct end time; same
   for a low alert (`Alert low ...`).
2. The segment combines correctly with other enabled segments (single line, space
   separated) and shows within seconds after snoozing (at the next redraw).
3. When the snooze period ends and the alert re-fires → the segment disappears.
4. Snooze from every entry point (snooze button, CHG1 overlay, CHG2 lock screen, CHG3/4
   volume keys, notification dismiss) → segment appears in all cases.
5. *Show on widget* on → the segment also appears on the widget; watch receives it via
   the synced status line.
6. Option off (or master switch off) → no segment, everything as before.
7. Disable alerts (instead of snoozing) → no segment (alert record is deleted).
