# ATAK Forwarder 

An ATAK plugin for forwarding CoT messages via a hardware layer. Currently supports [Meshtastic](https://www.meshtastic.org) devices.

Binaries signed for the Play Store version of ATAK are available here: [Binaries](https://civtak.org/atak-forwarder)

*IMPORTANT NOTE:* All configuration options / Tracker writing have been moved into ATAK's three button (kebab) menu under *Settings > Tool Preferences > ATAK Forwarder*

![Plugin Disconnected Indicator](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/plugin-disconnected-indicator.png)
<br>
![Status View](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/status-view.png)
<br>
![Channel Management](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/channel-management.png)
<br>
![QR Configuration](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/qr-configuration.png)
<br>
![Integrated Direct Messaging](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/integrated-direct-messaging.png)
<br>
![Transmit Map Markers](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/transmit-map-markers.png)
<br>
Supports Meshtastic devices without an ATAK EUD attached
![Support non-ATAK Devices](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/non-atak-devices-configuration.png)
![Support non-ATAK Devices](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/non-atak-devices-map-marker.png)
<br>
![Example Usage](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/example-usage.png)

# Features

* In-app device and channel management
* Broadcast messages are sent to the channel (e.g. map markers, PLI)
* Direct messages to other users (e.g. chat messages)
* Support for Meshtastic devices without an ATAK EUD attached
* Efficient comm. using libcotshrink -- can send approx 5 map markers or PLI per minute, or 2 chats, or 2.5 more complex markers
* Typical msg sizes, PLI: ~190 bytes, simple shape ~200 bytes, complex shape ~250 bytes, ~380 bytes, group chat ~400 bytes
* Filtering of repeated messages with a configurable TTL (e.g. auto-send markers)
* Message queue with priority (chat = pli > markers)

# Beta Features

* Support for USB devices -- early stage, you might need to pair the device in the Meshtastic app to get USB permissions before setting it up in ATAK

# Supported Versions

The plugin has been tested with these versions of the Meshtastic dependencies. If you run into issues try installing these versions of the Meshtastic software to resolve them.

| Dependency | Version |
|--|--|
| Meshtastic-Android | 1.2.52 |
| Meshtastic-device | 1.2.52 |

# To Do

* Remote channel management / updating
* Automatically adjust link speed / range based on # of lost messages
* Use T-Beam as a GPS source (if it proves to be more accurate than the phone's)
* Message IDs and receipt confirmation
* Improve chat message shrinking further
* Smarter sending -- Map Markers get higher priority unless PLI has not gotten sent in ~5 minutes
* Smarter sending -- Send a list of map markers to group, other clients can reply with which marker they are missing, build up a list of missing markers if more than 1 person is missing send to group, otherwise send to individuals
* Needs more real-world stability testing
* Re-add GoTenna support with a proper abstraction for communication layer
* Bridge between multiple comm. devices? E.g. Meshtastic + goTenna on one device. Alternative is to break that into more than one plugin instance since their preSendProcessors will see each other's messages.
* Use Dagger 2
* Get a proper CI setup going (GitHub Actions?)

# Building the Plugin

The below instructions assume that you are cloning everything side-by-side in the same directory, so you should end up with a directory tree like:

```
workspace/
  |
  \--- Meshtastic-Android/
  |
  \--- AndroidTacticalAssaultKit-CIV/
  |
  \--- atak-forwarder/
```

## Set Up Meshtastic

* Flash your devices with the lastest [Meshtastic Firmware](https://github.com/meshtastic/Meshtastic-device/releases/latest) (The plugin has been tested with 1.1.5 beta, if you have issues try that version)
* Install the [Meshtastic App](https://play.google.com/store/apps/details?id=com.geeksville.mesh) from the Play Store.
* That's all, you don't need to open the app to continue

## Build + Install ATAK

ATAK requires that plugins be signed with a whitelisted signature. In order to run your own builds of the plugin you will need to have a copy of ATAK that is signed with the same key you are using to sign your plugin build.

* Clone the ATAK-CIV repo: `git clone git@github.com:deptofdefense/AndroidTacticalAssaultKit-CIV.git`
* Follow the commands in the `.github/workflows` files or the instructions in `AndroidTacticalAssaultKit-CIV/BUILDING.md` to build and install ATAK
    * Note: you will need to configure a signing key in the `local.properties` file

## Build the ATAK Gradle Plugin

* Go to the `AndroidTacticalAssaultKit-CIV/atak-gradle-takdev` directory
* Run `./gradlew assemble`

## Build + Install ATAK Forwarder

* Clone the ATAK Forwarder repo: `git clone git@github.com:paulmandal/atak-forwarder.git`
* Copy the `local.properties` file that you created while you were building ATAK to the `atak-forwarder` directory
* Edit `local.properties` and add this line, remember to update the path to your correct path: `sdk.path=/path/to/your/AndroidTacticalAssaultKit-CIV/atak/ATAK/app/build/libs`
* Edit the `app` Run Configuration in `atak-forwarder` and set the Launch Options to `Nothing`
* Build the `atak-forwarder` plugin and install it on your devices (note: the environment variable `ATAK_VERSION` can be used to override the target ATAK version found in `app/build.gradle`)

# Setting up the Plugin

## Setting up your Comm Device

* In the Android Settings / Connected Devices or Bluetooth Settings tap `Pair a new device`
* Pair with your Meshtastic device
* Start ATAK, you should see a purple icon in the lower right corner of the screen
* Tap on the three dots menu in the upper right corner of the ATAK screen
* Tap on Settings
* Tap on Tool Preferences
* Tap on ATAK Forwarder Preferences
* Tap on `Refresh Devices`
* Tap on `Set Comm Device` and pick your device from the list
* Tap on `Set Region` and pick the region you are currently in
* The icon in the lower right corner of the ATAK map should turn red and then green

## Setting up your Channel

* In ATAK tap on the three dots menu in the upper right corner of the screen
* Tap on Settings
* Tap on Tool Preferences
* Tap on ATAK Forwarder Preferences
* Scroll to `Channel Name` and tap on it to set your channel name, max length is 11 characters
* Tap on `Mode / Speed` and pick a mode, try `Short Range / Fast` to start with
* Tap on `Generate PSK` to generate a new pre-shared key to encrypt your channel with
* Tap on `Show QR` to show a QR code with your channel's settings
* On your other devices, go to the same Settings screen and then tap `Scan QR` to scan the channel settings
    * You should see notifications about "discovery broadcasts" once all devices are on the same channel, if you do not check the channel name, hash, and try clicking `Broadcast Discovery` in the plugin settings menu (click the @)
    * You should soon see map markers for each of your devices

## Setting up a Tracker device

The ATAK Forwarder supports configuring Meshtastic devices that have a GPS but no phone controlling them to show up on the map with a configurable callsign, team, and icon. This can be useful for retrieving relay devices or use cases that only need to output location data (e.g. animal tracking)

* Pair your extra Meshtastic devices with your phone as normal
* In ATAK tap on the three dots menu in the upper right corner of the screen
* Tap on Settings
* Tap on Tool Preferences
* Tap on ATAK Forwarder Preferences
* Scroll to `Tracker` and configure your `Team`, `Role`, and other settings
* Click on `Write to Device` and pick your device from the dropdown, set a `Callsign` and click `OK`
* You will see a small spinning progress bar appear on the screen, wait until it disappears before doing anything else with the plugin
* After the spinning progress bar disappears check the devices channel on its LCD, if the channel is updated reset the device by pressing the reset button for a second or two
* You should see your device appear on the map after it boots back up, its location should start updating once it has a GPS lock

# Architecture Diagram

![Architecture Diagram](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/arch-diagram.png)
![Architecture Diagram Non-ATAK Devicess](https://github.com/paulmandal/atak-forwarder/raw/0.9.1/images/arch-diagram-non-atak-devices.png)

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

* reducing the message sizes without affecting features in ATAK (e.g. removing `detail.contact.endpoint` kills chat) -- check out https://github.com/paulmandal/libcotshrink for this effort
* increasing resilience of this plugin, it is basically fire-and-forget (and maybe lose your message) right now
* re-introducing goTenna Mesh support

The hardware/communication layer is (kinda) abstracted behind a `CommHardware` interface, this interface can be implemented against other hardware -- if you would like to give it a shot with another hardware layer please reach out to me and let me know how it goes.

