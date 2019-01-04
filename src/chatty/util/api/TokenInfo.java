
package chatty.util.api;

import chatty.lang.Language;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds info (username, scopes) about the current access token.
 * 
 * @author tduva
 */
public class TokenInfo {
    
    public enum Scope {
        CHAT("chat_login", "chat"),
        USERINFO("user_read", "user"),
        EDITOR("channel_editor", "editor"),
        EDIT_BROADCAST("user:edit:broadcast", "broadcast"),
        COMMERICALS("channel_commercial", "commercials"),
        SUBSCRIBERS("channel_subscriptions", "subscribers"),
        FOLLOW("user_follows_edit", "follow");
        
        public String scope;
        public String label;
        public String description;
        
        Scope(String scope, String langKey) {
            this.scope = scope;
            this.label = Language.getString("login.access."+langKey);
            this.description = Language.getString("login.access."+langKey+".tip", false);
        }
        
    }
    
    public final String name;
    public final String userId;
    public final boolean valid;
    public final Set<String> scopes;
    
    public TokenInfo() {
        valid = false;
        name = null;
        userId = null;
        scopes = new HashSet<>();
    }
    
    public TokenInfo(String name, String userId, Collection<String> scopes) {
        this.name = name;
        this.userId = userId;
        valid = true;
        this.scopes = new HashSet<>(scopes);
    }
    
    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
    
    public boolean hasScope(Scope scope) {
        return scopes.contains(scope.scope);
    }
}
