
package chatty.gui.components.menus;

import chatty.Helper;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.CustomCommands;
import chatty.util.commands.Parameters;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author tduva
 */
public class CommandMenuItems {

    private static final Logger LOGGER = Logger.getLogger(CommandMenuItems.class.getName());

    public enum MenuType {
        USER, CHANNEL, STREAMS, TEXT, ADMIN
    }
    
    private static final Map<MenuType, List<CommandMenuItem>> commands = new HashMap<>();
    public static CustomCommands customCommands;
    
    public static void setCommands(MenuType type, String data) {
        List<CommandMenuItem> parsed = parse(data);
        commands.put(type, parsed);
    }
    
    public static void addCommands(MenuType type, ContextMenu menu, Parameters baseParameters) {
        List<CommandMenuItem> c = commands.get(type);
        if (c != null) {
            for (CommandMenuItem item : c) {
                menu.addCommandItem(item, getMenuParameters(item, baseParameters));
            }
        }
        menu.removeEmpty();
    }
    
    /**
     * Create Parameters for menu labels with replacements and for restrictions.
     *
     * @param item The menu entry to get the Parameters for
     * @param baseParameters Context-specific Parameters provided by the menu
     * @return The Parameters object, or null if none are needed
     */
    public static Parameters getMenuParameters(CommandMenuItem item, Parameters baseParameters) {
        Parameters parameters = null;
        // Check if parameters are needed at all
        if (ContextMenuHelper.settings != null
                && (ContextMenuHelper.settings.getBoolean("menuCommandLabels")
                    || ContextMenuHelper.settings.getBoolean("menuRestrictions"))) {
            parameters = baseParameters != null ? baseParameters.copy() : Parameters.create("");
            parameters.putObject("settings", ContextMenuHelper.settings);
            
            // Enable features based on settings
            if (ContextMenuHelper.settings.getBoolean("menuCommandLabels")) {
                parameters.put("menuCommandLabels", "1");
            }
            if (ContextMenuHelper.settings.getBoolean("menuRestrictions")) {
                parameters.put("menuRestrictions", "1");
            }
            
            // Add values (for each command to get custom identifiers)
            if (item.hasValidLabelCommand() && customCommands != null) {
                customCommands.addCustomIdentifierParametersForCommand(item.getLabelCommand(), parameters);
            }
            if (item.hasRestrictionCommands() && customCommands != null) {
                for (CustomCommand cc : item.getRestrictionCommands()) {
                    // For this we only know that there *are* commands, so check
                    if (!cc.hasError()) {
                        customCommands.addCustomIdentifierParametersForCommand(cc, parameters);
                    }
                }
            }
        }
        return parameters;
    }
    
    public static List<CommandMenuItem> parse(String input) {
        List<CommandMenuItem> result = new LinkedList<>();
        if (input == null || input.isEmpty()) {
            return result;
        }
        String[] lines = input.split("\n");
        String submenuName = null;
        Map<String, CustomCommand> currentRestrictions = new HashMap<>();
        int lineNumber = 1;
        for (String line : lines) {
            CommandMenuItem submenu;
            CommandMenuItem separator;
            CommandMenuItem item;
            if (parseRestriction(line, currentRestrictions)) {
                // Do nothing, restrictions are already added to the map
            }
            else if ((submenu = parseSubmenu(line, lineNumber, currentRestrictions.values())) != null) {
                submenuName = submenu.getLabel();
                result.add(submenu);
                currentRestrictions.remove(null);
            }
            else if ((separator = parseSeparator(line, lineNumber, submenuName, currentRestrictions.values())) != null) {
                result.add(separator);
                currentRestrictions.remove(null);
            }
            else if ((item = parseCommand(line, lineNumber, submenuName, currentRestrictions.values())) != null) {
                result.add(item);
                currentRestrictions.remove(null);
            }
            else {
                result.addAll(addCustomCommands(line, lineNumber, submenuName, currentRestrictions.values()));
                currentRestrictions.remove(null);
            }
            lineNumber++;
        }
        return result;
    }
    
    private static final Pattern RESTRICTION_PATTERN = Pattern.compile("\\[(?<id>\\S+)? (?<restriction>.+) \\]|\\[/(?<idEnd>\\S+)\\]");
    
    private static boolean parseRestriction(String line, Map<String, CustomCommand> currentRestrictions) {
        line = line.trim();
        Matcher m = RESTRICTION_PATTERN.matcher(line);
        if (m.matches()) {
            String restriction = m.group("restriction");
            String id = m.group("id");
            String idEnd = m.group("idEnd");
            if (restriction != null) {
                // id may be null, which is used for "next entry only" restrictions
                currentRestrictions.put(id, CustomCommand.parse(restriction));
            }
            else {
                currentRestrictions.remove(idEnd);
            }
            return true;
        }
        return false;
    }
    
    private static final String POS_KEY_PATTERN = "(?:\\{(\\d+)\\})?(?:\\[([^]]*)\\])?";
    
    private static final Pattern SUBMENU_PATTERN = Pattern.compile("@([^\\[{]+)"+POS_KEY_PATTERN);
    
