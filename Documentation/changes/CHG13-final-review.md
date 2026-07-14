# Final pre-release review — CHG1 through CHG12 (CHG13)

| | |
|---|---|
| **Change ID** | CHG13 (pre-release review) |
| **Date** | 2026-07-12 |
| **Status** | Findings reported — decision per finding by the user (ER1 implemented) |
| **Scope** | All changes CHG1–CHG12: 12 code files, preferences, strings, manifest |
| **Safety frame** | xDrip is used by people with type 1 diabetes. Guiding principle: **no change may cause a glucose alert to be missed, suppressed or unintentionally muted.** Every finding has been weighed against that risk. |

## Method and evidence

- Full re-read of the **final state** of every touched file (not the individual edits):
  `SnoozeActivity`, `SnoozeOverlayActivity`, `SnoozeLockScreenActivity`, `AlertPlayer`,
  `Notifications`, `NotificationChannels`, `ActiveBgAlert`, `EditAlertActivity`, `Home`,
  `Preferences`, `StatusLine`, manifest and resource XML.
- Verification against every CHG specification and the recorded decisions.
- Scenario analysis of the interplay: alert lifecycle (firing, re-raise, snooze through
  every channel, self-ending, alert hand-over), key handling and key blocking,
  overlay/lock screen, channel/full-screen-intent behaviour, permissions and options
  on/off, behaviour for existing users after the update.
- Build evidence: `:app:compileFastDebugJavaWithJavac` **and** a full
  `:app:assembleFastDebug` pass (the APK builds end-to-end).

## Explicitly checked and found in order (no finding)

1. **No missed alerts through any of the changes.** An orphaned alert record (CHG7/A1)
   does not block new alerts: for a real alert `FileBasedNotifications` always restarts
   via `startAlert` (which cleans up the orphan). The CHG8/CHG12 suppression hides a text
   line only, never an alert. The CHG11 coupling only disables *snooze convenience* — the
   alert itself keeps sounding (fail-safe direction).
2. **The preference dependency (CHG11) cannot crash:** the parent switch precedes the
   dependent checkbox in the XML — Android's dependency resolution requires that order,
   otherwise the settings screen crashes. Implemented correctly.
3. **Key handling:** consume decision taken before the snooze (CHG4-A), auto-repeats do
   not count, UUID binding correctly cleared on reset/snooze, no duplicated handling (one
   shared handler), UP events consumed, everything only while actually alerting and with
   both options enabled (CHG11).
4. **Threading:** UI work always via posted runnables (`JoH.runOnUiThread` always posts),
   `AlertPlayer` methods synchronized, instance registry via `CopyOnWriteArrayList` +
   `WeakReference` — no leaks, no deadlock path found.
5. **singleInstance dedupe** prevents double pop-ups when the full-screen intent and the
   direct start both fire; tasks stay out of recents; the previous app / lock screen is
   restored on every close path, including external snoozes (CHG6/BVD2).
6. **Texts/resources:** all apostrophe escapes correct, CHG12 shares one string resource
   between home screen and notification (they can no longer diverge), the Wear module is
   untouched everywhere, all insertion points carry CHG markers (traceable).

---

## Findings

### ER1 — Silent behaviour changes for existing users *(severity: high — functional; release advice: no blocker, but cover before release)*

**For IT service managers.** Three familiar behaviours change after the update without
the user being told. (1) Anyone who silences an alarm with a single press of a volume
button must now press twice within 1.5 seconds. (2) More importantly: that volume-button
feature is **completely off** after the update until the user enables the new option
"Snooze screen over other apps" — the old option was on by default, so part of the user
base silently loses a habit. The alarm itself keeps sounding (it becomes louder rather
than quieter), so there is no medical danger — but a user who presses the volume button
at night and sees nothing happen loses trust in the app. (3) Users of "Override silent
mode" now get the snooze screen instead of the main screen when the display wakes.
Advice: clear release notes and possibly a one-time in-app message; and, for the offer to
the open-source project, explicitly discuss whether the coupling of item (2) is wanted
there — the maintainer asked for *fewer* switches.

