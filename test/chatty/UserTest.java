
package chatty;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class UserTest {
    
    @Test
    public void linesTest() {
        // max
        User maxLinesReached = new User("", Room.EMPTY);
        for (int i=0;i<100;i++) {
            maxLinesReached.addMessage(null, false, null);
        }
        assertFalse(maxLinesReached.linesCleared());
        assertFalse(maxLinesReached.maxLinesExceeded());
        
        // max+1
        User maxLinesExceeded = new User("", Room.EMPTY);
        for (int i=0;i<101;i++) {
            maxLinesExceeded.addMessage(null, false, null);
        }
        assertFalse(maxLinesExceeded.linesCleared());
        assertTrue(maxLinesExceeded.maxLinesExceeded());
        
        // cleared
        User linesCleared = new User("", Room.EMPTY);
        linesCleared.addMessage(null, false, null);
        linesCleared.clearMessagesIfInactive(0);
        assertTrue(linesCleared.linesCleared());
        assertFalse(linesCleared.maxLinesExceeded());
        
        // 120 -> cleared
        User linesClearedAfterExcceeded = new User("", Room.EMPTY);
        for (int i=0;i<120;i++) {
            linesClearedAfterExcceeded.addMessage(null, false, null);
        }
        linesClearedAfterExcceeded.clearMessagesIfInactive(0);
        assertTrue(linesClearedAfterExcceeded.linesCleared());
        assertFalse(linesClearedAfterExcceeded.maxLinesExceeded());
        
        // 120 -> cleared -> 120
        User linesExceededAfterCleared = new User("", Room.EMPTY);
        for (int i=0;i<120;i++) {
            linesExceededAfterCleared.addMessage(null, false, null);
        }
        linesExceededAfterCleared.clearMessagesIfInactive(0);
        for (int i=0;i<120;i++) {
            linesExceededAfterCleared.addMessage(null, false, null);
        }
        assertFalse(linesExceededAfterCleared.linesCleared());
        assertTrue(linesExceededAfterCleared.maxLinesExceeded());
        
        // 40 -> cleared -> max
        User linesMaxAfterCleared = new User("", Room.EMPTY);
        for (int i=0;i<40;i++) {
            linesMaxAfterCleared.addMessage(null, false, null);
        }
        linesMaxAfterCleared.clearMessagesIfInactive(0);
        for (int i=0;i<100;i++) {
            linesMaxAfterCleared.addMessage(null, false, null);
        }
        assertFalse(linesMaxAfterCleared.linesCleared());
        assertTrue(linesMaxAfterCleared.maxLinesExceeded());
        
        // Various tests
        User otherTypes = new User("", Room.EMPTY);
        for (int i=0;i<100;i++) {
            otherTypes.addMessage(null, false, null);
        }
        assertFalse(otherTypes.linesCleared());
        assertFalse(otherTypes.maxLinesExceeded());
        otherTypes.addInfo(null, null);
        assertFalse(otherTypes.linesCleared());
        assertTrue(otherTypes.maxLinesExceeded());
        otherTypes.clearMessagesIfInactive(0);
        assertTrue(otherTypes.linesCleared());
        assertFalse(otherTypes.maxLinesExceeded());
        for (int i=0;i<99;i++) {
            otherTypes.addAutoModMessage(null, null, null);
        }
        assertTrue(otherTypes.linesCleared());
        assertFalse(otherTypes.maxLinesExceeded());
        otherTypes.addBan(0, null, null);
        assertFalse(otherTypes.linesCleared());
        assertTrue(otherTypes.maxLinesExceeded());
    }
    
    @Test
    public void testSimilarMessages() {
        User user = new User("", Room.EMPTY);
        user.addMessage("first line", false, "");
        user.addMessage("second line", true, "");
        user.addMessage("third line", false, "");
        assertEquals(1, user.getNumberOfSimilarChatMessages("first line 2", 600, 0.8f));
        assertEquals(0, user.getNumberOfSimilarChatMessages("first line 2", 600, 1f));
        assertEquals(3, user.getNumberOfSimilarChatMessages("first line 2", 600, 0f));
        assertEquals(3, user.getNumberOfSimilarChatMessages("line", 600, 0.5f));
        user.addMessage("first line 2", false, "");
        assertEquals(2, user.getNumberOfSimilarChatMessages("first line 2", 600, 0.8f));
        assertEquals(2, user.getNumberOfSimilarChatMessages("first                                   line 2", 600, 0.8f));
        assertEquals(0, user.getNumberOfSimilarChatMessages("FIRST                                   LINE 2", 600, 0.8f));
    }
    
}
