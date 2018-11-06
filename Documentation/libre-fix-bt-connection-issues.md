

# If you have a lot of missed readings:

You can other settings:

* Solution 1:
  * ```Trust Auto Connect``` to OFF
  * ```close GATT on disconnect``` to ON (only if Trust Auto connect is OFF)
* Solution 2:
  * Enable or disable ```Use scanning```
* Solution 3:
  * Enable or disable ```Always discover services```
  
# On each try, make sure to:
* reset your device
* reboot phone

# Provide more info that we can support you
* solution 1:
  * Enable enginering mode
    * Look here: https://github.com/NightscoutFoundation/xDrip/wiki/Engineering-Mode
  * Make a screenshot from system status / BT devices
* solution 2:
  * Enable more logs:
    * ```Settings -> less common settings -> Extra logging settings -> Extra tags for logging``` and add:
      * DexCollectionService:v
  * Reproduce issue
  * Send logs from within the App
