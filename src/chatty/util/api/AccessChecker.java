
package chatty.util.api;

import chatty.TwitchClient;
import chatty.User;
import chatty.gui.components.eventlog.EventLog;
import chatty.lang.Language;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class AccessChecker {
    
    public enum UserType {
        MODERATOR, BROADCASTER
    }
    
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
    
    public static boolean isBroadcaster(String channel, TokenInfo.Scope scope) {
        return instance().check(scope.scope, channel, UserType.BROADCASTER, scope);
    }
    
    public static boolean isModerator(String channel, TokenInfo.Scope scope) {
        return instance().check(scope.scope, channel, UserType.MODERATOR, scope);
    }
    
    public static boolean isBroadcaster(User user, TokenInfo.Scope scope) {
        return instance().check(scope.scope, user, UserType.BROADCASTER, scope);
    }
    
    public static boolean isModerator(User user, TokenInfo.Scope scope) {
        return instance().check(scope.scope, user, UserType.MODERATOR, scope);
    }
    
    public static boolean isBroadcaster(String accessId, User user, TokenInfo.Scope... scopes) {
        return instance().check(accessId, user, UserType.BROADCASTER, scopes);
    }
    
    public static boolean isModerator(String accessId, User user, TokenInfo.Scope... scopes) {
        return instance().check(accessId, user, UserType.MODERATOR, scopes);
    }
    
    public static boolean hasScope(TokenInfo.Scope scope) {
        return instance().check(scope.scope, scope);
    }
    
    private boolean check(String accessId, String channel, UserType userType, TokenInfo.Scope... scopes) {
        if (userType == null) {
            return check(accessId, scopes);
        }
        User user = client.getLocalUser(channel);
        return check(accessId, user, userType, scopes);
    }
    
    private boolean check(String accessId, User user, UserType userType, TokenInfo.Scope... scopes) {
        if (user != null) {
            if (userType == UserType.BROADCASTER) {
                return user.isBroadcaster() && check(accessId, scopes);
            }
            else if (userType == UserType.MODERATOR) {
                return (user.isBroadcaster() || user.isModerator()) && check(accessId, scopes);
            }
        }
        return false;
    }
    
    private boolean check(String accessId, TokenInfo.Scope... scopes) {
        Set<String> missingScopeLabels = new HashSet<>();
        for (TokenInfo.Scope scope : scopes) {
            if (!settings.listContains("scopes", scope.scope)) {
                missingScopeLabels.add(scope.label);
            }
        }
        if (!missingScopeLabels.isEmpty()) {
            EventLog.addSystemEvent("accessMissing."+accessId,
                    Language.getString("eventLog.entry.accessMissing.title"),
                    Language.getString("eventLog.entry.accessMissing.text", StringUtil.join(missingScopeLabels, ", ")));
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
