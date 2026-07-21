# CHG14 — Adopt the reworked upstream NotificationChannels; port the channel migration onto it

| | |
|---|---|
| **Change ID** | CHG14 |
| **Date** | 2026-07-13 |
| **Status** | Implemented (variant and scope decided by the user, 2026-07-13) |
| **Module** | app (phone) |
| **Supersedes** | The `NotificationChannels.java` parts of [CHG7](CHG7-pr4510-adoptions.md) A3 and [CHG13](CHG13-final-review.md) ER2 (re-implemented here on the new upstream base) |

## Context: compatibility analysis against upstream master of 2026-07-13

A three-way comparison (base 2026-07-03 ↔ local ↔ upstream master 2026-07-13) showed that
upstream has reworked its notification architecture since our base snapshot, alongside a
min/targetSdk bump from 24 to 26 (which makes channels mandatory):

- Notification channels are now **always used** (the `use_notification_channels` opt-in
  preference was removed; `XdripNotificationCompat` always resolves a channel and sets
  `CATEGORY_ALARM`).
- `NotificationChannels.getChan(...)` was rewritten: channel groups removed, **every
  channel is created with `IMPORTANCE_HIGH`**, and the second overload now exclusively
  serves the ongoing notification.
- Our CHG7/A3 + CHG13/ER2 changes sat exactly in the rewritten regions (hard textual
  conflict), and their motivation is largely covered by upstream's new code — **except
  one gap**: upstream kept the same channel ids while flipping importance
  DEFAULT→HIGH, and Android ignores importance changes on existing channels. Upgraded
  installs therefore keep a default-importance glucose alert channel, and full-screen
  intents (the CHG2 lock-screen snooze screen) stay silently broken for exactly those
  users.

Of the other locally changed files, 9 are untouched upstream and 6 have upstream changes
in non-overlapping regions (mergeable via git three-way merge; they must not be
transferred as wholesale copies). `NotificationChannels.java` was the only real conflict.

## Decisions (user, 2026-07-13)

1. **Variant (i):** do not transfer our old `NotificationChannels` changes; adopt the
   upstream version and **rebuild the `!`-migration** as a small, targeted addition on
   top of it (the migration gap is real, also for upstream itself).
2. **Scope:** migrate **only the glucose alert channel** (generic migration of all
   channels would wipe upgraders' channel customisations at scale — the ER2 concern).
3. **Gate retained:** the migration only runs for users who actually need the
   full-screen intent — *Wake Screen* enabled or at least one active alert with
   *Override silent mode*. Fresh installs get HIGH regardless (upstream's decision);
   existing channels of users without full-screen-intent need remain untouched.

## Implementation

- `NotificationChannels.java` was **replaced by the upstream 2026-07-13 version**, then
  the migration was ported onto it (markers `CHG14`):
  - `bgMigrationWanted(baseChannelId)`: the ER2 gate (glucose alert channel **and**
    (*Wake Screen* or ≥1 active override-silent alert via `AlertType.getAllActive()`)).
  - `importanceSuffix(baseChannelId)`: `!` id marker when the gate passes; also included
    in the `my_text_hash` sound-change probe so the channel-rotation machinery keeps
    working.
  - In the first `getChan(...)` overload: the channel id carries the marker when gated,
    and the unused id variant is deleted symmetrically (a deleted channel resurrects
    with its previous settings when its id returns, so toggling the gate back restores
    the user's earlier customisations). Importance itself stays `IMPORTANCE_HIGH`
    unconditionally — that is upstream's choice for newly created channels.
  - The second overload (ongoing notification only) is untouched.
- The `AlertPlayer` event-log diagnostic from CHG7/A3 remains as the safety net for any
  remaining gap (e.g. a manually demoted channel).
- During the upcoming git merge onto current master, `NotificationChannels.java`
  resolves as "take ours" (= upstream + this port); it no longer conflicts.

## Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/NotificationChannels.java` | Replaced by the upstream 2026-07-13 version + CHG14 migration port (gate helpers, hash-probe marker, gated id suffix and symmetric cleanup in the alert overload) |

The CHG7/A3 and CHG13/ER2 register sections are annotated as superseded by this change.
The Wear module is untouched.

## Merge trial (2026-07-14) — passed

The full transfer was rehearsed locally in `C:\xampp\htdocs\xDrip-merged` (a copy of the
2026-07-13 upstream master):

1. The 9 upstream-untouched files, the CHG14 `NotificationChannels.java` and the
   `Documentation/changes` register were copied in as-is.
2. The 6 files with non-overlapping upstream changes were combined with a real three-way
   merge (`git merge-file`, base = 2026-07-03 snapshot): **0 conflicts in all six**.
3. Spot checks confirmed every merged file contains both sides (our CHG markers **and**
   the upstream changes, e.g. the storage-permission fix in `Home`, the removed
   `setGroup` in `AlertPlayer`, the removed `use_notification_channels` options).
4. The changed set of the merged tree versus the new master is **exactly the 16
   application files** of this change series.
5. A full `:app:assembleFastDebug` of the merged tree (min/targetSdk 26) is **green**
   (BUILD SUCCESSFUL, 117 tasks).

The merged tree is therefore ready to serve as the content of the pull-request branch.

## Test plan

1. Fresh install, gate off → glucose alert channel `bgAlertChannel<hash>` exists at
   importance High (upstream behaviour), no `!` variant.
2. Upgraded install with an existing default-importance channel, gate off → channel and
   its user customisations remain untouched; the AlertPlayer diagnostic logs when a
   full-screen intent would be needed.
3. Enable *Wake Screen* (or an override-silent alert) and fire an alert → a `...!`
   channel at High exists, the old variant is gone, and the lock-screen snooze screen
   appears via the full-screen intent.
4. Disable the gate again and fire an alert → the plain id returns (with the user's
   earlier settings), the `!` variant is gone.
5. Ongoing notification stays on the silent `ongoingChannel` (upstream behaviour).
6. Regression: alerts/snoozing of CHG1–CHG13 unchanged.
