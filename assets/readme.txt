Chatty is a Twitch Chat Client, supporting many Twitch-specific features such as emotes,
nick colors, displaying stream information and much more.

Website: https://chatty.github.io
E-Mail: chattyclient@gmail.com
Twitter: @ChattyClient (https://twitter.com/ChattyClient)
YouTube: https://www.youtube.com/user/chattyclient
Discord: https://discord.gg/WTuqGeJ

Requirements
------------

This is a Java program, so you need to have the JRE 8 (Java Runtime Environment) or later installed.

Installation
------------

No real need for installation, just extract the Chatty_*.zip file into a
folder and start the Chatty.jar with Java.

Starting Chatty
---------------

You may be able to just double-click the Chatty.jar. If it asks you to select a program
to open it with, choose Java.

You can also create a shortcut to start Chatty, when creating the shortcut use:

	javaw -jar "<Path to Chatty.jar>"

For example:

	javaw -jar "C:\Program Files (x86)\Chatty\Chatty.jar"

This also allows you to add launch options, for example defining the channel to join:

	javaw -jar "C:\Program Files (x86)\Chatty\Chatty.jar" -channel twitch

If you create a shortcut like this, make sure you set the working directory correctly
(specified in a field labeled "Run in" or similar). This can be important for some features.

You may also need to specify the full path to Java if just "javaw -jar" doesn't work:

	C:\Windows\System32\javaw.exe -jar "C:\Program Files (x86)\Chatty\Chatty.jar"

Or to use a specific JRE:

	C:\Program Files\Java\jre1.8.0_261\bin\javaw.exe -jar "<Path to Chatty.jar>"

Settings
--------

Settings are saved in a ".chatty" subfolder of your user directory by default. You can
find out where the settings are saved to by entering "/dir" in Chatty.

You can change this location to your current working directory by using the "-cd" launch
option, to a specified directory by using the "-d <dir>" launch option (the dir has to
exist already) or to a directory located next to the Chatty.jar by using the "-portable"
launch option.

Logfile
-------

There is a /debuglogs subdirectory in the settings directory which contains a
number of different debug logs. There is more information on them in the help.

More Help
---------

There is more help available in Chatty under <Help - About/Help> and online on the
Chatty website as mentioned at the beginning of this file.

Licence
-------

Chatty is released under the GPLv3. See the in-app help and the GitHub repository
for more detailed information.
