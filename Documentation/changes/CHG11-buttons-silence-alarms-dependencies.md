# CHG11 — Inform the user about the dependencies of "Buttons silence alarms"

| | |
|---|---|
| **Change ID** | CHG11 |
| **Date** | 2026-07-10 |
| **Status** | Implemented (2026-07-10): hard dependency + reordering, P1 summary text (variant V3) and P4 diagnostic |
| **Module** | app (phone) |
| **Related** | [CHG1](CHG1-snooze-over-other-apps.md), [CHG2](CHG2-fullscreen-snooze-on-lockscreen.md), [CHG3](CHG3-volume-key-snooze.md), [CHG4](CHG4-volume-key-double-press.md), [CHG7](CHG7-pr4510-adoptions.md) A5 |

## Question

Does *Buttons silence alarms* work without *Snooze screen over other apps* — is there a
dependency?

## Analysis: yes, a partial (scenario-dependent) dependency

Volume-key snooze only works while an xDrip screen has key focus. Per scenario, with
*Buttons silence alarms* enabled:

| Alarm scenario | Does volume-key snooze work? | Depends on |
|---|---|---|
| Home open | **Yes** | nothing |
| Another xDrip screen open | **Yes** — since CHG7/A5 the snooze screen pops up over xDrip itself, no preference or permission needed | nothing |
| Another **app** in the foreground (screen on, unlocked) | **Only** when the CHG1 overlay appears | *Snooze screen over other apps* **and** the system permission *Display over other apps* |
| Screen **off** or device **locked** | **Only** when the CHG2 lock-screen snooze screen appears | *Override silent mode* (per alert) **or** *Wake Screen*; the *Display over other apps* grant makes this path more robust (direct start besides the full-screen intent) |
| None of the above screens appear | **No** — fallback: tap the (heads-up) notification first, which opens the snooze screen, where the volume keys do work | — |

So: within xDrip there is **no** dependency; over other apps and on the lock screen the
feature only works **in combination with** the CHG1 c.q. CHG2 options. This scope was
accepted as a known limitation in CHG6/BVD4 but is currently **not communicated to the
user anywhere** — worse, the current summary of *Buttons silence alarms*
(`volume_buttons_snooze`, strings.xml:813: *"Pressing the volume up or down button will
snooze an active alarm when in the app"*) still describes the **pre-CHG4 behaviour**
(single press) and the pre-CHG1/2 scope.

## Proposals (nothing implemented yet)

1. **P1 — Correct and extend the preference summary** of *Buttons silence alarms*, e.g.:
   *"Double press the same volume button twice within 1.5 seconds to snooze an active
   alarm. Works inside xDrip; over other apps and on the lock screen only together with
   'Snooze screen over other apps' resp. 'Wake Screen'/Override silent mode."*
   Smallest change, always visible at the right moment. Note: rewording an existing
   string invalidates its Crowdin translations (CHG10 consideration).
2. **P2 — One-time dialog when switching the option ON** (`OnPreferenceChangeListener` in
   `Preferences.java`, same pattern as the CHG1 permission listener): checks the related
   settings and tells the user specifically what is missing for the over-other-apps and
   lock-screen scenarios (including a shortcut to enable them). Most informative;
   slightly more code and strings.
3. **P3 — Reorder the settings screen** so *Snooze screen over other apps* appears
   directly below *Buttons silence alarms* (both are already in *Glucose alerts
   settings*), visually suggesting the relation. Deliberately **no**
   `android:dependency`: the overlay/lock-screen features are also useful without volume
   snooze (snooze button on the pop-up), so a hard dependency would be wrong.
4. **P4 — Runtime diagnostic**: when an alarm fires with *Buttons silence alarms* on
   while no snooze screen could be shown (scenario "other app, overlay unavailable" or
   "locked, no wake trigger"), write a rate-limited event-log entry explaining which
   setting would enable it. Zero UI impact, helps remote troubleshooting.

Recommended combination: **P1 + P2** (direct answer at the setting itself plus an active
explanation at the moment of enabling), optionally P4 as cheap diagnostics. P3 is
cosmetic and optional.

## Decisions (2026-07-10)

1. **Hard dependency chosen** (user decision, overriding the proposal's advice against
   it): *Snooze screen over other apps* is shown **directly above** *Buttons silence
   alarms*, and the latter can only be toggled while the former is enabled
   (`android:dependency`). This supersedes P2 and P3.
2. **P1** approved; the user chose text variant **V3**: *"Press the same volume button
   twice within 1.5 seconds to snooze. Works wherever the snooze screen appears; for a
   dark or locked screen also enable Wake Screen or 'Override silent mode'"* (replaces
   the outdated `volume_buttons_snooze` summary; translation impact accepted).
