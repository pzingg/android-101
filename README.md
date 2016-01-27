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

If you do a new build (rename package or application, you will need new keys).  See
https://developers.google.com/maps/documentation/android-api/signup

To get the google_maps_key value, you enter the package name (com.gnatware.amber) that
is in the AndroidManifest.xml file, and the SHA-1 fingerprint obtained from running the
keytool command-line script.


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
    ...
    OK
    geo fix -122.547926 37.905996
    OK
    
    
Dealing with More than 65536 Methods
------------------------------------

After adding the Parse login logic (with its dependency on Twitter and Facebook APIs),
got the dreaded message

    Unable to execute dex: method ID not in [0, 0xffff]: 65536

when clicking on "Run 'app'" in the Android Studio Run menu.  This is because there's a
hard-coded limit of 65536 methods in a single "dex" file that is built for the application. To
get past this limitation, you can build multiple "dex" files.  First I nstalled the dexcount 
Gradle plugin to see how badly our app is doing. Edited this in the app's build.gradle:

    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath 'com.getkeepsafe.dexcount:dexcount-gradle-plugin:0.4.0'
        }
    }
    
    apply plugin: 'com.getkeepsafe.dexcount'
    
Then extended the main application class from MultiDexApplication, and finally added this to
the app's build.gradle:

    android {
        defaultConfig {
        multiDexEnabled true
        }
    }

Now after clicking on "Run 'app'", got this report that verifies that we're 2593 methods
over the limit, but at least it builds again:

    Total methods in app-debug.apk: 68128
    Total fields in app-debug.apk:  37786
    Methods remaining in app-debug.apk: -2593
    Fields remaining in app-debug.apk:  27749

But when the app was to be installed in the emulator, got this:

Failure [INSTALL_FAILED_DEXOPT]

Removing the app and re-installing fixed the bug.

Build > Clean Project
Settings > Applications > Manage Applications > [Find and Uninstall your App]
Restarting the emulator from the Android SDK and AVD Manager and selecting the 
option Wipe User Data.

I/AppCompatDelegate: The Activity's LayoutInflater already has a Factory installed so we can not install AppCompat's.

D/SignInActivity: onCreate
W/ActivityManager:   Force finishing activity 1 com.gnatware.amber/.SignInActivity
W/DropBoxManagerService: Dropping: data_app_crash (2091 > 0 bytes)
W/ActivityManager:   Force finishing activity 2 com.gnatware.amber/.RiderMapActivity
W/ActivityManager: Activity pause timeout for ActivityRecord{18a878f3 u0 com.gnatware.amber/.SignInActivity t72 f}
I/Process: Sending signal. PID: 17293 SIG: 9
I/ActivityManager: Process com.gnatware.amber (pid 17293) has died