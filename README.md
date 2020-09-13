# ATAK Packet Forwarder 

An ~~application/service~~ ATAK plugin for forwarding CoT messages via a hardware layer. Currently supports [Meshtastic](https://meshtastic.org) devices.

![Plugin Disconnected Indicator](https://github.com/paulmandal/atak-forwarder/raw/master/images/plugin-disconnected-indicator.png)
<br>
![Group Management](https://github.com/paulmandal/atak-forwarder/raw/master/images/group-management.png)
<br>
![Direct Messaging](https://github.com/paulmandal/atak-forwarder/raw/master/images/direct-messaging.png)
<br>
![Transmit Map Markers](https://github.com/paulmandal/atak-forwarder/raw/master/images/transmit-map-markers.png)

# Features

* Peer discovery
* In-app channel management
* Broadcast messages are sent to the channel (e.g. map markers, PLI)
* Direct messages to other users (e.g. chat messages)
* Efficient comm. using protobufs -- can send approx 5 map markers or PLI per minute, or 2 chats, or 2.5 more complex markers
* Typical msg sizes, PLI: ~190 bytes, simple shape ~200 bytes, complex shape ~250 bytes, ~380 bytes, group chat ~400 bytes
* Filtering of repeated messages with a configurable TTL (e.g. auto-send markers)
* Message queue with priority (chat = pli > markers)

# To Do

* Automatically adjust link speed / range based on # of lost messages
* Use T-Beam as a GPS source (if it proves to be more accurate than the phone's)
* Message IDs and receipt confirmation
* Improve chat message shrinking further
* Smarter sending -- Map Markers get higher priority unless PLI has not gotten sent in ~5 minutes
* Smarter sending -- Send a list of map markers to group, other clients can reply with which marker they are missing, build up a list of missing markers if more than 1 person is missing send to group, otherwise send to individuals
* Needs more real-world stability testing
* Re-add GoTenna support with a proper abstraction for communication layer

# Setup

To use this plugin you will need to build your own copy of ATAK-CIV and copy some files, to do that first:

* clone the ATAK-CIV repo: https://github.com/deptofdefense/AndroidTacticalAssaultKit-CIV
* clone this repo into the same parent directory that you cloned the DoD's ATAK repo to (i.e. not into the `AndroidTacticalAssaultKit-CIV` directory)
* get the ATAK-CIV SDK: http://7n7.us/civtak4sdk
* follow the instructions for building ATAK in the ATAK repo's `BULIDING.md` file, load the application onto your devices using the `installCivDebug` Gradle task
    * Note: you will need to configure a signing key in the local.properties file, you must use the same signing configuration in the plugin's `app/build.gradle` file!
    * Note: if you would like to use `installCivRelease` instead, you must add your key signature to `AtakPluginRegistry.ACCEPTABLE_KEY_LIST`
* copy the ATAK SDK into the `libs/` directory as `main.jar`
* Run `git submodule init` in the `atak-forwarder` directory
* clone the Meshtastic service/app: https://github.com/meshtastic/Meshtastic-Android
    * Comment out this line in `MeshService.kt`: `startLocationRequests()`
    * Build the service/app and install it onto your devices
* Pair your device with your Meshtastic radio in Android Settings > Connected Devices
* Edit the `app` Run Configuration in `atak-forwarder` and set the Launch Options to `Nothing`
* Build the `atak-forwarder` plugin and install it on your devices
* Open ATAK, the red @ sign in the lower right corner should turn green soon after opening the app, if it does not something is wrong with your BLE pairing, try re-pairing your Meshtastic device, then clicking on the red @ sign and clicking `Paired`
* Set up your channel in the Channel tab on one device, then show the QR code and scan it on your other device(s)
* You should see notifications about "discovery broadcasts" once all devices are on the same channel, if you do not check the channel name, hash, and try clicking `Broadcast Discovery` in the plugin settings menu (click the @)
* You should soon see map markers for each of your devices
* Note: this plugin will configure your Meshtastic device to send out position updates once per hour and to turn the LCD off after 1 second, you can tweak those values in `Config.java`

# Architecture Diagram (somewhat outdated)

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

The hardware/communication layer is (kinda) abstracted behind a `CommHardware` interface, this interface can be implemented against other hardware -- if you would like to give it a shot with another hardware layer please reach out to me and let me know how it goes.

