DexDrip
=======

Android Application that collects dexcom signals, allows calibrations, and (soon) uploads to a secure server

## Features that are complete
* Nightscout Uploading
* Basic Notifications (above or below threshold)
* Calibration Notifications
* Graph of BG levels (with scroll, zoom, and day preview)
* Graph of Calibration points
* A resonable Calibration Model
* OOB slope error handling
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
* Sensor information screen (Running MARD, Battery levels, statisticy stuff!)

## Features on the table but not yet started
* Robust Notifications (rise rates/drop rate)
* Specify transmitter id in response to wixel
* GCM for pushing readings to multiple devices
* Determine inteligent intercept bounding
* Reports and Statistics
* expire older data once confirmed it has been uploaded

## Possible Issues
* The way encryption will work there will be no recovering your data if you forget your password


### Thoughts or Questions?
* ask me questions in the issuse, I wont mind, I prommise!
* That thing Im doing looks dumb? Thats because I dont know anything about java, I did a lot of things to make it do what I want that Im sure are not correct
* Why do you not use camel casing all the time? Because I didnt know that was a thing until I was halfway into this project, I really dont know java
* So why are these activities and not fragments? Because I also didnt know those were things until I was halfway in


# LINKS
* [Project Site](http://stephenblackwasalreadytaken.github.io/DexDrip/)
* [What you will need & Diagrams](https://github.com/StephenBlackWasAlreadyTaken/DexDrip/blob/gh-pages/hardware_setup.md)
* [Wixel App](https://github.com/StephenBlackWasAlreadyTaken/wixel-DexDrip)
* [Android App](https://github.com/StephenBlackWasAlreadyTaken/DexDrip)
