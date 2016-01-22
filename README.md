Amber
=====
Android project based on the Uber app in Rob Percival's Udemy Android Programming course.

Build Instructions
------------------
Built with Android Studio 1.5 on OS X 10.11.

Build configuration versions:

 * Compile SDK Version 23
 * Build Tools Version 23.0.2
 * Minimum SDK version 19
 * Target SDK Version 23

To build, create debug.properties and release.properties files in this directory.
Both properties files should contain API secrets for Parse and Google Maps:

    parse_app_id=...
    parse_client_key=...
    google_maps_key=...

Floating Action Button Icons and Transformations
------------------------------------------------
Use Google icons from https://design.google.com/icons/
Download drawable-anydpi-v21 xml files from https://github.com/google/material-design-icons/blob/master/
Change fillColor attributes from black (#FF000000) to white (#FFFFFFFF).

Floating action button
Interior icon: 24 x 24dp
Floating action button circle: 56 x 56dp

Mini floating action button
Interior icon: 24 x 24dp
Floating action button circle: 40 x 40dp

The floating action button can transform into a toolbar upon press or appear from a toolbar
that transforms into the floating action button upon scroll.  The toolbar can contain related
actions, text and search fields, or any other items that would be useful at hand.

Example of FAB animations here: https://github.com/Learn2Crack/android-fab-animations
More FAB info here: https://guides.codepath.com/android/floating-action-buttons


Simulating Locations in the Emulator
------------------------------------
Open a Terminal window. Then telnet to port 5554 and use "geo fix" command.
NOTE: "geo fix" parameters are longitude first, latitude second!

    $ telnet localhost 5554
    Trying ::1...
    telnet: connect to address ::1: Connection refused
    Trying 127.0.0.1...
    Connected to localhost.
    Escape character is '^]'.
    Android Console: type 'help' for a list of commands
    OK
    geo fix -122.547926 37.905996
    OK