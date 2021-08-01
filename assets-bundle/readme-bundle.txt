
This is a bundled version of Chatty, which includes:

* The Chatty program (Chatty.jar) and related files in the `app` directory
* The Chatty.exe that uses the private Java Runtime in the `runtime` directory
  to launch Chatty
* Some additional fallback fonts

Advantages
----------

* You don't need to install Java on your system
* A version of Java that Chatty has been tested with
* The Chatty.exe is a native Windows program with an icon

Downsides
---------

* Larger download size
* You can't update the bundled Java yourself
* Currently only available for Windows

=================
Use and configure
=================

The "Chatty.exe" starts Chatty normally, with the launch arguments in
`app/Chatty.cfg`.

If available, the "ChattyPortable.exe" starts Chatty with the settings
directory `app/portable_settings`, with the launch arguments in
`app/ChattyPortable.cfg`. Note that using this method when Chatty is
installed in a protected locations such as "C:\Program Files.." it might
actually store the settings in something like:
"C:\Users\..\AppData\Local\VirtualStore\Program Files (x86)\..."

The working directory is always set to the `app` directory.

See the help on how to add launch options:
https://chatty.github.io/help/help-standalone.html

===========
Development
===========

This bundle was created from the regular Chatty.jar using the javapackager
program that is included in the JDK. Newer versions use the jpackage program
included in Java 14 or later.

See the README.md in the Chatty repository for information on building it
yourself.
