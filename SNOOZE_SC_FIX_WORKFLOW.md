# xDrip `snooze_sc` Branch — Build & Runtime Fix Workflow

Use this guide to reproduce the fixes on a fresh machine. All changes are relative to the `master` branch HEAD.

---

## Prerequisites

- Android Studio Meerkat or newer (AGP 9.0.1 compatible)
- JDK 17 or 21
- ADB installed and on PATH
- Gradle wrapper will download Gradle 9.1.0 automatically

---

## Step 1 — Upgrade Android Gradle Plugin

**File:** `build.gradle`

The branch upgrades AGP from 7.4.2 to 9.0.1 and the Google Services plugin from 4.3.10 to 4.4.2.

```gradle
// Before
classpath 'com.android.tools.build:gradle:7.4.2'
classpath 'com.google.gms:google-services:4.3.10'

// After
classpath 'com.android.tools.build:gradle:9.0.1'
classpath 'com.google.gms:google-services:4.4.2'
```

---

## Step 2 — Upgrade Gradle Wrapper

**File:** `gradle/wrapper/gradle-wrapper.properties`

AGP 9.0.1 requires Gradle 9.x.

```properties
# Before
distributionUrl=https\://services.gradle.org/distributions/gradle-7.5-bin.zip

# After
distributionUrl=https\://services.gradle.org/distributions/gradle-9.1.0-bin.zip
```

---

## Step 3 — Migrate `app/build.gradle` for AGP 9.0.1

**File:** `app/build.gradle`

AGP 9.0.1 deprecates several DSL APIs. Apply the following changes:

### 3a — Do NOT apply `kotlin-android` plugin (Kotlin is built into AGP 9.0.1)

```gradle
// Remove this line entirely — applying it causes "extension already registered" error:
apply plugin: 'kotlin-android'

// Replace with comment:
// kotlin-android is built into AGP 9.0.1, no separate plugin needed
```

### 3b — Add `namespace` to the `android` block

```gradle
android {
    namespace 'com.eveningoutpost.dexdrip'
    compileSdk 34
    // ...
}
```

### 3c — Replace `lintOptions` with `lint`

```gradle
// Before
lintOptions {
    checkReleaseBuilds false
    disable 'MissingTranslation'
    disable 'ExtraTranslation'
}

// After
lint {
    checkReleaseBuilds false
    disable 'MissingTranslation'
    disable 'ExtraTranslation'
}
```

### 3d — Replace `packagingOptions` with `packaging.resources`

```gradle
// Before
packagingOptions {
    exclude 'META-INF/DEPENDENCIES'
    exclude 'META-INF/LICENSE'
    // ... etc
    pickFirst 'org/bouncycastle/x509/CertPathReviewerMessages.properties'
}

// After
packaging {
    resources {
        excludes += ['META-INF/DEPENDENCIES', 'META-INF/LICENSE', /* ... */]
        pickFirsts += ['org/bouncycastle/x509/CertPathReviewerMessages.properties',
                       'org/bouncycastle/x509/CertPathReviewerMessages_de.properties']
    }
}
```

### 3e — Replace `dataBinding { enabled = true }` with `buildFeatures`

```gradle
// Before
dataBinding {
    enabled = true
}

// After
buildFeatures {
    dataBinding = true
    buildConfig = true
}
```

### 3f — Replace deprecated `applicationVariants` version name override

```gradle
// Before
applicationVariants.all { variant ->
    if (variant.buildType.name == "release") {
        variant.outputs.each { output ->
            output.versionNameOverride = generateVersionName()
        }
    }
}

// After
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.versionName.set(generateVersionName())
        }
    }
}
```

### 3g — Update ProGuard file reference

```gradle
// Before (both release and debug)
proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'

// After
proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
```

### 3h — Update databinding version allowlist

```gradle
// Add "9.0.1" to the allowed versions check
if (!["3.1.4", "2.3.0", "1.3.1", "3.4.3", "3.6.4", "7.4.2", "9.0.1"].contains(details.target.version)) {
```

### 3i — Remove wear module reference from flatDir

```gradle
// Before
flatDir {
    dirs project(':app').file('libs'), project(':wear').file('libs')
}

// After
flatDir {
    dirs project(':app').file('libs')
}
```

### 3j — Remove `wearApp` dependency (deprecated in newer Gradle)

```gradle
// Before
wearApp project(':wear')

// After (remove or comment out)
//implementation project(':wear')
```

---

## Step 4 — Disable Unused Modules in `settings.gradle`

