
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
        FOLLOW("user_follows_edit", "follow"),
        SUBSCRIPTIONS("user_subscriptions", "subscriptions"),
        CHAN_MOD("channel:moderate", "chanMod"),
        AUTOMOD("moderator:manage:automod", "automod"),
        POINTS("channel:read:redemptions", "points");
        
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
    public final String clientId;
    public final Set<String> scopes;
    public final long expiresIn;
    
    public TokenInfo() {
        valid = false;
        name = null;
        userId = null;
        clientId = null;
        scopes = new HashSet<>();
        expiresIn = -1;
    }
    
    public TokenInfo(String name, String userId, String clientId, Collection<String> scopes, long expiresIn) {
        this.name = name;
        this.userId = userId;
        valid = true;
        this.clientId = clientId;
        /**
         * Accept new chat scopes as well. Adding old "chat_login" scope for
         * them, which is checked for. Kind of a hack, but should work well
         * enough.
         */
        if (scopes.contains("chat:read") && scopes.contains("chat:edit")) {
            scopes.add("chat_login");
        }
        this.scopes = new HashSet<>(scopes);
        this.expiresIn = expiresIn;
    }
    
    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
    
    public boolean hasScope(Scope scope) {
        return scopes.contains(scope.scope);
    }
}
