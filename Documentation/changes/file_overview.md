# File overview — CHG1 … CHG14

Summary of every file that was **added** or **modified** for the changes CHG1–CHG5 and
CHG7–CHG9, and the review findings of CHG6 (BVD1–BVD6). CHG10 is a documentation/process
item (upstream strategy, no code). Last updated: 2026-07-10. The Wear module was not
touched by any of these changes.

## Application files

| File | Status | Changed by | Summary |
|---|---|---|---|
| `app/src/main/AndroidManifest.xml` | Modified | CHG1, CHG2 | `SYSTEM_ALERT_WINDOW` permission; registration of `.SnoozeOverlayActivity` (CHG1) and `.SnoozeLockScreenActivity` (CHG2), both in their own task and hidden from recents |
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeOverlayActivity.java` | **Added** (CHG1) | CHG1, CHG6/BVD1, CHG7/A5 | Floating snooze screen over the app in use; `launchIfEnabled()` guard/launcher; restores the previous app on close. BVD1: locks the navigation drawer in `onResume()`. CHG7: shown without preference/permission when xDrip itself is foreground |
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeLockScreenActivity.java` | **Added** (CHG2) | CHG2, CHG6/BVD1, CHG6/BVD5 | Full-screen snooze screen above the lock screen (show-when-locked / turn-screen-on); target of the full-screen intent and direct launch. BVD1: locks the drawer; BVD5: `wakeUpScreen()` also from `onNewIntent()` |
| `app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java` | Modified | CHG1, CHG3, CHG4 (+Addendum A), CHG6/BVD1-2-3, CHG7/A1-A2-A5 | CHG1: overridable hooks `applyThemeChoice()` / `openHomeAfterSnooze()`. CHG3: shared `volumeKeySnooze()` + `onKeyDown()`. CHG4: double-press state machine, confirmation toast, `volumeKeyConsumed()`, `onKeyUp()`. BVD1: `lockNavigationDrawer()` helper; BVD2: instance registry + `alertEnded()` + `onDestroy()`; BVD3: `resetVolumeKeyConfirmation()`. CHG7: `snoozeForType()` null guards, orphan cleanup, "Alert type not found", UUID-bound double press, per-button toasts, seconds display. CHG8: `recheckAlerts()` without rate limit. CHG11: `volumeKeySnoozeEnabled()` guard (dependency on the overlay option). CHG13: ER1 default-true reads; ER3 execution-time guard in `alertEnded()`; ER4 orphan sweep before the enabled check |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java` | Modified | CHG1, CHG2, CHG6/BVD2-3, CHG7/A3-A5 | CHG1: launch overlay after posting the alert notification. CHG2: full-screen intent retargeted to the lock-screen snooze screen and broadened (override silent **or** Wake Screen, independent of sound profile); direct launch when screen off/locked. BVD2/3: `alertEnded()` hooks in `Snooze(...)` and `stopAlert(ClearData)`. CHG7: channel-importance diagnostic; type-based snooze default in `GuessDefaultSnoozeTime()`. CHG11/P4: event-log diagnostics when volume-key snooze cannot work. CHG12: `staticUpdateNotification()` directly after `snooze()` in `Snooze(...)` and `PreSnooze()`. CHG13/ER3: `alertEnded()` hook moved after `snooze()` |
| `app/src/main/java/com/eveningoutpost/dexdrip/models/ActiveBgAlert.java` | Modified | CHG7/A1, CHG8 | `alertTypegetOnly()` no longer destructively clears the record on a failed lookup; `currentlySnoozed()` helper |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/Notifications.java` | Modified | CHG8 | Snooze line ("Alert &lt;name&gt; snoozed until HH:mm") in the ongoing BG notification, with threshold/vehicle-mode/stale-data suppression and prompt refresh on snooze-state changes |
| `app/src/main/java/com/eveningoutpost/dexdrip/EditAlertActivity.java` | Modified | CHG7/A1, CHG9 | REMOVE ALERT stops/clears a matching active alert and rebuilds notifications; test alert fires after a 5-second delay |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/NotificationChannels.java` | Modified | CHG14 (supersedes CHG7/A3 + CHG13/ER2 here) | Upstream 2026-07-13 version adopted (channels always on, everything HIGH) with the gated `!`-migration ported onto it, so upgraded installs of full-screen-intent users get a working high-importance glucose alert channel |
| `app/src/main/java/com/eveningoutpost/dexdrip/Home.java` | Modified | CHG3, CHG4 (+Addendum A), CHG13/ER1 | `onKeyDown()` delegates to the shared volume-key handler; consume decision (no volume change during alarm); new `onKeyUp()` override; ER1: startup prompt when the overlay permission is missing |
| `app/src/main/java/com/eveningoutpost/dexdrip/utils/Preferences.java` | Modified | CHG1 | On enabling *Snooze screen over other apps* without the permission: toast + open the system screen "Display over other apps" |
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/StatusLine.java` | Modified | CHG5, CHG12 | Extra-status-line segment while a snoozed active alert exists; CHG12: shows the alert name via the shared `notification_alert_snoozed_until` resource (identical to the notification line) |
| `app/src/main/res/values/styles.xml` | Modified | CHG1 | Floating theme `SnoozeOverlayTheme` (keeps the app behind running and in control of orientation) |
| `app/src/main/res/xml/pref_notifications.xml` | Modified | CHG1, CHG11 | Switch `snooze_over_other_apps` under *Glucose alerts settings*; CHG11: moved directly above *Buttons silence alarms*, which now has `android:dependency` on it |
| `app/src/main/res/xml/pref_advanced_settings.xml` | Modified | CHG5 | Checkbox `status_line_snoozed_alert` ("Inform on snoozed alert") in the *Extra Status Line* menu |
| `app/src/main/res/values/strings.xml` | Modified | CHG1, CHG4, CHG5, CHG7, CHG8, CHG9, CHG11 | CHG1: option title/summary + permission toast; CHG4: confirmation-toast text; CHG5: option title/summary; CHG7: orphan status + per-button volume-snooze texts; CHG8: notification snooze line; CHG9: delayed-test toast; CHG11/P1: reworded `volume_buttons_snooze` summary; CHG12: `summary_inform_on_snoozed_alert` now says "[name]" |

