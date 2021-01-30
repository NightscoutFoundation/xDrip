# BlueJay Tasker

The BlueJay xDrip interface allows for two-way interaction with Tasker via Android Broadcast Intents.

These can be used within Tasker or as a reference for implementation in other Android apps.

#### Prerequisites:

- Nightly version of xDrip
- `Enable Remote API` switched on in BlueJay advanced settings
- BlueJay X2 with firmware 2093 or above
- Tasker Android App


### Displaying Text on the BlueJay:

[Click this link on your mobile device to install the example task to display text](https://taskernet.com/shares/?user=AS35m8ke%2BaXfrfvOOvtrlCrJ9oEv8iYfsWFYUKYvLtaQW2lBAaoV7LJPj7fT0gOjcej%2BgG7Aux%2Bc&id=Task%3ABlueJay+Text)

Activating this task will display text on the BlueJay screen

### Displaying a multiple choice dialog:

[Click this link on your mobile device to install the example task to display a multiple choice dialog](https://taskernet.com/shares/?user=AS35m8ke%2BaXfrfvOOvtrlCrJ9oEv8iYfsWFYUKYvLtaQW2lBAaoV7LJPj7fT0gOjcej%2BgG7Aux%2Bc&id=Task%3ABlueJayDialog)

Activating this task will present a 4-way choice dialog. Select by tilting, long pres to confirm.

To cancel simply do nothing, the dialog will timeout.

Choice items in the parameter are delimited with the ^ character.

Confirmed selection will be sent to tasker.

### Displaying and setting a persistent multiple choice dialog:

[Click this link on your mobile device to install the example task to display a multiple choice dialog that is persistent](https://taskernet.com/shares/?user=AS35m8ke%2BaXfrfvOOvtrlCrJ9oEv8iYfsWFYUKYvLtaQW2lBAaoV7LJPj7fT0gOjcej%2BgG7Aux%2Bc&id=Task%3ABlueJayDialog+Persistent)

Activating this task will present a 4-way choice dialog. Select by tilting, long press to confirm.

Note the `S1` 5th parameter item which causes this to be saved in persistent slot 1.

To activate a persistently stored dialog on the watch, long-long press then release, tilt to select, long press to confirm.

Chosen item text will be delivered to Tasker as an event.

Persistent choice dialogs allow for arbitrary Tasker actions to be initiated from the watch.


### Reacting to incoming events:

[Click this link on your mobile device to install a profile to receive broadcast intents](https://taskernet.com/shares/?user=AS35m8ke%2BaXfrfvOOvtrlCrJ9oEv8iYfsWFYUKYvLtaQW2lBAaoV7LJPj7fT0gOjcej%2BgG7Aux%2Bc&id=Profile%3ABlueJay+Event+Incoming)

[Click this link on your mobile device to install an example event handler](https://taskernet.com/shares/?user=AS35m8ke%2BaXfrfvOOvtrlCrJ9oEv8iYfsWFYUKYvLtaQW2lBAaoV7LJPj7fT0gOjcej%2BgG7Aux%2Bc&id=Task%3ABlueJay+Event+Handle)

Install the above two items and modify the Option text and resulting actions in the event handler to your own needs.

If you are using choice dialogs you will probably want to disable the long press beep example but it can be useful during debugging.

### Erasing the persistent dialog:

[Click this link on your mobile device to install an example task to clear the persistent dialog](https://taskernet.com/shares/?user=AS35m8ke%2BaXfrfvOOvtrlCrJ9oEv8iYfsWFYUKYvLtaQW2lBAaoV7LJPj7fT0gOjcej%2BgG7Aux%2Bc&id=Task%3ABlueJayDialog+Erase+Persistent)

#### Troubleshooting:

If the events from the watch to Tasker seem unreliable then check the following two solutions:

- Go in to xDrip Inter-app settings and select `Identify Receiver` check that this includes:
`net.dinglisch.android.taskerm`

- Exit to Tasker home screen, then use the Android app show button and swipe Tasker off. It seems to sometimes have caching issues.