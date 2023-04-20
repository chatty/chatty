
package chatty.util.commands;

import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tduva
 */
class Json implements Item {
    
    private static final Logger LOGGER = Logger.getLogger(Json.class.getName());
    
    private final boolean isRequired;
    private final Item input;
    private final Item output;
    
    public Json(Item input, Item output, boolean isRequired) {
        this.input = input;
        this.output = output;
        this.isRequired = isRequired;
    }

    @Override
    public String replace(Parameters parameters) {
        String inputValue = input.replace(parameters);
        if (!Item.checkReq(isRequired, inputValue)) {
            return null;
        }
        
        try {
            JSONParser parser = new JSONParser();
            Parameters subParams = parameters.copy();
            subParams.putObject("json", parser.parse(inputValue));
            String outputValue = output.replace(subParams);
            if (!Item.checkReq(isRequired, outputValue)) {
                return null;
            }
            return outputValue;
        }
        catch (ParseException | NumberFormatException ex) {
            LOGGER.warning("Error parsing JSON: "+ex);
            return null;
        }
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, input, output);
    }

    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, input, output);
    }
    
    @Override
    public String toString() {
        return String.format("JSON %s/%s", input, output);
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
        final Json other = (Json) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.input, other.input)) {
            return false;
        }
        return Objects.equals(this.output, other.output);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + (this.isRequired ? 1 : 0);
        hash = 67 * hash + Objects.hashCode(this.input);
        hash = 67 * hash + Objects.hashCode(this.output);
        return hash;
    }

}
