# CHG3 — Volume-key snooze ("Buttons silence alarms") broken on the new snooze screens

| | |
|---|---|
| **Change ID** | CHG3 |
| **Date** | 2026-07-08 |
| **Status** | Implemented (plan approved 2026-07-08) |
| **Module** | app (phone) |
| **Related** | [CHG1](CHG1-snooze-over-other-apps.md), [CHG2](CHG2-fullscreen-snooze-on-lockscreen.md) |

## Finding (as reported)

The setting *Buttons silence alarms* (`buttons_silence_alert`) should let the user snooze a
glucose alarm with a volume key. This no longer appears to work.

## Analysis

- The feature is implemented in exactly **one** place: `Home.onKeyDown()`
  (`Home.java:3643-3667`). On VOLUME_DOWN/UP/MUTE it checks a 5-second rate limit, the
  `buttons_silence_alert` preference and an active BG alert, then calls
  `AlertPlayer.Snooze(ctx, -1)` with a toast, and always passes the key on (the volume
  still changes). No other component listens to volume keys or volume changes; the other
  `OpportunisticSnooze()` callers are watch buttons, Tasker, and broadcasts.

- Key events are delivered to the **foreground activity**. Before CHG1/CHG2, whenever a BG
  alarm demanded attention on the phone, the activity brought to the front was `Home` (via
  the notification's full-screen intent) — so volume-key snooze worked during alarms.

- **Since CHG1/CHG2 the foreground activity during an alarm is one of the new snooze
  screens instead:**
  - CHG1: `SnoozeOverlayActivity` floats above the app in use and has key focus;
  - CHG2: the full-screen intent now targets `SnoozeLockScreenActivity` (no longer `Home`),
    which is also started directly when the device is off/locked.

  Neither of them — nor their base class `SnoozeActivity` — overrides `onKeyDown()`, so
  volume presses during an alarm now only change the volume. **This is a regression
  introduced by CHG1/CHG2.** Volume-key snooze still works only when `Home` itself happens
  to be open.

- Pre-existing gap with the same root cause: opening the snooze screen by tapping the alert
  notification (plain `SnoozeActivity`) never supported volume-key snooze either.

## Implementation

1. **Shared handler.** The volume-key logic moved from `Home.onKeyDown()` into a static
   helper `SnoozeActivity.volumeKeySnooze(int keyCode)` with behaviour identical to before
   (same rate-limit key, preference check, active-alert check, default snooze time, toast
   and event log; the key event is never consumed, so the volume still changes).
   It returns whether a snooze actually happened. *(Later superseded by CHG4 and its
   Addendum A: double-press confirmation, and the volume keys are consumed — no volume
   change — while an alarm is alerting with the setting enabled.)* *(Since
   [CHG11](CHG11-buttons-silence-alarms-dependencies.md), annotated per CHG13/ER5:
   volume-key snooze is only active while 'Snooze screen over other apps' is also
   enabled; both options default to on since CHG13/ER1.)*

2. **`SnoozeActivity.onKeyDown()` override** calling that helper — inherited by the CHG1
   overlay and the CHG2 lock-screen variants, and also fixing the notification-tap snooze
   screen. After a successful volume-key snooze:
   - overlay / lock-screen variant (`openHomeAfterSnooze() == false`): the screen closes
     itself, restoring the previous app c.q. the lock screen — consistent with the CHG1/CHG2
     behaviour after pressing the *Snooze* button;
   - plain snooze screen: stays open (like `Home`) and refreshes its status text.

3. **`Home.onKeyDown()`** delegates to the same helper (identical behaviour, no
   duplication).

### Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` | New static helper `volumeKeySnooze()` + `onKeyDown()` override (inherited by both CHG1/CHG2 variants) |
| `app/src/main/java/com/eveningoutpost/dexdrip/Home.java` | `onKeyDown()` delegates to the shared helper |

All code insertion points are marked with a `CHG3` comment. No manifest, preference or
string changes; the Wear module is untouched.

## Decisions (2026-07-08)

1. Plan approved.
2. Overlay / lock-screen snooze screen closes automatically after a volume-key snooze:
   **yes** — kept consistent with the Snooze button behaviour of CHG1/CHG2.

## Known limitation (accepted per CHG6/BVD4, 2026-07-08)

Volume-key snooze (and the CHG4 Addendum A volume blocking) works while `Home` or one of
the snooze screens has key focus. If an alarm fires while another xDrip screen (settings,
history, tables …) is open and no CHG1 overlay appears on top of it, the volume keys
behave normally there and do not snooze. Reviewed as [CHG6](CHG6-review-chg1-chg5.md)
finding BVD4 and explicitly accepted: **documented, not implemented**.

## Test plan

1. *Buttons silence alarms* on; alert while using another app (CHG1 overlay in front) →
   volume key snoozes (toast + event log), overlay closes, previous app restored.
2. Alert with the screen off / device locked (CHG2 screen in front) → volume key snoozes,
   screen closes, lock screen returns; device never unlocked.
3. Alert with `Home` open → unchanged behaviour (snooze + toast, Home stays open).
4. Snooze screen opened by tapping the alert notification → volume key snoozes, status
   text updates, screen stays open.
5. Setting off → volume keys only change the volume on all of the above screens.
6. No active alert → volume keys only change the volume, no toast.
7. Volume still audibly changes when a volume-key snooze fires (event not consumed).
   *(Superseded by CHG4 Addendum A: with Buttons silence alarms enabled the volume keys no
   longer change the volume while an alarm is alerting.)*
