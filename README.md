# ATAK Packet Forwarder 

An ~~application/service~~ ATAK plugin for forwarding CoT messages via a hardware layer. Currently supports GoTenna Mesh.

![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/screenshots/plugin-integration.png "Plugin Integration")
<br>
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/screenshots/group-management.png "Group Management")
<br>
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/screenshots/direct-messaging.png "Direct Messaging")

# Features

* Peer discovery
* In-app group management
* Broadcast messages are sent to the group
* Direct messages to other users
* Abstracted communication for adapting to other physical layers

# To Do

* Persist a group once you join, persist user info for the group
* Improve peer discovery -- when peers receive a user broadcast they should send a reply to let them know they exist too
* Proper disconnect of GoTenna device when closing
* Repeated message filtering (e.g. stationary PLI updates, Markers set to Auto-Send)
* Message queuing for chat messages
* Better link / bandwidth management
* Reduced payload size
* Lat/Lon for GoTenna frequency configuration from live source instead of `Config.java`
* Better error handling

# Setup

To use this plugin you will need to build your own copy of ATAK-CIV, to do that first:

* clone the ATAK-CIV repo: https://github.com/deptofdefense/AndroidTacticalAssaultKit-CIV
* clone this repo into the same parent directory that you cloned the DoD's ATAK repo to (i.e. not into the `AndroidTacticalAssaultKit-CIV` directory)
* get the GoTenna SDK: https://github.com/gotenna/PublicSDK
* get the ATAK-CIV SDK: http://7n7.us/civtak4sdk
* follow the instructions for building ATAK in the ATAK repo's BULIDING.md file, load the application onto your devices using the `installCivDebug` Gradle task
    * Note: you will need to configure a signing key in the local.properties file, you must use the same signing configuration in the plugin's `app/build.gradle` file!
    * Note: instructions on getting this to work with `installCivRelease` will happen in the next few days, the key is to add your signing fingerprint to `AtakPluginRegistry.ACCEPTABLE_KEY_LIST`
* copy the GoTenna Public SDK into the `libs/` directory as `gotenna-public-sdk.aar`
* copy the ATAK SDK into the `libs/` directory as `main.jar`
* open this project in Android Studio
    * Edit `Config.java`, put your GoTenna SDK token in the `GOTENNA_SDK_TOKEN` variable
    * Set `FALLBACK_LATITUDE` and `FALLBACK_LATITUDE` to your approximate lat/lon, this is how the application determines which frequencies your GoTenna should use do DO NOT MISS THIS STEP!
    * Open ATAK, go to Settings -> My Callsign -> More -> Reporting Preferences and set:
    * Dynamic Reporting Rate Stationary (Unreliable) to 120
    * Minimum and Maximum (Unreliable) to 60

It will take quite a while for the first messages to be transmitted. It is easy to miss messages if there are many markers on the map. When a CoT message gets sent we break it up into 200 byte chunks and then send each chunk with a 10 second gap between them, any other CoT messages during that time will be ignored.

# Contributing

GoTenna Mesh doesn't have enough bandwidth to really work for this application, just sending a single position update takes ~1 min and isn't very reliable. It definitely won't work once you start drawing a bunch of bandits on the map.

The hardware/communication layer is abstracted behind a `CommHardware` interface, this interface can be implemented against other hardware -- I am going to try an XBee next, but if you would like to give it a shot with something else please reach out to me and let me know how it goes.

