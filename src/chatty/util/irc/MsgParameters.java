
package chatty.util.irc;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tduva
 */
public class MsgParameters {
    
    private final List<String> parameters = new ArrayList<>(2);
    
    public void add(String parameter) {
        parameters.add(parameter);
    }
    
    public boolean has(int index) {
        return index < parameters.size();
    }
    
    public String get(int index) {
        return parameters.get(index);
    }
    
    public int size() {
        return parameters.size();
    }
    
    public boolean isChan(int index) {
        return getOrEmpty(index).startsWith("#");
    }
    
    public String getOrEmpty(int index) {
        return has(index) ? get(index) : "";
    }
    
    public boolean isEmpty(int index) {
        return getOrEmpty(index).isEmpty();
    }
    
    @Override
    public String toString() {
        return parameters.toString();
    }
    
}