### GitHub commit texts per application file

Texts to enter in the GitHub *Commit changes* dialog when uploading each file to the
branch: the first line is the commit title, the second the optional extended description.
Kept functional — technical details are in the source (CHG markers) and the CHG documents.

**`app/src/main/AndroidManifest.xml`**
- Title: `Register snooze screens and the display-over-other-apps permission`
- Description: `Adds the Android permission 'Display over other apps' and registers the two new snooze screens, so glucose alerts can be acknowledged on top of the app in use and on the lock screen.`

**`app/src/main/java/com/eveningoutpost/dexdrip/SnoozeOverlayActivity.java`**
- Title: `Add snooze screen that appears over the app in use`
- Description: `New floating snooze screen shown when a glucose alert fires while another app is in the foreground; after snoozing, the previous app is restored automatically.`

**`app/src/main/java/com/eveningoutpost/dexdrip/SnoozeLockScreenActivity.java`**
- Title: `Add snooze screen that wakes the display on the lock screen`
- Description: `New full-screen snooze screen that turns the display on and appears above the lock screen (without unlocking) when a glucose alert fires; closing it returns to the lock screen.`

**`app/src/main/java/com/eveningoutpost/dexdrip/SnoozeActivity.java`**
- Title: `Snooze alerts with a double volume-button press`
- Description: `Volume buttons snooze an active alert with a double press of the same button within 1.5 seconds and no longer change the phone volume during an alarm. Pop-up snooze screens close automatically when the alert is snoozed elsewhere or ends, their navigation menu is disabled, and orphaned alert leftovers are cleaned up.`

**`app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/AlertPlayer.java`**
- Title: `Show the snooze screen when an alert fires`
- Description: `When a glucose alert fires, the snooze screen is brought up over the app in use or - with Wake Screen or override-silent - above the lock screen. The ongoing notification refreshes immediately after snoozing, and the event log explains when a snooze screen cannot be shown.`

**`app/src/main/java/com/eveningoutpost/dexdrip/models/ActiveBgAlert.java`**
- Title: `Keep test alerts snoozable and expose the snooze state`
- Description: `An active alert whose alert type is missing (such as a test alert) is no longer deleted during lookups, so it can still be snoozed; adds a helper to read the snoozed state.`

**`app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/Notifications.java`**
- Title: `Show the snoozed-alert status in the ongoing notification`
- Description: `While a glucose alert is snoozed, the ongoing notification shows "Alert <name> snoozed until HH:mm"; the line disappears when glucose recovers or the snooze ends.`

