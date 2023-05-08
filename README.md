# AircoreMediaSample
AircoreMediaSample is a sample app that gives an example of how to use the [Aircore Android Flex SDK](https://docs.aircore.io/SDK-download/flex-android-download).

### Running AndroidMediaSample
#### Requirements
1. [Android Studio](https://developer.android.com/studio) version *Electric Eel* (2022.1.1) or higher.
2. A device running [Android 13](https://developer.android.com/about/versions/13)
3. A USB cable to tether the device to the machine where you will run [Android Studio](https://developer.android.com/studio).
4. An **Aircore** *Publishable API Key*. Refer to the [documentation](https://docs.aircore.io/authentication) for an understanding of creating an app and API Keys

#### Preparing Your Device
1. [Enable Developer Options on your device](https://developer.android.com/studio/debug/dev-options#enable), note that this process may vary depending on your device and Android version
2. [Enable USB Debugging](https://developer.android.com/studio/debug/dev-options#Enable-debugging)

#### Building, Installing, and Running
1. Install [Android Studio](https://developer.android.com/studio) version *Electric Eel* (2022.1.1) or higher.
2. Start [Android Studio](https://developer.android.com/studio).
3. Select the device from the 'running devices' pulldown menu at the top of the screen (should be next to the configurations drop down menu). You should now see the device name at the top of the screen.
4. Select Open from the Project menu and select the path to the where you cloned this repository. You should see the AndroidMediaSample project is now selected.
5. Click the green hammer icon to build the sample application. If that is successful, click the triangle icon to run and install it on your device.
6. The sample application should now be running on your device.

# Using **AircoreMediaSample**
## SDK Version
The app always displays the version of Aircore Android Flex SDK that it is using.
## Joining a Channel
Users publish media to, and receive media from, from a _Channel_. To join a Channel, There is a field above the `Join Channel` button which needs to be completely filled out to successfully join.
Input an Aircore Publishable API Key/Publishable App Token, as well as the desired channel name and user ID then press the button.
Upon success, the channel `Joinstate` field below should update to `joining` then `joined`; upon an error, the `TerminationCause` below will reflect what went wrong.
## Publishing Audio
Once you have joined the channel, the `Publish` button will allow for the publication of a LocalStream, enabling other users in the channel to hear audio recorded.
This requires giving the application microphone permissions.
Mute and unmute of the publication is enabled after publication starts.
The `Vad Indicator` field below will light up with a green icon if voice activity is detected while publishing, and the `Connection State` and `Termination Cause` will reflect the state of the LocalStream.
## Playing Audio
*AircoreMediaSample* will play out audio from other users who are publishing into the same _Channel_ (i.e., using the same application and channel identifiers), these appear to users as _RemoteStreams_.
Below the publication section, any RemoteStreams in the channel will be listed with the `UserID` the remote publisher, the `Connection State` of the RemoteStream, and buttons to toggle the mute/unmute state of the RemoteStream, as well as to list the local and remote mute state of the stream.