# CHG15 — Remove all new string resources (English literals pending a follow-up strings PR)

| | |
|---|---|
| **Change ID** | CHG15 |
| **Date** | 2026-07-15 |
| **Status** | Implemented (applied to the local tree and to the merged tree) |
| **Module** | app (phone) |
| **Source** | Review feedback from Navid200 on [PR #4623](https://github.com/NightscoutFoundation/xDrip/pull/4623): *"It will take a while to make the strings perfect. I suggest removing all the strings from this PR."* — keep the texts as plain English so string wording/translation review does not delay the functionality; agree on final strings in a later, dedicated PR. |

## Decision

All **15 new** string resources introduced by CHG1–CHG13 are removed from `strings.xml`,
and the **one reworded existing** resource is restored, so `strings.xml` is byte-for-byte
identical to upstream again and drops out of the PR entirely (16 → 15 changed files).
The user-facing texts remain, as English literals:

| Former resource | Now | Where |
|---|---|---|
| `snooze_over_other_apps` (+ `_summary`) | XML literals | `pref_notifications.xml` (title/summary of the overlay option) |
| `inform_on_snoozed_alert` (+ `summary_...`) | XML literals | `pref_advanced_settings.xml` (Extra Status Line option) |
| `volume_buttons_snooze` (existing, was reworded by CHG11/P1) | **Resource restored to its original text** (all translations untouched); the corrected wording lives as an XML literal on the preference | `strings.xml` reverted; `pref_notifications.xml` |
| `press_same_volume_button_again`, `volume_down_confirm_snooze`, `volume_up_confirm_snooze`, `volume_button_wrong_button`, `snoozing_due_volume_down_button_press`, `snoozing_due_volume_up_button_press` | Java literals (helpers now return `String`) | `SnoozeActivity.java` |
| `alert_type_not_found` | Java literal | `SnoozeActivity.java` (`displayStatus()`) |
| `test_alert_fires_in_5_seconds` | Java literal | `EditAlertActivity.java` |
| `please_allow_display_over_other_apps` | Java literal | `Preferences.java` |
| `overlay_permission_needed_message` | Java literal | `Home.java` (startup dialog) |
| `notification_alert_snoozed_until` | **One shared helper** `Notifications.snoozeLineText(name, until)` used by both the ongoing notification and the extra status line, preserving the CHG12 guarantee that both surfaces stay identical | `Notifications.java`, `StatusLine.java` |

Existing, already-translated resources that the changes merely reuse (`snooze`,
`please_allow_permission`, `snoozing_due_button_press` for the mute path, `high`/`low`,
`buttons_silence_alarms`, …) are unaffected — they need no review and stay as resources.

## Implementation notes

- Inline literals in preference XML follow an existing codebase precedent (e.g. the
  "Use AOD chip style" option). Apostrophes need no escaping in XML attribute values.
- `showVolumeKeyConfirmToast` and the two per-button text helpers in `SnoozeActivity` now
  take/return `String` instead of resource ids.
- The now-unused `R`/`xdrip` imports were removed from `StatusLine.java`.
- Applied identically to **both** trees: the local development tree and the verified
  merged tree (`xDrip-merged`), each with a green compile.
- All insertion points are marked with a `CHG15` comment. The string lists mentioned in
  the CHG1–CHG13 documents describe the state before this change.

## Follow-up (upstream)

After this PR is merged, open a dedicated **strings PR** that converts the literals into
translatable resources with agreed wording (Crowdin round without functional risk) —
added to the upstream action list of [CHG10](CHG10-upstream-strategy-pr4510.md).

## Test plan

1. Settings show the English texts for the overlay option, the buttons option and the
   Extra Status Line option (all languages).
2. Volume-key toasts (confirm, wrong button, snoozing DOWN/UP) show the English literals;
   the mute snoozing toast still uses the translated existing resource.
3. Snooze line reads "Alert <name> snoozed until HH:mm" and is identical in the ongoing
   notification and the extra status line.
4. Startup permission dialog and the preference toast show the English literals.
5. `strings.xml` is byte-identical to upstream (verified with `cmp`); translated locales
   are unaffected.
6. Regression: all CHG1–CHG14 behaviour unchanged (texts only).