**`app/src/main/java/com/eveningoutpost/dexdrip/EditAlertActivity.java`**
- Title: `Fire test alerts after 5 seconds and clean up on alert removal`
- Description: `The test alert now fires after five seconds, so the lock-screen and over-other-apps behaviour can be tested; removing an alert also stops and clears its active alarm.`

**`app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/NotificationChannels.java`**
- Title: `Ensure the alert channel can show the lock-screen snooze screen`
- Description: `For users of the wake-screen or override-silent features, the glucose alert notification channel is recreated with high importance, which Android requires for full-screen alerts; all other users keep their existing channel and its settings.`

**`app/src/main/java/com/eveningoutpost/dexdrip/Home.java`**
- Title: `Volume-button snooze on the home screen and a startup permission check`
- Description: `The home screen uses the shared double-press volume snooze and, when the snooze-over-other-apps option is enabled without the required Android permission, asks at startup to grant it.`

**`app/src/main/java/com/eveningoutpost/dexdrip/utils/Preferences.java`**
- Title: `Guide the user to the display-over-other-apps permission`
- Description: `Enabling 'Snooze screen over other apps' without the system permission opens the Android settings screen where it can be granted.`

**`app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/StatusLine.java`**
- Title: `Optionally show the snoozed alert in the extra status line`
- Description: `With the new option enabled, the extra status line shows "Alert <name> snoozed until HH:mm" while an alert is snoozed, identical to the notification text.`

**`app/src/main/res/values/styles.xml`**
- Title: `Add a floating theme for the snooze screen over other apps`
- Description: `The floating theme keeps the app behind the snooze screen visible and running, so it is restored unchanged after snoozing.`

**`app/src/main/res/xml/pref_notifications.xml`**
- Title: `Add the snooze-over-other-apps option, coupled with buttons-silence-alarms`
- Description: `New switch for the snooze screen over other apps (default on), placed directly above 'Buttons silence alarms', which can only be enabled together with it.`

**`app/src/main/res/xml/pref_advanced_settings.xml`**
- Title: `Add 'Inform on snoozed alert' to the Extra Status Line options`
- Description: `New option that shows the snoozed-alert status in the extra status line.`

**`app/src/main/res/values/strings.xml`**
- Title: `Add texts for the new snooze features`
- Description: `New user-facing texts for the snooze screens, the volume-button snooze, the snoozed-alert status and the permission prompts.`

## Documentation files (change register)

| File | Status | Changed by | Summary |
|---|---|---|---|
| `Documentation/changes/CHG1-snooze-over-other-apps.md` | Pre-existing spec, annotated | CHG1, CHG6/BVD6 | CHG1 specification/register; BVD6 added the cross-reference to CHG2 for the locked-device case |
| `Documentation/changes/CHG2-fullscreen-snooze-on-lockscreen.md` | **Added** (CHG2) | CHG2 | Analysis, implementation, decisions and test plan of CHG2 |
| `Documentation/changes/CHG3-volume-key-snooze.md` | **Added** (CHG3) | CHG3, CHG4 (supersession notes), CHG6/BVD4 | CHG3 register; annotated where CHG4/Addendum A superseded statements; BVD4 recorded as accepted known limitation |
| `Documentation/changes/CHG4-volume-key-double-press.md` | **Added** (CHG4) | CHG4, Addendum A | CHG4 register including Addendum A (volume keys change nothing during an alarm) |
| `Documentation/changes/CHG5-statusline-snoozed-alert.md` | **Added** (CHG5) | CHG5 | CHG5 register |
| `Documentation/changes/CHG6-review-chg1-chg5.md` | **Added** (CHG6) | CHG6 | Review report with findings BVD1–BVD6, functional (management) descriptions, decisions, per-finding files and fix test plan |
| `Documentation/changes/CHG7-pr4510-adoptions.md` | **Added** (CHG7) | CHG7 | Adoptions from upstream PR #4510: robustness cluster + volume-snooze details (items A1/A2/A3/A5 of the comparison review) |
| `Documentation/changes/CHG8-notification-snooze-line.md` | **Added** (CHG8) | CHG8 | Snooze line in the ongoing BG notification (item A4 of the comparison review) |
| `Documentation/changes/CHG9-delayed-test-alert.md` | **Added** (CHG9) | CHG9 | Test alert fires after a 5-second delay so CHG1/CHG2 can be exercised |
| `Documentation/changes/CHG10-upstream-strategy-pr4510.md` | **Added** (CHG10) | CHG10 | Upstream status/strategy for PR #4510 + traceability of the full comparison answer (no code) |
| `Documentation/changes/CHG11-buttons-silence-alarms-dependencies.md` | **Added** (CHG11) | CHG11 | Dependency analysis of "Buttons silence alarms"; implemented: hard dependency + reordering, reworded summary (P1/V3) and event-log diagnostics (P4) |
| `Documentation/changes/CHG12-consistent-snooze-text-and-prompt-refresh.md` | **Added** (CHG12) | CHG12 | Identical snooze text (alert name) on all surfaces + immediate notification refresh after snoozing |
| `Documentation/changes/CHG14-adopt-upstream-notificationchannels.md` | **Added** (CHG14) | CHG14 | Compatibility analysis vs upstream master 2026-07-13 + adoption of the reworked upstream NotificationChannels with the gated migration ported onto it |
| `Documentation/changes/CHG13-final-review.md` | **Added** (CHG13) | CHG13 | Pre-release review of CHG1–CHG12: findings ER1–ER7 with dual (management/developer) argumentation and decision table; ER1 implemented (defaults on + startup permission prompt) |
| `Documentation/changes/pull-request-draft.md` | **Added** | CHG10 (action 2) | Draft title + description for the new upstream PR that supersedes draft #4510 |
| `Documentation/changes/file_overview.md` | **Added** | — | This overview |

