
package chatty.util.commands;

import java.awt.KeyboardFocusManager;
import java.util.Objects;
import java.util.Set;
import javax.swing.JOptionPane;

/**
 * Shows a dialog where the user can input text.
 * 
 * @author tduva
 */
public class Input implements Item {
    
    private final boolean isRequired;
    private final Item type;
    private final Item message;
    private final Item initial;
    
    public Input(Item type, Item message, Item initial, boolean isRequired) {
        this.type = type;
        this.message = message;
        this.initial = initial;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String messageValue = null;
        if (message != null) {
            messageValue = message.replace(parameters);
            if (messageValue == null) {
                return null;
            }
        }
        String initialValue = null;
        if (initial != null) {
            initialValue = initial.replace(parameters);
            if (initialValue == null) {
                return null;
            }
        }
        String result = JOptionPane.showInputDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(), messageValue, initialValue);
        if (!Item.checkReq(isRequired, result)) {
            return null;
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "Input "+type+" '"+message+"' "+initial+"";
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, type, message, initial);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, type, message, initial);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Input other = (Input) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.message, other.message)) {
            return false;
        }
        if (!Objects.equals(this.initial, other.initial)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + (this.isRequired ? 1 : 0);
        hash = 83 * hash + Objects.hashCode(this.type);
        hash = 83 * hash + Objects.hashCode(this.message);
        hash = 83 * hash + Objects.hashCode(this.initial);
        return hash;
    }
    
}
