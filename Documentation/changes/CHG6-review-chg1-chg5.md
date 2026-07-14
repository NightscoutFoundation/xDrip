# CHG6 — Review of CHG1–CHG5 (functional changes and technical implementation)

| | |
|---|---|
| **Change ID** | CHG6 |
| **Date** | 2026-07-08 |
| **Status** | Fixes implemented (decisions 2026-07-08; BVD4 accepted without implementation) |
| **Module** | app (phone) |
| **Scope** | All changes of [CHG1](CHG1-snooze-over-other-apps.md), [CHG2](CHG2-fullscreen-snooze-on-lockscreen.md), [CHG3](CHG3-volume-key-snooze.md), [CHG4](CHG4-volume-key-double-press.md) (incl. Addendum A), [CHG5](CHG5-statusline-snoozed-alert.md) |

## Method

Re-read of the final state of every touched file (manifest, `SnoozeActivity`,
`SnoozeOverlayActivity`, `SnoozeLockScreenActivity`, `AlertPlayer`, `Home`, `Preferences`,
`StatusLine`, styles/prefs/strings XML), verification of each implementation against its
approved CHG specification, and scenario analysis of the interplay between the changes
(alert lifecycle, re-raises, external snooze routes, key event routing, task/keyguard
behaviour, threading).

## Verified as correct (no findings)

- Each CHG implements exactly its approved specification; all `fastDebug` builds pass.
- Manifest/task configuration of both new activities (own task, `singleInstance`,
  excluded from recents, not exported) gives the intended restore behaviour and dedupes
  double launches (full-screen intent + direct start).
- Key handling: repeat filtering, consume-decision taken *before* the snooze flips the
  alerting state, `is_snoozed` scoping, and never-consuming non-volume keys.
- The three notification PendingIntents (content / full-screen / delete) target different
  components or types and cannot collide.
- CHG5 segment: null-safe, correct lifecycle (only while snoozed), 5 s cache, automatic
  propagation to Home/widget/watch/BlueJay.
- Toast handling runs on the UI thread only (key events); no activity context leaks
  (application-context toasts); version guard for `setShowWhenLocked` (min API 24).
- Stale snooze-screen content self-heals on window focus change (`displayStatus()`).
- Wear module untouched throughout.

## Findings (BVD)

### BVD1 — Navigation drawer is usable on the lock-screen snooze screen *(medium, CHG2)*

The snooze layout contains the navigation drawer fragment (`activity_snooze.xml:109`) and
`ActivityWithMenu.onResume()` activates it. On `SnoozeLockScreenActivity` an edge swipe
therefore opens the full xDrip menu **without unlocking**. Tapping an item launches an
activity that lands behind the keyguard (so no data is exposed), but the menu itself is
visible, and the resulting "nothing happens" is confusing. The same drawer on the CHG1
overlay is not a security issue (device unlocked) but navigating from it strands its
background task.

**Functional description (management):** During an alarm on a locked phone, anyone holding
the device can swipe open xDrip's application menu without unlocking it. No medical data
becomes visible — opening a menu item still requires unlocking — but the menu itself is
exposed, and the apparent dead-end behaviour looks broken. The fix disables the menu on
the locked alarm screen, so only the alarm controls the user explicitly chose to allow
there (snooze, disable, remote snooze) remain available.

**Proposal:** lock the drawer closed
(`DrawerLayout.setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED)` after `super.onResume()`,
try/catch) on `SnoozeLockScreenActivity`, and — for consistency, decision for the user —
also on `SnoozeOverlayActivity`.

**Files changed by the fix:**

- `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` — new shared helper `lockNavigationDrawer()`
- `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeOverlayActivity.java` — new `onResume()` override locking the drawer
- `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeLockScreenActivity.java` — new `onResume()` override locking the drawer

### BVD2 — Overlay / lock-screen snooze screen lingers when the alert ends another way *(medium, CHG1+CHG2)*

`AlertPlayer.Snooze(...)` is also reached from the notification snooze button/swipe
(`SnoozeOnNotificationDismissService`), watches (Pebble, MiBand, Amazfit, BlueJay, Wear),
remote/GCM, Tasker and broadcasts; `stopAlert(..., ClearData=true, ...)` is called by
`Notifications` when the alert ends on its own (e.g. glucose recovered). None of these
close an open CHG1 overlay or CHG2 lock-screen snooze screen: it keeps floating over the
previous app or lock screen with stale content until dismissed manually — contrary to the
CHG1/CHG2 goal of automatic restore.