## Cross-reference: files per change / finding

- **CHG1** — `AndroidManifest.xml`, `SnoozeOverlayActivity.java` (new), `SnoozeActivity.java`, `AlertPlayer.java`, `styles.xml`, `pref_notifications.xml`, `Preferences.java`, `strings.xml`
- **CHG2** — `AndroidManifest.xml`, `SnoozeLockScreenActivity.java` (new), `AlertPlayer.java`
- **CHG3** — `SnoozeActivity.java`, `Home.java`
- **CHG4 (+ Addendum A)** — `SnoozeActivity.java`, `Home.java`, `strings.xml`
- **CHG5** — `pref_advanced_settings.xml`, `strings.xml`, `StatusLine.java`
- **CHG6/BVD1** — `SnoozeActivity.java`, `SnoozeOverlayActivity.java`, `SnoozeLockScreenActivity.java`
- **CHG6/BVD2** — `SnoozeActivity.java`, `AlertPlayer.java`
- **CHG6/BVD3** — `SnoozeActivity.java` (+ shared BVD2 hooks in `AlertPlayer.java`)
- **CHG6/BVD4** — no code (accepted); known-limitation note in `CHG3-volume-key-snooze.md`
- **CHG6/BVD5** — `SnoozeLockScreenActivity.java`
- **CHG6/BVD6** — `CHG1-snooze-over-other-apps.md`
- **CHG7** — `ActiveBgAlert.java`, `EditAlertActivity.java`, `SnoozeActivity.java`, `SnoozeOverlayActivity.java`, `AlertPlayer.java`, `NotificationChannels.java`, `strings.xml`
- **CHG8** — `Notifications.java`, `ActiveBgAlert.java`, `SnoozeActivity.java`, `strings.xml`
- **CHG9** — `EditAlertActivity.java`, `strings.xml`
- **CHG10** — documentation only: `CHG10-upstream-strategy-pr4510.md`
- **CHG11** — `pref_notifications.xml`, `SnoozeActivity.java`, `strings.xml`, `AlertPlayer.java`
- **CHG12** — `StatusLine.java`, `AlertPlayer.java`, `strings.xml`
- **CHG13** — `CHG13-final-review.md` (findings ER1–ER7); ER1 implemented in `pref_notifications.xml`, `SnoozeActivity.java`, `SnoozeOverlayActivity.java`, `Home.java`, `strings.xml`; ER2 in `NotificationChannels.java` (superseded by CHG14); ER3 in `SnoozeActivity.java` + `AlertPlayer.java`; ER4 in `SnoozeActivity.java`; ER5 as annotations in `CHG3-volume-key-snooze.md` and `CHG4-volume-key-double-press.md`
- **CHG14** — `NotificationChannels.java` (upstream base + migration port)

All code insertion points are marked in the source with a `CHG1` … `CHG5`, `CHG6 BVDn`,
`CHG7 A<n>`, `CHG8` or `CHG9` comment, so each change remains traceable per file.
