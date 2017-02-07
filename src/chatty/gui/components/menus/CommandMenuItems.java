
package chatty.gui.components.menus;

import chatty.Helper;
import chatty.util.commands.CustomCommand;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author tduva
 */
public class CommandMenuItems {

    

    public enum MenuType {
        USER, CHANNEL, STREAMS
    }
    
    private static final Map<MenuType, List<CommandMenuItem>> commands = new HashMap<>();
    
    public static void setCommands(MenuType type, String data) {
        List<CommandMenuItem> parsed = parse(data);
        commands.put(type, parsed);
    }
    
    public static void addCommands(MenuType type, ContextMenu menu) {
        List<CommandMenuItem> c = commands.get(type);
        if (c != null) {
            for (CommandMenuItem item : c) {
                menu.addCommandItem(item);
            }
        }
    }
    
    public static List<CommandMenuItem> parse(String input) {
        List<CommandMenuItem> result = new LinkedList<>();
        if (input == null || input.isEmpty()) {
            return result;
        }
        String[] lines = input.split("\n");
        String currentSubmenu = null;
        for (String line : lines) {
            String submenu = parseSubmenu(line);
            if (submenu != null) {
                currentSubmenu = removeKey(submenu);
                result.add(new CommandMenuItem(removeKey(submenu), null, null, getKey(submenu)));
                continue;
            }
            if (line.trim().equals("-")) {
                result.add(new CommandMenuItem(null, null, null, null));
                continue;
            }
            if (line.trim().equals(".-")) {
                result.add(new CommandMenuItem(null, null, currentSubmenu, null));
                continue;
            }
            CommandMenuItem item = parseCommand(line, currentSubmenu);
            if (item != null) {
                result.add(item);
            } else {
                result.addAll(addCustomCommands(line, currentSubmenu));
            }
        }
        return result;
    }
    
    private static String parseSubmenu(String line) {
        line = line.trim();
        if (line.startsWith("@") && line.length() > 1) {
            return line.substring(1);
        }
        return null;
    }
    
    private static String getKey(String input) {
        if (input == null) {
            return null;
        }
        if (input.indexOf("[") > 0 && input.endsWith("]")) {
            String key = input.substring(input.lastIndexOf("[") + 1, input.length() - 1);
            return key.isEmpty() ? null : key;
        }
        return null;
    }
    
    private static String removeKey(String input) {
        if (input.indexOf("[") > 0 && input.endsWith("]")) {
            return input.substring(0, input.lastIndexOf("[")).trim();
        }
        return input;
    }
    
    public static void main(String[] args) {
        List<CommandMenuItem> items = parse("/slap\n"
                + "[Joshimuz]\n"
                + " FAQ=FAQ: http://blahblah\n"
                + "Message=/openUrl http://twitch.tv/inbox/compose?target=$$1");
        for (CommandMenuItem item : items) {
            System.out.println(item);
        }
        System.out.println(getKey("abc[e]")+" "+removeKey("abc["));
    }
    
    private static final Pattern PATTERN = Pattern.compile("([^\\[=]+)(?:\\[([^]]*)\\])?=(.+)");
    
    private static CommandMenuItem parseCommand(String line, String currentSubmenu) {
        Matcher m = PATTERN.matcher(line);
        if (!m.matches()) {
            return null;
        }
        
        String label = m.group(1);
        String key = m.group(2);
        String command = m.group(3).trim();
        //System.out.println("'"+label+"' '"+key+"' '"+command+"'");
        
        if (!label.startsWith(".")) {
            currentSubmenu = null;
        } else {
            label = label.substring(1);
        }
        
        return makeItem(label, command, currentSubmenu, key);
        
//        String[] split = line.split("=", 2);
//        if (split.length != 2) {
//            return null;
//        }
//        String label = split[0].trim();
//        String command = split[1];
//        String key = null;
//        if (label.isEmpty() || command.trim().isEmpty()) {
//            return null;
//        }
//        if (!label.startsWith(".")) {
//            currentSubmenu = null;
//        } else {
//            label = label.substring(1);
//        }
//        if (label.contains("[") && !label.startsWith("[") && label.endsWith("]")) {
//            key = label.substring(label.lastIndexOf("[")+1, label.length()-1);
//            label = label.substring(0, label.lastIndexOf("["));
//        }
//        return makeItem(label, command.trim(), currentSubmenu, key);
    }
    
//    private static final Pattern CUSTOM_COMMANDS_PATTERN
//            = Pattern.compile("(\\|)|(?:/?/?([^,\\s]+))");
    
    public static final String CUSTOM_COMMANDS_SUBMENU = "More..";
    
    private static final Pattern PATTERN_COMPACT = Pattern.compile("(\\|)|(?:(?:([0-9]+)([smhd]?)|/?/?([^\\[,\\s]+))(?:\\[([^,\\s]+)\\])?)");
    
    public static List<CommandMenuItem> addCustomCommands(String line, String parent) {
        List<CommandMenuItem> result = new LinkedList<>();
        if (!line.startsWith(".")) {
            parent = null;
        } else {
            line = line.substring(1);
        }
        Matcher matcher = PATTERN_COMPACT.matcher(line);
        boolean sep = false;
        while (matcher.find()) {
            String match = matcher.group();
            if (match.equals("|")) {
                sep = true;
            } else {
                String submenu = parent;
                CommandMenuItem item;
                String key = matcher.group(5);
                if (match.startsWith("//")) {
                    submenu = CUSTOM_COMMANDS_SUBMENU;
                }
                if (matcher.group(2) != null) {
                    String number = matcher.group(2);
                    String factor = matcher.group(3);
                    item = createTimeoutItem(number, factor, submenu, key);
                } else {
                    String command = matcher.group(4);
                    item = createItem(command, submenu, key);
                }
                if (sep) {
                    result.add(makeItem(null, null, submenu, null));
                }
                //result.add(makeItem(label, "/" + command + " $1-", submenu, null));
                result.add(item);
            }
        }
        return result;
    }
    
    private static CommandMenuItem createItem(String command, String subMenu, String key) {
        String label = Helper.replaceUnderscoreWithSpace(command);
        return makeItem(label, "/"+command+" $1-", subMenu, key);
    }
    
    private static CommandMenuItem createTimeoutItem(String number, String factor, String subMenu, String key) {
        int time = Integer.parseInt(number);
        String label;
        if (!factor.isEmpty()) {
            time *= getFactor(factor);
            label = number + factor;
        } else {
            label = timeFormat(time);
        }
        String command = "/timeout $1 "+time+" $2-";
        return makeItem(label, command, subMenu, key);
    }

    private static int getFactor(String factorString) {
        switch (factorString) {
            case "s": return 1;
            case "m": return 60;
            case "h": return 60*60;
            case "d": return 60*60*24;
            default: return 1;
        }
    }
    
    private static String timeFormat(int seconds) {
        if (seconds < 60) {
            return seconds+"s";
        }
        if (seconds < 60*60) {
            int minutes = seconds / 60;
            return String.format("%dm", (int) minutes);
        }
        if (seconds < 60*60*24*2+1) {
            return String.format("%dh", seconds / (60*60));
        }
        return String.format("%dd", seconds / (60*60*24));
    }
    
    private static CommandMenuItem makeItem(String label, String command, String submenu, String key) {
        if (command == null) {
            // For separators
            return new CommandMenuItem(null, null, submenu, key);
        }
        CustomCommand parsedCommand = CustomCommand.parse(command.trim());
        return new CommandMenuItem(label, parsedCommand, submenu, key);
    }
    
}
