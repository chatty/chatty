
package chatty.util.commands;

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
            return "'"+literal+"'";
        }
        
        public String getLiteral() {
            return literal;
        }

        @Override
        public Set<String> getIdentifiersWithPrefix(String prefix) {
            return null;
        }
        
    }