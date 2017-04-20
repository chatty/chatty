
package chatty.gui.components.menus;

import chatty.util.commands.CustomCommand;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class CommandMenuItemsTest {
    
    @Test
    public void testParse() {
        
        assertEquals(CommandMenuItems.parse(null), new ArrayList<>());
        assertEquals(CommandMenuItems.parse(""), new ArrayList<>());
        
        String input1 = ""
                + "Test=/Hello\n"
                + "Test{0}=/Hello\n"
                + "Test{0}[k]=/Hello\n"
                + "Test[k]=/Hello\n"
                + "Test [k]=/Hello\n"
                + "/Test /Test{0} /Test{0}[k] /Test[k]\n"
                + "/Hello_World\n"
                + "@Menu Name\n"
                + "@Menu Name{0}\n"
                + "@Menu Name{0}[k]\n"
                + ".-\n"
                + "-\n"
                + "-{0}\n"
                + "-{1}\n"
                + ".-{0}\n"
                + "1m 1m[ctrl+t] 1m[ctrl+t|ct] 1m{0}\n"
                + "./Test //Test\n"
                + "\n"
                + ". Test = /Test $1- \n"
                + "60s 1m 60\n"
                + "Uptime=Stream Uptime: $(streamuptime)";
        
        List<CommandMenuItem> expected1 = new LinkedList<>();
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Hello"), null, -1, null));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Hello"), null, 0, null));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Hello"), null, 0, "k"));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Hello"), null, -1, "k"));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Hello"), null, -1, "k"));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Test $1-"), null, -1, null));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Test $1-"), null, 0, null));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Test $1-"), null, 0, "k"));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Test $1-"), null, -1, "k"));
        expected1.add(new CommandMenuItem("Hello World", CustomCommand.parse("/Hello_World $1-"), null, -1, null));
        expected1.add(new CommandMenuItem("Menu Name", null, null, -1, null));
        expected1.add(new CommandMenuItem("Menu Name", null, null, 0, null));
        expected1.add(new CommandMenuItem("Menu Name", null, null, 0, "k"));
        expected1.add(new CommandMenuItem(null, null, "Menu Name", -1, null));
        expected1.add(new CommandMenuItem(null, null, null, -1, null));
        expected1.add(new CommandMenuItem(null, null, null, 0, null));
        expected1.add(new CommandMenuItem(null, null, null, 1, null));
        expected1.add(new CommandMenuItem(null, null, "Menu Name", 0, null));
        expected1.add(new CommandMenuItem("1m", CustomCommand.parse("/timeout $1 60 $2-"), null, -1, null));
        expected1.add(new CommandMenuItem("1m", CustomCommand.parse("/timeout $1 60 $2-"), null, -1, "ctrl+t"));
        expected1.add(new CommandMenuItem("1m", CustomCommand.parse("/timeout $1 60 $2-"), null, -1, "ctrl+t|ct"));
        expected1.add(new CommandMenuItem("1m", CustomCommand.parse("/timeout $1 60 $2-"), null, 0, null));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Test $1-"), "Menu Name", -1, null));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Test $1-"), CommandMenuItems.CUSTOM_COMMANDS_SUBMENU, -1, null));
        expected1.add(new CommandMenuItem("Test", CustomCommand.parse("/Test $1-"), "Menu Name", -1, null));
        expected1.add(new CommandMenuItem("60s", CustomCommand.parse("/timeout $1 60 $2-"), null, -1, null));
        expected1.add(new CommandMenuItem("1m", CustomCommand.parse("/timeout $1 60 $2-"), null, -1, null));
        expected1.add(new CommandMenuItem("1m", CustomCommand.parse("/timeout $1 60 $2-"), null, -1, null));
        expected1.add(new CommandMenuItem("Uptime", CustomCommand.parse("Stream Uptime: $(streamuptime)"), null, -1, null));
        
        assertEquals(CommandMenuItems.parse(input1), expected1);
    }
    
}
