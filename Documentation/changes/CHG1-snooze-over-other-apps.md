# CHG1 — Snooze screen over other apps ("Display over other apps")

| | |
|---|---|
| **Change ID** | CHG1 |
| **Date** | 2026-07-07 (updated 2026-07-08) |
| **Status** | Implemented |
| **Module** | app (phone) |

## Summary

xDrip now declares the Android system permission **`SYSTEM_ALERT_WINDOW`** ("Display over
other apps"). When the new setting *Snooze screen over other apps* is enabled and the user
has granted that permission, the snooze screen is shown **on top of the app the user is
currently using** whenever a glucose (BG) alert is raised. After pressing *Snooze*, the
snooze screen closes and the **previously used app is restored automatically**.

## Motivation

Previously, a BG alert raised while the user was actively using another app only produced a
(heads-up) notification. The user had to tap the notification, snooze inside xDrip and then
navigate back to the app they were using. This change makes acknowledging an alarm a
one-step action without losing the context of the foreground app.

## User-facing behaviour

1. **Settings → Alarms and Alerts → Glucose alerts settings → Snooze screen over other apps**
   (new switch, default **off**). *(Since CHG13/ER1, 2026-07-12: default **on**; a startup
   prompt in Home asks for the missing system permission.)*
2. When switching it on without the system permission, xDrip shows a toast and opens the
   system screen *Settings → Apps → Special app access → Display over other apps* so the
   user can grant the permission for xDrip. The permission can also be granted or revoked
   there manually at any time, because the permission is declared in the manifest.
3. When a BG alert fires while the feature is enabled, the permission is granted, the screen
   is on and the device is unlocked, the snooze screen pops up over the current foreground
   app (in addition to the regular notification, sound and vibration).
4. Pressing *Snooze* snoozes the alert and closes the screen; Android then automatically
   returns to the app that was in use before the alert. Pressing *Back* dismisses the screen
   without snoozing and also returns to the previous app.

The overlay is intentionally **not** shown when:

- the feature toggle is off or the permission is not granted (falls back to the previous
  notification-only behaviour),
- the screen is off or the device is locked (the existing notification / full-screen-intent
  path covers that situation; since [CHG2](CHG2-fullscreen-snooze-on-lockscreen.md) the
  dedicated lock-screen snooze screen handles this case — annotation added per CHG6/BVD6),
- a phone call is ongoing and *Don't alarm during phone calls* is enabled.

If the alert re-fires (it keeps re-alerting until snoozed), the overlay is raised again.

## Technical implementation

The "Display over other apps" grant is what exempts xDrip from Android 10+ background
activity launch restrictions, allowing the snooze activity to be started from the alert
(service) context while another app is in the foreground.

The overlay variant of the snooze screen runs in its **own task** (`taskAffinity=""`,
`launchMode="singleInstance"`, `excludeFromRecents="true"`). Finishing that task reveals the
task below it — the app the user was using — which implements the automatic restore without
tracking the previous app explicitly.

The overlay uses a **floating window** (`SnoozeOverlayTheme`, a dialog-style non-fullscreen
theme, sized to full screen in code because the snooze layout's `DrawerLayout` requires
exact measurement). Floating is essential for restoring the previous app *unchanged*: an
opaque fullscreen activity would (1) stop the activity behind it and (2) take over the
display orientation (typically rotating to portrait), so that e.g. a fullscreen landscape
video app would be resumed with a changed configuration and drop out of fullscreen
(observed with YouTube, fixed 2026-07-08). With the floating theme the app behind stays
visible (dimmed), keeps running, keeps control of the orientation and regains focus with
its state — including fullscreen video — intact.

### Files changed

| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | Added `SYSTEM_ALERT_WINDOW` permission; registered `.SnoozeOverlayActivity` in its own task, hidden from recents, with the floating `SnoozeOverlayTheme` |
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeOverlayActivity.java` | **New.** Subclass of `SnoozeActivity` with static guard/launch helper `launchIfEnabled()`; overrides `openHomeAfterSnooze()` to return `false` so the previous app is restored, keeps the floating theme via `applyThemeChoice()` and sizes its window explicitly |
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` | Snooze button handler now consults new overridable hook `openHomeAfterSnooze()`; holo theme selection moved into overridable hook `applyThemeChoice()` (unchanged behaviour for the regular snooze screen) |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java` | `VibrateNotifyMakeNoise()` calls `SnoozeOverlayActivity.launchIfEnabled(context)` after posting the alert notification |
| `app/src/main/res/values/styles.xml` | New floating dialog theme `SnoozeOverlayTheme` |
| `app/src/main/res/xml/pref_notifications.xml` | New `SwitchPreference` `snooze_over_other_apps` under *Glucose alerts settings* |
| `app/src/main/java/com/eveningoutpost/dexdrip/utils/Preferences.java` | On enabling the preference without the permission: toast + open `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` for the xDrip package |
| `app/src/main/res/values/strings.xml` | New strings `snooze_over_other_apps`, `snooze_over_other_apps_summary`, `please_allow_display_over_other_apps` |

All code insertion points are marked with a `CHG1` comment.

### Preference key

- `snooze_over_other_apps` (boolean, default `false`), constant
  `SnoozeOverlayActivity.PREF_SNOOZE_OVER_OTHER_APPS`.

## Scope and limitations

- Applies to **BG (glucose) alerts** raised through `AlertPlayer` (low/high alerts,
  including re-raises). Other notification types (calibration, missed readings, persistent
  high, falling/rising) keep their existing behaviour.
- Phone app only; the Wear module is unchanged.
- The permission is granted by the user via the system settings screen; xDrip re-checks
  `Settings.canDrawOverlays()` at every alert and degrades gracefully to the normal
  notification when it is missing or revoked.

## Test plan

1. Fresh install → switch *Snooze screen over other apps* on → system permission screen
   opens; grant "Display over other apps" for xDrip.
2. Open another app (e.g. a browser). Trigger a low/high alert → snooze screen appears on
   top of the browser; alarm sounds as before.
3. Press *Snooze* → alert snoozed, snooze screen closes, browser is in the foreground again.
3a. Play a YouTube video fullscreen in landscape, trigger an alert, press *Snooze* → the
   video is still fullscreen in landscape afterwards; the screen must not rotate to
   portrait while the snooze overlay is shown (regression test for the 2026-07-08 fix).
4. Trigger an alert, press *Back* instead of snooze → browser restored, notification still
   present, alarm re-raises later and the overlay reappears.
5. Revoke the permission in system settings, trigger an alert → no overlay, regular
   notification behaviour, no errors in the event log.
6. Repeat step 2 with the device locked → no overlay; unchanged lock-screen behaviour.
7. Enable *Don't alarm during phone calls*, trigger an alert during a call → no overlay.
8. With the toggle off (default) → behaviour identical to before this change.
