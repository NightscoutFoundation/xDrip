# Local Web Services

xDrip+ has the facility to operate a local web server. This feature is enabled within the Inter-App settings menu.

A web server will respond to requests on address `127.0.0.1` port `17580`

### Nightscout Pebble endpoint

`/pebble` emulates the same URL on Nightscout, so with a watchface which supports this you can enter the address on the watchface, for example, Set the data endpoint on Pebble Nightscout watchface to:

    http://127.0.0.1:17580/pebble


### sgv.json endpoint

`/sgv.json` emulates `/api/v1/entries/sgv.json?count=24` on Nightscout

    http://127.0.0.1:17580/sgv.json

You can also access the steps endpoint by appending a query parameter

    http://127.0.0.1:17580/sgv.json?steps=1234

When doing this look for `steps_result` number in the first record of the json reply, 200 indicates success, anything else failure to set the steps value. Steps value should be the current cummulative step counter from the device.

### Tasker endpoint

The `/tasker` endpoint lets you push requests to the tasker interface via http. So a watch face which can only support web based endpoints (eg FitBit) could send a snooze request as below:

    http://127.0.0.1:17580/tasker/SNOOZE

### Steps endpoint

`/steps` allows for setting the current step counter data

    http://127.0.0.1:17580/steps/set/1234

This should be the current cumulative step counter on the device measuring steps. Not a historical record.

---

#### SSL option

The service also listens on port `17581` for https protocol connections. The certificate presented is self-signed for `127.0.0.1`
In its current implementation I wouldn't expect this to be that useful.

---

#### Implementation

Code relating to this feature is in the `webservices` package folder

