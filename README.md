# ATAK + GoTenna Mesh

An application/service for forwarding ATAK UDP packets via GoTenna Mesh.

# Setup

First you will need to set up your ATAK to send its mesh broadcasts to your localhost so that this application/service can receive and forward the messages, to do that:

Go to Settings
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/screenshots/instructions-step-0.png "Go to Settings")

Go to Network Connections
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/screenshots/instructions-step-1.png "Go to Network Connections")

Go to Network Connections
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/screenshots/instructions-step-2.png "Go to Network Connections")

Go to Manage Outputs
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/screenshots/instructions-step-3.png "Go to Manage Outputs")

Add a Loopback Output
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/screenshots/instructions-step-4.png "Add a Loopback Output")

Make sure Loopback Output is checked
![alt text](https://github.com/paulmandal/atak-forwarder/raw/master/screenshots/instructions-step-5.png "Make sure Loopback Output is checked")

* Copy the GoTenna Public SDK into the `gotenna-public-sdk/` folder as `gotenna-public-sdk.aar`
* Edit `MainActivity.java`, put your GoTenna SDK token in the `GOTENNA_SDK_TOKEN` variable
* Set `PRIMARY_DEVICE` to `true` and build the application onto your first device
* Set `PRIMARY_DEVICE` to `false` and build the application onto your secondary device
* Open ATAK on each device

It will take quite a while for the first messages to be transmitted. During my testing I only tested in one direction -- having both devices transmit might take too much bandwidth for the GoTenna Mesh.