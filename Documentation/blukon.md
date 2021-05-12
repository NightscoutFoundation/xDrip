# Required:

* xDrip+ installed
* PIN code from Blucon
* Blucon placed on top of Libre Sensor


# Howto:

* Unpair Blucon (from any devices where you could have already paired your Blukon with) 
via Settings -> Connected Devices -> Bluetooth -> BLU00XXX -> Forget
* Download and install latest version from https://github.com/NightscoutFoundation/xDrip/releases
* Install xDrip+ and Open App
* Accept all app prompts
* In the top left "burger" menu:
  * ```settings -> Hardware Data source -> Libre Bluetooth```
  * ```settings -> Less Common Settings```
    * ```Advanced calibration```
     * ```Calibration plugin -> datricsae```
     * Select ```Use Plugin Glucose```
     * if you want [EXPERIMETAL] ```Non-fixed Libre slopes```
   * ```Bluetooth settings``` select *ONLY*:
     * ```Turn Bluetooth ON```
     * ```Bluetooth watchdog```
     * ```timer for BT watchdog``` = 13min
     * ```Trust Auto-connect```
     * ```Always discover services``` 
   * Enable ```Agressive servive restarts```
   * Enable ```Display Bridge Battery```
   * ```Other misc options```:
     * ```Retrieve Libre history```
   * ```settings -> Alarms and Alerts```
     * ```Missed reading alert```
       * 8 minutes
       * 5 minutes before raising
* Make sure that Blucon is on top of sensor
* Press reset on blucon
* In the top left "burger" menu, open Bluetooth Scan and Scan for BLU00XXX device
* Once found select it and enter PIN (PIN can be found on the side of the Blucon device)
* xDrip+ should ask to start sensor, if not select Start Sensor from top left "burger" menu.
* Select the time/date you attached the sensor when asked.
* Your Blucon should now be linked with your phone, you will now need to wait between 10 and 15 minutes.
* After 2 BG readings from Blucon, xDrip+ will ask you to enter one or two calibrated value (blood prick).
* Enter this now.


# If lots of connection issues:
* Under Bluetooth settings, try ```Allow blucon unbunding```
* look this page: [libre-fix-bt-connection-issues.md](./libre-fix-bt-connection-issues.md)


# Regarding battery:

* best is simply to exchange it together with the sensor (every 14 days)
* Low battery will be detected and we suggest then changing it as soon as possible. Detection is not perfect
* @keencave: I think the edge measured with a voltage meter is < 2.8x V. In my test setup I feeded the system with an adjustable power source down to 2,65 V before I see the battery low indication message. As any battery has an idle voltage and an internal resistance this will lead to a slightly higher voltage in real life. I guess that the peak current to drive the RF circuit is too low with voltages < ~2.8x Vs. My last 4 batteries all have had a voltage > 2.8x V after 14 days and I got no battery low indication at all. Seems that 2.8 V measured with a voltage meter is a good citeria to check wether an unknown battery can be used or not. Fresh ones should have >3.2x V.


# If really nothing works
You might have a hardware issue, please check this:
* is led flashing shortly when you push 2 times reset button?
 * If not maybe battery empty
 * Battery contacts need maybe to be bent a bit (be careful).
 * Or your device is maybe dead
  * need claim it.
  * did you try first with manufacturer App?


# If issues with Wear
* With wear 2.0 you need to uninstall app and reinstall it
* Enable Wear logs: ```Settings -> Smart Watch Features -> Android Wear Integration -> Send wear Logs``` 
* Send/check your logs


Credits to: @StoneISStephan and @gregorybel
