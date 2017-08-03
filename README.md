Chatty
======

Chatty is a Twitch Chat Client for Desktop written in Java featuring many
Twitch specific features.

* Website: http://chatty.github.io
* E-Mail: chattyclient@gmail.com
* Twitter: @ChattyClient (https://twitter.com/ChattyClient)

I learned about most of the Java techniques and APIs used in this during
development, so many things won't be designed ideally. I also never
released such a project as opensource before, so if I missed anything or
didn't adhere to some license correctly, please tell me.

Download
========

Go to the [website](http://chatty.github.io) for ready to use downloads
and more information on the features.

License Information
===================

External Libraries/Resources
----------------------------

* JSON-Simple:
  * File: `assets/lib/json-simple-*.jar`
  * Website: https://code.google.com/p/json-simple/
  * License: "Apache License 2.0"
	(for the license text see the APACHE_LICENSE file
	or http://www.apache.org/licenses/LICENSE-2.0).

* JIntellitype:
  * Files: `assets/lib/jintellitype-*.jar, Jintellitype*.dll`
  * Website: https://code.google.com/p/jintellitype/
  * License: "Apache License 2.0"
	(for the license text see the APACHE_LICENSE file
	or http://www.apache.org/licenses/LICENSE-2.0).

* Tyrus:
  * Files: `assets/lib/tyrus-standalone-client-1.12.jar`
  * Website: https://tyrus.java.net
  * License: "CDDL 1.1" and "GPL 2 with CPE"
	(see https://tyrus.java.net/license.html)

* Favorites Icon by Everaldo Coelho:
  * File: `star.png`
  * Source: https://www.iconfinder.com/icons/17999/bookmark_favorite_star_icon 
  * License: LGPL
	(for the license text see the LGPL file or
	http://www.gnu.org/licenses/lgpl.html)

* Misc Icons from the Tango Icon Theme:
  * Files: `list-add.png, list-remove.png, view-refresh.png,
		help-browser.png, preferences-system.png,
		dialog-warning.png, go-down.png, go-up.png, go-next.png,
		go-previous.png, go-home.png, go-web.png,
		image-icon.png, commandline.png, edit-copy.png, sort.png,
		edit-all.png (edited)`
  * Source: http://tango.freedesktop.org/Tango_Icon_Library
  * License: Released into the Public Domain

* Misc Icons from NUVOLA ICON THEME for KDE 3.x
		by David Vignoni:
  * Files: `edit.png, ok.png, no.png`
  * Source: http://www.icon-king.com/projects/nuvola/
  * License: LGPL
	(for the license text see the LGPL file or
	http://www.gnu.org/licenses/lgpl.html)

* Robot Icon by Yusuke Kamiyamane:
  * File: `icon_bot.png`
  * Source: https://www.iconfinder.com/icons/46205/robot_icon
  * License: CC-BY 3.0
	(http://creativecommons.org/licenses/by/3.0/)

* Twitter Emoji Images
  * Files: `gui/emoji/twemoji`
  * Source: https://github.com/twitter/twemoji
  * License: CC-BY 4.0
	(https://creativecommons.org/licenses/by/4.0/)

* Emoji One Images
  * Files: `gui/emoji/e1`
  * Source: http://emojione.com/
  * License: CC-BY 4.0
	(https://creativecommons.org/licenses/by/4.0/)

* Emoji Metadata based on EmojiOne emoji.json
  * Source: https://github.com/Ranks/emojione/blob/master/emoji.json
  * License: MIT
	(https://opensource.org/licenses/MIT)

Chatty
------

This application (except the parts mentioned above) is released under the
MIT License.

Copyright (c) 2014 tduva

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.


Notes on building the program yourself
======================================

The project is compiled using Gradle. Once you've checked it out, you can run
`gradlew build` to compile and run the tests, and `gradlew release` to package
the release artifacts.

If you have Hotkey Support enabled (Windows only), you need to include the
JIntellitype32.dll or the JIntellitype64.dll for the 32/64bit versions of Java
respectively (but always renamed to JIntellitype.dll). If you use the release
task mentioned above, several different zip versions are created for this.

In Chatty.java you should set your own client id which you get from Twitch. You
may also want to disable the Version Checker depending on how you will distribute
the compiled program. See the comments in Chatty.java for more information.

Windows Standalone Bundle
-------------------------

You can create a standalone Windows version (including a JRE) using the
javapackager program included in the JDK. Use the `releaseWindows` task to
build both the regular zip files and the standalone version, or the
`windowsZip` task to just build the standalone version.

You must specify the path to the javapackager program like this:
`gradlew windowsZip -PjavapackagerPath="<path_to>/javapackager.exe"`

You may also specify the path to the JRE to bundle using the `-PjrePath`
parameter, otherwise it will use the default JRE of the system.

Currently the build includes the JIntellitype32.dll, so you may have to
exchange that file if you bundle a 64bit version of Java.
