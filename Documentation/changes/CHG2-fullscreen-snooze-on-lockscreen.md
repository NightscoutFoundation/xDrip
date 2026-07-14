# CHG2 — Full-screen snooze screen when the device is off/locked

| | |
|---|---|
| **Change ID** | CHG2 |
| **Date** | 2026-07-08 |
| **Status** | Implemented (plan approved 2026-07-08) |
| **Module** | app (phone) |
| **Related** | [CHG1](CHG1-snooze-over-other-apps.md) — snooze screen over other apps |

## Finding (as reported)

When an alert fires and *override_silent_mode* (per-alert setting) or *Wake Screen*
(`wake_phone_during_alerts`) is enabled, the snooze screen should appear as a full-screen
activity. This does not work. Desired behaviour, in combination with "Display over other
apps" (CHG1): a glucose alarm turns the phone on and shows the snooze screen **on top of
the lock screen**; CHG1 already covers the screen-on/unlocked case.

## Analysis

All findings are in `AlertPlayer.VibrateNotifyMakeNoise()` unless stated otherwise.

- **F1 — The full-screen intent opens `Home`, not the snooze screen**
  (`AlertPlayer.java:577`: `setFullScreenIntent(... new Intent(context, Home.class) ...)`).
  When it fires, the user gets the main graph screen, not the snooze screen.

- **F2 — The launched activity stays behind the lock screen.** The FSI intent carries no
  `Home.HOME_FULL_WAKEUP` extra, and only that extra path applies Home's
  `FLAG_SHOW_WHEN_LOCKED | FLAG_TURN_SCREEN_ON` flags (`Home.java:984-997, 1099-1103`).
  `SnoozeActivity` sets no lock-screen flags either. Net effect: the screen lights up, the
  user sees only the lock screen, and after manually unlocking lands in `Home` — which is
  exactly the reported "full-screen snooze does not work".

- **F3 — The FSI is gated too narrowly.** It is only set when the alert has
  `override_silent_mode` **and** the alert profile is not *vibrate only* / *silent* (the FSI
  code sits inside the audible-profile block, `AlertPlayer.java:555, 574-578`). The *Wake
  Screen* setting plays no role in the full-screen decision at all.

- **F4 — *Wake Screen* only sets a MediaPlayer wake-mode** (`AlertPlayer.java:349-351`), so
  it can only wake the screen while an alert sound actually plays. It does nothing when the
  profile is vibrate-only/silent, when the phone is in silent/vibrate mode without
  override-silent (`playFile` is then not called, lines 580-583), or during the initial
  minutes of the delayed-ascending profile (`volumeFrac == 0` → `playFile` returns early,
  lines 333-336). Even when it works it merely lights up the lock screen.

- **F5 — CHG1 deliberately excludes this scenario.** `SnoozeOverlayActivity.launchIfEnabled()`
  returns when the screen is off or the keyguard is locked (per CHG1 spec). So today no code
  path shows a snooze screen when the device is locked.

- **Platform note.** Android launches a full-screen intent only when the screen is off or
  the keyguard is showing; when the device is in use it shows a heads-up notification
  instead. CHG1 (screen on + unlocked) and CHG2 (screen off / locked) therefore complement
  each other and together cover the whole matrix.

## Implementation

