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
        assertTrue(notification.matchesChannel("foobar"));
        assertTrue(notification.matchesChannel("Foobar"));
        notification = createStreamStatusNotification("#fOOBAR");
        assertTrue(notification.matchesChannel(null));
        assertTrue(notification.matchesChannel("#foobar"));
        assertTrue(notification.matchesChannel("#FOObar"));
        assertTrue(notification.matchesChannel("foobar"));
        assertTrue(notification.matchesChannel("fooBar"));
    }

    @Test
    public void testThatMultipleChannelMatchingWorks() {
        Notification notification = createStreamStatusNotification(" FOO, bar ,baZ ");
        assertTrue(notification.matchesChannel("foo"));
        assertTrue(notification.matchesChannel("bar"));
        assertTrue(notification.matchesChannel("baz"));
        assertTrue(notification.matchesChannel("#foo"));
        assertTrue(notification.matchesChannel("#bar"));
        assertTrue(notification.matchesChannel("#baz"));
        assertTrue(notification.matchesChannel("#FOO"));
        assertTrue(notification.matchesChannel("#baR"));
        notification = createStreamStatusNotification("#fOO ,#BAR, #BAZ");
        assertTrue(notification.matchesChannel("foo"));
        assertTrue(notification.matchesChannel("bar"));
        assertTrue(notification.matchesChannel("baz"));
        assertTrue(notification.matchesChannel("#foo"));
        assertTrue(notification.matchesChannel("#bar"));
        assertTrue(notification.matchesChannel("#baz"));
        assertTrue(notification.matchesChannel("#FOO"));
        assertTrue(notification.matchesChannel("#baR"));
    }

    private Notification createStreamStatusNotification(String channels) {
        Notification.Builder builder = new Notification.Builder(Notification.Type.STREAM_STATUS)
                .setChannels(channels);
        return new Notification(builder);
    }
}
