package chatty.gui.notifications;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NotificationTest {
    @Test
    public void testThatMatchingAgainstNullChannelsWorks() {
        Notification notification = createStreamStatusNotification(null);
        assertTrue(notification.matchesChannel(null));
        assertTrue(notification.matchesChannel("#foobar"));
        assertTrue(notification.matchesChannel("#fooBAR"));
        assertTrue(notification.matchesChannel("foobar"));
        assertTrue(notification.matchesChannel("FOObar"));
    }

    @Test
    public void testThatSingleChannelMatchingWorks() {
        Notification notification = createStreamStatusNotification("FOObar");
        assertTrue(notification.matchesChannel(null));
        assertTrue(notification.matchesChannel("#foobar"));
        assertTrue(notification.matchesChannel("#fooBAR"));
        notification = createStreamStatusNotification("#fOOBAR");
        assertTrue(notification.matchesChannel(null));
        assertTrue(notification.matchesChannel("#foobar"));
        assertTrue(notification.matchesChannel("#FOObar"));
    }

    private Notification createStreamStatusNotification(String channel) {
        Notification.Builder builder = new Notification.Builder(Notification.Type.STREAM_STATUS)
                .setChannel(channel);
        return new Notification(builder);
    }
}
