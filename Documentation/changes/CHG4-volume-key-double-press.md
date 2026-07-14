# CHG4 — Volume-key snooze requires a double press on the same button

| | |
|---|---|
| **Change ID** | CHG4 |
| **Date** | 2026-07-08 |
| **Status** | Implemented (plan approved 2026-07-08) |
| **Module** | app (phone) |
| **Related** | [CHG3](CHG3-volume-key-snooze.md) — volume-key snooze on all snooze screens |

> **Supersession note (CHG13/ER5, 2026-07-12):** everything in this document — including
> Addendum A's volume-key blocking — only applies while *Snooze screen over other apps*
> is enabled, per the hard dependency of
> [CHG11](CHG11-buttons-silence-alarms-dependencies.md); both options default to on since
> CHG13/ER1.

## Request

Extend *Buttons silence alarms*: when enabled, the user must press **the same volume
button twice within 1.5 seconds** to snooze the alarm. After the first press a toast
informs the user to press the same button again; the moment the user presses that button
a second time, the toast disappears (and the alert is snoozed).

## Analysis

Current state (after CHG3): a **single** volume-key press snoozes immediately, in the
shared handler `SnoozeActivity.volumeKeySnooze(int keyCode)`, called from `Home.onKeyDown()`
and `SnoozeActivity.onKeyDown()` (inherited by the CHG1 overlay and CHG2 lock-screen
variants). Findings for the double-press design:

1. **The existing 5-second rate limit is incompatible** with a 1.5 s double press: the
   first press consumes `quietratelimit("button-press", 5)`, so the second press would be
   swallowed. The rate limit's purpose (accidental-press protection) is taken over by the
   double-press confirmation itself, so it is removed.
2. **Dismissing the toast on the second press** requires holding a reference to the shown
   `Toast` and calling `cancel()`; `JoH.static_toast_*` does not expose it. The
   confirmation toast uses `LENGTH_SHORT` (≈2 s), which fits the 1.5 s window, so an
   ignored confirmation also fades away by itself almost immediately after the window
   expires.
3. **"The same button"** is matched on the exact key code; volume-up, volume-down and mute
   are distinct. A *different* volume key within the window does not snooze but re-arms the
   confirmation for that key (with a new toast).
4. **Key repeats:** holding a volume button generates auto-repeat key events that would
   arrive well within 1.5 s and would turn a single long press into an accidental snooze —
   precisely what the double press must prevent. Repeats are filtered out with
   `KeyEvent.getRepeatCount() == 0`; the handler therefore now receives the `KeyEvent`
   instead of just the key code.
5. **Scope refinement:** the handler only reacts while the alert is actually alerting:
   `ActiveBgAlert.currentlyAlerting()` (active **and not snoozed**). The old code reacted
   on any active alert record, so a volume press while an alert was already silently
   snoozed would stealthily re-snooze/extend it; that hidden behaviour disappears.
6. **State** (pending key code, timestamp of the first press, toast reference) is static in
   `SnoozeActivity`, so the confirmation works identically on every screen that delegates
   key events (Home, overlay, lock screen, notification snooze screen). No timers are
   needed: expiry of the 1.5 s window is evaluated lazily at the next press.
7. **CHG3 behaviour is preserved** at the moment of the *second* press: the handler returns
   true, the CHG1/CHG2 variants close themselves (previous app c.q. lock screen restored),
   the plain snooze screen refreshes its status. Volume keys are still never consumed, so
   both presses also change the volume as usual. *(The last point is superseded by
   Addendum A: while the alarm is alerting and the setting is enabled, the volume keys are
   consumed and no longer change the volume.)*

## Implementation

- **`SnoozeActivity.java`** — reworked `volumeKeySnooze(KeyEvent event)`:
  - constant `VOLUME_KEY_CONFIRM_WINDOW_MS = 1500` (no new preference);
  - first press (or different key / expired window): store pending key + timestamp, show
    the confirmation toast;
  - second press on the same key within 1.5 s: cancel the confirmation toast immediately,
    snooze with the default time (`AlertPlayer.Snooze(ctx, -1)`), show the existing
    "Snoozing..." toast, write the event log entry, return true;
  - guard: preference enabled, `ActiveBgAlert.currentlyAlerting()`, `getRepeatCount() == 0`.
- **`Home.java`** — delegate call passes the `KeyEvent` (one line).
- **`strings.xml`** — new string `press_same_volume_button_again`
  ("Press the same volume button again to snooze the alert").

### Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` | Double-press state machine + confirmation-toast management in `volumeKeySnooze(KeyEvent)` |
| `app/src/main/java/com/eveningoutpost/dexdrip/Home.java` | Delegate call passes the `KeyEvent` |
| `app/src/main/res/values/strings.xml` | New confirmation-toast string `press_same_volume_button_again` |

All code insertion points are marked with a `CHG4` comment. The Wear module is untouched.

