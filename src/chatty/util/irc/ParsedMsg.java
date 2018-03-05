
package chatty.util.irc;

import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class ParsedMsg {
    
    private static final Logger LOGGER = Logger.getLogger(ParsedMsg.class.getName());
    
    private final MsgTags tags;
    private final String prefix;
    private final String nick;
    private final String command;
    private final MsgParameters parameters;
    
    private ParsedMsg(MsgTags tags, String prefix, String nick, String command, MsgParameters parameters) {
        this.tags = tags;
        this.prefix = prefix;
        this.nick = nick;
        this.command = command;
        this.parameters = parameters;
    }
    
    public MsgTags getTags() {
        return tags;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public String getNick() {
        return nick;
    }
    
    public String getCommand() {
        return command;
    }
    
    public MsgParameters getParameters() {
        return parameters;
    }
    
    public static ParsedMsg parse(String input) {
        if (input == null) {
            return null;
        }
        
        //------
        // Tags
        //------
        MsgTags tags = MsgTags.EMPTY;
        if (input.startsWith("@")) {
            int endOfTags = input.indexOf(" ");
            if (endOfTags == -1) {
                LOGGER.warning("Parsing error: Couldn't find whitespace after tags: "+input);
                return null;
            }
            tags = MsgTags.parse(input.substring(1, endOfTags));
            input = input.substring(endOfTags+1);
        }
        //System.out.println("Tags: "+tags);
        
        //--------
        // Prefix
        //--------
        String prefix = "";
        String command = null;
        MsgParameters parameters = new MsgParameters();
        
        int endOfPrefix = -1;
        
        // Get prefix if available
        if (input.startsWith(":")) {
            endOfPrefix = input.indexOf(" ");
            if (endOfPrefix == -1) {
                LOGGER.warning("Parsing error: Couldn't find whitespace after prefix: "+input);
                return null;
            }
            prefix = input.substring(1,endOfPrefix);
        }
        
        //------------
        // Parameters
        //------------
        int next = endOfPrefix;
        do {
            int start = next+1;
            // Stop if already at the end
            if (start == input.length()) {
                break;
            }
            // Find next space
            next = input.indexOf(" ", start);
            int end;
            if (next == -1 || input.charAt(start) == ':') {
                // No further space found or trailing start, so until string end
                if (input.charAt(start) == ':') {
                    // Cut off trailing indicator
                    start++;
                }
                end = input.length();
                next = -1;
            } else {
                end = next;
            }
            // Set as command if not set yet, as parameter otherwise
            String part = input.substring(start, end);
            if (!part.isEmpty()) {
                if (command == null) {
                    command = part;
                } else {
                    parameters.add(part);
                }
            }
        } while (next != -1);
        
        if (command == null) {
            return null;
        }
        return new ParsedMsg(tags, prefix, getNickFromPrefix(prefix), command, parameters);
    }
    
    /**
     * Extracts the nick from the prefix (like nick!mail@host)
     * 
     * @param sender
     * @return 
     */
    private static String getNickFromPrefix(String sender) {
        int endOfNick = sender.indexOf("!");
        if (endOfNick == -1) {
            return sender;
        }
        return sender.substring(0, endOfNick);
    }
    
    @Override
    public String toString() {
        return tags+"/"+prefix+"/"+nick+"/"+command+"/"+parameters;
    }
    
}
