
# Integration with xDrip via Broadcast Intent

## Overview
This documentation provides guidance on how to send a broadcast intent from a third-party Android application to insert a glucose sensor record into the xDrip application. There are a few different broadcast receivers but this documents the Nightscout Emulation receiver just for glucose records.

## Sending a Broadcast Intent
To insert a glucose sensor record into xDrip, construct and send a broadcast intent with specific parameters as described below.

### Intent Action
Use the following intent action when constructing the intent:
```java
Intent intent = new Intent("com.eveningoutpost.dexdrip.NS_EMULATOR");
```

### Intent Package
Always address the broadcast to xDrip explicitly:
```java
intent.setPackage("com.eveningoutpost.dexdrip");
```
This is required. Manifest-declared receivers stopped receiving implicit broadcasts in Android 8. An implicit broadcast will appear to work, because xDrip also registers this receiver at runtime, but it stops being delivered whenever the xDrip process is not running, with no error on either side.

If your own app targets API 30 or later, package visibility rules apply as well and `setPackage` alone is not enough. Declare xDrip in your manifest, as a direct child of `<manifest>`:
```xml
<queries>
    <package android:name="com.eveningoutpost.dexdrip" />
</queries>
```

### Extra Parameters
Add extra parameters to the intent for the data collection and JSON payload containing the sensor readings.

- Collection: Indicate the type of data collection using the key `"collection"` with the value `"entries"`.
- Data: Provide the glucose sensor readings in a JSON array format with the key `"data"`.

### JSON Payload Structure
Construct the JSON payload with the glucose sensor readings as follows:
- Each reading should be a JSON object with the following attributes:
    - `"type"`: Set to `"sgv"` for sensor glucose value record.
    - `"date"`: The timestamp of the reading in milliseconds since epoch.
    - `"sgv"`: The glucose value in mg/dL.
    - `"direction"`: The rate of change of the glucose value, represented by a string such as `"SingleUp"`. Refer to `BgReading.slopefromName()` for possible values.

Send exactly one reading per broadcast. An array with more than one element is treated as a different payload and no glucose record is inserted.

Here is an example JSON payload with a single reading:
```java
final JSONArray sgv_array = new JSONArray();
final JSONObject sgv_object = new JSONObject();
sgv_object.put("type", "sgv");
sgv_object.put("date", 1526817691000.0);
sgv_object.put("sgv", 158);
sgv_object.put("direction", "SingleUp");
sgv_array.put(sgv_object);
intent.putExtra("data", sgv_array.toString());
```

A code example of this is in the unit test source tree in `NSEmulatorReceiverTest.bgReadingExampleBroadcast()`

### Sending the Intent
After constructing the intent, use `sendBroadcast()` to send it:
```java
context.sendBroadcast(intent);
```

## Testing the Integration
For testing purposes, make sure you set xDrip `Hardware Data Source` to `Inter-app broadcast` to enable the receiver. Releases made before that option was renamed list it as `640G / EverSense`.
