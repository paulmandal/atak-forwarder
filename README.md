# ATAK Packet Forwarder 

An ~~application/service~~ ATAK plugin for forwarding CoT messages via a hardware layer. Currently supports GoTenna Mesh.

![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/images/plugin-disconnected-indicator.png "Plugin Disconnected Indicator")
<br>
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/images/group-management.png "Group Management")
<br>
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/images/direct-messaging.png "Direct Messaging")
<br>
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/images/plugin-menu.png "Plugin Menu")

# Features

* Peer discovery
* In-app group and device management
* Broadcast messages are sent to the group (e.g. map markers, PLI)
* Direct messages to other users (e.g. chat messages)
* Efficient comm. using protobufs -- can send approx 2.5 map markers per minute or 1.5 markers and one PLI per
* Abstracted communication for adapting to other physical layers
* Filtering of repeated messages with a configurable TTL (e.g. auto-send markers)
* Message queue with priority (chat = pli > markers)

# To Do

* Get this working with Release builds of ATAK
* Figure out why some messages fail to parse (1/8 msgs)
* Modify CotComparar.areCotPointsEqual() to allow for some configurable imprecision in comparison so that PLI msgs don't pile up
* Lat/Lon for GoTenna frequency configuration from live source instead of `Config.java`
* Retry on disconnection from comm. device
* Smarter sending -- Map Markers get higher priority unless PLI has not gotten sent in ~5 minutes
* Smarter sending -- Send a list of map markers to group, other clients can reply with which marker they are missing, build up a list of missing markers if more than 1 person is missing send to group, otherwise send to individuals
* GoTennaCommHardware has gotten large, retry functionality should be broken into another class at least, possibly other changes
* Needs more real-world stability testing

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
    * (Optional for best performance) Open ATAK, go to Settings -> My Callsign -> More -> Reporting Preferences and set:
    * Dynamic Reporting Rate Stationary (Unreliable) to 120
    * Minimum and Maximum (Unreliable) to 60
    * If you skip these steps PLI will take up most of your message bandwidth, map markers will likely take a long time to propagate

# Architecture Diagram

![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/images/arch-diagram.png "Architecture Diagram")

# Notes on Message Handling

Message handling follows a few simple rules:

- Messages from ATAK that are not PLI or chat are checked against a Recently Sent cache, if a message was recently sent it was dropped. This prevents spamming of auto-send map markers.
- Messages are then queued in a prioritized queue, with the priority: chat = pli > marker
- If a similar message already exists in the queue (e.g. PLI) it will be overwritten with the new message, this way a queued PLI won't be sent with out of date data if newer data is available
- This compares Lat/Lon exactly so device GPS imprecision will probably cause PLIs to get queued up, either way there should never be more than 1 PLI in the queue
- Messages are fetched from this queue by the CommHardware class and sent

# Contributing

GoTenna Mesh doesn't have enough bandwidth to really work for this application, just sending a single position update takes ~1 min and isn't very reliable. It definitely won't work once you start drawing a bunch of bandits on the map.

The hardware/communication layer is abstracted behind a `CommHardware` interface, this interface can be implemented against other hardware -- I am going to try an XBee next, but if you would like to give it a shot with something else please reach out to me and let me know how it goes.

