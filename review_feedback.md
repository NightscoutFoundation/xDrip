# Code Review & Feedback: Scheduled Transmitter ID Switch (G7 Soak & Handoff)

We have reviewed your most recent changes (committed locally in `1dd3865c1` and the PR diff). The separation of the timing and scheduling logic into the pure `SoakSchedule` class, the restriction of the next transmitter ID field to Dexcom G5/G6/G7, and the unit tests in `SoakScheduleTest` are excellent additions that directly address the previous code quality concerns.

During our review, we identified a few important **silent failure modes, lifecycle race conditions, and UI polish items** that should be addressed before merging.

---

## 1. Thread-Safety & Context Race Condition (High Priority)

In [SystemStatusFragment.java](file:///a:/Development%20-%20Android/xDrip-G7Pr/app/src/main/java/com/eveningoutpost/dexdrip/SystemStatusFragment.java#L190-L203), `performAutoSwitch` starts a background thread to handle database operations and restart the service:

```java
    private void performAutoSwitch(final String nextId) {
        Pref.setString("dex_txid", nextId);
        SoakSchedule.deactivate();

        new Thread(() -> {
            UserError.Log.d(TAG, "Automatically switching to new Transmitter ID: " + nextId);
            clearDataWhenTransmitterIdEntered(nextId);
            CollectionServiceStarter.restartCollectionServiceBackground();
            Home.staticRefreshBGCharts();
            final Context context = safeGetContext();
            if (context instanceof Activity) {
                LocationHelper.requestLocationForBluetooth((Activity) context);
            }
        }).start();

        JoH.static_toast_long(gs(R.string.auto_switched_to_new_transmitter, nextId));
        set_current_values();
    }
```

### The Issues:
1. **Context Lifecycle Race:** If the user rotates the device or leaves the fragment immediately after the auto-switch triggers, the fragment will detach before the background thread executes `safeGetContext()`. If detached, `safeGetContext()` falls back to `xdrip.getAppContext()` (Application Context). Since Application Context is not an `Activity`, `context instanceof Activity` evaluates to `false`, and **`LocationHelper.requestLocationForBluetooth` is silently skipped**. If location services are off, Bluetooth pairing will silently fail.
2. **UI Operations on Background Thread:** `LocationHelper.requestLocationForBluetooth` displays dialogs and checks permissions. Running UI-related logic directly on a background thread is prone to crashes or undefined UI behavior.

### Recommended Fix:
Capture the `Activity` reference on the main thread while the fragment is active, and delegate the location permission check back to the UI thread:

```java
    private void performAutoSwitch(final String nextId) {
        Pref.setString("dex_txid", nextId);
        SoakSchedule.deactivate();

        final Activity activity = getActivity(); // Capture on the UI thread

        new Thread(() -> {
            UserError.Log.d(TAG, "Automatically switching to new Transmitter ID: " + nextId);
            clearDataWhenTransmitterIdEntered(nextId);
            CollectionServiceStarter.restartCollectionServiceBackground();
            Home.staticRefreshBGCharts();
            if (activity != null) {
                activity.runOnUiThread(() -> LocationHelper.requestLocationForBluetooth(activity));
            }
        }).start();

        JoH.static_toast_long(gs(R.string.auto_switched_to_new_transmitter, nextId));
        set_current_values();
    }
```

---

## 2. Stale Notification Drawer (Medium Priority / UX)

Currently, when the switch time is reached, [Notifications.java](file:///a:/Development%20-%20Android/xDrip-G7Pr/app/src/main/java/com/eveningoutpost/dexdrip/utilitymodels/Notifications.java#L954-L961) raises the **New Sensor Ready** notification (`soakTimerNotificationId`). 

### The Issue:
Once the switch is performed (either automatically by opening the status screen or if the user cancels/manually overrides the queued transmitter), **the notification remains in the drawer**. The user has to swipe it away manually.

### Recommended Fix:
Add a public static helper method to `Notifications.java` to cancel notifications:

```java
    public static void cancelNotification(int notificationId) {
        try {
            final Context context = xdrip.getAppContext();
            final NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotifyMgr != null) {
                mNotifyMgr.cancel(notificationId);
            }
        } catch (Exception e) {
            Log.e("Notifications", "Failed to cancel notification: " + notificationId, e);
        }
    }
```

Then, call this helper to clear the notification in the following three places:
1. **At Auto-Switch Time** in `SystemStatusFragment.performAutoSwitch`:
   ```java
   Notifications.cancelNotification(Notifications.soakTimerNotificationId);
   ```
2. **On Manual Changeover** in `Preferences.java` (clearing pending switch when active ID changes):
   ```java
   SoakSchedule.deactivate();
   Notifications.cancelNotification(Notifications.soakTimerNotificationId);
   ```
3. **On Explicit Cancellation** in `Preferences.java` (blanking the next ID):
   ```java
   SoakSchedule.clearAll();
   Notifications.cancelNotification(Notifications.soakTimerNotificationId);
   ```

---

## 3. Grace Window vs. Short Delays (Low Priority)

In `SoakSchedule.java`, the grace period is defined as:
```java
public static final long GRACE_MS = 60000L; // 1 minute
```
The schedule is considered due when:
```java
now >= (switchTime - GRACE_MS)
```

### The Issue:
If a user schedules a switch with a **1-minute delay**, the target `switchTime` is `NOW + 60,000ms`. When checked immediately:
`NOW >= (NOW + 60,000 - 60,000)` $\rightarrow$ `NOW >= NOW` (which is **true**).
This means a 1-minute delay will evaluate as due immediately and fire. While a 1-minute delay is rarely used in practice, it is a minor timing quirk that is good to be aware of.

---

## 4. Developer / Test Tip: Gradle Ambiguity

When running Gradle commands on this multi-flavor project (such as `./gradlew :app:testDebugUnitTest`), the build system fails with:
`Cannot locate tasks that match ':app:testDebugUnitTest' as task 'testDebugUnitTest' is ambiguous in project ':app'.`

For unit testing the change:
* Run the flavor-specific task instead: `.\gradlew :app:testFastDebugUnitTest`
* Target the specific class to save compilation time:
  ```bash
  $env:JAVA_HOME="C:\Program Files\Android\Android Studio1\jbr"
  .\gradlew :app:testFastDebugUnitTest --tests "com.eveningoutpost.dexdrip.cgm.dex.SoakScheduleTest"
  ```
