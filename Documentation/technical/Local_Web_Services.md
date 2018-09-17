# Local Web Services

xDrip+ has the facility to operate a local web server. This feature is enabled within the Inter-App settings menu.

A web server will respond to requests on address `127.0.0.1` port `17580`

### Nightscout Pebble endpoint

`/pebble` emulates the same URL on Nightscout, so with a watchface which supports this you can enter the address on the watchface, for example, Set the data endpoint on Pebble Nightscout watchface to:

    http://127.0.0.1:17580/pebble


### sgv.json endpoint

`/sgv.json` emulates `/api/v1/entries/sgv.json?count=24` on Nightscout

    http://127.0.0.1:17580/sgv.json

The first record will contain a hint as to the units being used, `units_hint` will be `mgdl` or `mmol` to indicate the user's preference but the values will always be sent as mgdl

You can also access the steps endpoint by appending a query parameter

    http://127.0.0.1:17580/sgv.json?steps=1234

When doing this look for `steps_result` number in the first record of the json reply, 200 indicates success, anything else failure to set the steps value. Steps value should be the current cummulative step counter from the device.

You can also access the heart endpoint by appending a query parameter

    http://127.0.0.1:17580/sgv.json?heart=123

When doing this look for `heart_result` number in the first record of the json reply, 200 indicates success, anything else failure to set the heart bpm value. Heart value should be the current bpm now from the device.

You can combine both like a normal query string:

    http://127.0.0.1:17580/sgv.json?steps=1234&heart=123

There is another option `brief_mode=Y` which you can use to exclude some of the data fields from the results to reduce response size.

There is another option `no_empty=Y` which you can use so that an empty data set returns `""` instead of `"[]"`

There is another option `all_data=Y` which you can use to get data additionally from the previous sensor session.

You can also access the tasker endpoint by appending a query parameter

    http://127.0.0.1:17580/sgv.json?tasker=osnooze

look for `tasker_result` in the first line of the json reply to indicate success or failure.

`OSNOOZE` is opportunistic snooze which you can call even when there is no alert playing

`SNOOZE` is the traditional tasker snooze which will send snooze to followers and uses more cpu

### status.json endpoint

This implements a small subset of the data you might receive from Nightscouts status.json

    http://127.0.0.1:17580/status.json

Results look like:

    {"thresholds":{"bgHigh":9.4,"bgLow":3.9}}

High and low marks set within the app represented in the local units.

### Tasker endpoint

The `/tasker` endpoint lets you push requests to the tasker interface via http. So a watch face which can only support web based endpoints (eg FitBit) could send a snooze request as below:

    http://127.0.0.1:17580/tasker/SNOOZE

### Steps endpoint

`/steps` allows for setting the current step counter data

    http://127.0.0.1:17580/steps/set/1234

This should be the current cumulative step counter on the device measuring steps. Not a historical record.


### Heart endpoint

`/heart` allows for setting the current heart rate bpm data

    http://127.0.0.1:17580/heart/set/123/1

This should be the current cumulative rate bpm on the device measuring. Not a historical record. The first parameter is BPM and the second is accuracy. If unsure of accuracy parameter just set to 1.

---

#### SSL option

The service also listens on port `17581` for https protocol connections. The certificate presented is self-signed for `127.0.0.1`
In its current implementation I wouldn't expect this to be that useful.

---

### Open option

The service has an `Open Web Service` option. If this is enabled then connections can be made through any network interface instead of being restricted to the loopback on-device network only. Typically enabling this option exposes the wifi / lan / bluetooth pan connection of the device although it is possible that it could be exposed via cellular as well if the carrier supports public ip addressing.

Be very careful enabling this option as there are powerful features accessible, for example the tasker interface. It is best used in conjunction with the Authentication option described below.

---

### Authentication

If the `xDrip Web Service Secret` is set to anything other than an empty string (the default) then requests coming in via the open non-loopback networks will have to supply a http header `api-secret` which contains the SHA1 hash of the same secret password or their connection will be rejected. Rejection information is stored in the Event Log and returned via the http response. The result code 403 (forbidden) is set when a connection is rejected.

Additionally, if a client supplies the `api-secret` header, then even if the xDrip secret is not set then the request will be rejected. This feature is so that client devices can be assured they are connecting to the correct xDrip instance (by using different secrets). This could be significant when looping for example.

Authentication is not required on the loopback local network interface (127.0.0.1)

    api-secret: 915858afa2278f25527f192038108346164b47f2

Above shows http header for password `Abc`


---

#### Implementation

Code relating to this feature is in the `webservices` package folder