**File:** `settings.gradle`

The `:libkeks` module has a Lombok/Java 21 incompatibility and `:wear` is not required for the main app build.

```gradle
// Before
include ':app'
include ':localeapi'
include ':libkeks'
include ':libglupro'
include ':wear'

// After
include ':app'
include ':localeapi'
//include ':libkeks'
include ':libglupro'
//include ':wear'
```

Also remove the dependency from `app/build.gradle`:

```gradle
// Before
implementation project(':libkeks')

// After
//implementation project(':libkeks')
```

---

## Step 5 — Fix Runtime Crash: keks Plugin (Loader.java)

**File:** `app/src/main/java/com/eveningoutpost/dexdrip/plugin/Loader.java`

**Crash:** `RuntimeException: keks plugin not available` thrown in `Ob1G5CollectionService` when a G5/G6 transmitter is used.

```java
// Before — crashes the Bluetooth collection service
case "keks":
    // return jamorham.keks.Plugin.getInstance(parameter);
    throw new RuntimeException("keks plugin not available - module disabled");

// After — graceful degradation; callers already handle null
case "keks":
    // libkeks module disabled due to Lombok/Java 21 incompatibility
    return null;
```

---

## Step 6 — Fix Runtime Crash: Menu Inflation (menu_home.xml)

**File:** `app/src/main/res/menu/menu_home.xml`

**Crash:** `InflateException: Couldn't resolve menu item onClick handler showHelpFromMenu` — all `android:onClick` attributes were stripped from menu items. The `onOptionsItemSelected` in `Home.java` only calls `super`, so without these attributes every menu item is a no-op (or crash on inflate if ProGuard keeps the methods).

Restore `android:onClick` on every item per the table below:

| Item ID | `android:onClick` value |
|---|---|
| `showhelp` | `showHelpFromMenu` |
| `showreminders` | `showRemindersFromMenu` |
| `showassist` | `showAssistFromMenu` |
| `cloud_backup` | `cloudBackup` |
| `action_export_database` | `exportDatabase` |
| `action_import_db` | `restoreDatabase` |
| `action_export_csv_sidiary` | `exportCSVasSiDiary` |
| `importsettings` | `settingsSDcardExport` |
| `showmap` | `showMapFromMenu` |
| `parakeetsetup` | `parakeetSetupMode` |
| `action_resend_last_bg` | `resendGlucoseToWatch` |
| `action_open_watch_settings` | `openSettingsOnWatch` |
| `action_sync_watch_db` | `resetWearDb` |
| `deleteAllBG` | `deleteAllBG` |
| `testFeature` | `testFeature` |
| `crowdtranslate` | `crowdTranslate` |
| `userEvents` | `viewEventLog` |
| `libreLastMinutes` | `ShowLibreTrend` |
| `sharemyconfig` | `shareMyConfig` |
| `sendbackfill` | `doBackFillBroadcast` |
| `checkforupdate` | `checkForUpdate` |
| `sendfeedback` | `sendFeedback` |
| `action_toggle_speakreadings` | `toggleSpeakReadings` |

> Note: `synctreatments` intentionally has no `android:onClick` (feature disabled).

---

## Step 7 — Fix R8 Shrinking Crashes (proguard-rules.pro)

**File:** `app/proguard-rules.pro`

The switch to `proguard-android-optimize.txt` enables more aggressive R8 shrinking. Three categories of code are silently removed because they are only accessed via reflection.

Append all of the following to the end of `proguard-rules.pro`:

```proguard
# Keep TypeToken and all anonymous subclasses (e.g. new TypeToken<List<X>>(){}).
# R8 class-merging strips the Signature attribute from these anonymous classes even when
# -keepattributes Signature is set, causing TypeToken.getSuperclassTypeParameter() to throw
# "Missing type parameter" at static-initializer time (ExceptionInInitializerError on launch).
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Keep Activity methods called via android:onClick in menu XML (reflection-based dispatch).
# Use *** (any return type) not void — showSearch in Preferences returns boolean, not void.
# Without this, R8 removes them since no direct Java call site exists.
-keepclassmembers public class * extends android.app.Activity {
    public *** *(android.view.MenuItem);
}

# Keep Home inner classes used for Gson JSON deserialization of the voice lexicon.
# Without this, R8 removes them and Gson falls back to LinkedTreeMap, causing a ClassCastException
# in Home.classifyWord() when processing voice input.
-keep class com.eveningoutpost.dexdrip.Home$wordData { *; }
-keep class com.eveningoutpost.dexdrip.Home$wordDataWrapper { *; }
```

