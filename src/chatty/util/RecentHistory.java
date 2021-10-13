package chatty.util;

import chatty.Room;
import chatty.TwitchConnection;
import chatty.gui.MainGui;
import chatty.util.settings.Settings;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.logging.Logger;

public class RecentHistory {
    private static final Logger LOGGER = Logger.getLogger(RecentHistory.class.getName());

    private static final String URL_HISTORY = "https://recent-messages.robotty.de/api/v2/recent-messages/";

    private final TwitchConnection connection;
    private final Settings settings;
    private final MainGui mainGui;

    public RecentHistory(TwitchConnection connection, Settings settings, MainGui mainGui) {
        this.connection = connection;
        this.settings = settings;
        this.mainGui = mainGui;
    }

    public synchronized void requestHistory(Room room) {
        String url = URL_HISTORY + room.getStream();
        UrlRequest request = new UrlRequest(url);
        request.setLabel("RecentHistory");
        request.async((result, responseCode) -> {
            if (responseCode == 200 && result != null) {
                loadHistory(result, room);
            } else if (String.valueOf(responseCode).startsWith("4")) {
                onHistoryError(result, room);
            } else {
                mainGui.printLine(room, "Failed to load recent-history");
            }
        });
    }

    private void onHistoryError(String json, Room room) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            String error = (String)root.get("error");
            mainGui.printLine(room, "Failed to load recent-history: " + error);
        } catch (ParseException e) {
            LOGGER.warning("|[RecentHistory] Error parsing recent-history error response: "+e);
        }
    }

    private void loadHistory(String json, Room room) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            JSONArray messages = (JSONArray)root.get("messages");
            for (Object o : messages) {
                connection.simulate((String)o);
            }
            mainGui.printLine(room, "Chat history ends here");
        } catch (ParseException e) {
            LOGGER.warning("|[RecentHistory] Error parsing recent-history response: "+e);
        }
    }
}
