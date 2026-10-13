package chatty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Mods {

    private final Set<String> silentModsRequest = Collections.synchronizedSet(new HashSet<String>());
    private TwitchConnection c;

    public Mods(TwitchConnection c) {
        this.c = c;
    }

    private boolean onChannel(String channel, boolean message) {
        return c.onChannel(channel, message);
    }

    private void printLine(String channel, String message) {
        c.info(channel, message, null);
    }

    public void modsSilent(String channel) {
        if (onChannel(channel, true)) {
            printLine(channel, "Trying to fix moderators..");
            requestModsSilent(channel);
        }
    }

    public void requestModsSilent(String channel) {
        if (onChannel(channel, false)) {
            silentModsRequest.add(channel);
            c.sendSpamProtectedMessage(channel, ".mods", false);
        }
    }

    public boolean removeModsSilent(String channel) {
        return silentModsRequest.remove(channel);
    }

    public boolean waitingForModsSilent() {
        return !silentModsRequest.isEmpty();
    }

}