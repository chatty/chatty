
package chatty.util.api;

import chatty.lang.Language;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds info (username, scopes) about the current access token.
 * 
 * @author tduva
 */
public class TokenInfo {
    
        
    public enum ScopeCategory {
        BASIC("basic",
                Scope.CHAT_READ,
                Scope.CHAT_EDIT,
                Scope.WHISPER_READ,
                Scope.WHISPER_EDIT,
                Scope.WHISPER_MANAGE,
                Scope.MANAGE_COLOR,
                Scope.POINTS,
                Scope.FOLLOWS,
                Scope.CLIPS_EDIT
        ),
        MODERATION("moderation",
                Scope.MANAGE_CHAT,
                Scope.MANAGE_WARNINGS,
                Scope.MANAGE_BANS,
                Scope.MANAGE_MSGS,
                Scope.CHAN_MOD,
                Scope.AUTOMOD,
                Scope.BLOCKED_READ,
                Scope.BLOCKED_MANAGE,
                Scope.ANNOUNCEMENTS,
                Scope.MANAGE_SHIELD,
                Scope.MANAGE_SHOUTOUTS,
                Scope.CHANNEL_FOLLOWERS
        ),
        BROADCASTER("broadcaster",
                Scope.MANAGE_MODS,
                Scope.MANAGE_VIPS,
                Scope.EDITOR,
                Scope.EDIT_BROADCAST,
                Scope.COMMERICALS,
                Scope.SUBSCRIBERS,
                Scope.MANAGE_RAIDS,
                Scope.MANAGE_POLLS
        );
        
        public List<Scope> scopes;
        public String label;

        private ScopeCategory(String langKey, Scope... scopes) {
            this.scopes = Arrays.asList(scopes);
            this.label = Language.getString("login.accessCategory."+langKey);
        }
        
        public static List<Scope> getUncategorized() {
            List<Scope> result = new ArrayList<>(Arrays.asList(Scope.values()));
            result.removeIf(scope -> getCategory(scope) != null);
            return result;
        }
        
        public static ScopeCategory getCategory(Scope scope) {
            for (ScopeCategory cat : values()) {
                if (cat.scopes.contains(scope)) {
                    return cat;
                }
            }
            return null;
        }
        
    }
    
    public enum Scope {
        CHAT_READ("chat:read", "chatRead"),
        CHAT_EDIT("chat:edit", "chatWrite"),
        WHISPER_READ("whispers:read", "whisperRead"),
        WHISPER_EDIT("whispers:edit", "whisperWrite"),
        WHISPER_MANAGE("user:manage:whispers", "whisperManage"),
        EDITOR("channel_editor", "editor"), // ?
        EDIT_BROADCAST("channel:manage:broadcast", "broadcast"),
        COMMERICALS("channel_commercial", "commercials"),
        FOLLOWS("user:read:follows", "follows"), // Followed streams
        CHANNEL_FOLLOWERS("moderator:read:followers", "viewFollowers"),
        SUBSCRIBERS("channel:read:subscriptions", "subscribers"),
        CHAN_MOD("channel:moderate", "chanMod"), // PubSub topics
        AUTOMOD("moderator:manage:automod", "automod"),
        BLOCKED_READ("moderator:read:blocked_terms", "blockedRead"),
        BLOCKED_MANAGE("moderator:manage:blocked_terms", "blockedManage"),
        POINTS("channel:read:redemptions", "points"),
        ANNOUNCEMENTS("moderator:manage:announcements", "announcements"),
        MANAGE_BANS("moderator:manage:banned_users", "manageBans"),
        MANAGE_CHAT("moderator:manage:chat_settings", "manageChat"),
        MANAGE_MODS("channel:manage:moderators", "manageMods"),
        MANAGE_VIPS("channel:manage:vips", "manageVips"),
        MANAGE_MSGS("moderator:manage:chat_messages", "manageMsgs"),
        MANAGE_SHIELD("moderator:manage:shield_mode", "manageShield"),
        MANAGE_COLOR("user:manage:chat_color", "manageColor"),
        MANAGE_RAIDS("channel:manage:raids", "manageRaids"),
        MANAGE_POLLS("channel:manage:polls", "managePolls"),
        MANAGE_SHOUTOUTS("moderator:manage:shoutouts", "manageShoutouts"),
        MANAGE_WARNINGS("moderator:manage:warnings", "manageWarnings"),
        CLIPS_EDIT("clips:edit", "editClips");
        
        public String scope;
        public String label;
        public String description;
        
        Scope(String scope, String langKey) {
            this.scope = scope;
            this.label = Language.getString("login.access."+langKey);
            this.description = Language.getString("login.access."+langKey+".tip", false);
        }
        
        public static Scope fromScopeString(String scopeString) {
            for (Scope scope : values()) {
                if (scope.scope.equals(scopeString)) {
                    return scope;
                }
            }
            return null;
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
        return hasScope(Scope.CHAT_READ) && hasScope(Scope.CHAT_EDIT);
    }
    
    public static void main(String[] args) {
        System.out.println(ScopeCategory.getUncategorized());
    }
    
}
