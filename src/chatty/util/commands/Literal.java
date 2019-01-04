
package chatty.util.commands;

import java.util.Objects;
import java.util.Set;

/**
 * The simplest kind of Item, which simply returns a static String, completely
 * ignoring any given Parameters.
 * 
 * @author tduva
 */
class Literal implements Item {

    private final String literal;

    public Literal(String literal) {
        this.literal = literal;
    }

    @Override
    public String replace(Parameters parameters) {
        return literal;
    }

    @Override
    public String toString() {
        return "'" + literal + "'";
    }

    public String getLiteral() {
        return literal;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return null;
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Literal other = (Literal)obj;
        return literal.equals(other.literal);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.literal);
        return hash;
    }

}