    private static CommandMenuItem parseSubmenu(String line, int lineNumber, Collection<CustomCommand> restrictions) {
        line = line.trim();
        Matcher m = SUBMENU_PATTERN.matcher(line);
        if (m.matches()) {
            String name = m.group(1).trim();
            int pos = m.group(2) == null ? -1 : Integer.parseInt(m.group(2));
            String key = m.group(3);
            return new CommandMenuItem(name, null, null, pos, key, restrictions, lineNumber);
        }
        return null;
    }
    
    private static final Pattern SEPARTOR_PATTERN = Pattern.compile("(\\.)?-"+POS_KEY_PATTERN);
    
    private static CommandMenuItem parseSeparator(String line, int lineNumber, String currentSubmenu, Collection<CustomCommand> restrictions) {
        line = line.trim();
        Matcher m = SEPARTOR_PATTERN.matcher(line);
        if (m.matches()) {
            String submenu = m.group(1) != null ? currentSubmenu : null;
            int pos = m.group(2) == null ? -1 : Integer.parseInt(m.group(2));
            return new CommandMenuItem(null, null, submenu, pos, null, restrictions, lineNumber);
        }
        return null;
    }
    
    public static void main(String[] args) {
        List<CommandMenuItem> items = parse("/slap\n"
                + "[Joshimuz]\n"
                + " FAQ=FAQ: http://blahblah\n"
                + "Message=/openUrl http://twitch.tv/inbox/compose?target=$$1");
        for (CommandMenuItem item : items) {
            System.out.println(item);
        }
        System.out.println(true || false && false);
    }
    
    private static final Pattern PATTERN = Pattern.compile("([^\\[{=]+)"+POS_KEY_PATTERN+"=(.+)");
    
    private static CommandMenuItem parseCommand(String line, int lineNumber, String currentSubmenu, Collection<CustomCommand> restrictions) {
        Matcher m = PATTERN.matcher(line);
        if (!m.matches()) {
            return null;
        }
        
        String label = m.group(1).trim();
        int pos = m.group(2) == null ? -1 : Integer.parseInt(m.group(2));
        String key = m.group(3);
        String command = m.group(4).trim();
        //System.out.println("'"+label+"' '"+key+"' '"+command+"'");
        
        if (!label.startsWith(".")) {
            currentSubmenu = null;
        } else {
            label = label.substring(1).trim();
        }
        return makeItem(label, command, currentSubmenu, pos, key, restrictions, lineNumber);
    }
    
    public static final String CUSTOM_COMMANDS_SUBMENU = "More..";
    
    private static final Pattern PATTERN_COMPACT = Pattern.compile(
            "(\\|)|(?:(?:([0-9]+)([smhd]?)|/?/?([^\\[{,\\s]+))(?:\\{(\\d+)\\})?(?:\\[([^,\\s]+)\\])?)");
    
    public static List<CommandMenuItem> addCustomCommands(String line, int lineNumber, String parent, Collection<CustomCommand> restrictions) {
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
                int pos = matcher.group(5) == null ? - 1 : Integer.parseInt(matcher.group(5));
                String key = matcher.group(6);
                if (match.startsWith("//")) {
                    submenu = CUSTOM_COMMANDS_SUBMENU;
                }
                if (matcher.group(2) != null) {
                    String number = matcher.group(2);
                    String factor = matcher.group(3);
                    item = createTimeoutItem(number, factor, submenu, pos, key, restrictions, lineNumber);
                } else {
                    String command = matcher.group(4);
                    item = createItem(command, submenu, pos, key, restrictions, lineNumber);
                }
                if (sep) {
                    result.add(makeItem(null, null, submenu, -1, null, restrictions, lineNumber));
                    sep = false;
                }
                result.add(item);
            }
        }
        return result;
    }
    
    private static CommandMenuItem createItem(String command, String subMenu, int pos, String key, Collection<CustomCommand> restrictions, int lineNumber) {
        String label = Helper.replaceUnderscoreWithSpace(command);
        return makeItem(label, "/"+command+" $1-", subMenu, pos, key, restrictions, lineNumber);
    }
    
    private static CommandMenuItem createTimeoutItem(String number, String factor, String subMenu, int pos, String key, Collection<CustomCommand> restrictions, int lineNumber) {
        int time = Integer.parseInt(number);
        String label;
        if (!factor.isEmpty()) {
            time *= getFactor(factor);
            label = number + factor;
        } else {
            label = timeFormat(time);
        }
        String command = "/timeout $1 "+time+" $2-";
        return makeItem(label, command, subMenu, pos, key, restrictions, lineNumber);
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
    
    private static CommandMenuItem makeItem(String label, String command, String submenu, int pos, String key, Collection<CustomCommand> restrictions, int lineNumber) {
        if (command == null) {
            // For separators
            return new CommandMenuItem(null, null, submenu, pos, key, restrictions, lineNumber);
        }
        CustomCommand parsedCommand = CustomCommand.parse(command.trim());
        return new CommandMenuItem(label, parsedCommand, submenu, pos, key, restrictions, lineNumber);
    }
    
}
