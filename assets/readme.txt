Chatty is a Twitch Chat Client, supporting many Twitch-specific features such as emotes,
nick colors, displaying stream information and much more.

Website: http://chatty.github.io
E-Mail: chattyclient@gmail.com
Twitter: @ChattyClient (https://twitter.com/ChattyClient)
YouTube: http://www.youtube.com/user/chattyclient

Requirements
------------

This is a Java program, so you need to have the JRE 7 (Java Runtime Environment) or later installed.

Installation
------------

No real need for installation, just have the Chatty.jar and the /lib-Folder with the
contained files in the same folder.

If your version has Hotkey support for Windows, you also need the JIntellitype.dll in the
folder you execute Chatty from.

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

You may also need to specify the full path to Java if just "javaw -jar" doesn't work, for
example:

	C:\Windows\System32\javaw.exe -jar "C:\Program Files (x86)\Chatty\Chatty.jar"

Settings
--------

Settings are saved in a ".chatty" subfolder of your user directory by default. You can
find out where the settings are saved to by entering "/dir" in Chatty.

You can change this location to your current working directory by using the "-cd" launch
option.

Logfile
-------

There is a debugging logfile saved in the same folder as the settings named "debug.log",
which is overwritten everytime you start Chatty. If you experience a crash or any other
bugs it is usually prudent to copy it before starting Chatty again. There are however also
other debug logfiles named "debug<number>.log", which are rotated once they reach a certain
size, so even if the other logfile is overwritten, there may still be relevant info in one
of those files.

More Help
---------

There is more help available in Chatty under <Help - About/Help> and online on the
Chatty website as mentioned at the beginning of this file.

Licence
-------

Chatty is released under the MIT License, except for the parts mentioned below.

External Libraries
------------------

* JSON-Simple [lib/json-simple-*.jar]
	Website: https://code.google.com/p/json-simple/)
	License: "Apache License 2.0"
	http://www.apache.org/licenses/LICENSE-2.0

* JIntellitype [lib/jintellitype-*.jar, Jintellitype*.dll] (only some versions)
	Website: https://code.google.com/p/jintellitype/
	License: "Apache License 2.0"
	http://www.apache.org/licenses/LICENSE-2.0

* Favorites Icon [star.png] by Everaldo Coelho
	Source: https://www.iconfinder.com/icons/17999/bookmark_favorite_star_icon 
	License: LGPL
	http://www.gnu.org/licenses/lgpl.html

* Misc Icons [list-add.png, list-remove.png, view-refresh.png,
		help-browser.png, preferences-system.png,
		dialog-warning.png, go-down.png, go-up.png, go-next.png,
		go-previous.png, go-home.png, go-web.png] from the Tango Icon Theme
	Source: http://tango.freedesktop.org/Tango_Icon_Library
	License: Released into the Public Domain

* Misc Icons [edit.png, ok.png, no.png] from NUVOLA ICON THEME for KDE 3.x
		by David Vignoni
	Source: http://www.icon-king.com/projects/nuvola/
	License: LGPL
	http://www.gnu.org/licenses/lgpl.html

* Robot Icon [icon_bot.png] by Yusuke Kamiyamane
	Source: https://www.iconfinder.com/icons/46205/robot_icon
	License: CC-BY 3.0
	(http://creativecommons.org/licenses/by/3.0/)