**Root cause detail:**
- `-dontobfuscate` prevents *renaming* but NOT *shrinking*. R8 still removes code it considers unreachable.
- `showHelpFromMenu` and 22 other menu handlers are only called via `SupportMenuInflater` reflection — R8 can't trace this.
- `showSearch` in `Preferences` returns `boolean` — a `void`-only keep rule misses it.
- `wordData` / `wordDataWrapper` are inner classes used only by `Gson.fromJson()` reflection.
- Anonymous `TypeToken<T>` subclasses lose their generic `Signature` attribute via R8 class-merging, breaking `TypeToken.getType()`.

---

## Step 8 — AndroidManifest.xml Cleanup

**File:** `app/src/main/AndroidManifest.xml`

Two cleanup changes for AGP 9.0.1 / AndroidX migration:

1. Remove the legacy support library `appComponentFactory` override:
```xml
<!-- Remove these attributes from <application> -->
tools:replace="android:appComponentFactory"
android:appComponentFactory="android.support.v4.app.CoreComponentFactory"
```

2. Suppress the `androidx.startup` InitializationProvider (optional, avoids startup warnings):
```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="remove" />
```

---

## Step 9 — Fix Crash in BgReading.find_slope() (BgReading.java)

**File:** `app/src/main/java/com/eveningoutpost/dexdrip/models/BgReading.java` line 1731

**Crash:** `IndexOutOfBoundsException: Index 0 out of bounds for length 0` in `find_slope()`, triggered by `WixelReader` on first run when the database is empty.

**Root cause:** The `assert` at line 1731 calls `last_2.get(0)` before the null/size guard on line 1734. This was safe with `proguard-android.txt` (which includes `-dontoptimize`, leaving assert bytecode as dead code), but `proguard-android-optimize.txt` keeps the assert branch live in debuggable builds, so `get(0)` executes on the empty list.

```java
// Before — crashes when database is empty on first run
assert last_2.get(0).uuid.equals(this.uuid)
        : "Invariant condition not fulfilled: calculating slope and current reading wasn't saved before";

// After — guard with isEmpty() so the invariant only fires when data is present
assert last_2 == null || last_2.isEmpty() || last_2.get(0).uuid.equals(this.uuid)
        : "Invariant condition not fulfilled: calculating slope and current reading wasn't saved before";
```

---

## Step 10 — New Feature: Snooze All Alerts Button

Adds a bell-off icon button below the "Add note" button in the Home screen top-right column.
- **Tap**: snoozes all alerts using the active alert's default duration (falls back to 30 min), shows a toast.
- **Long-press**: opens `SnoozeActivity` with the full duration picker.

### 10a — New vector drawable

**Create:** `app/src/main/res/drawable/ic_bell_snooze_grey600_24dp.xml`

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:height="24dp" android:viewportHeight="24.0" android:viewportWidth="24.0" android:width="24dp">
    <path android:fillColor="#757575" android:pathData="M20,18.69L4.08,2.77 2.81,4.05l2.36,2.36C4.42,7.28 4,8.58 4,10v7l-2,2v1h17.73l1.41,1.41 1.27,-1.27 -2.41,-2.45z"/>
    <path android:fillColor="#757575" android:pathData="M18,17H8.27L18,7.27z"/>
    <path android:fillColor="#757575" android:pathData="M12,22c1.1,0 2,-0.9 2,-2h-4C10,21.1 10.9,22 12,22z"/>
    <path android:fillColor="#757575" android:pathData="M6,14.27V10c0,-2.42 1.36,-4.5 3.5,-5.45L18,13.09V10c0,-3.07 -1.63,-5.64 -4.5,-6.32V2c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v1.68C7.63,4.36 6,6.92 6,10v4.27z"/>
</vector>
```

### 10b — String resources

**File:** `app/src/main/res/values/strings.xml` — add near the existing `snooze` strings:

```xml
<string name="snooze_all_alerts">Snooze all alerts</string>
<string name="all_alerts_snoozed">All alerts snoozed for %s</string>
```

### 10c — Layout

**File:** `app/src/main/res/layout/activity_home.xml`

Insert `btnSnooze` between `btnNote` and `btnUndo`, and update `btnUndo`'s `layout_below`:

```xml
<!-- Insert after btnNote's closing /> -->
<ImageButton
    android:id="@+id/btnSnooze"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_alignEnd="@+id/btnNote"
    android:layout_below="@+id/btnNote"
    android:layout_marginTop="4dp"
    android:background="@android:color/transparent"
    android:contentDescription="@string/snooze_all_alerts"
    android:gravity="right"
    android:paddingBottom="2dp"
    android:paddingLeft="2dp"
    android:paddingRight="6dp"
    android:paddingTop="2dp"
    android:src="@drawable/ic_bell_snooze_grey600_24dp" />

