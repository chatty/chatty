
package chatty.util.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Allows adding a listener to be informed about certain API request results.
 * 
 * This is similar to the TwitchApiResultListener whose results are consumed in
 * various places, except that this is more flexible since objects can add a
 * listener if they want all the data from certain API requests rather than
 * having to send it to them from a central listener.
 * 
 * @author tduva
 */
public class ResultManager {

    private final Map<Type, Set<Object>> listeners = new HashMap<>();
    private final Map<Object, Object> uniqueListeners = new HashMap<>();
    
    public enum Type {
        CATEGORY_RESULT(CategoryResult.class),
        SHIELD_MODE_RESULT(ShieldModeResult.class),
        CREATE_CLIP(CreateClipResult.class);
        
        private final Class c;
        
        Type(Class c) {
            this.c = c;
        }
    }
    
    public void subscribe(Type type, Object listener) {
        subscribe(type, null, listener);
    }
    
    /**
     * Subscribe to the given type, but remove any previous listener with the
     * same unique object provided.
     * 
     * @param type
     * @param unique If provided remove previous listener added with this object
     * @param listener 
     */
    public void subscribe(Type type, Object unique, Object listener) {
        if (listener != null) {
            if (!type.c.isInstance(listener)) {
                throw new RuntimeException("Invalid parameter");
            }
            if (!listeners.containsKey(type)) {
                listeners.put(type, new HashSet<>());
            }
            if (unique != null) {
                Object prevListener = uniqueListeners.remove(unique);
                if (prevListener != null) {
                    listeners.forEach((k,v) -> v.remove(prevListener));
                }
                uniqueListeners.put(unique, listener);
            }
            listeners.get(type).add(listener);
        }
    }
    
    @SuppressWarnings("unchecked") // Cast
    public <T> void inform(Type type, Consumer<T> func) {
        if (!listeners.containsKey(type)) {
            return;
        }
        for (Object listener : listeners.get(type)) {
            func.accept((T) listener);
        }
    }
    
    public interface CategoryResult {
        public void result(Collection<StreamCategory> categories);
    }
    
    public interface ShieldModeResult {
        public void result(String stream, boolean enabled);
    }
    
    public interface CreateClipResult {
        public void result(String editUrl, String viewUrl, String error);
    }
    
    public static void main(String[] args) {
        ResultManager m = new ResultManager();
        m.subscribe(Type.CATEGORY_RESULT, (CategoryResult) categories -> {
            System.out.println(categories);
        });
        m.subscribe(Type.CATEGORY_RESULT, (CategoryResult) categories -> {
            System.out.println("b"+categories);
        });
//        m.subscribe(Type.CATEGORY_SEARCH_RESULT, new Consumer<Object> () {
//            @Override
//            public void accept(Object t) {
//                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//            }
//        });
        m.inform(Type.CATEGORY_RESULT, (CategoryResult result) -> {
            result.result(null);
        });
        
        Object o = new CategoryResult() {
            @Override
            public void result(Collection<StreamCategory> categories) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }
    
}