**Functional description (management):** When an alarm is silenced through any other
channel — the notification, a smartwatch, a caregiver via remote snooze, an automation —
or ends by itself because the glucose value recovered, the pop-up alarm screen currently
stays on top of whatever the user was doing (or on the lock screen) with outdated
information, until it is closed by hand. This breaks the core promise of CHG1/CHG2 that
the user is returned to their previous context automatically, and it shows stale alarm
information. The fix closes the pop-up automatically the moment the alarm is snoozed or
ends via any channel.

**Proposal:** keep a static registry (weak references) of live overlay-mode instances
(`openHomeAfterSnooze() == false`), registered in `onCreate`/`onDestroy`. A static
`SnoozeActivity.closeOverlayVariants()` — called from the core
`AlertPlayer.Snooze(Context, int, boolean)` and from `stopAlert(...)` when
`ClearData == true` — finishes them on the UI thread. Double-finish from the in-screen
flows is harmless; `startAlert()`'s stop-then-raise sequence briefly closes and reopens
the screen (acceptable: fresh state for the new alert).

**Files changed by the fix:**

- `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` — weak-reference registry (registration in `onCreate()`, cleanup in new `onDestroy()`) and static `alertEnded()`
- `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java` — `alertEnded()` hooks in the core `Snooze(Context, int, boolean)` and in `stopAlert(...)` when `ClearData == true`

### BVD3 — Double-press state/toast not reset on external snooze or alert end *(low, CHG4)*

When the alert is snoozed/ended via another route, a pending first press and its
confirmation toast survive; the toast can float ~2 s over the restored app or lock screen.
Functionally guarded by the 1.5 s window, but unclean.

**Functional description (management):** After a first volume-button press the user sees a
short hint ("press the same button again to snooze"). If the alarm ends through another
channel within those seconds, the hint can linger briefly on top of the restored app or
lock screen. Purely cosmetic and momentary; the fix removes the hint the moment the alarm
ends.

**Proposal:** static `SnoozeActivity.resetVolumeKeyConfirmation()` (clear pending key,
cancel toast on the UI thread), invoked from the same hooks as BVD2 — one combined hook
resolves both findings.

**Files changed by the fix:**

- `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` — new `resetVolumeKeyConfirmation()`, invoked from `alertEnded()`
- `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java` — shared with BVD2: the `alertEnded()` hooks cover this finding as well

### BVD4 — Volume-key snooze inconsistent on other xDrip screens *(low, CHG3/4, optional)*

Volume-key snooze/blocking works on Home and the three snooze screens. If an alarm fires
while the user is in another xDrip screen (settings, history, tables …) and the CHG1
overlay is unavailable (feature off or permission missing), the volume keys neither snooze
nor get blocked there. Not a regression (before CHG3 only Home worked), but inconsistent.

**Functional description (management):** Snoozing with the volume buttons works on the
main screen and on the alarm pop-ups, but not while the user happens to be in another
xDrip screen (settings, history) at the moment the alarm sounds and no pop-up appears
there. The same button press then silently does nothing, which users may read as "the
feature is unreliable". The optional fix makes the volume-button behaviour identical on
every xDrip screen.

**Proposal (optional):** move the `onKeyDown`/`onKeyUp` handling to the shared base class
`ActivityWithMenu` (Home extends it; the Home/SnoozeActivity overrides collapse into the
base implementation, the snooze-screen close/refresh behaviour stays in `SnoozeActivity`).
Alternative: explicitly accept and document the current scope.

**Files changed by the fix:** none — accepted without implementation (user decision
2026-07-08); documented as a known limitation in
`Documentation/changes/CHG3-volume-key-snooze.md`.

### BVD5 — Screen may stay dark on a re-alert of the existing lock-screen instance *(low/medium, CHG2)*

The wake-up nudge (`JoH.fullWakeLock` + `FLAG_TURN_SCREEN_ON`) only runs in `onCreate()`.
With `singleInstance`, a re-alert while the instance is already front — but with the
screen turned off via the power button — arrives as `onNewIntent()` without `onCreate()`;
on some devices an already-added window does not re-trigger `TURN_SCREEN_ON`, so the
screen may stay dark for the re-alert.

