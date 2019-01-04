
package chatty.util.commands;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class CustomCommandTest {
    
    @Test
    public void testCommands() {
        String[] lowerTests1 = new String[] {
            "ABC", "abc",
            "A B C", "a",
            "a b c", "a",
            "", "",
        };
        
        String[] lowerTests2 = new String[] {
            "ABC", "abc",
            "A B C", "a",
            "a b c", "a",
            "", null,
        };
        
        test("$lower($1)", lowerTests1);
        test("$lower(1)", lowerTests1);
        test("$lower($$1)", lowerTests2);
        test("$$lower($1)", lowerTests2);
        
        test("$ifeq($$1,$$2,one,two)", new String[]{
            "", null,
            "a a", "one",
            "a", null,
            "a b", "two"
        });
        
        test("$ifeq($1,$2,one,$3)", new String[]{
            "", "one",
            "a a", "one",
            "a", "",
            "a b", "",
            "a b c", "c",
        });
        
        test("$ifeq($1,$2,one,$$3)", new String[]{
            "", "one",
            "a a", "one",
            "a", null,
            "a b", null,
            "a b c", "c",
        });
        
        test("$join($lower($1-),/)", new String[]{
            "a b C", "a/b/c",
            "", "",
            "A ", "a",
            "A   ", "a",
            "  A", "//a"
        });
        
        test("", new String[]{
            "a", "",
            "", ""
        });
        
        test("$if(2,a,$ifeq(1,cake,cheesecake))", new String[]{
            "a", "",
            "", "",
            "a b", "a",
            "cake", "cheesecake",
            "  b", ""
        });
        
        test("$if(1,a)", new String[] {
            "", "",
            "123", "a"
        });
        
        test("$$if(1,a)", new String[] {
            "", null,
            "123", "a"
        });
        
        test("$if($$1,a)", new String[] {
            "", null,
            "123", "a"
        });
        
        test("$if(1,$$2)", new String[] {
            "", "",
            "123", null,
            "123 456", "456"
        });
        
        test("$ifeq(1,123,a)", new String[] {
            "", "",
            "123", "a",
            "abc", ""
        });
        
        test("$$ifeq(1,123,a)", new String[] {
            "", null,
            "123", "a",
            "abc", null
        });
        
        test("$ifeq($$1,123,a)", new String[] {
            "", null,
            "123", "a",
            "abc", ""
        });
        
        test("$ifeq(1,123,$$2)", new String[] {
            "", "",
            "123", null,
            "abc", "",
            "123 abc", "abc"
        });
    }
    
    private void test(String command, String[] tests) {
        CustomCommand c = CustomCommand.parse(command);
        if (c.hasError()) {
            throw new RuntimeException(c.getError());
        }
        for (int i=0; i<tests.length; i += 2) {
            assertEquals(c.replace(Parameters.create(tests[i])), tests[i+1]);
        }
    }
    
    @Test
    public void testRandom() {
        CustomCommand random1 = CustomCommand.parse("$rand($1,b,c)");
        CustomCommand random2 = CustomCommand.parse("$rand($$(1),b,c)");
        CustomCommand random3 = CustomCommand.parse("$rand($$(1) a,b,c)");
        CustomCommand random4 = CustomCommand.parse("$$rand($(1) a,b,c)");
        CustomCommand random5 = CustomCommand.parse("$$rand($(1),b,c)");
        String[] randomOutput1 = new String[]{"a", "b", "c"};
        String[] randomOutput1b = new String[]{"", "b", "c"};
        String[] randomOutput2b = new String[]{null, "b", "c"};
        String[] randomOutput3 = new String[]{"a a", "b", "c"};
        String[] randomOutput4b = new String[]{" a", "b", "c"};
        for (int i=0; i<100; i++) {
            assertTrue(Arrays.asList(randomOutput1).contains(random1.replace(Parameters.create("a"))));
            assertTrue(Arrays.asList(randomOutput1b).contains(random1.replace(Parameters.create(""))));
            assertTrue(Arrays.asList(randomOutput1).contains(random2.replace(Parameters.create("a"))));
            assertTrue(Arrays.asList(randomOutput2b).contains(random2.replace(Parameters.create(""))));
            assertTrue(Arrays.asList(randomOutput3).contains(random3.replace(Parameters.create("a"))));
            assertTrue(Arrays.asList(randomOutput2b).contains(random3.replace(Parameters.create(""))));
            assertTrue(Arrays.asList(randomOutput3).contains(random4.replace(Parameters.create("a"))));
            assertTrue(Arrays.asList(randomOutput4b).contains(random4.replace(Parameters.create(""))));
            assertTrue(Arrays.asList(randomOutput1).contains(random5.replace(Parameters.create("a"))));
            assertTrue(Arrays.asList(randomOutput2b).contains(random5.replace(Parameters.create(""))));
        }
    }
    
    @Test
    public void testCommands2() {
        Parameters parameters = Parameters.create("");
        parameters.put("msg-id", "123456");
        Parameters parameters2 = Parameters.create("");
        
        CustomCommand command = CustomCommand.parse("$(msg-id)");
        CustomCommand command2 = CustomCommand.parse("$$(msg-id)");
        assertEquals(command.replace(parameters), "123456");
        assertEquals(command.replace(parameters2), "");
        assertEquals(command2.replace(parameters), "123456");
        assertEquals(command2.replace(parameters2), null);
    }
    
}
