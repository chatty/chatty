
package chatty.util.api;

import chatty.TwitchClient;
import chatty.User;
import chatty.gui.components.eventlog.EventLog;
import chatty.lang.Language;
import chatty.util.settings.Settings;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tduva
 */
public class AccessChecker {
    
    private static volatile AccessChecker instance;
    
    private final Settings settings;
    private final TwitchClient client;
    
    public AccessChecker(Settings settings, TwitchClient client) {
        this.settings = settings;
        this.client = client;
    }
    
    public static void setInstance(AccessChecker checker) {
        if (checker != null) {
            instance = checker;
        }
    }
    
    public static AccessChecker instance() {
        return instance;
    }
    
    public boolean check(String channel, TokenInfo.Scope scope, boolean modRequired, boolean broadcasterRequired) {
        if (!modRequired && !broadcasterRequired) {
            return check(scope);
        }
        User user = client.getLocalUser(channel);
        return check(user, scope, modRequired, broadcasterRequired);
    }
    
    public boolean check(User user, TokenInfo.Scope scope, boolean modRequired, boolean broadcasterRequired) {
        if (user != null) {
            if (broadcasterRequired) {
                return user.isBroadcaster() && check(scope);
            }
            else if (modRequired) {
                return (user.isBroadcaster() || user.isModerator()) && check(scope);
            }
        }
        return false;
    }
    
    public boolean check(TokenInfo.Scope scope) {
        if (!settings.listContains("scopes", scope.scope)) {
            EventLog.addSystemEvent("accessMissing."+scope.scope,
                    Language.getString("eventLog.entry.accessMissing.title"),
                    Language.getString("eventLog.entry.accessMissing.text", scope.label));
            return false;
        }
        return true;
    }
    
    public void removeWarningsForAvailableScopes() {
        List<String> scopes = new ArrayList<>();
        settings.getList("scopes", scopes);
        for (String scope : scopes) {
            EventLog.removeSystemEvent("accessMissing."+scope);
        }
    }
    
}