**Functional description (management):** On a locked phone the alarm screen turns the
display on. If the user presses the power button instead of snoozing, the alarm re-sounds
a few minutes later — but on some devices the display then stays dark. The audible alarm
still plays; the visual alarm — a key goal of CHG2 — can be missing precisely at the
repeat alarm, the moment the user has already ignored one alarm. For a medical alert this
lowers the chance the alarm is noticed. The fix guarantees the display also wakes for
every repeated alarm.

**Proposal:** extract the wake-up logic into a `wakeUpScreen()` helper and call it from
both `onCreate()` and a new `onNewIntent()` override.

**Files changed by the fix:**

- `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeLockScreenActivity.java` — wake-up logic extracted to `wakeUpScreen()`, called from `onCreate()` and the new `onNewIntent()` override

### BVD6 — CHG1 document not annotated for the CHG2 behaviour *(low, documentation)*

The CHG1 document still states the overlay is not shown when "the screen is off or the
device is locked (the existing notification / full-screen-intent path covers that
situation)". Since CHG2 the dedicated lock-screen snooze screen covers this case.

**Functional description (management):** The change register still describes the pre-CHG2
behaviour for locked phones in the CHG1 document. There is no user impact, but consulting
the register (maintenance, audit, handover) can lead to wrong conclusions about how the
app behaves today. The fix is a one-line cross-reference.

**Proposal:** annotate that bullet with a reference to CHG2, in the same style as the
existing CHG3/CHG4 supersession notes.

**Files changed by the fix:**

- `Documentation/changes/CHG1-snooze-over-other-apps.md` — CHG2 cross-reference added to the "not shown when locked" bullet

## Decisions (2026-07-08)

1. BVD1: fix approved for **both** the lock-screen variant and the overlay.
2. BVD2 + BVD3: fix approved.
3. BVD4: **accepted — documented, not implemented** (known-limitation note added to the
   CHG3 document).
4. BVD5: fix approved.
5. BVD6: fix approved.

## Fix summary

| BVD | Severity | Outcome | Files changed |
|---|---|---|---|
| BVD1 | medium | Fixed | `SnoozeActivity.java` (shared `lockNavigationDrawer()`), `SnoozeOverlayActivity.java`, `SnoozeLockScreenActivity.java` (both lock the drawer in `onResume()`) |
| BVD2 | medium | Fixed | `SnoozeActivity.java` (weak-reference registry + `alertEnded()`), `AlertPlayer.java` (hooks in `Snooze(...)` core and `stopAlert(...)` when `ClearData`); *refined per [CHG13-final-review.md](CHG13-final-review.md) ER3 (2026-07-13): hook moved after `snooze()` + execution-time guard so an alert hand-over no longer closes the new pop-up* |
| BVD3 | low | Fixed | `SnoozeActivity.java` (`resetVolumeKeyConfirmation()`, invoked by the BVD2 hook) |
| BVD4 | low | Accepted, not implemented | Known-limitation note in `CHG3-volume-key-snooze.md` |
| BVD5 | low/medium | Fixed | `SnoozeLockScreenActivity.java` (`wakeUpScreen()` from both `onCreate()` and new `onNewIntent()`) |
| BVD6 | low | Fixed | `CHG1-snooze-over-other-apps.md` (CHG2 cross-reference annotation) |

All fix insertion points are marked with a `CHG6 BVDn` comment.

## Fix test plan

1. BVD1: on the CHG2 lock screen and the CHG1 overlay, an edge swipe (and the action-bar
   icon) no longer opens the navigation drawer; on the normal snooze screen and Home the
   drawer still works.
2. BVD2: with the overlay or lock-screen snooze screen open, snooze via the notification
   button, a watch, remote snooze or Tasker → the screen closes itself; previous app c.q.
   lock screen restored. Let glucose recover so the alert clears → same result.
3. BVD3: press a volume key once (hint toast appears), then snooze via the notification →
   the hint disappears immediately with the closing screen.
4. BVD5: alert on locked phone → screen on; press the power button (screen off) without
   snoozing; wait for the re-alert → the screen turns on again showing the snooze screen.
5. Regression: snooze-button flows of CHG1/CHG2/CHG3/CHG4 unchanged (screens close and
   restore as before); alert re-raise while a snooze screen is open does not close it.
