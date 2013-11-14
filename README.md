Power-Monitor-Lite
==================

A simple app that monitors and detects power levels for Windows PCs.

How It works
=============

There are two sorts of monitors available.
One of the monitors listens for AC Power cable detachment or Power supply interruption.
When this monitor is used, action is triggered only if power goes off, or power supply to the computer is interrupted.

The second monitor triggers action, when the Battery percentage level is below the specified value. Thus it keeps
checking the battery percentage, until battery percentage level is below the specified level, and acts only if 
it's lower than that which was specified. This monitor is ineffective when the AC Power Cable is plugged in.
Cos, it's of no use, then, right? ;-)

An interval could be set for when action is performed after requirement has been met.
This means that you could choose to hibernate/shutdown your pc after a specific time has passed from when the action
is triggered. For example, if I decide to specify an interval of 30 seconds, on the AC Power monitor, it means that
when power supply is interrupted, the app will wait for 30 seconds before hibernating or shutting down my pc.

The interval is placed to allow for a situation in which power supply is restored before the interval expires.

Essentially, if power supply is interrupted, and ten seconds into the interval, it's restored back, action (hibernating/
shutting down) is deferred, until power is interrupted again, and then the process of monitoring begins all over.

The selected monitor could be stopped at anytime.