**For developers.** (a) `buttons_silence_alert` (upstream default `true`, single press in
`Home.onKeyDown`) is now gated on `snooze_over_other_apps` (default `false`) via
`SnoozeActivity.volumeKeySnoozeEnabled()` — after the update, volume-key snooze is dead
everywhere until the user enables the new pref; the greyed-out-but-checked checkbox is
the only hint. (b) CHG4 replaced the single press + `quietratelimit` with a double press.
(c) The full-screen intent targets `SnoozeLockScreenActivity` instead of `Home`. All
deliberately decided (CHG2/4/11), but nowhere communicated to the end user. Proposal:
generate a release-notes section from the CHG registers; optionally a one-time
`JoH.show_ok_dialog` on first start after the update when
`buttons_silence_alert==true && !snooze_over_other_apps`; raise the CHG11 gate as a
separate discussion point for the upstream PR.

### ER2 — Channel migration (CHG7/A3) overrides user settings of the alert channel *(severity: medium — functional + technical; release advice: decision needed)*

**For IT service managers.** Android stores per-notification-channel settings that the
user can adjust (importance, sound off, and so on). To make the wake screen reliable on
locked phones, the app creates a new high-priority channel for glucose alerts and removes
the old one. Consequence: anyone who deliberately made the old channel quieter loses that
adjustment after the update and gets fully conspicuous notifications again. That is the
safe direction (alerts become more conspicuous rather than quieter), but it does override
a user's choice — and it also happens to users who do not use the new wake-screen
functionality at all. Advice: perform the migration only for users who enable the
relevant functions, or consciously accept it and mention it in the release notes.

**For developers.** `NotificationChannels.getChan(...)` now creates every channel with
base id `BG_ALERT_CHANNEL` as `IMPORTANCE_HIGH` under a new id (suffix `!`) and deletes
the old id — unconditionally, so for everyone with `use_notification_channels` enabled
(that pref defaults to off, which limits the blast radius). The user's per-channel
customisation (importance lowered, different system sound) is lost; this is exactly the
PR-4510/H4 theme. Options: **(a)** accept + release note (fail-safe direction); **(b)**
gate the migration on full-screen-intent relevance; **(c)** drop the suffix/migration and
only create HIGH on fresh installs. Advice: (b) — covers the target group, respects the
customisation of everyone else.

**Rationale for the advice (added 2026-07-12, incl. a correction of the gate):**

1. *Doing nothing is worse for the target group than the intervention:* without HIGH,
   Android silently ignores the full-screen intent — with channels on + wake-screen use,
   CHG2 appears broken without any feedback (the alert audio still plays, the screen
   stays dark). Reverting is not an option; targeting is.
