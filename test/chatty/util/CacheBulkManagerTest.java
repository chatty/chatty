
package chatty.util;

import static chatty.util.CachedBulkManager.*;
import chatty.util.CachedBulkManager.Requester;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class CacheBulkManagerTest {
    
    @Test
    public void testA() {
//        Requester requester = new CachedBulkManager.Requester<String, String>() {
//
//            private int request = 0;
//            
//            @Override
//            public void request(CachedBulkManager<String, String> manager, Set<String> asap, Set<String> normal) {
//                request++;
//                System.out.println("Request "+request+" "+asap);
//                if (request == 1) {
//                    assertTrue(asap.isEmpty());
//                    assertTrue(normal.contains("abc"));
//                    assertTrue(normal.size() == 1);
//                }
//                if (request == 2) {
//                    assertTrue(normal.isEmpty());
//                    assertTrue(asap.contains("a"));
//                    assertTrue(asap.size() == 1);
//                }
//            }
//        };
        
        MyRequester requester = makeRequester(new String[][][]{
            {
                {}, {"abc"}, {}, {"abc"}
            }
        });
        MyResultListener listener = makeListener(new String[][][]{
            {
                {"abc", null}
            } 
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.query(listener, NONE, "abc");
        m.doRequests();
        listener.calledCount(0);
        requester.requestCount(1);
        m.doRequests();
        requester.requestCount(1);
        m.setNotFound("abc");
    }
    
    @Test
    public void testSet() {
        MyRequester requester = makeRequester(new String[][][]{
            
        });
        MyResultListener listener = makeListener(new String[][][]{
            
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON | UNIQUE);
        m.query(null, NONE, "a", "b", "c");
        assertEquals(m.pendingRequests(), 1);
        m.query(null, NONE, "a", "b", "c");
        assertEquals(m.pendingRequests(), 1);
        m.query(null, RETRY, "a", "b", "c");
        assertEquals(m.pendingRequests(), 2);
        m.query(null, RETRY, "a", "b", "c");
        assertEquals(m.pendingRequests(), 2);
        m.query(listener, RETRY, "a", "b", "c");
        assertEquals(m.pendingRequests(), 3);
        
        CachedBulkManager<String, String> m2 = new CachedBulkManager<>(requester, DAEMON);
        m2.query(null, NONE, "a", "b", "c");
        assertEquals(m2.pendingRequests(), 1);
        m2.query(null, NONE, "a", "b", "c");
        assertEquals(m2.pendingRequests(), 2);
        m2.query(null, RETRY, "a", "b", "c");
        assertEquals(m2.pendingRequests(), 3);
        m2.query(null, RETRY, "a", "b", "c");
        assertEquals(m2.pendingRequests(), 4);
        m2.query(listener, RETRY, "a", "b", "c");
        assertEquals(m2.pendingRequests(), 5);
        m2.query(listener, RETRY | UNIQUE, "a", "b", "c");
        assertEquals(m2.pendingRequests(), 6);
        m2.query(listener, RETRY | UNIQUE, "a", "b", "c");
        assertEquals(m2.pendingRequests(), 6);
    }
    
    @Test
    public void testPartialAll() {
        MyRequester requester = makeRequester(new String[][][]{
        });
        MyResultListener listener = makeListener(new String[][][]{
            {
                {"a", "r.a"},
            },
            {
                {"a", "r.a"},
                {"b", "r.b"}
            },
            {
                {"a", "r.a"},
                {"b", "r.b"},
                {"c", "r.c"},
            }
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.query(listener, PARTIAL, "a", "b", "c");
        assertEquals(m.pendingRequests(), 1);
        m.setResult("a", "r.a");
        assertEquals(m.pendingRequests(), 1);
        m.setResult("b", "r.b");
        
        // Unrelated, shouldn't create a result
        m.setResult("d", "r.d");
        
        m.setResult("c", "r.c");
        assertEquals(m.pendingRequests(), 0);
    }
    
    @Test
    public void testPartial() {
        MyRequester requester = makeRequester(new String[][][]{
        });
        MyResultListener listener = makeListener(new String[][][]{
            {
                {"a", "r.a"},
                {"b", "r.b"},
            },
            {
                {"a", "r.a"},
                {"b", "r.b"},
                {"c", "r.c"},
            }
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.query(listener, RETRY, "a", "b", "c");
        assertEquals(m.pendingRequests(), 1);
        m.setResult("a", "r.a");
        assertEquals(m.pendingRequests(), 1);
        m.setResult("b", "r.b");
        
        // Unrelated, shouldn't create a result
        m.setResult("d", "r.d");
        
        listener.calledCount(0);
        m.setError("c");
        listener.calledCount(1);
        assertEquals(m.pendingRequests(), 1);
        m.setResult("c", "r.c");
        listener.calledCount(2);
        assertEquals(m.pendingRequests(), 0);
    }
    
    @Test
    public void testWait() {
        MyRequester requester = makeRequester(new String[][][]{
            {
                {}, {"c"}, {}, {"c"}
            }
        });
        MyResultListener listener = makeListener(new String[][][]{
            {
                {"a", "r.a"},
                {"b", null},
                {"c", "r.c"}
            }
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.setResult("a", "r.a");
        m.setNotFound("b");
        m.query(listener, WAIT, "a", "b", "c");
        m.doRequests();
        m.doRequests();
        requester.requestCount(1);
        m.setResult("a", "r.a");
        listener.calledCount(0);
        m.setError("c");
        listener.calledCount(0);
        m.setResult("c", "r.c");
        listener.calledCount(1);
        assertEquals(m.pendingRequests(), 0);
    }
    
    @Test
    public void test5() {
        MyRequester requester = makeRequester(new String[][][]{
            
        });
        MyResultListener listener = makeListener(new String[][][]{
            {
                {"a", null},
                {"b", null},
                {"c", null}
            },
            {
                {"d", "r.d"},
            },
            {
                {"d", "r.d"},
                {"e", "r.e"},
            },
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.getOrQuery(this, listener, RETRY, "a", "b", "c");
        m.setNotFound("a", "b", "c");
        assertEquals(m.pendingRequests(), 0);
        m.getOrQuery(this, listener, RETRY, "d", "e");
        m.setError("d", "e");
        m.setResult("d", "r.d");
        m.setResult("e", "r.e");
    }
    
    /**
     * Request again after error, with ASAP and NONE
     */
    @Test
    public void test6() {
        MyRequester requester = makeRequester(new String[][][]{
            {
                {"a"}, {}, {}, {}
            }
        });
        MyResultListener listener = makeListener(new String[][][]{
            {
                {"a", null},
            },
            {
                {"a", null},
            },
            {
                {"a", null},
            },
            {
                {"a", null},
            },
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.getOrQuerySingle(this, listener, ASAP, "a");
        m.setError("a");
        // Listener call
        m.getOrQuerySingle(this, listener, ASAP, "a");
        m.getOrQuerySingle(this, listener, NONE, "a");
        // No listener call
        m.getOrQuerySingle(this, listener, RETRY, "a");
        m.getOrQuerySingle(this, listener, WAIT, "a");
        // Listener call
        m.getOrQuerySingle(this, listener, UNIQUE, "a");
    }
    
    @Test
    public void testWait2() {
        MyRequester requester = makeRequester(new String[][][]{
            {
                {}, {"c"}, {}, {"c"}
            },
            {
                {"d"}, {}, {"c"}, {}
            }
        });
        MyResultListener listener = makeListener(new String[][][]{
            {
                {"a", "r.a"},
                {"b", null},
                {"c", "r.c"}
            }
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.setResult("a", "r.a");
        m.setNotFound("b");
        m.query(listener, WAIT, "a", "b", "c");
        m.doRequests();
        m.doRequests();
        requester.requestCount(1);
        m.setResult("a", "r.a");
        listener.calledCount(0);
        m.setError("c");
        listener.calledCount(0);
        m.query(listener, ASAP, "d");
        m.setResult("c", "r.c");
        listener.calledCount(1);
        assertEquals(m.pendingRequests(), 1);
    }
    
    @Test
    public void testRefresh() {
        MyRequester requester = makeRequester(new String[][][]{
            {
                {"a", "b", "c"}, {}, {}, {"a", "b", "c"}
            },
            {
                {}, {"b", "c"}, {}, {"b", "c"}
            },
            {
                {}, {"b", "c"}, {}, {"b", "c"}
            }
        });
        MyResultListener listener = makeListener(new String[][][]{
            {
                {"a", "r.a"},
                {"b", "r.b"},
                {"c", null}
            },
            {
                {"b", null},
                {"c", null}
            },
            {
                {"b", null},
            }
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.setResult("a", "r.a");
        m.setNotFound("b");
        m.setError("c");
        m.query(listener, ASAP | REFRESH, "a", "b", "c");
        m.doRequests();
        m.doRequests();
        requester.requestCount(1);
        m.setResult("a", "r.a");
        m.setResult("b", "r.b");
        m.setError("c");
        listener.calledCount(1);
        m.setNotFound("abc");
        m.query(listener, REFRESH, "b", "c");
        requester.requestCount(1);
        m.doRequests();
        m.setError("b", "c");
        requester.requestCount(2);
        m.query(listener, REFRESH | RETRY, "b", "c");
        requester.requestCount(2);
        m.doRequests();
        assertEquals(m.pendingRequests(), 1);
        m.setError("b", "c");
        requester.requestCount(3);
        listener.calledCount(2);
        m.setNotFound("b");
        listener.calledCount(3);
        assertEquals(m.pendingRequests(), 1);
    }
    
    @Test
    public void testReplace() {
        MyRequester requester = makeRequester(new String[][][]{
            {
                {"a", "b"}, {}, {}, {"a", "b"}
            },
            {
                {}, {"b", "c"}, {}, {"b", "c"}
            }
        });
        MyResultListener listenerA = makeListener(new String[][][]{
            {
                {"nope", "nope"},
            },
        });
        MyResultListener listenerB = makeListener(new String[][][]{
            {
                {"a", "r.a"},
                {"b", "r.b"},
            },
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON | ASAP);
        m.query(m, listenerA, NONE, "a", "b");
        m.query(m, listenerB, NONE, "a", "b");
        m.setResult("a", "r.a");
        m.setResult("b", "r.b");
        listenerA.calledCount(0);
        listenerB.calledCount(1);
        requester.requestCount(1);
    }
    
    @Test
    public void testNoReplace() {
        MyRequester requester = makeRequester(new String[][][]{
            {
                {"a", "b"}, {}, {}, {"a", "b"}
            },
            {
                {}, {"b", "c"}, {}, {"b", "c"}
            }
        });
        MyResultListener listenerA = makeListener(new String[][][]{
            {
                {"a", "r.a"},
                {"b", "r.b"},
            },
        });
        MyResultListener listenerB = makeListener(new String[][][]{
            {
                {"nope", "nope"},
            },
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON | ASAP);
        m.query(m, listenerA, NO_REPLACE, "a", "b");
        m.query(m, listenerB, NO_REPLACE, "a", "b");
        m.setResult("a", "r.a");
        m.setResult("b", "r.b");
        listenerA.calledCount(1);
        listenerB.calledCount(0);
        requester.requestCount(1);
    }
    
    private static class CustomObject {
        
        public final String key;
        public final String someValue;
        
        public CustomObject(String key, String someValue) {
            this.key = key;
            this.someValue = someValue;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CustomObject other = (CustomObject) obj;
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return true;
        }
        
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.key);
            return hash;
        }
        
    }
    
    /**
     * An object as key that equals on part of the object properties.
     */
    @Test
    public void testCustomObject() {
        Requester<CustomObject, String> requester = new Requester<CustomObject, String>() {

            @Override
            public void request(CachedBulkManager<CustomObject, String> manager, Set<CustomObject> asap, Set<CustomObject> normal, Set<CustomObject> backlog) {
                Set<CustomObject> keys = manager.makeAndSetRequested(asap, normal, backlog, 100);
                for (CustomObject key : keys) {
                    assertNotNull(key.someValue);
                }
            }
        };
        CachedBulkManager<CustomObject, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.getOrQuery(null, null, NONE, new CustomObject("a", "stuff"));
        m.setResult(new CustomObject("b", null), "r.b");
        assertEquals(m.get(new CustomObject("a", null)), null);
        m.setResult(new CustomObject("a", null), "r.a");
        assertEquals(m.get(new CustomObject("a", null)), "r.a");
    }
    
    @Test
    public void testErrorAsap() {
        MyRequester requester = makeRequester(new String[][][]{
            {
                {"a"}, {}, {}, {}
            }
        });
        MyResultListener listener = makeListener(new String[][][]{
            {
                {"a", null},
            }
        });
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.getOrQuerySingle(this, listener, ASAP, "a");
        m.setError("a");
    }
    
    @Test
    public void testErrorAsap2() {
        MyRequester requester = makeRequester(new String[][][]{
            {
                {"a"}, {}, {}, {}
            },
            {
                {"b"}, {}, {}, {}
            }
        });
        MyResultListener listener = makeListener(new String[][][]{
            {
                {"a", null}
            },
            {
                {"a", null},
                {"b", "r.b"}
            }
        });
        
        Collection<String> keys = new ArrayList<>();
        keys.add("a");
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        Result<String, String> result = m.getOrQuery(this, listener, ASAP, keys);
        assertNull(result);
        m.setError("a");
        keys.add("b");
        result = m.getOrQuery(this, listener, ASAP, keys);
        assertNull(result);
        m.setResult("b", "r.b");
        result = m.getOrQuery(this, listener, ASAP, keys);
        assertEquals(result.getResults().size(), 2);
    }
    
    @Test
    public void testUnique() {
        MyRequester requester = makeRequester(new String[][][]{
            {{}, {"b"}, {}, {"b"}}
        });
        MyResultListener listener = makeListener(new String[][][]{});
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.query(this, listener, NONE, "a");
        assertEquals(m.pendingRequests(), 1);
        m.query(this, null, NONE, "b");
        assertEquals(m.pendingRequests(), 1);
        m.doRequests();
        requester.requestCount(1);
        m.setResult("a", "r.a");
        assertEquals(m.pendingRequests(), 1);
        m.setResult("b", "r.b");
        assertEquals(m.pendingRequests(), 0);
        listener.calledCount(0);
    }
    
    @Test
    public void testMake() {
        Requester requester = new Requester() {

            private int request = 0;
            
            @Override
            public void request(CachedBulkManager manager, Set asap, Set normal, Set backlog) {
                manager.makeAndSetRequested(asap, normal, backlog, 10);
                request++;
                if (request == 1) {
                    assertTrue(normal.size() == 3);
                }
                if (request == 2) {
                    assertTrue(normal.size() == 1);
                }
            }
        };
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
        m.query(null, NONE, "a", "b", "c");
        m.doRequests();
        m.query(null, NONE, "d");
        m.doRequests();
    }
    
    @Test
    public void testSync() throws InterruptedException {
        for (int i=0;i<100;i++) {
            System.out.println("---");
            MyRequester requester = new MyRequester(new String[][][]{
                {{}, {"a","b","c"}, {}, {"a","b","c"}}
            });

            CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, DAEMON);
            m.query(null, NONE, "a", "b", "c");
            new Thread(new Runnable() {

                @Override
                public void run() {
                    System.out.println("a");
                    m.doRequests();
                }
            }).start();
            System.out.println("b");
            m.doRequests();
        }
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(CacheBulkManagerTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
    
    private static MyRequester makeRequester(String[][][] data) {
        return new MyRequester(data);
    }
    
    private static MyResultListener makeListener(String[][][] data) {
        return new MyResultListener(data);
    }
    
    private static class MyRequester implements Requester<String, String> {

        private final String[][][] data;
        private int request = 0;
        
        public MyRequester(String[][][] data) {
            this.data = data;
        }
        
        public void requestCount(int count) {
            assertEquals("Wrong request called count", count, request);
        }

        @Override
        public void request(CachedBulkManager<String, String> manager, Set<String> asap, Set<String> normal, Set<String> backlog) {
//            System.out.println("Request " + request + " " + asap + normal);
            assertTrue("More requests than defined in test", data.length > request);
            String[][] items = data[request];
            String[] asapTest = items[0];
            String[] normalTest = items[1];
            String[] backlogTest = items[2];
            String[] requested = items[3];
            assertEquals("Wrong asap request amount", asap.size(), asapTest.length);
            for (String key : asapTest) {
                asap.contains(key);
            }
            assertEquals("Wrong normal request amount", normal.size(), normalTest.length);
            for (String key : normalTest) {
                normal.contains(key);
            }
            assertEquals("Wrong backlog request amount", backlog.size(), backlogTest.length);
            for (String key : backlogTest) {
                backlog.contains(key);
            }
            manager.setRequested(requested);
            request++;
        }
        
    }
    
    private static class MyResultListener implements ResultListener<String, String> {
        
        private final String[][][] data;
        private int called = 0;
        
        public MyResultListener(String[][][] data) {
            this.data = data;
        }

        public void calledCount(int amount) {
            assertEquals("Wrong listener called count", amount, called);
        }

        @Override
        public void result(Result<String, String> result) {
            assertTrue("Not enough result definitions", data.length > called);
            String[][] items = data[called];
            assertEquals("Invalid result count", items.length, result.getResults().size());
            for (String[] item : items) {
                assertTrue("Missing result item", result.getResults().containsKey(item[0]));
                assertEquals("Invalid test: Result must be key-value", item.length, 2);
                assertEquals("Result item", item[1], result.get(item[0]));
            }
            called++;
        }
        
    }
    
    @Test
    public void testB() {
        Requester requester = new CachedBulkManager.Requester<String, String>() {

            private int request = 0;
            
            @Override
            public void request(CachedBulkManager<String, String> manager, Set<String> asap, Set<String> normal, Set<String> backlog) {
                request++;
                System.out.println("Request "+request);
                if (request == 1) {
                    assertTrue(normal.isEmpty());
                    assertTrue(asap.contains("b"));
                    assertTrue(asap.size() == 1);
                }
            }
        };
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, 0);
        m.setResult("a", "aResult");
        m.query(null, ASAP, "a", "b");
    }
    
    @Test
    public void testSomething() {
        
        Requester requester = new CachedBulkManager.Requester<String, String>() {

            private int request = 0;
            
            @Override
            public void request(CachedBulkManager<String, String> manager, Set<String> asap, Set<String> normal, Set<String> backlog) {
                request++;
                System.out.println("Request "+request);
                if (request == 1) {
                    assertTrue(normal.contains("test1"));
                    assertTrue(normal.contains("abc"));
                    assertTrue(normal.size() == 2);
                    assertTrue(asap.contains("test2"));
                    assertTrue(asap.size() == 1);
                } else if (request == 2) {
                    assertTrue(normal.contains("test1"));
                    assertTrue(normal.contains("abc"));
                    assertTrue(normal.size() == 2);
                    assertTrue(asap.isEmpty());
                } else if (request == 3) {
                    assertTrue(normal.contains("test1"));
                    assertTrue(normal.contains("abc"));
                    assertTrue(normal.size() == 2);
                    assertTrue(asap.isEmpty());
                }
            }
        };
        
        CachedBulkManager<String, String> m = new CachedBulkManager<>(requester, 0);
        m.query(null, 0, "test1", "abc");
        m.query(r -> {
            assertTrue(r.hasAllKeys());
            assertEquals(r.get("test2"), "test2Result");
        }, ASAP, "test2");
        m.setResult("test2", "test2Result");
        m.query(null, ASAP, "test2");
        m.setError("abc");
        m.query(null, ASAP, "test2");
    }
    
}
