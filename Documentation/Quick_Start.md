# Quick Start

### Currently Recommended IDE

Android Studio Giraffe | 2022.3.1 Patch 3


### Lombok Plugin

Until we are using Android Studio Iguana (where the lombok regression is believed to be corrected) you will need to manually install the lombok plugin jar from the etc source tree folder.

To install it you go to Settings -> Plugins -> Cog icon -> Install plugin from disk -> select the lombok-giraffe.jar file

### Android Studio Bugs and Regressions

Be aware that each new version of Android Studio introduces more regressions and bugs. There are still uncorrected bugs which were introduced back in version 3.6! Also each IDE version is tightly coupled with various versions of gradle which are also tightly coupled with various versions of the Android framework tools.

What this means, is that even though the command line compilation always works fine, any single version of Android Studio is unable to compile the entire xDrip source tree history and is likely to break in misleading and confusing ways based on simple dependency changes as there is such poor support in Android Studio for forward or backwards compatibility.

Expect to have to run multiple different versions of Android Studio over time or where in the commit history you may be working from and for each new version to potentially introduce new and unpredictable issues. Expect to have to clear cache and restart to avoid being misled about the source of errors.