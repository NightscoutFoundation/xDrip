# xDrip Development Guide
The following steps describe how to setup a development environment required to build xDrip.
- Fork https://github.com/NightscoutFoundation/xDrip in GitHub.
- Download, install and launch the latest Android Studio from https://developer.android.com/studio.
- In the startup menu, click `Get from VCS`.
- Select GitHub in the left panel and then click `Log In via GitHub...`.
- Click `Authorize in GitHub` on the newly opened page.
- You should now see a list of your GitHub project, including newly forked `xDrip`.
Select it and click `Clone`.
- You will probably see `Unsupported Java` error. We need to use an older JDK version
to be able to build xDrip:
    - Open following menu: *File* -> *Settings* -> *Build, Execution, Deployment* -> *Build Tools* -> *Gradle*.
    - In `Gradle JDK` dropdown select `Download JDK...`.
    - In `Version:` select `11`, leave the remaining options unchanged.
    - Click `Download`, click `Apply`.
    - Close the settings window.
    - Restart Android Studio.

You should now be able to build xDrip and run it in an emulator or deploy to a physical device.