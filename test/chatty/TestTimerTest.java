package chatty;


import org.junit.Test;
import org.mockito.Mockito;

import java.util.function.Consumer;

public class TestTimerTest {

    /*
    It defines the test parameters:

    max: The maximum number of times the action should be executed.
    delay: The delay in milliseconds between each execution of the action.
    mockAction: A mock implementation of a Consumer<Integer> interface, which represents
                an action that takes an integer parameter.
    Creating the testTimer instance and applying the required logic then starting and joining the thread
    to see if the timer completed its execution and  is working properly.
    Finally verifying it was executed as expected
     */
    @Test
    public void testRun() throws InterruptedException {
        // Define the test parameters for the
        int max = 5;
        int delay = 100; // Delay in milliseconds
        Consumer<Integer> mockAction = Mockito.mock(Consumer.class);

        // Create a TestTimer instance
        TestTimer testTimer = new TestTimer(mockAction, max, delay);

        // Start a thread for the TestTimer
        Thread testThread = new Thread(testTimer);
        testThread.start();

        // Wait for the thread to complete
        testThread.join();

        // Verify that the mockAction was called the expected number of times
        Mockito.verify(mockAction, Mockito.times(max)).accept(Mockito.anyInt());
    }
}

