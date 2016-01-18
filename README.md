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