3. **P4** approved: rate-limited event-log diagnostics.

## Implementation — hard dependency (done)

- `pref_notifications.xml`: the CHG1 switch `snooze_over_other_apps` moved from below
  *Wake Screen* to directly above `buttons_silence_alert`; `buttons_silence_alert` got
  `android:dependency="snooze_over_other_apps"` (greyed out and untogglable while the
  parent is off).
- `SnoozeActivity`: new guard `volumeKeySnoozeEnabled()` — requires **both** preferences —
  used by `volumeKeySnooze()` and `volumeKeyConsumed()`. This runtime enforcement is
  necessary because `buttons_silence_alert` defaults to **true**: a UI-only dependency
  would leave the feature silently active (greyed out but checked) for every user who has
  not enabled the parent option.

**Behavioural consequences (intended):**

- Volume-key snooze (and the CHG4 volume blocking) is now inactive **everywhere** —
  including Home and the lock-screen snooze screen — while *Snooze screen over other
  apps* is off.
- Because `buttons_silence_alert` defaults to true, enabling *Snooze screen over other
  apps* immediately activates volume-key snooze for users who never touched the buttons
  option.
- *(Since CHG13/ER1, 2026-07-12: `snooze_over_other_apps` defaults to **on** — code reads
  with default true — so the dependency is satisfied out of the box and volume-key snooze
  keeps working after the update; a startup prompt asks for the missing system
  permission.)*

## Implementation — P1 (summary text, variant V3)

`strings.xml`: the summary `volume_buttons_snooze` now describes the double press (same
button, twice within 1.5 seconds) and the one dependency the greyed-out parent switch
cannot visualise: for a dark or locked screen, *Wake Screen* or the alert's *Override
silent mode* must also be enabled.

## Implementation — P4 (runtime diagnostics)

`AlertPlayer.VibrateNotifyMakeNoise()`, after the snooze-screen launch branch, writes a
rate-limited (1×/hour) event-log entry when volume-key snooze is enabled but a snooze
screen cannot appear:

- screen off / locked without *Wake Screen* or *Override silent mode* → "enable Wake
  Screen or the alert's Override silent mode";
- screen on without the *Display over other apps* grant → "may not work over other apps:
  the permission ... is not granted" (worded with "may" because it does work when xDrip
  itself is in the foreground, per CHG7/A5).

## Files changed

| File | Change |
|---|---|
| `app/src/main/res/xml/pref_notifications.xml` | Switch moved above the buttons option; `android:dependency` added |
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` | `volumeKeySnoozeEnabled()` guard (public) in `volumeKeySnooze()` and `volumeKeyConsumed()` |
| `app/src/main/res/values/strings.xml` | P1: reworded `volume_buttons_snooze` summary (V3) |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java` | P4: two rate-limited event-log diagnostics after the launch branch |

Insertion points are marked with a `CHG11` comment.

## Test plan

1. *Snooze screen over other apps* off → *Buttons silence alarms* is greyed out and
   cannot be toggled; volume keys behave completely normally during an alarm (also in
   Home and on the lock-screen snooze screen).
2. Enable *Snooze screen over other apps* → the buttons option becomes togglable; with it
   checked (default), double-press volume snooze works again everywhere.
3. The settings screen shows the two options adjacent, snooze-screen switch on top.
4. The new summary text is shown under *Buttons silence alarms*.
5. P4: alarm with volume snooze enabled, screen off, no Wake Screen/override-silent → one
   event-log hint per hour; same for missing overlay permission with the screen on.
6. Regression: CHG3/CHG4/CHG7 volume behaviour unchanged when both options are on.
