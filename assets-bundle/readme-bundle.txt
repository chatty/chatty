
This is a bundled version of Chatty, which includes:

* The Chatty program (Chatty.jar), the original readme.txt and other original
  Chatty files in the `app` subdirectory
* A private Java Runtime Environment (Java 1.8.0_151, 32bit) in the `runtime`
  subdirectory, which is used to run Chatty without having to have Java
  installed on the system
* The Chatty.exe and some helper libraries

Advantages
----------

* You don't need to install Java on your system
* The Chatty.exe is a native Windows program with an icon

Downsides
---------

* Larger download size
* You can't update the bundled Java yourself

=================
Use and configure
=================

Execute the Chatty.exe to start the Chatty with the bundled runtime. The
working directory should be the `app` directory.

You can add/remove JVM and Chatty commandline options in the `app/Chatty.cfg`
file. For example add `-cd` to the `ArgOptions` section to save settings to
the `app` directory, which makes Chatty portable.

===========
Development
===========

This bundle was created from the regular Chatty.jar using the javapackager
program that is included in the JDK.

See the README.md in the Chatty repository for information on building it
yourself.

Documentation:
http://docs.oracle.com/javase/8/docs/technotes/tools/windows/javapackager.html