<!-- Update btnUndo: change layout_below from btnNote → btnSnooze -->
android:layout_below="@+id/btnSnooze"
```

### 10d — Home.java

**File:** `app/src/main/java/com/eveningoutpost/dexdrip/Home.java`

Add field near the other `ImageButton` fields (~line 252):
```java
private ImageButton btnSnooze;
```

Wire up in `onCreate` after the `btnNote` block:
```java
this.btnSnooze = (ImageButton) findViewById(R.id.btnSnooze);
btnSnooze.setOnClickListener(v -> snoozeAllAlerts());
btnSnooze.setOnLongClickListener(v -> {
    startActivity(new Intent(Home.this, SnoozeActivity.class));
    return true;
});
```

Add private method (near other action methods, e.g. after `doBackFillBroadcast`):
```java
private void snoozeAllAlerts() {
    final int minutes = AlertPlayer.getPlayer().GuessDefaultSnoozeTime();
    SnoozeActivity.snoozeForType(minutes, SnoozeActivity.SnoozeType.ALL_ALERTS,
            PreferenceManager.getDefaultSharedPreferences(this));
    AlertPlayer.getPlayer().Snooze(this, minutes);
    JoH.static_toast_short(String.format(gs(R.string.all_alerts_snoozed),
            SnoozeActivity.getNameFromTime(minutes)));
}
```

> All required imports (`PreferenceManager`, `AlertPlayer`) are already present in `Home.java`. `SnoozeActivity` is in the same package and needs no import.

---

## Verification

```bash
# 1. Clean build
./gradlew assembleFastDebug
# Expected: BUILD SUCCESSFUL, Task :app:compileFastDebugKotlin present

# 2. Install on device/emulator
adb install -r app/build/outputs/apk/fast/debug/app-fast-debug.apk

# 3. Launch the app
adb shell am start -n com.eveningoutpost.dexdrip/.Home

# 4. Check no crash on open
adb logcat -d -b crash

# 5. Verify overflow menu works (tap ⋮ → "Check for updated version")

# 6. Navigate to Settings via nav drawer — should open without crash

# 7. Verify snooze button appears below "Add note" in top-right column
#    Tap  → toast "All alerts snoozed for 30 min"
#    Long-press → SnoozeActivity opens

# 8. Confirm crash buffer is empty throughout
adb logcat -d -b crash
```

---

## Files Changed Summary

| File | Change | Reason |
|---|---|---|
| `build.gradle` | AGP 7.4.2 → 9.0.1, GMS 4.3.10 → 4.4.2 | AGP upgrade |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 7.5 → 9.1.0 | Required by AGP 9.0.1 |
| `settings.gradle` | Comment out `:libkeks`, `:wear` | Lombok incompatibility, not needed |
| `app/build.gradle` | DSL migration (lint, packaging, buildFeatures, androidComponents), remove kotlin-android plugin, namespace | AGP 9.0.1 API changes |
| `app/proguard-rules.pro` | Add 4 keep rules (TypeToken, Activity menu handlers, Gson inner classes) | R8 shrinking crashes |
| `app/src/main/res/menu/menu_home.xml` | Restore 22 `android:onClick` attributes | Menu items were non-functional |
| `app/src/main/AndroidManifest.xml` | Remove legacy `appComponentFactory`, add startup provider removal | AndroidX migration cleanup |
| `app/src/main/java/.../plugin/Loader.java` | `throw` → `return null` for keks case | G5/G6 service crash |
| `app/src/main/java/.../models/BgReading.java` | Guard assert with `isEmpty()` check | First-run crash in `find_slope()` |
| `app/src/main/res/drawable/ic_bell_snooze_grey600_24dp.xml` | New file — vector drawable | Snooze button icon |
| `app/src/main/res/values/strings.xml` | Add `snooze_all_alerts`, `all_alerts_snoozed` | Snooze button strings |
| `app/src/main/res/layout/activity_home.xml` | Add `btnSnooze`, update `btnUndo` anchor | Snooze button layout |
| `app/src/main/java/.../Home.java` | Add `btnSnooze` field, wiring, `snoozeAllAlerts()` | Snooze button logic |
