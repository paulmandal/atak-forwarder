# ATAK Packet Forwarder 

An ~~application/service~~ ATAK plugin for forwarding CoT messages via a hardware layer. Currently supports [Meshtastic](https://meshtastic.org) devices.

![Plugin Disconnected Indicator](https://github.com/paulmandal/atak-forwarder/raw/master/images/plugin-disconnected-indicator.png)
<br>
![Status View](https://github.com/paulmandal/atak-forwarder/raw/master/images/status-view.png)
<br>
![Channel Management](https://github.com/paulmandal/atak-forwarder/raw/master/images/channel-management.png)
<br>
![QR Configuration](https://github.com/paulmandal/atak-forwarder/raw/master/images/qr-configuration.png)
<br>
![Integrated Direct Messaging](https://github.com/paulmandal/atak-forwarder/raw/master/images/integrated-direct-messaging.png)
<br>
![Transmit Map Markers](https://github.com/paulmandal/atak-forwarder/raw/master/images/transmit-map-markers.png)
<br>
Supports Meshtastic devices without an ATAK EUD attached
![Support non-ATAK Devices](https://github.com/paulmandal/atak-forwarder/raw/master/images/non-atak-devices-configuration.png)
![Support non-ATAK Devices](https://github.com/paulmandal/atak-forwarder/raw/master/images/non-atak-devices-map-marker.png)
<br>
![Example Usage](https://github.com/paulmandal/atak-forwarder/raw/master/images/example-usage.png)

# Features

* Peer discovery
* In-app device and channel management
* Broadcast messages are sent to the channel (e.g. map markers, PLI)
* Direct messages to other users (e.g. chat messages)
* Efficient comm. using protobufs -- can send approx 5 map markers or PLI per minute, or 2 chats, or 2.5 more complex markers
* Typical msg sizes, PLI: ~190 bytes, simple shape ~200 bytes, complex shape ~250 bytes, ~380 bytes, group chat ~400 bytes
* Filtering of repeated messages with a configurable TTL (e.g. auto-send markers)
* Message queue with priority (chat = pli > markers)

# To Do

* Use `ChatManagerMapComponent` for incoming chats instead of turning them into CoTs
* Remote channel management
* Automatically adjust link speed / range based on # of lost messages
* Use T-Beam as a GPS source (if it proves to be more accurate than the phone's)
* Message IDs and receipt confirmation
* Improve chat message shrinking further
* Smarter sending -- Map Markers get higher priority unless PLI has not gotten sent in ~5 minutes
* Smarter sending -- Send a list of map markers to group, other clients can reply with which marker they are missing, build up a list of missing markers if more than 1 person is missing send to group, otherwise send to individuals
* Needs more real-world stability testing
* Re-add GoTenna support with a proper abstraction for communication layer

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

## Build + Install Meshtastic

* Clone my fork of Meshtastic-Android: `git clone git@github.com:paulmandal/Meshtastic-Android.git`
    * This contains a change to make `MeshService.setDeviceAddress()` work for external callers
* Enter the `Meshtastic-Android` directory: `cd Meshtastic-Android`
* Run the commands in `Meshtastic-Android/README.md` under "Build Instructions"
* Open `Meshtastic-Android` in Android Studio, build and run the project, you can close the Meshtastic app

## Build + Install ATAK

It is currently not possible or at least not easy to get a 3rd party plugin signed, so you will need to build your own copy of ATAK. ATAK checks the signature on any plugins it loads against a whitelist and will not load any plugin that is not signed with a whitelisted key.

* Clone the ATAK-CIV repo: `git clone git@github.com:deptofdefense/AndroidTacticalAssaultKit-CIV.git`
* Follow the instructions in `AndroidTacticalAssaultKit-CIV/BUILDING.md` to build and install ATAK
    * Note: you will need to configure a signing key in the local.properties file, you must use the same signing configuration in the plugin's `app/build.gradle` file!
    * Note: if you would like to use `installCivRelease` instead, you must add your key signature to `AtakPluginRegistry.ACCEPTABLE_KEY_LIST`

## Build + Install ATAK Forwarder

* Clone the ATAK Forwarder repo: `git clone git@github.com:paulmandal/atak-forwarder.git`
* Run `git submodule update --init --recursive`
* Edit the `app` Run Configuration in `atak-forwarder` and set the Launch Options to `Nothing`
* Build the `atak-forwarder` plugin and install it on your devices

# Setting up the Plugin

## Setting up your Comm Device

* In the Android Settings / Connected Devices or Bluetooth Settings tap `Pair a new device`
* Pair with your Meshtastic device
* Start ATAK, you should see an orange icon in the lower right corner of the screen
* Tap on the icon, the plugin menu should open
* Tap on the `Devices` tab
* Tap on the `Refresh` button in the lower left corner of the plugin screen
* Tap on your Meshtastic device when it shows up in the list of devices
* Tap on the `Set Comm Device` button, this will set your primary comm device
* The orange icon in the lower right corner of the ATAK map should turn green soon

## Setting up your Channel

* On one device, tap on the `Channel` tab
* Tap on the `Edit Channel` button
* Pick your range/speed settings, recommended to start with the fastest / lowest range setting and work from there
* Pick a good name for your channel
* Tap on the `Gen PSK` button
* Tap on the `Save` button
* Wait until your new channel settings show up in the `Status` or `Channel` tab, if they do not show up after a minute retry the edit channel steps
* Once the channel settings are updated, click on the `Show QR` button to show a scannable QR code that you can use to configure your other devices
* On your other devices, click on th `Scan QR` button in the `Channel` tab to scan a channel QR
    * You should see notifications about "discovery broadcasts" once all devices are on the same channel, if you do not check the channel name, hash, and try clicking `Broadcast Discovery` in the plugin settings menu (click the @)
    * You should soon see map markers for each of your devices
    * Note: this plugin will configure your Meshtastic device to send out position updates once per hour and to turn the LCD off after 1 second, you can tweak those values in `Config.java`

## Setting up a non-ATAK Device

The ATAK Forwarder supports configuring Meshtastic devices that have a GPS but no phone controlling them to show up on the map with a configurable callsign, team, and icon. This can be useful for retrieving relay devices or use cases that only need to output location data (e.g. animal tracking)

* Pair your extra Meshtastic devices with your phone as normal
* In the `Devices` tab, click the `Refresh` button
* Click on a device that is not your primary comm device, it will show up in the `Target` textview
* Enter the settings for your non-ATAK device
* Click on `Write to non-ATAK`
* You will see a small spinning progress bar appear on the screen, wait until it disappears before doing anything else with the plugin
* After the spinning progress bar disappears check the devices channel on its LCD, if the channel is updated reset the device by pressing the reset button for a second or two
* You should see your device appear on the map after it boots back up, its location should start updating once it has a GPS lock

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

The hardware/communication layer is (kinda) abstracted behind a `CommHardware` interface, this interface can be implemented against other hardware -- if you would like to give it a shot with another hardware layer please reach out to me and let me know how it goes.