## Decisions (2026-07-08)

1. Plan approved.
2. A held (long-pressed) volume button does **not** count as a double press: **yes**.
3. While an alert is already (silently) snoozed, volume keys do nothing anymore — the old
   hidden re-snooze/extend behaviour is removed: **yes**.

---

## Addendum A (2026-07-08) — volume keys must not change the volume during an alarm

**Status: Implemented** (plan approved 2026-07-08)

### Request

While a glucose alarm is alerting, pressing the volume buttons must have no effect on the
phone's sound volume; the buttons then serve exclusively as the CHG4 double-press snooze.

### Analysis

1. Android adjusts the volume when volume-key events are **not consumed** by the foreground
   activity: unconsumed VOLUME_UP/DOWN/MUTE events fall through to the framework, which
   forwards them to the audio service. Returning `true` from `onKeyDown`/`onKeyUp` consumes
   the event and prevents any volume change (for every audio stream). No `onKeyUp` or
   `dispatchKeyEvent` overrides exist anywhere in the app yet, so this can be added cleanly.
2. Both the DOWN and the UP event must be consumed, as well as the auto-repeat DOWN events
   of a held button (each repeat would otherwise adjust one step).
3. Key events only reach xDrip while one of its screens has key focus: `Home`, the CHG1
   overlay, the CHG2 lock-screen snooze screen or the notification snooze screen. Thanks to
   CHG1/CHG2 that covers the alarm scenarios when those features are enabled. **Limitation:**
   if the alarm fires while another app is in the foreground and the CHG1 overlay is not
   available (feature off or permission missing), the volume keys are delivered to that app
   and cannot (and should not) be intercepted system-wide. Volume changes made via the
   on-screen volume slider or quick settings are not blocked either.
4. Blocking applies exactly while the CHG4 snooze mechanism is armed:
   `buttons_silence_alert` enabled **and** `ActiveBgAlert.currentlyAlerting()`. With the
   setting off, or once the alert is snoozed, the volume keys behave completely normally
   again.
5. Ordering subtlety: the second (snoozing) press flips `currentlyAlerting()` to false
   while it is being handled, so the consume decision is taken **before** the snooze
   handler runs — the snoozing press itself is also consumed. Its later UP event is no
   longer consumed, which is harmless: a lone UP event never adjusts the volume.
6. `AlertPlayer`'s volume management (set volume for the alert, revert afterwards) is
   unaffected; it even becomes more predictable because the alert stream can no longer be
   bumped by keys mid-alert.

### Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` | New static helper `volumeKeyConsumed(keyCode)`; `onKeyDown()` computes the consume decision before running the snooze handler and returns `true` when consumed; new `onKeyUp()` override consuming the same keys (inherited by the CHG1/CHG2 variants) |
| `app/src/main/java/com/eveningoutpost/dexdrip/Home.java` | Same consume behaviour in `onKeyDown()`; new `onKeyUp()` override |

All addendum insertion points are marked with a `CHG4 addendum A` comment. No manifest,
preference or string changes.

### Decisions (2026-07-08)

1. Addendum plan approved.
2. Blocking only applies while *Buttons silence alarms* is enabled: **yes** — with the
   setting off everything behaves as before.

### Addendum test plan

1. Alarm alerting, setting on, on each xDrip screen (Home / overlay / lock screen /
   notification snooze screen): a volume press shows the confirmation toast and the volume
   level does not change (no system volume UI appears).
2. Double press → snooze; the volume has not changed at any point.
3. Press and hold a volume button during the alarm → no volume ramp, no snooze.
4. Setting off → volume keys adjust the volume normally during an alarm.
5. No active alarm, or alert already snoozed → volume keys behave normally on all screens.
6. Alarm while another app is in the foreground and the CHG1 overlay is unavailable →
   volume keys work for that app (documented limitation).

---

## Test plan

1. Alert ringing, setting on: one press → confirmation toast, no snooze; nothing further
   happens after the 1.5 s window expires. *(The volume does not change — see Addendum A.)*
2. Two presses on the same key within 1.5 s → snooze on the second press; the confirmation
   toast disappears at that moment; "Snoozing..." toast shows; CHG1 overlay / CHG2
   lock-screen close themselves, Home and the plain snooze screen stay open.
3. Volume-down followed by volume-up within 1.5 s → no snooze; confirmation re-armed for
   volume-up (new toast); another volume-up within 1.5 s → snooze.
4. Press and hold a volume button → volume ramps, one confirmation toast, **no** snooze.
5. Alert silently snoozed → volume keys only change the volume (no toast, no re-snooze).
6. Setting off, or no active alert → volume keys behave completely normally.
7. Behaviour identical on all four screens: Home, CHG1 overlay, CHG2 lock screen,
   notification snooze screen.
8. Second press after more than 1.5 s → treated as a new first press (toast again).
