# xDrip+ Wear Setup and Troubleshooting Guide
- [Enable xDrip+ Android Wear Integration](#enable-xdrip-android-wear-integration)
    - [Initial Wear Enablement Requests Location Permission](#initial-wear-enablement-requests-location-permission)
    - [Syncing Phone and Wear Preferences](#syncing-phone-and-wear-preferences)
    - [Syncing BGs and Wear Database](#syncing-bgs-and-wear-database)
    - [XDrip Prefs Overview](#xdrip-prefs-overview)
        - [XDrip BT Settings](#xdrip-bt-settings)
        - [XDrip Watchface Settings](#xdrip-watchface-settings)
        - [Watchface Tap Feature](#watchface-tap-feature)
        - [Battery Usage](#battery-usage)
- [Troubleshooting xDrip+ Wear](#troubleshooting-xdrip-wear)
    - [Confirm the following in Android Wear app on phone](#confirm-the-following-in-android-wear-app-on-phone)
    - [Confirm Wear Integration preferences are consistent on both phone and watch](#confirm-wear-integration-preferences-are-consistent-on-both-phone-and-watch)
    - [Confirm Collection Method is consistent on both phone and watch](#confirm-collection-method-is-consistent-on-both-phone-and-watch)
    - [Confirm Collector device exists under Watch Settings->Bluetooth -> Devices](#confirm-collector-device-exists-under-watch-settings-bt-devices)
    - [Confirm Calibration Preferences are consistent on both phone and watch](#confirm-calibration-preferences-are-consistent-on-both-phone-and-watch)
    - [Confirm Noise Preferences are consistent on both phone and watch](#confirm-noise-preferences-are-consistent-on-both-phone-and-watch)
    - [Confirm Collector runs on the Phone with Wear Integration](#confirm-collector-runs-on-the-phone-with-wear-integration)
    - [Confirm Collector runs on the Watch with Wear Integration](#confirm-collector-runs-on-the-watch-with-wear-integration)
    - [Debugging Android Wear](#debugging-android-wear)

##Enable xDrip+ Android Wear Integration
xDrip+ supports wear integration through its wear app.  The xDrip+ wear app is installed with the standard xDrip+ apk.  The latest release supports a standalone version which allows the wear app to communicate directly with the Bluetooth collector, whether it is a Dexcom G5, a Dexcom G4 + xBridge, or a Libre LimiTTer.

The wear standalone feature is enabled via the following xDrip+ Settings located under **Smart Watch Features**, and corresponding watch **XDrip Prefs** settings.

<img align="middle" src="./images/prefs-wear-sync.png" title="xDrip+ Wear Integration Preferences">

|Phone Settings                     | Watch Settings   | Description             |
| --------------------------------- | ---------------- | ------------------------|
| Android Wear Integration          | NA               | Enables Wear integration|
| Enable Wear Collection Service    | Enable Collector | Enables the BT Collector to run on the wear device when the smartphone is out-of-range.|
| Force Wear Collection Service     | Force Collector  | Forces the BT Collector to run on the wear device even when the smartphone is in-range.|
| Device running Collection Service | NA               | Read-only; Displays the wear device running the BT Collector.  This will be the watch display name + uuid when Force Wear is enabled. |
| NA                                | BT Collector     | Read-only; Corresponds to xDrip+ Setting -> **Hardware Data Source**.  For example, if the **Hardware Data Source** is set to **G5 Transmitter (test)**, then the wear app BT Collector will display **DexcomG5**. |
| Sync Wear Logs                    | NA               | Triggers Wear log entries to be synced to the phone. You can view the logs on your phone using xDrip+ app's upper, right menu item, **View Events Log**. You need to enable ExtraLogTags in xDrip+ Settings -> LCS -> Extra Logging Settings -> **Extra tags for logging** to enable log generation for services s.a. **G5CollectionService:v**.|
| Wear Log Prefix                   | NA               | The wear event logs are prefixed with the **Wear Log Prefix**.  If you tap it, you'll see that it defaults to **wear** but you can enter something else, or nothing. This prefix helps to distinguish where the log entry was generated since services are similarly named on both devices. For example, **wearG5CollectionService** indicates the log was generated on the wear device.|

These settings are listed in order of dependency.  Note, the Watch's **XDrip BT Settings** will only be visible when Wear Integration is enabled on the phone.

* **Enable Wear**

  If you ONLY want to use the watch in standalone mode when the phone is out-of-range, then just select the Enable Wear checkbox. This simply enables the BT collection service on the watch to run only when the phone is out-of-range. Upon reconnecting to the phone, the watch will stop its BT collection service and send its BG readings. The phone will sync the received readings and startup its own BT collection service. There may be a delay, perhaps up to 14 minutes, on some smartphones (eg., if the smartphone has poor BT connectivity) to get an initial bg reading after it is switched back to the phone.
* **Force Wear**

  Enabling **Force Wear** will cause xDrip+ to use the watch BT collection service.  The watch BT collection service will sync each bg reading with the phone as readings are received from the collector, or upon reconnecting to the phone.  **Force Wear** for everyday use has the advantage of offloading some of the proccessing required on the smartphone to the watch, thus saving battery and CPU usage on the smartphone.

  However, this offloading means the watch battery may not last as long as when not using Force Wear.  As an example, some users find that the Sony Smartwatch 3 (SW3) can use 20%+ overnight (7-8 hrs) when unplugged and running G5 collector and always-on screen.

  Force Wear may also provide better BT connectivity over that provided by the smartphone.  As an example, some users find that the SW3 provides better BT connectivity than their Samsung Galaxy Note 4 smartphone.

###Initial Wear Enablement Requests Location Permission
Upon initial enablement of standalone wear, by selecting the the Enable Wear preference on watch or phone, a **Location Permission is Required** dialog box will be displayed.  Android Wear requires **Location Access** to be manually accepted by the user, therefore, the user must accept the Location Permission query in order for standalone mode to work.  Alternatively, the user can enable Location Permission in Watch -> Settings -> Permissions - XDrip Prefs, then enable Location.

<img align="middle" src="./images/prefs-wear-permissions.png" title="xDrip+ Wear Integration Preferences">

###Syncing Phone and Wear Preferences
Note, xDrip+ and Wear will sync their co-existing preferences.  Preference syncing takes the following precedence on connection:

  1. xDrip+ app startup.  xDrip+ will send its preferences to the watch and the watch will update its values to the phone.
  2. On re-connect, the wear app will send its preferences to the phone and phone will update its values to the watch.

For example, if the user changes the Force Wear preference on the watch, it will immediately be sent to the phone upon re-connection, and the phone will update its settings.

###Syncing BGs and Wear Database
* Sync DB - The watch data (BGs, Logs)is saved in the watch database.  The watch will attempt to sync its data with the phone upon connection until all delta data have been synced. So, for example, if you have 8 hours of overnight data generated while disconnected from the phone, the watch will attempt to send all data upon re-connection with the phone.
* Reset Wear DB - The watch data exists on the phone until you:

  1. **Reset Wear DB** on the phone via the xDrip+ upper right menu.  This removes data already synced with the phone.
  2. **Reset Wear DB** is auto-executed on a daily basis at 4 am.
  3. The app is uninstalled.
* UserError Table - Similar to the xDrip+ phone app, UserError log messages are saved in the watch UserError table.  To access the watch log entries on the phone, enable the **Sync Wear Logs** preference shown in the above image.  The log entries will be prefixed with the **Wear Log Prefix**, which defaults to **wear**, but is user-configurable.  This allows users to identify which device generated the log entry.  The log entries can be viewed using the follwoing options:
  - Users can view log messages on the phone via the xDrip+ upper right menu item, **View Events Log**.
  - As with the xDrip+ phone app, specific log entries can be enabled by entering the extra log tag and severity level preference via the xDrip+ phone app settings, Less Common Settings (LCS) - Extra Logging Settings - **Extra tags for logging**.

The following image shows an example of the phone **View Events Log** containing phone and watch log entries.

<img align="middle" src="./images/prefs-wear-vieweventslog.png" title="xDrip+ Wear Integration Preferences">

###XDrip Prefs Overview
The watch XDrip Prefs app is used to set the xDrip+ wear app preferences.  In addition to the Wear Integration preferences mentioned above under [Enable xDrip+ Android Wear Integration](#enable-xdrip-android-wear-integration), XDrip Prefs provide the following new preferences used in the standalone version.

####XDrip BT Settings

  Provides the Wear Integration preferences listed and the following:
  - XDrip G5 Settings

    Wear provides G5 BT settings similar to those provided by the xDrip+ app, such as **Scan for G5 Contantly**, under **G5 Debug Settings**.  As with the xDrip+ app, they should only be enabled if the watch has connectivity issues.

    For example, many users find that the **Sony Smartwatch 3 (SW3)** does not require any of these settings enabled.
    But some SW3 users find enabling **Scan for G5** helpful.

    Whereas, users of the **Moto 360 2nd Gen** watch report the **Unbond G5/Read** pref is required.

    There are the following two exceptions:
    - **Force Screen On** - Some watches, such as the **Moto 360 2nd Gen**, fall into deep sleep preventing the BT Collector from retrieving the transmitter data.  Enabling this preference will trigger the watch to wakeup to read the transmitter data, then fall back to sleep.

        Rather than use this preferences, it is recommended that watches should enable **Screen Always-on** on their watch or Android Wear app, when supported.  The **Moto 360 2nd Gen** is currently the only known watch that does not support Screen Always-on.
    - **Auth G5/Read** - This should be enabled if using the latest, Dexcom G5 transmitter firmware (released in November 2016, **firmware 1.0.4.10**, or newer than **firmware 1.0.0.17**).

  - Alerts

    Alerts can be enabled on the watch when in standalone mode (i.e., when Force Wear is enabled) by enabling Enable Alerts and High Alerts on the watch.  This will allow alerts to be triggered when disconnected from the watch.  Currently the following xDrip+ app alerts under **Alarms and Alerts** are supported on the watch:

    1. **Alert List** - All alerts.
    2. **Other Alerts** - **Bg falling fast** and **Bg risng fast**.
    3. **Extra Alerts (xDrip+)** - **Persistent High Alert**.

    Watch alerts have the following restrictions:

     1. Currently only support vibrate-only profiles.  Audio is not currently supported.
     2. Glucose Alerts Settings are currently not configurable.  They use app defaults.

The following image shows xDrip+ app alerts under **Alarms and Alerts** which are supported on the watch.

<img align="middle" src="./images/prefs-alerts-phone.png" title="xDrip+ Wear Integration Preferences">

The following image show example alerts on the watch.  Users will continue to receive those phone alerts which are not supported on the watch.  Phone and watch alerts can be distinguished by their **Open** dialog.  The phone alert will display **Open on phone**.  Whereas the watch alert will display **Open**.  Upon tapping Open, the Snooze dialog will be displayed.  The watch Snooze performs the same functionality that the phone Snooze performs.  Tapping the Snooze and number from the NumberPicker will snooze the alert on both the phone and the watch.  Whereas, the Snooze buttons will only snooze the alarms on the active device while watch notifications are enabled.

<img align="middle" src="./images/prefs-alerts-phone.png" title="xDrip+ Wear Integration Preferences">

####XDrip Watchface Settings
  - Show Status - Show Loop Status on the XDrip and XDrip(Large) watchfaces.  This will display the HAPP status message containing Basal%, IOB, COB.
  - Opaque Card - Show notifications cards with opaque background.  This will allow cards to be read more easily in ambient mode.
  - Small Font - Fontsize of small text in status and delta time fields on the XDrip and XDrip(Large) watchfaces.
  - Show Bridge Battery - Show bridge battery usage on the XDrip and XDrip(Large) watchfaces.  This setting will only be displayed when the BT Collector uses a battery, for example, LimiTTer or Wixel/xBridge.

####Watchface Tap Feature
Watchface tap feature is now implemented for the following preferences:
* Chart Timeframe - double tap on the chart in any of the watchfaces will toggle the chart timeframe allowing one to zoom in/out of a frame.
* Small Font - double tap on the status line or the delta time in the XDrip or XDrip(Large) watchface will toggle the fontsize allowing one to toggle size of the text for ease of viewing.

####Battery Usage
The wear app supports the display of two battery usage options:
* Bridge - displays the wixel or LimiTTer battery usage.  The Show Bridge Battery must be enabled to display the bridge battery usage.
* Uploader or Wear - will display the battery usage of the device running the collection service.  So, if Enable Wear and Force Wear prefs are enabled, it will display the **watch** battery usage.  If only Enable Wear is enabled, then it will display the battery usage of whichever device is actually running the collection service.  If neither prefs are enabled, it displays the phone's battery usage.  The label, **Uploader** or **Wear** corresponds to the device running the collector.  **Uploader** for phone which is the default, and **Wear** for the watch.  This will allow users to identify which device is running the collection service.

##Troubleshooting xDrip Wear
The BT Collector connects to the transmitter every 5 mins by design. This is how the Collector's BLE works. The following provides some troubleshooting suggestions if readings are not being receiving every 5 minutes.

###Confirm the following in Android Wear app on phone
- Watch is connected.
- Watch Settings always-on screen is enabled.  This will prevent watch doze mode from shutting down the BT Collector.

  To verify devices are connected, check the phone Android Wear app.  Android wear (on the watch) displays the **cloud** icon if the devices are not in-range, or if the user manually disconnects the devices in Android Wear.

	Similarly, some users have found it necessary to enable the **Stay awake while charging** setting under their watch Settings **Developer Options**.  In testing thus far, only the Moto 360 2nd Gen watch has required this option.
###Confirm Wear Integration preferences are consistent on both phone and watch

  **Enable Wear** and  **Force Wear** should have same settings on phone and watch.  If not, reset them accordingly.  The xDrip+ should sync these values whenever the user modifies them or at application startup, but both phone and watch must be connected and in-range for syncing to be performed.  See **Confirm phone and watch are connected** above.

###Confirm Collection Method is consistent on both phone and watch

  Confirm the phone's Harware Data Source preference matches the watch's BT Collector preference.  The watch's BT Collector preference is a read-only preference.  It gets set based on the phone's Hardware Data Source preference. The following values correspond to the collectors:
   - BluetoothWixel("BluetoothWixel"),
   - DexcomShare("DexcomShare"),
   - DexbridgeWixel("DexbridgeWixel"),
   - LimiTTer("LimiTTer"),
   - WifiWixel("WifiWixel"),
   - DexcomG5("DexcomG5"),
   - WifiDexBridgeWixel("WifiDexbridgeWixel"),
   - LibreAlarm("LibreAlarm")

###Confirm Collector device exists under Watch Settings BT Devices

  Once the BT Collection Service executes it will perform a BT scan, and upon detecting the BT Collector device, will populated the Watch Settings under Bluetooth Devices.  Typically it will show as disconnected as it only connects briefly to receive the BG reading.

###Confirm Calibration Preferences are consistent on both phone and watch

  The watch app does not yet support Calibration Plugins.  Therefore, to confirm BG readings are consistently calculated on both phone and watch, it is best to turn off Calibration Plugins on the phone.
  - LCS - **Advanced Calibration** - all should be off, including **Adrian calibration mode**.

###Confirm Noise Preferences are consistent on both phone and watch

  The watch app does not yet support Noise smoothing.  Therefore, to confirm BG readings are consistently calculated on both phone and watch, it is best to turn off Noise Smoothing on the phone.

  - xDrip+ Display Settings - **Show Noise workings**

    When Show Noise Workings is enabled, **BG Original** and **BG Estimate** will display on the home screen.
    - BG Original should correspond to your watch value, and
    - BG Estimate should correspond to your phone value.

  - xDrip+ Display Settings - **Smooth Sensor Noise**

###Confirm Collector runs on the Phone with Wear Integration

  Ensure Wear Integration preferences are set as follows:
  - **Wear Integration** is enabled.
  - **Enable Wear** is selected.
  - **Force Wear** is **NOT** selected.

This will allow your phone to use the G5 collector as normal when both phone and watch are in-range. After receiving a reading on your phone, ensure it displays on your watch.

After you confirm that you are get a reading on your phone, enable **Force Wear**, either on the phone or watch XDrip Prefs.

This will force the watch to use its BT collector, and force the phone to stop its BT collector service discussed next.

###Confirm Collector runs on the Watch with Wear Integration
  Ensure Wear Integration preferences are set as follows on both phone and watch:
  - **Wear Integration** is enabled.
  - **Enable Wear** is selected.
  - **Force Wear** is selected.
  - **Device running Collection Service** corresponds to the watch display name + uuid.
  - XDrip Prefs **BT Collector** corresponds to phone's Hardware Data Source.

  Confirm Environment:
  - Disable engineering mode.
  - Disable Calibration plugin (incl. Adrian calibration mode).
  - Disable then re-enable Wear Integration.
  - Enable Force Wear.
  - Show raw values.
  - Smooth sensor noise off.
  - Reset Wear DB, restart Watch, restart phone.
  - Confirm phone and watch are connected via Android Wear.
  - Change Watchface to big chart and then back to standard xDrip.


#ADB DEBUG
###Debugging Android Wear
[Howto Enable Debugging](http://www.androidpolice.com/2014/07/05/how-to-android-wear-enable-debugging-take-screenshots-unlock-the-bootloader-and-root-the-lg-g-watch/)

1. Open Settings.
	1. Tap on Wear's watch face. This will take you to the voice prompt. Be sure to hit the watch face instead of a notification card.
	2. Wear will wait up to 3 seconds for you to say something, then it'll change to a scrollable list of native actions. You can speed this up by swiping up or tapping on the voice prompt.
	3. Scroll down and select Settings.
2. Open About.
3. Find Build number and tap on it 7 times. You're done when a toast popup appears with the message, "You are now a developer!"
4. Swipe right (to go back) to the Settings menu.
5. Open Developer options.
6. Find and set ADB debugging to Enabled.
7. You'll be asked if you're sure you want to enable. Tap the checkmark button to confirm.
8. [Optional] If you want to also turn on debugging over Bluetooth, Find and set Debug over Bluetooth to Enabled.

At the terminal, issue:

```
D:\Android\sdk\platform-tools>adb devices
List of devices attached
14502D1AF252D74 device
D:\Android\sdk\platform-tools>adb -s 14502D1AF252D74 logcat > wear.log
```
If you see **unauthorized** description of your device, ensure that ADB debugging is enabled on your watch under Developer Options.

Enter the following cmd to generate a logcat log, where -s arg is your watch device if you have more than one device connected to your computer, otherwise, omit the -s arg. You can retrieve the device id using cmd adb devices: 

```
D:\Android\sdk\platform-tools>adb devices
D:\Android\sdk\platform-tools>adb -s 14502D1AF252D74 logcat > wear.log
```