Chatty
======

Chatty is a Twitch Chat Client for Desktop written in Java featuring many
Twitch specific features.

* Website: https://chatty.github.io
* E-Mail: chattyclient@gmail.com
* Twitter: @ChattyClient (https://twitter.com/ChattyClient)
* YouTube: https://www.youtube.com/user/chattyclient
* Discord: https://discord.gg/WTuqGeJ

I learned about most of the Java techniques and APIs used in this during
development, so many things won't be designed ideally. I also never
released such a project as opensource before, so if I missed anything or
didn't adhere to some license correctly, please tell me.

Download
========

Go to the [website](https://chatty.github.io) for ready to use downloads
and more information on the features.

Contributions
=============

Contributions to Chatty under the terms of the GPLv3 License (or compatible) are
welcome.

If you're contributing code that you didn't write yourself, make sure to adhere
to whatever license terms it is under (like retaining copyright notices) and to
detail that in the Pull Request.

Obviously, I won't be able to accept all contributions, for example I may
already be working on something similiar. If you plan to put more than a little
effort into a Pull Request, consider asking first if what you're doing has a
chance of being added.

License Information
===================

Chatty, as a whole, is released under the GPLv3 or later (see included
`LICENSE` file).

    Copyright (C) 2017-2020  tduva and contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

Partly based on source code (most files located in the `src/chatty/` directory
and subdirectories) licensed under the MIT license.

    Copyright (c) 2014-2017  tduva and contributors

    Permission is hereby granted, free of charge, to any person obtaining a
    copy of this software and associated documentation files (the "Software"),
    to deal in the Software without restriction, including without limitation
    the rights to use, copy, modify, merge, publish, distribute, sublicense,
    and/or sell copies of the Software, and to permit persons to whom the
    Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
    FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
    DEALINGS IN THE SOFTWARE.

External Libraries
------------------

* JSON-Simple:
  * Files under `assets/lib/`: `json-simple-*.jar`, `json-simple-tag_release_1_1_1.zip`
  * Website: <https://code.google.com/p/json-simple/>
  * License: "Apache License 2.0"
	(for the license text see the APACHE_LICENSE file
	or <http://www.apache.org/licenses/LICENSE-2.0>).

* JKeyMaster (modified):
  * Files under `assets/lib/`: `jkeymaster-*`
  * Website: <https://github.com/tulskiy/jkeymaster>
  * License: "LGPL-3.0 License"
  * Using JNA (<https://github.com/java-native-access/jna>) under "Apache License 2.0"

* JTattoo by MH Software-Entwicklung:
  * Files under `assets/lib/`: `JTattoo-*.jar`, `JTattoo-*-sources.zip`
  * Website: <http://www.jtattoo.net>
  * License: GPLv2 or later (see <https://www.gnu.org/licenses/gpl-2.0.txt>)
  * Based on v1.6.12 with some modifications by tduva

* Java-Websocket:
  * Files under `assets/lib/`: `JavaWebsocket-*.jar`
  * Website: <https://github.com/TooTallNate/Java-WebSocket>
  * License: "MIT"
	(see <https://github.com/TooTallNate/Java-WebSocket/blob/master/LICENSE>)
  * Requires SLF4J (http://www.slf4j.org/, MIT, `slf4j-*.jar`)

* Txtmark
  * Files under `assets/lib/`: `txtmark-0.13.jar`, `txtmark-txtmark-0.13.zip`
  * Website: <https://github.com/rjeschke/txtmark>
  * License: "Apache License 2.0"
	(for the license text see the APACHE_LICENSE file
	or <http://www.apache.org/licenses/LICENSE-2.0>).

* Additional external/modified libraries integrated under `src/chatty/` have license/source
  information in the file header

Images / Other
--------------

* Favorites Icon by Everaldo Coelho:
  * File (in various folders): `star.png`
  * Source: <https://www.iconfinder.com/icons/17999/bookmark_favorite_star_icon>
  * License: LGPL
	(for the license text see the LGPL file or
	<http://www.gnu.org/licenses/lgpl.html>)

* Misc Icons from the Tango Icon Theme:
  * Files (in various folders): `list-add.png, list-remove.png, view-refresh.png,
		help-browser.png, preferences-system.png,
		dialog-warning.png, go-down.png, go-up.png, go-next.png,
		go-previous.png, go-home.png, go-web.png,
		image-icon.png, commandline.png, edit-copy.png, sort.png,
		edit-all.png (edited), reply.png (edited)`
  * Source: <http://tango.freedesktop.org/Tango_Icon_Library>
  * License: Released into the Public Domain

* Misc Icons from NUVOLA ICON THEME for KDE 3.x
		by David Vignoni:
  * Files (in various folders): `edit.png, ok.png, no.png, colorpicker.png,
		warning.png, download.png, game.png`
  * Source: <http://www.icon-king.com/projects/nuvola/>
  * License: LGPL
	(for the license text see the LGPL file or
	http://www.gnu.org/licenses/lgpl.html)

* Robot Icon by Yusuke Kamiyamane:
  * File (in various folders): `icon_bot.png`
  * Source: https://www.iconfinder.com/icons/46205/robot_icon
  * License: CC-BY 3.0
	(http://creativecommons.org/licenses/by/3.0/)

* Twitter Emoji Images:
  * Files: `gui/emoji/twemoji/*`
  * Source: <https://github.com/twitter/twemoji>
  * License: CC-BY 4.0
	(https://creativecommons.org/licenses/by/4.0/)

* Emoji One Images:
  * Files: `gui/emoji/e1/*`
  * Source: <http://emojione.com/>
  * License: CC-BY 4.0
	(<https://creativecommons.org/licenses/by/4.0/>)
  * Old version (seems no longer available), not updated since newer
    versions are not under a free license

* Emoji Metadata based on:
  * Source: https://github.com/github/gemoji/blob/master/db/emoji.json
  * License: MIT
	(<https://opensource.org/licenses/MIT>)

* Emoji Metadata supplemented by:
  * Source: https://github.com/joypixels/emojione/blob/master/emoji.json
  * License: MIT
	(<https://opensource.org/licenses/MIT>)

* Example Sounds by tduva:
  * Files: `assets/sounds/*`
  * Source: Recorded myself
  * License: CC-BY 4.0
	(<https://creativecommons.org/licenses/by/4.0/>)

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

