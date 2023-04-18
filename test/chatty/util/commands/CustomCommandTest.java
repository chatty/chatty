
package chatty.util.commands;

import chatty.Helper;
import chatty.Room;
import chatty.User;
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
        //------
        // Misc
        //------
        test("", new String[]{
            "a", "",
            "", ""
        });
        
        //-------------------
        // upper() / lower()
        //-------------------
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
        
        test("$upper(1)", new String[]{
            "", "",
            "a", "A",
            "abc", "ABC",
            "A", "A",
            "ä", "Ä"
        });
        
        //--------
        // ifeq()
        //--------
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
        
        //--------
        // join()
        //--------
        test("$join($lower($1-),/)", new String[]{
            "a b C", "a/b/c",
            "", "",
            "A ", "a",
            "A   ", "a",
            "  A", "//a"
        });
        
        //------
        // if()
        //------
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

        //--------
        // sort()
        //--------
        test("$sort($$1)", new String[] {
            "", null,
            "123", "123",
            "1 3 2", "1",
            "123 abc", "123"
        });
        
        test("$sort($$1-)", new String[] {
            "", null,
            "123", "123",
            "1 3 2", "1 2 3",
            "123 abc", "123 abc",
            "abc 123", "123 abc",
            "Abc 123", "123 Abc",
            "a B c", "a B c"
        });
        
        test("$sort($$1-,Abc)", new String[] {
            "", null,
            "123", "123",
            "1 3 2", "1 2 3",
            "123 abc", "123 abc",
            "abc 123", "123 abc",
            "Abc 123", "123 Abc",
            "a B c", "B a c"
        });
        
        test("$sort($$1-,Abc,#)", new String[] {
            "", null,
            "123", "123",
            "1#3#2", "1#2#3",
            "123#abc", "123#abc",
            "abc#123", "123#abc",
            "Abc#123", "123#Abc",
            "a#B#c", "B#a#c"
        });
        
        //-------------
        // urlencode()
        //-------------
        test("$urlencode($$1-)", new String[] {
            "abc", "abc",
            "a b c", "a+b+c"
        });
        
        //-------------
        // cs() / fs()
        //-------------
        test("$cs($1-)", new String[] {
            "a|b|c", "a||b||c",
            "a>b<c", "a>b<c",
            "a b c", "a b c",
            ">a|b|c<", ">a||b||c<"
        });
        
        test("$fs($1-)", new String[] {
            "a|b|c", "a|b|c",
            "a>b<c", "a>>b<c",
            "a b c", "a b c",
            ">a|b|c<", ">>a|b|c<"
        });
        
        test("$cs($fs($1-))", new String[] {
            "a|b|c", "a||b||c",
            "a>b<c", "a>>b<c",
            "a b c", "a b c",
            ">a|b|c<", ">>a||b||c<"
        });
        
        test("$cs($fs($1-))", new String[]{
            "a|b|c", "a||b||c",
            "a>b<c", "a>>b<c",
            "a b c", "a b c",
            ">a|b|c<", ">>a||b||c<"
        }, new String[]{
            "escape-pipe", "true"
        });
        
        test("$cs($fs($1-))", new String[]{
            "a|b|c", "a||b||c",
            "a>b<c", "a>>b<c",
            "a b c", "a b c",
            ">a|b|c<", ">>a||b||c<"
        }, new String[]{
            "escape-greater", "true"
        });
        
        test("$1-", new String[]{
            "a|b|c", "a||b||c",
            "a>b<c", "a>b<c",
            "a b c", "a b c",
            ">a|b|c<", ">a||b||c<"
        }, new String[]{
            "escape-pipe", "true"
        });
        
        test("$1-", new String[]{
            "a|b|c", "a|b|c",
            "a>b<c", "a>>b<c",
            "a b c", "a b c",
            ">a|b|c<", ">>a|b|c<"
        }, new String[]{
            "escape-greater", "true"
        });
        
        test("$1-", new String[]{
            "a|b|c", "a||b||c",
            "a>b<c", "a>>b<c",
            "a b c", "a b c",
            ">a|b|c<", ">>a||b||c<"
        }, new String[]{
            "escape-greater", "true",
            "escape-pipe", "true"
        });
    }
    
    private void test(String command, String[] tests) {
        test(command, tests, null);
    }
    
    private void test(String command, String[] tests, String[] params) {
        CustomCommand c = CustomCommand.parse(command);
        if (c.hasError()) {
            throw new RuntimeException(c.getError());
        }
        for (int i=0; i<tests.length; i += 2) {
            Parameters parameters = Parameters.create(tests[i]);
            if (params != null) {
                for (int j = 0; j < params.length; j += 2) {
                    parameters.put(params[j], params[j + 1]);
                }
            }
            assertEquals(tests[i+1], c.replace(parameters));
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
        
        CustomCommand.parse("$rand()").replace(Parameters.create(""));
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
    
    @Test
    public void testErrorBuilding() {
        /**
         * Test if building the error message at the very least doesn't throw
         * an error.
         */
        CustomCommand.parse("$");
        CustomCommand.parse("$(");
        CustomCommand.parse("$$");
        CustomCommand.parse("$abc");
        CustomCommand.parse(" $ ");
        CustomCommand.parse(" $join(");
        CustomCommand.parse(" $join( ");
    }
    
    @Test
    public void testUser() {
        User user = new User("username", "User Name", Room.EMPTY);

        Parameters parameters = Parameters.create("");
        Helper.addUserParameters(user, null, null, parameters);
        
        CustomCommand command = CustomCommand.parse("$(nick) $(display-nick) $(full-nick) $(custom-nick) $(special-nick) $(display-nick2) $(full-nick2) $(user-id)");
        assertEquals("username User Name User Name User Name true User Name (username) User Name (username) ", command.replace(parameters));
        
        user.setModerator(true);
        user.setCustomNick("custom");
        user.setDisplayNick("asdas");
        parameters = Parameters.create("");
        Helper.addUserParameters(user, null, null, parameters);
        
        assertEquals("username asdas @custom custom true asdas (username) @custom (username) ", command.replace(parameters));
        
        user.setId("id");
        user.setDisplayNick("UserName");
        parameters = Parameters.create("");
        Helper.addUserParameters(user, null, null, parameters);
        
        assertEquals("UserName UserName @custom custom  UserName @custom id", command.replace(parameters));
        
        user.setCustomNick(null);
        parameters = Parameters.create("");
        Helper.addUserParameters(user, null, null, parameters);
        
        assertEquals("UserName UserName @UserName UserName  UserName @UserName id", command.replace(parameters));
    }
    
    @Test
    public void testCustom() {
        Parameters parameters = Parameters.create("ABC");
        assertEquals("abc %lower(1) $lower(1)", CustomCommand.parseCustom("%lower(1) `%lower(1) $lower(1)", "%", "`").replace(parameters));
        assertTrue(CustomCommand.parseCustom("%lower(1)", "%", "%").hasError());
        assertEquals("abc abc", CustomCommand.parseCustom("%lower(1) %lower(1)", "%", "").replace(parameters));
        assertEquals("$lower(1)", CustomCommand.parseCustom("$lower(1)", "", "").replace(parameters));
        assertEquals("$lower(1)", CustomCommand.parseCustom("%$lower(1)", "", "%").replace(parameters));
        assertEquals("%$lower(1)", CustomCommand.parseCustom("%%$lower(1)", "", "%").replace(parameters));
    }
    
    @Test
    public void testLiteral() {
        Parameters parameters = Parameters.create("ABC");
        assertEquals("abc / Dollar: $", CustomCommand.parse("$lower(1) / $'Dollar: $'").replace(parameters));
        assertEquals("abc / Dollar: $", CustomCommand.parse("$lower(1) / $'Dollar: $").replace(parameters));
        assertEquals("abc / Quote: '", CustomCommand.parse("$lower(1) / $'Quote: ''").replace(parameters));
        assertEquals("abc / Quote: ' / Dollar: $", CustomCommand.parse("$lower(1) / $'Quote: '' / Dollar: $'").replace(parameters));
        assertEquals("abc / Dollar: $ / abc / Dollar: $", CustomCommand.parse("$lower(1) / $'Dollar: $' / $lower(1) / $'Dollar: $'").replace(parameters));
        assertEquals("abc,123", CustomCommand.parse("$rand($'abc,123')").replace(parameters));
        assertTrue(CustomCommand.parse("$rand($'abc,123)").hasError());
    }
    
    @Test
    public void testParameters() {
        User user = new User("username", "User Name", Room.EMPTY);
        Parameters parameters = Parameters.create("");
        
        assertFalse(parameters.notEmpty("custom-nick", "full-nick2"));
        assertEquals(parameters.get("display-nick"), null);
        
        Helper.addUserParameters(user, null, null, parameters);
        
        assertTrue(parameters.notEmpty("custom-nick", "full-nick2"));
        assertEquals(parameters.get("display-nick"), "User Name");
    }
    
    @Test
    public void testJson() {
        String json = "{\n"
                + "        \"books\":[\n"
                + "            {\"title\":\"book1\", \"author\":\"author1\", \"tags\":[\"tag1\",\"tag2\"]},\n"
                + "            {\"title\":\"book2\", \"author\":\"author2\", \"tags\":[\"tag1\"]},\n"
                + "            {\"title\":\"book3\", \"author\":\"author2\", \"tags\":[\"tag1\", \"tag3\"]},\n"
                + "            {\"title\":\"book4\", \"author\":\"author2\"}\n"
                + "        ],\n"
                + "        \"authors\":{\n"
                + "            \"author1\":{\"name\":\"name1\", \"age\":24},\n"
                + "            \"author2\":{\"name\":\"name2\", \"age\":62}\n"
                + "        },\n"
                + "        \"numBooks\": 4,\n"
                + "        \"numAuthors\": 2\n"
                + "    }";
        
        testJson(json,
                "$json($(j),$j(numBooks))", "4",
                "$json($(j),$j(books[size]))", "4",
                "$json($(j),$j(books[last]->tags))", "",
                "$json($(j),$j(books[0]->tags[join]))", "tag1, tag2",
                "$json($(j),$j(books[size]) $j(books[collect:author][unique][size]))", "4 2",
                "$json($(j),$j(books[filter:author=author2][combine:tags]))", "[\"tag1\",\"tag1\",\"tag3\"]",
                "$json($(j),$j(books[filter:author=author2][combine:tags][join]))", "tag1, tag1, tag3",
                "$json($(j),$j(books[filter:author=author2][combine:tags][unique][join]))", "tag1, tag3",
                "$json($(j),$j(books[collect:author][join:/]))", "author1/author2/author2/author2",
                "$json($(j),$j(books[collect:author][join:'[]']))", "author1[]author2[]author2[]author2",
                "$json($(j),$j(books[collect:author][join:'['']']))", "author1[']author2[']author2[']author2",
                "$json($(j),$j(books[collect:author][join:'''[]''']))", "author1'[]'author2'[]'author2'[]'author2",
                "$json($(j),$j(books[filter:tags=.*tag2.*][collect:author][join]))", "author1",
                // Combine filters on each individual array item
                "$json($(j),$j(books[combine:tags='.*[0-2]'][join]))", "tag1, tag2, tag1, tag1",
                // Collect filters on the array in total
                "$json($(j),$j(books[collect:tags='.*[0-2].*'][combine:][join]))", "tag1, tag2, tag1, tag1, tag3",
                
                // Path that doesn't exist in the given JSON
                "$json($(j),$j(abc))", "",
                "$$json($(j),$j(abc))", null,
                "$$json($(j),$j(abc)abc)", "abc",
                "$json($(j),$$j(abc))", null,
                "$json($(j),$j(abc,nope))", "nope",
                
                // Can't use outside of $json()
                "$json($(j),$j(numBooks))$j(numBooks)", null,
                // No JSON
                "$json(,$j())", null,
                "$json($(j),$j([]))", null,
                
                // Invalid path (regex parsing error)
                "$json($(j),$j(books[collect:author=[a-z0-9]+][size]))", null,
                // Same path, but regex quoted with ' '
                "$json($(j),$j(books[collect:author='[a-z0-9]+'][size]))", "4",
                
                // Collect from object
                "$json($(j),$j(authors[collect:name][sort][join]))", "name1, name2",
                "$json($(j),$j(authors,,each:$j(name),$j([sort][join])))", "name1, name2",
                "$json($(j),$j(authors,,each:$(key) $j(name),$j([sort][join])))", "author1 name1, author2 name2",
                "$json($(j),$j(books,,each:$(index) $j(title),$j([sort][join])))", "0 book1, 1 book2, 2 book3, 3 book4",
                "$json($(j),$j(books,,each:$(index) $j(title),$j([join])))", "0 book1, 1 book2, 2 book3, 3 book4",
                // Sort for array from object, since it may not always have the same order
                "$json($(j),$j(authors,,each:$(key),$j([sort][join])))", "author1, author2"
        );
        
        // Construct the path using replacements
        test("$json($(j),$j($1-))",
                new String[]{
                    "numBooks", "4"
                },
                new String[]{"j", json});
    }
    
    private static void testJson(String json, String... tests) {
        Parameters parameters = Parameters.create("");
        parameters.put("j", json);
        for (int i = 0; i < tests.length; i += 2) {
            CustomCommand c = CustomCommand.parse(tests[i]);
            if (c.hasError()) {
                throw new RuntimeException(c.getError());
            }
            assertEquals(tests[i + 1], c.replace(parameters));
        }
    }
    
}