2. *Respect for user choice and platform intent:* Android deliberately makes channel
   importance immutable for apps; the new-id migration circumvents that. For users who
   deliberately quieted the channel (e.g. a parent's phone next to an alerting watch), an
   unrequested reset is a trust and helpdesk risk — and the channels-enabled population
   is precisely the group that tunes deliberately.
3. *Upstream:* in PR #4510 (finding H4) the channel intervention was only accepted when
   coupled to **explicit opt-in**; an unconditional migration makes the future PR
   unnecessarily vulnerable.
4. *Correction of the earlier gate wording:* `snooze_over_other_apps` does **not** belong
   in the gate — it is not a full-screen-intent trigger, and since ER1 that option is on
   by default, so it no longer discriminates. The correct gate:
   `wake_phone_during_alerts` enabled **or** at least one active alert with
   `override_silent_mode` (a small `AlertType` query at channel creation). Anyone outside
   the gate is caught by the existing CHG7/A3 event-log diagnostic (layered defence).
5. *Why (c) falls away:* fresh-installs-only abandons precisely the existing users who
   need the fix; migrating existing installs was the whole purpose of the new channel id.
6. *(a) remains defensible* — one-time effect, reversible by the user, failure direction
   is the safe side — but towards the user and upstream it is the lesser choice.

Order of preference: **(b) with the corrected gate**, otherwise (a) + release notes.

### ER3 — Race on alert hand-over: pop-up can close right after appearing *(severity: low/medium — technical; release advice: small hardening recommended, no blocker)*

**For IT service managers.** In one specific case — one alert transitions seamlessly into
another (e.g. from "low" to "high") — the pop-up screen of the new alert can be closed
immediately by a clean-up action of the old alert. The sound and the notification keep
working and at the next re-raise (within a few minutes) the screen appears again; nobody
misses an alert. So it is a cosmetic flaw with a small probability, but it can be
prevented structurally with two small adjustments.

**For developers.** `startAlert()` → `stopAlert(ClearData=true)` →
`SnoozeActivity.alertEnded()` posts finish runnables; immediately afterwards
`VibrateNotifyMakeNoise` launches the pop-up again. With `singleInstance` reuse, the
posted `finish()` can close the just-reused instance → no screen for the new alert until
the next re-raise (self-healing). Proposal (verified against `JoH.runOnUiThread`, which
always posts): (1) in `Snooze(Context,int,boolean)` move the `alertEnded()` call to after
`activeBgAlert.snooze(...)`, so the snooze state is settled before the runnable can ever
run; (2) give the runnable in `alertEnded()` a guard: only `finish()` when
`!ActiveBgAlert.currentlyAlerting()` at execution time — on the alert hand-over the new,
un-snoozed record already exists and the screen stays; on snooze/end it closes as
intended.

### ER4 — Orphan cleanup via the volume key lapses when the overlay option is off *(severity: low — technical; release advice: acceptable, choice)*

**For IT service managers.** A small clean-up mechanism (removing a left-over alert
remnant after, for example, a test alert) recently ended up behind the same switch as the
volume-button feature. When that is off, the app cleans the remnant up slightly later
through another path. No user impact of significance.

**For developers.** In `volumeKeySnooze()` the CHG7/A1 orphan sweep sits after the
`volumeKeySnoozeEnabled()` check (CHG11), so that clean-up path is no longer reached with
the overlay option off. Alternative clean-up remains (`startAlert` → `stopAlert(true)`,
`ClockTick` on snooze expiry). Proposal: move the sweep before the enabled check (a
two-line move) or explicitly accept.

### ER5 — Register documents CHG3/CHG4 do not yet mention the CHG11 dependency *(severity: low — documentation; release advice: update)*

**For IT service managers.** The change register is the truth for administration and
audit. Two older documents still describe the volume-button feature as working
unconditionally, while it has had a precondition since CHG11. Anyone reading only those
documents draws the wrong conclusion.

**For developers.** `CHG3-volume-key-snooze.md` and `CHG4-volume-key-double-press.md`
(incl. Addendum A: "volume keys are reserved while alerting") lack a supersession
annotation to CHG11 ("only works with `snooze_over_other_apps` enabled") — the same
annotation pattern used earlier for CHG3/CHG4→Addendum A. Documentation work only, no
code.

### ER6 — Test alert always has a 5-second delay *(severity: low — functional; release advice: acceptable, choice)*

**For IT service managers.** The test button now deliberately waits five seconds so the
lock-screen behaviour can be tested. Anyone who just wants to hear a sound quickly also
waits five seconds. A small inconvenience, with a clear message on screen.

**For developers.** CHG9 delays every test firing by a fixed
`TEST_ALERT_DELAY_MS=5000` via `Inevitable.task`. Options: accept (the purpose of CHG9),
apply the delay only when a pop-up scenario is possible, or make it configurable.
Advice: accept + release note; the toast already explains it.

### ER7 — The snooze line can be truncated in the collapsed notification *(severity: low — cosmetic; release advice: verify on device)*

**For IT service managers.** The extra line "Alert … snoozed until …" sits in the
notification below the glucose information. In the smallest (collapsed) presentation of
the notification, that second line may drop off on some devices; expanded it is always
visible. Checking on your own devices is sufficient.

**For developers.** `withSnoozeLine()` appends "\n…" to `notification_summary` in the
collapsed `RemoteViews` (`notification_bg_collapsed`); whether that TextView is
multi-line differs per layout/OEM. On the user's test device the line is visible.
Optionally: omit the line in the collapsed view and only show it in expanded/BigText —
only do this when a device actually shows the problem.

---

## Decision table

| No | Severity | Proposal | User decision |
|---|---|---|---|
| ER1 | high (functional) | Release notes + possibly a one-time in-app message; CHG11 gate as an upstream discussion point | ✔ Implemented (2026-07-12): both options default ON + startup check on the permission — see "Implementation of ER1" below |
| ER2 | medium | Gate the migration on the full-screen-intent-relevant settings (option b) | ✔ Implemented (2026-07-12): option (b) with the corrected gate — see "Implementation of ER2" below |
| ER3 | low/medium | Move `alertEnded()` + guard `!currentlyAlerting()` | ✔ Implemented (2026-07-13) — see "Implementation of ER3" below |
| ER4 | low | Orphan sweep before the CHG11 check, or accept | ✔ Implemented (2026-07-13): sweep moved before the enabled check — see "Implementation of ER4/ER5" below |
| ER5 | low | Annotate the CHG3/CHG4 registers with CHG11 | ✔ Implemented (2026-07-13): supersession notes added — see "Implementation of ER4/ER5" below |
| ER6 | low | Accept + release note | ✔ Accepted (2026-07-13): the current implementation is sufficient, no change needed |
| ER7 | low | Verify on device; only change when a device actually shows the problem | ✔ Accepted (2026-07-13): no change needed — the existing documentation is sufficient; verify on device during release testing |

**Overall verdict:** none of the findings is a release blocker from the standpoint of
patient safety — in all cases the alert itself keeps sounding and the failure direction
is "more conspicuous/louder" or "convenience temporarily gone", never "quieter/missed".
ER1 (communication) and ER2 (respect for user settings) do deserve a decision before the
release; ER3 is a cheap hardening. The build is green end-to-end (`assembleFastDebug`).

---

## Implementation of ER1 (user decision, 2026-07-12)

Chosen solution: **both options default ON** instead of relying on release-notes
communication, plus a **startup check on the system permission**:

1. *Snooze screen over other apps* now has `defaultValue=true`; because an XML default
   does not automatically reach existing installs, all code paths now read both options
   with default **true** (`Pref.getBoolean(key, true)` in `volumeKeySnoozeEnabled()` and
   `SnoozeOverlayActivity.launchIfEnabled()`). Volume-button snooze therefore keeps
   working after the update (the core of ER1) and the CHG11 dependency is satisfied out
   of the box.
2. **Startup check:** `Home.checkOverlayPermission()` (invoked next to the existing
   battery-optimization check) shows — at most once per hour, never during tests — a
   dialog when the option is enabled but the permission *Display over other apps* is
   missing; OK opens the system screen for xDrip directly
   (`ACTION_MANAGE_OVERLAY_PERMISSION`, with a fallback without the package URI). New
   string `overlay_permission_needed_message`.
3. Remaining ER1 item that stays on the release-notes list: the switch to the **double
   press** (CHG4) and the changed full-screen-intent target (CHG2) remain worth
   mentioning; the risk of silent feature loss has been removed by this implementation.

**Files touched (markers `CHG13 ER1`):** `pref_notifications.xml` (default),
`SnoozeActivity.java` + `SnoozeOverlayActivity.java` (default-true reads),
`Home.java` (startup check), `strings.xml` (dialog text).

---

## Implementation of ER2 (user decision, 2026-07-12): option (b) with the corrected gate

> **Superseded by [CHG14](CHG14-adopt-upstream-notificationchannels.md) (2026-07-13):**
> upstream reworked the channel architecture (channels always on, everything
> `IMPORTANCE_HIGH`); the `NotificationChannels` implementation below was replaced by the
> upstream version with the same gated `!`-migration ported onto it. The gate and its
> rationale are unchanged; the AlertPlayer diagnostic remains.

The channel migration of CHG7/A3 now only runs for users who actually need the
full-screen intent, per the corrected gate from the ER2 rationale:

1. New `NotificationChannels.highImportanceWanted(baseChannelId)`: true only for the
   glucose alert channel **and** when *Wake Screen* (`wake_phone_during_alerts`) is
   enabled **or** at least one active alert has `override_silent_mode`
   (`AlertType.getAllActive()` scan). `snooze_over_other_apps` is deliberately **not**
   part of the gate (not a full-screen-intent trigger; default on since ER1, so
   non-discriminating).
2. `importanceSuffix()` and the channel construction in **both** `getChan(...)` overloads
   use this gate: inside the gate the channel is created as `IMPORTANCE_HIGH` under the
   `!`-marked id (migration as before); outside the gate the channel keeps the original
   id and `IMPORTANCE_DEFAULT` — existing user customisations remain untouched.
3. The gate is evaluated at every notification build, so enabling *Wake Screen* or an
   override-silent alert later migrates the channel at the next alert. The unused id
   variant is deleted symmetrically; because Android resurrects a deleted channel with
   its previous settings when its id returns, toggling the gate back restores the user's
   earlier customisations.
4. Residual coverage: the CHG7/A3 event-log diagnostic in `AlertPlayer` remains for the
   cases the gate cannot fix (e.g. a manually demoted new channel).

**Files touched (markers `CHG13 ER2`):** `NotificationChannels.java` (gate helper, gated
construction and symmetric cleanup in both overloads).

---

## Implementation of ER3 (user decision, 2026-07-13)

Both proposed hardening steps applied (markers `CHG13 ER3`):

1. **`AlertPlayer.Snooze(Context, int, boolean)`** now calls `SnoozeActivity.alertEnded()`
   **after** `activeBgAlert.snooze(...)`, so the snooze state is settled before the posted
   runnable can ever run (also when `Snooze` is called from a background thread). In the
   no-active-alert early return, `alertEnded()` is still called so stale overlay screens
   keep closing.
2. **`SnoozeActivity.alertEnded()`** re-checks at execution time: the posted runnable only
   finishes the screen when `!ActiveBgAlert.currentlyAlerting()` (and the instance is not
   already finishing). On an alert hand-over (`startAlert` clears the old record and
   immediately creates a new, un-snoozed one), the new record already exists when the
   runnable runs, so the pop-up stays for the new alert instead of being closed by the
   clean-up of the old one. All intended close paths are unaffected: snoozed (any
   channel) → not alerting → closes; alert cleared/removed → no record → closes;
   `start_snoozed`/`PreSnooze` records are snoozed → closes.

## Implementation of ER4 and ER5 (user decisions, 2026-07-13)

- **ER4:** the CHG7/A1 orphan sweep in `SnoozeActivity.volumeKeySnooze()` now runs
  **before** the `volumeKeySnoozeEnabled()` check, so an orphaned alert record is also
  cleaned up on a volume-key press while the overlay option is off (marker `CHG13 ER4`).
- **ER5:** supersession annotations added to
  [CHG3](CHG3-volume-key-snooze.md) (implementation note) and
  [CHG4](CHG4-volume-key-double-press.md) (document-wide note under the header): both
  features only apply while *Snooze screen over other apps* is enabled (CHG11), with both
  options defaulting to on since CHG13/ER1.
