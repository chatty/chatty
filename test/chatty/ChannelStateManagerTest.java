package chatty;

import chatty.ChannelState;
import chatty.ChannelStateManager;
import chatty.ChannelStateManager.ChannelStateListener;
import org.junit.Test;
import org.junit.Before;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ChannelStateManagerTest {

    private ChannelStateListener channelStateListener;
    private ChannelStateManager channelState;
    @Before
    public void setUp() {
        channelStateListener = Mockito.mock(ChannelStateListener.class);
        channelState = new ChannelStateManager();
        channelState.addListener(channelStateListener);
    }

    @Test
    public void testResetChannel() {
        ChannelStateManager manager = new ChannelStateManager();
        ChannelStateListener listener = Mockito.mock(ChannelStateListener.class);
        manager.addListener(listener);

        String channel = "testChannel";
        int slowmodeLength = 10;
        manager.setSlowmode(channel, slowmodeLength);

        // Reset the channel state
        manager.reset(channel);

        // Verify that the state was reset
        ChannelState state = manager.getState(channel);
        assertEquals(-1, state.slowMode());

        // Verify that the listener was notified
        verify(listener, times(2)).channelStateUpdated(state);
    }

    @Test
    public void testResetAllChannels() {
        ChannelStateManager manager = new ChannelStateManager();
        ChannelStateListener listener = Mockito.mock(ChannelStateListener.class);
        manager.addListener(listener);

        String channel1 = "channel1";

        int slowmodeLength = 10;

        // Set slowmode for two channels
        manager.setSlowmode(channel1, slowmodeLength);

        // Reset all channel states
        manager.reset();

        // Verify that the states were reset for both channels
        ChannelState state1 = manager.getState(channel1);
        assertEquals(-1, state1.slowMode());

        // Verify that the listener was notified for both channels
        verify(listener, times(2)).channelStateUpdated(any(ChannelState.class));
    }
}
