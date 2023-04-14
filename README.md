# ATAK Forwarder

An ATAK plugin for forwarding CoT messages via a hardware layer. Currently supports [Meshtastic](https://www.meshtastic.org) devices.

Binaries signed for the Play Store version of ATAK are available here: [Binaries](https://civtak.org/atak-forwarder)

*IMPORTANT NOTE:* All configuration options / Tracker writing have been moved into ATAK's three button (kebab) menu under *Settings > Tool Preferences > Specific Tool Preferences > ATAK Forwarder*

[![](https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif)](https://www.paypal.com/paypalme/paypaulmandal)

## Share Markers and PLI

![Share Markers and PLI](https://github.com/paulmandal/atak-forwarder/raw/2.0.6/images/0-markers-and-pli.png)
<br>
![Plugin Status Screen](https://github.com/paulmandal/atak-forwarder/raw/2.0.6/images/1-status.png)
<br>
<br>

## Send Chat Messages

![Chat Messages](https://github.com/paulmandal/atak-forwarder/raw/2.0.6/images/2-chat-messages.png)
<br>
<br>

## Configurable Channel Settings / Share with QR

![Channel Mode Selection](https://github.com/paulmandal/atak-forwarder/raw/2.0.6/images/3-channel-config.png)
<br>
![QR Channel Sharing](https://github.com/paulmandal/atak-forwarder/raw/2.0.6/images/4-qr-code-sharing.png)
<br>
<br>

## Use standalone Meshtastic devices as Trackers

![Write to Tracker](https://github.com/paulmandal/atak-forwarder/raw/2.0.6/images/5-write-to-tracker.png)
<br>
<br>

# Features

* In-app device and channel management
* Broadcast messages are sent to the channel (e.g. map markers, PLI)
* Direct messages to other users (e.g. chat messages)
* Support for Meshtastic devices without an ATAK EUD attached
* Efficient comm. using libcotshrink -- can send approx 5 map markers or PLI per minute, or 2 chats, or 2.5 more complex markers
* Typical msg sizes, PLI: ~190 bytes, simple shape ~200 bytes, complex shape ~250 bytes, ~380 bytes, group chat ~400 bytes
* Filtering of repeated messages with a configurable TTL (e.g. to prevent auto-send markers from flooding)
* Message queue with priority (chat = pli > markers)
* Support for USB devices

# Supported Versions

The plugin has been tested with these versions of the Meshtastic dependencies. If you run into issues try installing these versions of the Meshtastic software to resolve them.

| Dependency | Version |
|--|--|
| Meshtastic App | 2.1.8 |
| Meshtastic Firmware | 2.1.8 |

# To Do

* Needs more real-world stability testing
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

* Flash your devices with the supported version of the [Meshtastic Firmware](https://github.com/meshtastic/Meshtastic-device/releases/latest) from the table above
* Install the [Meshtastic App](https://play.google.com/store/apps/details?id=com.geeksville.mesh) from the Play Store.
* Open the Meshtastic App, click on the Settings (gear) icon on the right of the screen, then click the `+` icon, the app will ask for Bluetooh permissions, grant them.

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
* Tap on Specific Tool Preferences
* Tap on ATAK Forwarder Preferences
* Tap on `Refresh Devices`
* Tap on `Set Comm Device` and pick your device from the list
* Tap on `Set Region` and pick the region you are currently in
* The red line through the icon in the lower right corner of the screen should disappear within about a minute

## Setting up your Channel

* In ATAK tap on the three dots menu in the upper right corner of the screen
* Tap on Settings
* Tap on Tool Preferences
* Tap on Specific Tool Preferences
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
* Tap on Specific Tool Preferences
* Tap on ATAK Forwarder Preferences
* Scroll to `Tracker` and configure your `Team`, `Role`, and other settings
* Click on `Write to Device` and pick your device from the dropdown, set a `Callsign` and click `OK`
* You will see a small spinning progress bar appear on the screen, wait until it disappears before doing anything else with the plugin
* After the spinning progress bar disappears check the devices channel on its LCD, if the channel is updated reset the device by pressing the reset button for a second or two
* You should see your device appear on the map after it boots back up, its location should start updating once it has a GPS lock

# Notes on Message Handling

Message handling follows a few simple rules:

- Messages from ATAK that are not chat are checked against a Recently Sent cache, if a message was recently sent it was dropped. This prevents spamming of auto-send map markers.
- Messages are then queued in a prioritized queue, with the priority: chat = pli > marker
- If a similar message already exists in the queue (e.g. PLI) it will be overwritten with the new message, this way a queued PLI won't be sent with out of date data if newer data is available
- Messages are fetched from this queue by the CommHardware class and sent

- The plugin will attempt to first use a "minimal" protobuf that saves space, but if it will result in dropped fields or a failed mapping on the receiving size it will fall back to the regular protobufs
- When values appear more than once in a payload we attempt to replace subsequent appearances with a marker/placeholder that is swapped back for the value when rebuilding the original message

# Contributing

Areas I'd especially like help are:

* reducing the message sizes without affecting features in ATAK (e.g. removing `detail.contact.endpoint` kills chat) -- check out https://github.com/paulmandal/libcotshrink for this effort
* increasing resilience of this plugin, it is basically fire-and-forget (and maybe lose your message) right now
