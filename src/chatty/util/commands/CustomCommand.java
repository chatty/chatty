
package chatty.util.commands;

import java.text.ParseException;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class CustomCommand {
    
    private final Items items;
    private final String error;

    private CustomCommand(Items items) {
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
    
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return items.getIdentifiersWithPrefix(prefix);
    }
    
    public String getCommandName() {
        if (items != null && !items.isEmpty()) {
            Item firstToken = items.getItem(0);
            if (firstToken instanceof Literal) {
                String text = ((Literal)firstToken).getLiteral();
                if (text.startsWith("/")) {
                    if (text.contains(" ")) {
                        return text.substring(1, text.indexOf(" "));
                    } else {
                        return text.substring(1);
                    }
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
            return new CustomCommand(ex.getLocalizedMessage());
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CustomCommand other = (CustomCommand) obj;
        if (!Objects.equals(this.items, other.items)) {
            return false;
        }
        if (!Objects.equals(this.error, other.error)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.items);
        hash = 73 * hash + Objects.hashCode(this.error);
        return hash;
    }

    public static void main(String[] args) {
        CustomCommand command = CustomCommand.parse("$join");
        System.out.println(command);
    }
    
}
