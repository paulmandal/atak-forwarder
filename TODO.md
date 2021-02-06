# UI
- Maybe improve status top part of the screen? layout is a little dense
- Add ATAK icon to TAK-associated devices in the Status screen, show the TAK uname (DeviceID)
- Sort TAK-associated devices to the top
- Add settings button and move all settings stuff into another menu
- Move Set Comm Device into settings menu
- More icon colors / maybe #s to show status, single icon with many colors seems to be good
  - configurable window size (# of packets) to check, maybe configurable thresholds for green->brown?
    Green - Connected, 100% of last 5 packets delivered
    Orange - Connected, 50% delivered
    Yellow - Connected, 20% delivered
    Brown - Connected, 0% delivered
    Blue - No Comm Device Configured
    Red - Not Connected to Device

# Tracker
- Rename feature to Trackers (e.g. Devices tab becomes Trackers)
- Make it so you set the callsign/name when the Write button is clicked in teh confirm dialog
- Tracker devices should always be drawn on the map in their last known position
- Configurable grey-out time for Tracker devices (e.g. 10 mins after last known pos)
- Write to device, check settings, rewrite if failed, see if there's a way to reboot them

# Channel / Settings
- Move manual channel config into settings menu
- Maybe move show/scan QR into settings menu too

# Config
- Add config for hopLimit for PLI, Chat, and Other
- Add the option to read/write config to sdcard
- Write more config values to device (e.g. channel settings)
- Option to reset config to defaults

# Internal
- Check Meshtastic App vs. our impl of service integration
- get rid of updateChannelMembers(), instead build channel members lists from broadcasts and node_change?
- use NODEINFO_APP and POSITION_APP instead of NODE_CHANGED?
- Don't read channel settings from device, write them when setting up if a comm. device is configured
- maybe we send msgs to Meshtastic rapidly with a msgId+segment as first 2 bytes, on the receiving end we can assemble them in a map<map> until we have the whole thing or a timeout / cleaning
  - to do this we need to validate that we can send msgs to the service faster than the device can send them to the mesh, and they'll still eventually get there
  - for that we need our own test harness app
  - we need to track all outbound msgs in a structure and resend as appropriate, maybe not resend PLIs and shit like that
  - InFlightMessage properties(type, msgId, segmentId, message[]) something like that
- re-validate how messages from an ATAK EUD that we have not received a broadcast are treated, can we see the PLI on the map but just not chat/directmsg?


### Runoff

- location doesn't update from TBeams? do their GPSs even work?
- QR doesn't scan on Pixel 4?
- find out how/when T-Beams reset their user info?

- determine bandwidth on slow link -- how many PLIs can go through per min? how many others? scale PLI TTL based on that + modem speed
^-- create a button or in Advanced to shit out like 10-20 items and time how long it takes to send them across the link

- Do some range testing around the hood on all 4 modes

- have different @ icons for each state
- remote updating of channel settings for all members?

### Next phase

- link-speed management
- can we use T-Beam as a GPS source? separate plugin?
- test fast/short range distance -- can it do to S Broadway?

#### Random Crap
  
- if we know the type, map it, otherwise use a string

bugfixes:
- why does msg queue grow forever when disconnected / unpaired after being in a group?

##### 

- why does uploading the plugin crash the Moto 6?
- Better error handling
- make Toasts for important errors?