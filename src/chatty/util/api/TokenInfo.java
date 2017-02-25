
package chatty.util.api;

/**
 * Holds info (username, scopes) about the current access token.
 * 
 * @author tduva
 */
public class TokenInfo {
    
    public final boolean channel_commercials;
    public final boolean channel_editor;
    public final boolean chat_access;
    public final boolean user_read;
    public final boolean channel_subscriptions;
    public final boolean user_follows_edit;
    public final String name;
    public final String userId;
    public final boolean valid;
    
    public TokenInfo() {
        valid = false;
        channel_commercials = false;
        channel_editor = false;
        chat_access = false;
        user_read = false;
        name = null;
        userId = null;
        channel_subscriptions = false;
        user_follows_edit = false;
    }
    
    public TokenInfo(String name, String userId, boolean chat_access, boolean channel_editor,
            boolean channel_commercials, boolean user_read, boolean channel_subscriptions,
            boolean user_follows_edit) {
        this.name = name;
        this.userId = userId;
        this.channel_editor = channel_editor;
        this.channel_commercials = channel_commercials;
        this.chat_access = chat_access;
        this.user_read = user_read;
        this.channel_subscriptions = channel_subscriptions;
        this.user_follows_edit = user_follows_edit;
        valid = true;
    }
}
