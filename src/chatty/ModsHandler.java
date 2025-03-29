package chatty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ModsHandler {

    private final Set<String> silentModsRequestChannel = Collections.synchronizedSet(new HashSet<String>());
    private TwitchConnection c;

    public ModsHandler(TwitchConnection c) {
        this.c = c;
    }

    public void modsSilent(String channel) {
        if (onChannel(channel, true)) {
            printLine(channel, "Trying to fix moderators..");
            requestModsSilent(channel);
        }
    }

    public void requestModsSilent(String channel) {
        if (onChannel(channel, false)) {
            silentModsRequestChannel.add(channel);
            c.sendSpamProtectedMessage(channel, ".mods", false);
        }
    }

    public boolean removeModsSilent(String channel) {
        return silentModsRequestChannel.remove(channel);
    }

    public boolean waitingForModsSilent() {
        return !silentModsRequestChannel.isEmpty();
    }

    private boolean onChannel(String channel, boolean message) {
        return c.onChannel(channel, message);
    }

    private void printLine(String channel, String message) {
        c.info(channel, message, null);
    }
}
