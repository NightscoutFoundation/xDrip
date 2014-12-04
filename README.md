DexDrip
=======

Android Application that collects dexcom signals, allows calibrations, and (soon) uploads to a secure server

## Features that are complete
* Basic Notifications (above or below threshold)
* Graph of BG levels (with scroll, zoom, and day preview)
* Graph of Calibration points
* A resonable Calibration Model
* Basic slope OOB error handling
* Predictive Bg Levels and Predicive slope
* BLE listening and reconnecting on phone restart/app crash
* Initial sensor sensitivity offsets


## Features that proggress has started on
* Server Uploading
* Decay Modled Calibrations (a long ways off unfortunately)
* In app encryption/decryption(double seeded)
* Server Registration
* Server Sign in
* Re-authentication on Token expiration
* Cleaning up layouts for tablets landscape mode
* Calibration fading overconfidence (Might be a bit shady... not sure if I will continue with it)
* Sensor information screen (Running MARD, Battery levels, statisticy stuff!)

## Features on the table but not yet started
* Robust Notifications (when you should calibrate, rise rates/drop rate)
* Specify transmitter id in response to wixel
* GCM for pushing readings to multiple devices
* Smart slope OOB error handling
* Determine inteligent intercept bounding
* More sophisticated prediction algorithm
* Reports and Statistics
* expire older data once confirmed it has been uploaded

## Possible Issues
* There may be an issue with sensors that exhibit low sensitivity, A reduced lower slope bound may help
* A cutoff by calibration age may be beneficial
* Initial sensor sensitivity offset may be a bit much, consider lowering
* The way encryption will work there will be no recovering your data if you forget your password


### Thoughts or Questions?
* ask me questions in the issuse, I wont mind, I prommise!
* That thing Im doing looks dumb? Thats because I dont know anything about java, I did a lot of things to make it do what I want that Im sure are not correct
* Why do you not use camel casing all the time? Because I didnt know that was a thing until I was halfway into this project, I really dont know java
* So why are these activities and not fragments? Because I also didnt know those were things until I was halfway in
