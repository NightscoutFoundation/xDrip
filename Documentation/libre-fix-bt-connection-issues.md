# If you have a lot of missed readings:

You can try other settings:
* Solution 0:
  * Make sure xDrip is not being killed by OS
   * set xDrip into the "ignored battery optimization" list
   * If you use a Huawei phone:
     * lock app
     * allow xDrip to be launched at start
* Solution 1:
  * ```Trust Auto Connect``` to ON
  * ```close GATT on disconnect``` to OFF
* Solution 2:
  * ```Trust Auto Connect``` to OFF
  * ```close GATT on disconnect``` to ON (only if Trust Auto connect is OFF)
* Solution 3:
  * Enable or disable ```Use scanning```
* Solution 4:
  * Enable or disable ```Always discover services```
  
# On each try, make sure to:

* reset your device
* reboot phone
* use the latest xDrip app

# Provide more info so that we can support you:

* solution 1:
  * Enable engineering mode
    * Look here: https://github.com/NightscoutFoundation/xDrip/wiki/Engineering-Mode
  * Make a screenshot from system status / BT devices
* solution 2:
  * Enable more logs:
    * ```Settings -> less common settings -> Extra logging settings -> Extra tags for logging``` and add:
      * DexCollectionService:v
  * Reproduce issue
  * Send logs from within the App
   * Click 3 points on main screen
   * ```View event logs```
   * ```Upload logs```
