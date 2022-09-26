
package chatty.util.api;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class TokenInfoTest {

    @Test
    public void testUncategorized() {
        // All scopes should be categorized
        assertEquals(0, TokenInfo.ScopeCategory.getUncategorized().size());
    }
    
}
