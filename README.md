# ATAK Packet Forwarder 

An ~~application/service~~ ATAK plugin for forwarding CoT messages via a hardware layer. Currently supports GoTenna Mesh.

![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/images/plugin-disconnected-indicator.png "Plugin Disconnected Indicator")
<br>
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/images/group-management.png "Group Management")
<br>
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/images/direct-messaging.png "Direct Messaging")
<br>
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/images/transmit-map-markers.png "Transmit Map Markers")

# Features

* Peer discovery
* In-app group and device management
* Broadcast messages are sent to the group (e.g. map markers, PLI)
* Direct messages to other users (e.g. chat messages)
* Efficient comm. using protobufs -- can send approx 5 map markers or PLI per minute, or 2 chats, or 2.5 more complex markers
* Typical msg sizes, PLI: ~190 bytes, simple shape ~200 bytes, complex shape ~250 bytes, ~380 bytes, group chat ~400 bytes
* Abstracted communication for adapting to other physical layers
* Filtering of repeated messages with a configurable TTL (e.g. auto-send markers)
* Message queue with priority (chat = pli > markers)

# To Do

* Get this working with Release builds of ATAK
* Message IDs and receipt confirmation
* Improve chat message shrinking further
* Figure out why some messages fail to parse (1/8 msgs)
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

# Architecture Diagram

![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/images/arch-diagram.png "Architecture Diagram")

# Notes on Message Handling

Message handling follows a few simple rules:

- Messages from ATAK that are not chat are checked against a Recently Sent cache, if a message was recently sent it was dropped. This prevents spamming of auto-send map markers.
- Messages are then queued in a prioritized queue, with the priority: chat = pli > marker
- If a similar message already exists in the queue (e.g. PLI) it will be overwritten with the new message, this way a queued PLI won't be sent with out of date data if newer data is available
- This compares Lat/Lon exactly so device GPS imprecision will probably cause PLIs to get queued up, either way there should never be more than 1 PLI in the queue
- Messages are fetched from this queue by the CommHardware class and sent

- The plugin will attempt to first use a "minimal" protobuf that saves space, but if it will result in dropped fields or a failed mapping on the receiving size it will fall back to the regular protobufs
- When values appear more than once in a payload we attempt to replace subsequent appearances with a marker/placeholder that is swapped back for the value when rebuilding the original message

# Contributing

Areas I'd especially like help are: 

* reducing the message sizes without affecting features in ATAK (e.g. removing `detail.contact.endpoint` kills chat)
* increasing resilience of this plugin, it is basically fire-and-forget (and maybe lose your message) right now
* port this to other hardware -- i have some Meshtastic devices on the way but support for any other devices would be great

The hardware/communication layer is abstracted behind a `CommHardware` interface, this interface can be implemented against other hardware -- I am going to try Meshtastic or XBees next, but if you would like to give it a shot with something else please reach out to me and let me know how it goes.