1. **New `SnoozeLockScreenActivity extends SnoozeActivity`** (mirrors the CHG1 subclass
   approach; reuses the CHG1 hook `openHomeAfterSnooze()`):
   - Manifest: own task (`taskAffinity=""`, `launchMode="singleInstance"`),
     `excludeFromRecents="true"`, `exported="false"`, normal full-screen `AppTheme`.
   - Window flags `FLAG_SHOW_WHEN_LOCKED | FLAG_TURN_SCREEN_ON | FLAG_KEEP_SCREEN_ON |
     FLAG_ALLOW_LOCK_WHILE_SCREEN_ON` (plus `setShowWhenLocked/setTurnScreenOn` on API
     27+), and a short self-releasing full wake lock (`JoH.fullWakeLock`) for devices in
     deep sleep — same pattern as `Reminders.wakeUpScreen()` (`Reminders.java:1402-1425`).
     **Not** `FLAG_DISMISS_KEYGUARD`: the device stays locked; the snooze screen is only
     displayed above the keyguard.
   - `openHomeAfterSnooze()` → `false`: after snoozing the screen closes and the lock screen
     is simply revealed again (symmetric with CHG1's restore behaviour).
   - The full snooze screen, **including** the disable/enable-alert buttons and the
     remote-snooze button, is available on the lock screen (explicit user decision
     2026-07-08; the earlier proposal to hide those buttons while locked was rejected).

2. **`AlertPlayer.VibrateNotifyMakeNoise()`**:
   - New trigger: `fullScreenWanted = alert.override_silent_mode ||
     Pref("wake_phone_during_alerts")`.
   - Move the FSI out of the audible-profile block; when `fullScreenWanted`, set
     `CATEGORY_ALARM` + full-screen intent → **`SnoozeLockScreenActivity`** (was `Home`).
     This fixes F1/F2/F3 and works without any special permission, for all users.
   - After posting the notification: if the screen is off or the keyguard is locked and
     `fullScreenWanted` and "Display over other apps" is granted, also **start
     `SnoozeLockScreenActivity` directly** (deterministic; the SYSTEM_ALERT_WINDOW grant
     provides the background-start exemption; `singleInstance` dedupes if the FSI fires
     too). Otherwise the existing CHG1 overlay path runs, unchanged, for the
     screen-on/unlocked case.
   - The existing MediaPlayer wake-mode (F4) stays as a harmless backup.

### Files changed

| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | Registered `.SnoozeLockScreenActivity` (own task, hidden from recents, default full-screen theme) |
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeLockScreenActivity.java` | **New.** Lock-screen capable snooze screen with static helpers `fullScreenWanted()`, `screenOffOrLocked()` and `launchIfWanted()`; `openHomeAfterSnooze()` returns `false` |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java` | Full-screen intent retargeted to `SnoozeLockScreenActivity`, trigger broadened to `override_silent_mode \|\| Wake Screen` and moved out of the audible-profile block; after posting the notification: direct launch when screen off/locked, otherwise the CHG1 overlay path |

All code insertion points are marked with a `CHG2` comment.

No new preferences or strings: the triggers are the existing *Override silent mode*
(per alert) and *Wake Screen* settings.

### Intentional behaviour changes

- Override-silent alerts with the device locked now open the snooze screen above the lock
  screen instead of (after unlocking) the Home graph.
- *Wake Screen* now also produces the full-screen snooze screen (previously it only lit up
  the lock screen, and only while sound played).
- Full-screen snooze also fires for vibrate-only/silent alert profiles (visual alarm).

## Decisions (2026-07-08)

1. Plan approved.
2. Disable/enable-alert and remote-snooze buttons **remain visible** on the lock screen
   (proposal to hide them was rejected).
3. Full-screen snooze also fires for vibrate-only/silent profiles: **yes**.

## Test plan

1. Override-silent low/high alert, screen off, device locked → screen turns on, snooze
   screen appears above the lock screen, alarm audible; *Snooze* → alert snoozed, lock
   screen returns, device never unlocked.
2. Same with *Wake Screen* on and override-silent off (loud phone) → same result.
3. Same with alert profile *vibrate only* → snooze screen appears (no sound).
4. While locked: the disable/enable-alert buttons (and, when applicable, the remote-snooze
   button) are shown and usable, exactly as on the normal snooze screen.
5. Screen on, device in use → CHG1 overlay behaviour unchanged (regression).
6. "Display over other apps" revoked → full-screen intent path still shows the snooze
   screen above the lock screen when the alarm fires with the screen off.
7. Back button on the lock-screen snooze screen → lock screen returns, alarm keeps
   re-raising later.
8. Both toggles off / alert without override-silent → behaviour identical to before CHG2.
