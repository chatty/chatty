package chatty;


import chatty.ChannelStateManager.ChannelStateListener;
import org.junit.Test;
import org.junit.Before;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ChannelStateManagerTest {

    private ChannelStateListener channelStateListener;
    private ChannelStateManager channelState;
    /*
    Setting up all the instantiation for the test cases and running it before
     */
    @Before
    public void setUp() {
        channelStateListener = Mockito.mock(ChannelStateListener.class);
        channelState = new ChannelStateManager();
        channelState.addListener(channelStateListener);
    }

    /*
    Test cae to test if channel is reseted or not. Which is done by first making a
    manager instance and mocking the listener than starting the slowmode and finally reseting the
    channel and testing it using assert and finally verifying it.
     */
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

    /*
    Test case to test if  2 channel is reseted or not. Which is done by first making a
    manager instance and mocking the listener than starting the slowmode and finally reseting the
    channel and testing it using assert and finally verifying it.
    The test case is to check if multiple channels are reseted or not which is important
    if the user is using multiple channels at a time.
     */
    @Test
    public void testResetAllChannels() {
        ChannelStateManager manager = new ChannelStateManager();
        ChannelStateListener listener = Mockito.mock(ChannelStateListener.class);
        manager.addListener(listener);

        String channel1 = "channel1";
        String channel2 = "channel2";
        int slowmodeLength = 10;

        // Set slowmode for two channels
        manager.setSlowmode(channel1, slowmodeLength);
        manager.setSlowmode(channel2, slowmodeLength);

        // Reset all channel states
        manager.reset();

        // Verify that the states were reset for both channels
        ChannelState state1 = manager.getState(channel1);
        ChannelState state2 = manager.getState(channel2);
        assertEquals(-1, state1.slowMode());
        assertEquals(-1, state2.slowMode());

        // Verify that the listener was notified for both channels
        verify(listener, times(4)).channelStateUpdated(any(ChannelState.class));
    }
}
