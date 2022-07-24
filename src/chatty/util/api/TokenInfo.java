
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
        CHAT_READ("chat:read", "chatRead"),
        CHAT_EDIT("chat:edit", "chatWrite"),
        WHISPER_READ("whispers:read", "whisperRead"),
        WHISPER_EDIT("whispers:edit", "whisperWrite"),
        USERINFO("user_read", "user"),
        EDITOR("channel_editor", "editor"),
        EDIT_BROADCAST("user:edit:broadcast", "broadcast"),
        COMMERICALS("channel_commercial", "commercials"),
        FOLLOWS("user:read:follows", "follows"),
        SUBSCRIBERS("channel_subscriptions", "subscribers"),
        SUBSCRIPTIONS("user_subscriptions", "subscriptions"),
        CHAN_MOD("channel:moderate", "chanMod"),
        AUTOMOD("moderator:manage:automod", "automod"),
        BLOCKED_READ("moderator:read:blocked_terms", "blockedRead"),
        BLOCKED_MANAGE("moderator:manage:blocked_terms", "blockedManage"),
        POINTS("channel:read:redemptions", "points"),
        ANNOUNCEMENTS("moderator:manage:announcements", "announcements");
        
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
        this.scopes = new HashSet<>(scopes);
        this.expiresIn = expiresIn;
    }
    
    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
    
    public boolean hasScope(Scope scope) {
        return scopes.contains(scope.scope);
    }
    
    public boolean hasChatAccess() {
        return hasScope(Scope.CHAT)
                || (hasScope(Scope.CHAT_READ) && hasScope(Scope.CHAT_EDIT));
    }
    
}
