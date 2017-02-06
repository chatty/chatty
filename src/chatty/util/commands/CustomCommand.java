
package chatty.util.commands;

import chatty.util.commands.Parser.Item;
import java.text.ParseException;

/**
 *
 * @author tduva
 */
public class CustomCommand {
    
    private final Parser.Items items;
    private final String error;

    private CustomCommand(Parser.Items items) {
        this.items = items;
        this.error = null;
    }

    private CustomCommand(String error) {
        this.error = error;
        this.items = null;
    }
    
    public String replace(Parameters parameters) {
        return items.replace(parameters);
    }
    
    @Override
    public String toString() {
        if (items != null) {
            return items.toString();
        }
        return error;
    }
    
    public String getError() {
        return error;
    }
    
    public boolean hasError() {
        return error != null;
    }
    
    public boolean containsIdentifier(String prefix) {
        return items.containsIdentifier(prefix);
    }
    
    public String getCommand() {
        if (items != null && !items.isEmpty()) {
            Item firstToken = items.getItem(0);
            if (firstToken instanceof Parser.Literal) {
                String text = ((Parser.Literal)firstToken).getLiteral();
                if (text.startsWith("/") && text.contains(" ")) {
                    return text.substring(1, text.indexOf(" "));
                }
            }
        }
        return null;
    }

    public static CustomCommand parse(String input) {
        Parser parser = new Parser(input);
        try {
            return new CustomCommand(parser.parse());
        } catch (ParseException ex) {
            return new CustomCommand("Error: "+ex.getLocalizedMessage());
        }
    }
    
    public static void main(String[] args) {
        CustomCommand command = CustomCommand.parse("$join");
        System.out.println(command);
    }
    
}
