---
name: "Please do not open an issue unless asked to do so by a developer in a discussion (see below)"
about: Please include a link to the discussion in the first post.
label: status-triage
---

### Please don't open an issue if you haven't already consulted with the developers
- Please go back and use Discussions (on the previous page) to present your case if you haven't already.
- If you have consulted with the developers and were asked to open an issue, please proceed.
- Please also search the existing issues for similar problems before opening a new one.
- Please do not prefix you issue title with BUG: or LIBRE: for instance. The issue maintainer will tag every issue with appropriate labels.
- Add screenshots only if necessary, e.g. write the version number instead of adding a screenshot of the system status page.

### Subject of the issue
Link to the discussion thread:  
A clear and concise description of what the bug or request is.
Example: The BG graph shows negative data points between 3 and 5 am, which is incorrect and can only be an artefact.

### Your environment
- What version of xDrip, e.g. 2020.11.27 or 2011270800 or "Nightly build 27th Nov 2020". The version can be found at the system status page next to "Code:".
- What type of hardware data source and how do you get your data there (e.g. G6 or Libre with bluecon bridging device, Nightscout Follower, etc.)
- Is your issue specific to a device like and Android phone or a smartwatch? If yes, please provide the exact device name and type as well as the OS version (e.g. Samsung Galaxy S7 running Android 10.1).
- All settings related to the issue, e.g. activated auto calibration.

### Expected behavior
A clear and concise description of what you expected to happen.
Example: BG values are always positive and should be displayed in line with the previous and following values, regardless of the time of day.

### Actual behavior
A clear and concise description of what actual happens.
Example: After enabling automatic calibration and entering a fingerstick test value, all new BG values received from the G6 sensor are displayed below the BG chart. Clicking on a data point shows a negative value.

### Steps to reproduce the behavior:
For example:
1. Go to '...'
2. Enable setting '...'
3. Wait x minutes
4. Click on '...'

### Screenshots
If applicable, add screenshots to help explain your problem. Please do not add oversized screenshots!
