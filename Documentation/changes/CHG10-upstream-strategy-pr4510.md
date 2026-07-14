# CHG10 — Upstream status and strategy for PR #4510

| | |
|---|---|
| **Change ID** | CHG10 |
| **Date** | 2026-07-10 |
| **Status** | **Registered — upstream follow-up actions open** (documentation item, no code change) |
| **Module** | — (process/upstream) |
| **Source** | Section C of the comparison review of [PR #4510](https://github.com/NightscoutFoundation/xDrip/pull/4510) vs. CHG1–CHG6 (2026-07-08). Sections A and B of that review are covered by CHG7/CHG8 c.q. CHG1–CHG6; this item records the remaining, previously unregistered part. |
| **Related** | [CHG7](CHG7-pr4510-adoptions.md), [CHG8](CHG8-notification-snooze-line.md) |

## Upstream status (as of the review, 2026-07-08)

PR #4510 ("Snooze glucose alerts with volume buttons, also from lock screen", opened
2026-04-28) is in **draft**; 30 discussion comments, 0 code-review comments. Key feedback:

- **Navid200** tested and confirmed the core works ("snoozes the alert with the screen
  locked by double clicking"), but judged the PR "not ready to be merged yet"; asked
  whether it could be implemented **without yet another switch**; was **reserved about new
  strings** (each one creates translation work in Crowdin); questioned why
  **`EditAlertActivity`** is changed in this PR (scope); asked not to post links to forks.
- **jamorham** asked for screenshots (before/after) and whether it had enough testing;
  **ahaarrestad** asked for automated tests documenting behaviour.
- There is a **merge conflict in the manifest** against current master.
- The author's final comment announces a **clean, reduced-scope re-implementation** — that
  is this local CHG1–CHG9 track.

## How the local track already aligns with the feedback

- *No extra switch:* CHG2 reuses the existing *Wake Screen* setting instead of the PR's
  new `volume_button_snooze_wake_screen` preference (CHG1 adds one switch for the separate
  over-other-apps feature).
- *Documentation/tests:* every CHG has a register document with decisions and a test plan;
  CHG9 (delayed test alert) makes the CHG1/CHG2 scenarios demonstrable for screenshots.
- *EditAlertActivity scope:* the robustness cluster is isolated as CHG7/A1 and can be
  offered upstream as a separate fix-PR.

## Open upstream actions (no code in this tree)

1. Decide the split: offer the CHG7/A1 robustness cluster (orphaned alert / NPE fixes) as
   a **separate upstream fix-PR**, per Navid200's scope remark.
2. Prepare the new clean PR(s) from CHG1–CHG9 on a fresh branch against current master
   (this also resolves the manifest merge conflict of #4510).
3. Review the added strings before submitting (CHG1: 3, CHG4: 1, CHG5: 2, CHG7: 6,
   CHG8: 1, CHG9: 1) and keep them minimal, given the translation-workload concern.
4. Provide before/after screenshots and the per-CHG test evidence in the PR description.
5. Do not link to forks in the upstream repository (maintainer request).

## Traceability: where each point of the comparison answer is registered

| Point of the answer | Registered in |
|---|---|
| A1 orphaned-alert/NPE cluster | CHG7 (A1) — implemented |
| A2 UUID-bound double press | CHG7 (A2) — implemented |
| A3 channel importance for the full-screen intent | CHG7 (A3) — implemented (creation-time approach + diagnostic) |
| A4 snooze line in the ongoing notification | CHG8 — implemented |
| A5 per-button/wrong-button toasts, seconds display, xDrip-foreground popup, 3-layer snooze default | CHG7 (A5) — implemented |
| B1 own tasks + floating overlay theme; every close path restores | CHG1, CHG2 |
| B2 screen-off robustness (direct start + wake lock; FSI only fallback) | CHG2, CHG6/BVD5 |
| B3 held button is not a double press | CHG4 (decision 2); CHG7 "deliberately not adopted" |
| B4 volume blocking scoped to actually alerting, incl. UP events | CHG4 Addendum A + decision 3; CHG7 "deliberately not adopted" |
| B5 one shared key handler | CHG3; CHG7 "deliberately not adopted" |
| B6 guided permission flow | CHG1 |
| B7 proactive auto-close on external snooze / alert end | CHG6 (BVD2, BVD3) |
| B8 navigation drawer locked on lock screen/overlay | CHG6 (BVD1) |
| B9 wake on re-alert; call respect; vibrate-only/silent get the visual presentation; status line on Home/widget/watch; formal register | CHG6/BVD5; CHG1 (call bullet); CHG2 (decision 3); CHG5; the CHG register itself |
| C upstream status, maintainer feedback and upstream strategy | **CHG10 (this item)** — previously unregistered |
