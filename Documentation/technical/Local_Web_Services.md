# Local Web Services

xDrip+ has the facility to operate a local web server. This feature is enabled within the Inter-App settings menu.

A web server will respond to requests on address `127.0.0.1` port `17580`

### Nightscout Pebble endpoint

`/pebble` emulates the same URL on Nightscout, so with a watchface which supports this you can enter the address on the watchface, for example, Set the data endpoint on Pebble Nightscout watchface to:

    http://127.0.0.1:17580/pebble


### sgv.json endpoint

`/sgv.json` emulates `/api/v1/entries/sgv.json?count=24` on Nightscout

    http://127.0.0.1:17580/sgv.json

### Tasker endpoint

The `/tasker` endpoint lets you push requests to the tasker interface via http. So a watch face which can only support web based endpoints (eg FitBit) could send a snooze request as below:

    http://127.0.0.1:17580/tasker/SNOOZE


#### Implementation

Code relating to this feature is in the `webservices` package folder

