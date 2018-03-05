
package chatty.util.irc;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class ParsedMsgTest {
    
    @Test
    public void test() {
        test(":jtv MODE #lotsofs +o da00",
                "jtv", "jtv", "MODE",
                "#lotsofs", "+o", "da00");
        
        test(":jtv MODE #lotsofs +o",
                "jtv", "jtv", "MODE",
                "#lotsofs", "+o");
        
        test("jtv MODE #lotsofs +o da00",
                "", "", "jtv",
                "MODE", "#lotsofs", "+o", "da00");
        
        test("MODE #lotsofs +o da00",
                "", "", "MODE",
                "#lotsofs", "+o", "da00");
        
        test(": MODE #lotsofs +o da00",
                "", "", "MODE",
                "#lotsofs", "+o", "da00");
        
        test(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs :EMOTESET tduva [130,793,33]",
                "jtv!jtv@jtv.tmi.twitch.tv", "jtv", "PRIVMSG",
                "#lotsofs", "EMOTESET tduva [130,793,33]");
        
        test(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs :",
                "jtv!jtv@jtv.tmi.twitch.tv", "jtv", "PRIVMSG",
                "#lotsofs");
        
        test(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs",
                "jtv!jtv@jtv.tmi.twitch.tv", "jtv", "PRIVMSG",
                "#lotsofs");
        
        test(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG     #lotsofs   ",
                "jtv!jtv@jtv.tmi.twitch.tv", "jtv", "PRIVMSG",
                "#lotsofs");
        
        test(" @tag=value;tag2=value2 :jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs :test invalid start",
                "", "", "@tag=value;tag2=value2",
                "jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs :test invalid start");
        
        test("PING :tmi.twitch.tv",
                "", "", "PING",
                "tmi.twitch.tv");
        
        test("PING :",
                "", "", "PING");
        
        test("PING",
                "", "", "PING");
        
        test(":tduva!tduva@tduva.tmi.twitch.tv JOIN #lotsofs",
                "tduva!tduva@tduva.tmi.twitch.tv", "tduva", "JOIN",
                "#lotsofs");
        
        test(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs :"+(char)1+"ACTION action",
                "jtv!jtv@jtv.tmi.twitch.tv", "jtv", "PRIVMSG",
                "#lotsofs", (char)1+"ACTION action");

        test(":tduva.tmi.twitch.tv 353 tduva = #lotsofs :krazyrasmus deenglow r4m80 adoms",
                "tduva.tmi.twitch.tv", "tduva.tmi.twitch.tv", "353",
                "tduva", "=", "#lotsofs", "krazyrasmus deenglow r4m80 adoms");
        
        test(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG :tduvatest is now hosting you for 0 viewers. [0]",
                "jtv!jtv@jtv.tmi.twitch.tv", "jtv", "PRIVMSG",
                "tduvatest is now hosting you for 0 viewers. [0]");
        
        test(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG  :tduvatest is now hosting you for 0 viewers. [0]",
                "jtv!jtv@jtv.tmi.twitch.tv", "jtv", "PRIVMSG",
                "tduvatest is now hosting you for 0 viewers. [0]");
        
        test(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG tduva :この名前は使用できません is now hosting you.",
                "jtv!jtv@jtv.tmi.twitch.tv", "jtv", "PRIVMSG",
                "tduva", "この名前は使用できません is now hosting you.");

        assertNull(ParsedMsg.parse(""));
        assertNull(ParsedMsg.parse(" "));
        assertNull(ParsedMsg.parse(":"));
        assertNull(ParsedMsg.parse(null));
    }
    
    private void test(String input, String prefix, String nick, String command, String... parameters) {
        ParsedMsg p = ParsedMsg.parse(input);
        assertEquals(p.getPrefix(), prefix);
        assertEquals(p.getNick(), nick);
        assertEquals(p.getCommand(), command);
        for (int i = 0; i < parameters.length; i++) {
            assertEquals(p.getParameters().get(i), parameters[i]);
        }
        assertEquals(p.getParameters().size(), parameters.length);
    }
    
}
