
package chatty.gui.components.eventlog;

import java.awt.Color;

/**
 *
 * @author tduva
 */
public class Event {
    
    public enum Type {
        NOTIFICATION, SYSTEM
    }

    public final long createdAt = System.currentTimeMillis();
    public final Type type;
    public final String title;
    public final String text;
    public final Color foreground;
    public final Color background;
    public final String id;

    public Event(Type type, String id, String title, String text, Color foreground, Color background) {
        this.type = type;
        this.title = title;
        this.text = text;
        this.foreground = foreground;
        this.background = background;
        this.id = id;
    }

}
