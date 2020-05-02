
package chatty.util.commands;

import java.util.Objects;
import java.util.Set;

/**
 * Allows simple calculactions.
 * 
 * @author tduva
 */
public class Calc implements Item {
    
    private final Item item;
    private final boolean isRequired;

    public Calc(Item item, boolean isRequired) {
        this.item = item;
        this.isRequired = isRequired;
    }

    @Override
    public String replace(Parameters parameters) {
        String value = item.replace(parameters);
        if (!Item.checkReq(isRequired, value)) {
            return null;
        }
        try {
            double d = eval(value);
            if ((int)d == d) {
                return String.valueOf((int)d);
            }
            return String.valueOf(d);
        }
        catch (Exception ex) {
            return ex.getLocalizedMessage();
        }
    }

    @Override
    public String toString() {
        return "Calc " + item;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, item);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, item);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Calc other = (Calc) obj;
        if (!Objects.equals(this.item, other.item)) {
            return false;
        }
        if (this.isRequired != other.isRequired) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.item);
        hash = 47 * hash + (this.isRequired ? 1 : 0);
        return hash;
    }
    
    
    /**
     * https://stackoverflow.com/a/26227947
     * "All code in this answer released to the public domain. Have fun!"
     * 
     * @param str
     * @return 
     */
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') {
                    nextChar();
                }
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }
                return x;
            }

        // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor
            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) {
                        x += parseTerm(); // addition
                    }
                    else if (eat('-')) {
                        x -= parseTerm(); // subtraction
                    }
                    else {
                        return x;
                    }
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) {
                        x *= parseFactor(); // multiplication
                    }
                    else if (eat('/')) {
                        x /= parseFactor(); // division
                    }
                    else {
                        return x;
                    }
                }
            }

            double parseFactor() {
                if (eat('+')) {
                    return parseFactor(); // unary plus
                }
                if (eat('-')) {
                    return -parseFactor(); // unary minus
                }
                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                }
                else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') {
                        nextChar();
                    }
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                }
                else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') {
                        nextChar();
                    }
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) {
                        x = Math.sqrt(x);
                    }
                    else if (func.equals("sin")) {
                        x = Math.sin(Math.toRadians(x));
                    }
                    else if (func.equals("cos")) {
                        x = Math.cos(Math.toRadians(x));
                    }
                    else if (func.equals("tan")) {
                        x = Math.tan(Math.toRadians(x));
                    }
                    else {
                        throw new RuntimeException("Unknown function: " + func);
                    }
                }
                else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) {
                    x = Math.pow(x, parseFactor()); // exponentiation
                }
                return x;
            }
        }.parse();
    }
    
}
