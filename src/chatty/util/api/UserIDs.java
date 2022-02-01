
package chatty.util.api;

import chatty.Helper;
import chatty.util.StringUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class UserIDs {
    
    private static final Logger LOGGER = Logger.getLogger(UserIDs.class.getName());
    
    private static final long CHECK_PENDING_DELAY = 10*1000;
    private static final long REQUEST_DELAY = 5;
    private static final long ERROR_PENALTY = 30;
    
    private final Data data = new Data();
    private final Collection<Request> requests = new LinkedList<>();
    private final Set<String> requestPending = new HashSet<>();
    private int errors = 0;
    private long lastRequest = 0;
    
    private final TwitchApi api;
    
    public UserIDs(TwitchApi api) {
        this.api = api;
        Timer checkPending = new Timer("UserIDPending", true);
        checkPending.schedule(new TimerTask() {

            @Override
            public void run() {
                checkRequest();
            }
        }, CHECK_PENDING_DELAY, CHECK_PENDING_DELAY);
    }
    
    /**
     * Request the userids for the given names, and wait for the result. Will
     * only return a result if the id for *all* names is available, so the
     * caller may end up waiting forever. This is useful if not having the code
     * prevents you from progressing and any way.
     * 
     * These are not removed, unless the request succeeds, but the name is not
     * found. This means that requests can more easily stack up, so the caller
     * should make sure to prevent that, or else if errors clear up (like the
     * API can suddenly be reached after some time) a whole lot of request
     * listeners may be called at once.
     * 
     * @param result
     * @param usernames 
     */
    public void waitForUserIDs(UserIdResultListener result, String... usernames) {
        Collection<String> names = prepareNames(usernames);
        addRequest(result, names, true);
        checkDoneRequests(true);
        checkRequest();
    }
    
    /**
     * Request the ID for each of the given names, as soon as possible. This
     * will not wait for a request, it will request missing IDs immediately.
     * This can be used for actions that are triggered by the user and thus
     * should be done asap.
     * 
     * @param result
     * @param usernames 
     */
    public void getUserIDsAsap(UserIdResultListener result, String... usernames) {
        Collection<String> names = prepareNames(usernames);
        addRequest(result, names, false);
        checkDoneRequests(true);
        performRequest();
    }
    
    /**
     * Convenience method for getUserIDs(UserIdResultListener, Collection<String>).
     * 
     * @param result
     * @param usernames 
     */
    public void getUserIDs(UserIdResultListener result, String... usernames) {
        Collection<String> names = prepareNames(usernames);
        getUserIDs(result, names);
    }
    
    /**
     * Gets the given user ids, if cached, or otherwise requests them on the
     * normal request cycle (so it could take a while).
     * 
     * @param result
     * @param names 
     */
    public void getUserIDs(UserIdResultListener result, Collection<String> names) {
        addRequest(result, names, false);
        checkDoneRequests(true);
    }
    
    /**
     * Return cached results if all names are cached (whether they have an id
     * or not, but have been requsted before), or null. Adds any names missing
     * an id to be requested.
     * 
     * @param usernames
     * @return 
     */
    public synchronized UserIdResult requestUserIDs(String... usernames) {
        Collection names = prepareNames(usernames);
        UserIdResult result = getCachedResult(names);
        if (result == null || result.getValidIDs().size() < names.size()) {
            addRequest(null, names, false);
        }
        return result;
    }
    
    private Collection<String> prepareNames(String[] usernames) {
        Collection<String> names = new ArrayList<>();
        for (String name : usernames) {
            names.add(StringUtil.toLowerCase(name));
        }
        return names;
    }
    
    public void setUserId(String name, String id) {
        if (name == null || id == null || name.isEmpty() || id.isEmpty()) {
            return;
        }
        data.setId(StringUtil.toLowerCase(name), id);
    }
    
    public void handleRequestResult(Set<String> requestedNames, Map<String, String> result) {
        synchronized(this) {
            requestPending.removeAll(requestedNames);
            
            if (result == null) {
                requestedNames.stream().forEach(n -> data.setError(n));
                errors++;
            } else {
                for (String name : requestedNames) {
                    if (!result.keySet().contains(name)) {
                        data.setNotFound(name);
                    } else {
                        data.setId(name, result.get(name));
                    }
                }
                errors = errors > 0 ? errors - 1 : 0;
            }
        }
        checkDoneRequests(false);
    }
    
    private static Map<String, String> parseResult(String text) {
        if (text == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(text);
            JSONArray users = (JSONArray)root.get("users");
            for (Object o : users) {
                JSONObject user = (JSONObject)o;
                
                String id = (String)user.get("_id");
                String name = (String)user.get("name");
                if (id != null && name != null) {
                    result.put(name, id);
                }
            }
            return result;
        } catch (Exception ex) {
            LOGGER.warning("Error parsing userids: "+ex);
            return null;
        }
    }
    
    private synchronized void checkRequest() {
        long timePassed = System.currentTimeMillis() - lastRequest;
        if (timePassed > (REQUEST_DELAY + ERROR_PENALTY * errors) * 1000) {
            performRequest();
        }
    }
    
    private synchronized void performRequest() {
        if (requests.isEmpty()) {
            return;
        }
        Set<String> namesToRequest = new HashSet<>();
        requests.stream().forEach(r -> {
            r.usernames.stream().forEach(n -> {
                if (!requestPending.contains(n) && data.shouldRequest(n) && namesToRequest.size() < 100) {
                    if (Helper.isValidStream(n)) {
                        namesToRequest.add(n);
                        requestPending.add(n);
                    } else {
                        data.setNotFound(n);
                    }
                }
            });
        });

        if (!namesToRequest.isEmpty()) {
            api.requests.requestUserIDs(namesToRequest);
            lastRequest = System.currentTimeMillis();
        } else {
            checkDoneRequests(false);
        }
    }
    
    private synchronized void addRequest(UserIdResultListener result, Collection<String> usernames,
            boolean wait) {
        requests.add(new Request(new HashSet<>(usernames), result, wait));
        //System.out.println("Added request: "+requests);
    }
    
    private void checkDoneRequests(boolean onlyComplete) {
        Collection<Request> done = getDoneRequests(onlyComplete);
        if (done == null) {
            return;
        }
        for (Request r : done) {
            if (r.listener != null) {
                r.listener.result(r.getResult());
            }
        }
        clearUp();
    }
    
    private synchronized Collection<Request> getDoneRequests(boolean onlyComplete) {
        if (requests.isEmpty()) {
            return null;
        }
        Collection<Request> result = new ArrayList<>();
        for (Request r : requests) {
            UserIdResult idResult = getCachedResult(r.usernames);
            if (idResult != null
                    && (!r.wait || !idResult.hasError())
                    && (!onlyComplete || !eligibleForRequest(r))) {
                r.setResult(idResult);
                result.add(r);
            }
        }
        requests.removeAll(result);
        return result;
    }
    
    /**
     * Returns the Entry object for all given usernames, or null if not all
     * are cached. Note that the Entry objects may not have an idea attached,
     * they could also have been added due to a request error. It just means
     * that they have been requested before, successful or not.
     * 
     * @param usernames
     * @return 
     */
    private synchronized UserIdResult getCachedResult(Collection<String> usernames) {
        Map<String, Entry> result = data.get(usernames);
        if (result.size() < usernames.size()) {
            return null;
        }
        return new UserIdResult(result);
    }
    
    private synchronized void clearUp() {
//        System.out.println("Before cleanup: "+requests);
        if (!requests.isEmpty()) {
            Iterator<Request> it = requests.iterator();
            while (it.hasNext()) {
                if (!eligibleForRequest(it.next())) {
                    it.remove();
                }
            }
        }
//        System.out.println("After cleanup: "+requests);
    }
    
    /**
     * Returns true if at least one of the names in the Request is still
     * eligible for request.
     * 
     * @param request
     * @return 
     */
    private boolean eligibleForRequest(Request request) {
        for (String name : request.usernames) {
            if (data.shouldRequest(name)) {
                return true;
            }
        }
        return false;
    }
    
    public interface UserIdResultListener {
        public void result(UserIdResult result);
    }
    
    public static class UserIdResult {
        
        private final Map<String, String> data = new HashMap<>();
        private boolean hasError;
        private String error;
        
        private UserIdResult(Map<String, Entry> result) {
            for (Entry entry : result.values()) {
                if (entry.id != null) {
                    data.put(entry.name, entry.id);
                }
                if (entry.id == null) {
                    hasError = true;
                }
                if (entry.notFound) {
                    error = "User not found";
                }
            }
        }
        
        public boolean hasError() {
            return hasError;
        }
        
        public String getError() {
            if (hasError && error == null) {
                return "Unknown Error";
            }
            return error;
        }
        
        public Collection<String> getValidIDs() {
            return data.values();
        }
        
        public Map<String, String> getData() {
            return data;
        }
        
        public String getId(String name) {
            return data.get(StringUtil.toLowerCase(name));
        }
        
        @Override
        public String toString() {
            return data.toString()+"/"+getError();
        }
    }
    
    public static void main(String[] args) {
//        UserIDs2 u = new UserIDs2();
//        u.setUserId("a", null);
//        u.setUserId("b", "bid");
//        u.getUserIDs(r -> {
//            System.out.println(r.hasError()+" "+r.getValidIDs());
//        }, "a", "b");
    }
    
    private static class Request {
        
        private final Set<String> usernames;
        private final UserIdResultListener listener;
        private final boolean wait;
        
        private UserIdResult result;
        
        Request(Set<String> usernames, UserIdResultListener listener, boolean wait) {
            this.usernames = usernames;
            this.listener = listener;
            this.wait = wait;
        }
        
        public synchronized void setResult(UserIdResult result) {
            this.result = result;
        }
        
        public synchronized UserIdResult getResult() {
            return result;
        }
        
        @Override
        public String toString() {
            return String.format("{%s/%s}", usernames.toString(), wait);
        }
        
    }

    
    private static class Data {
        
        private final Map<String, Entry> data = new HashMap<>();
        
        public synchronized void put(String name, Entry entry) {
            data.put(name, entry);
        }
        
        public synchronized boolean containsKey(String name) {
            return data.containsKey(name);
        }
        
        public synchronized Entry get(String name) {
            return data.get(name);
        }
        
        public synchronized boolean setId(String name, String id) {
            if (!data.containsKey(name) || get(name).id == null) {
                data.put(name, new Entry(name, id));
                return true;
            }
            return false;
        }
        
        public synchronized void setNotFound(String name) {
            Entry entry;
            if (!data.containsKey(name)) {
                entry = new Entry(name, null);
                data.put(name, entry);
            } else {
                entry = data.get(name);
            }
            entry.notFound = true;
            entry.errors += 4;
        }
        
        public synchronized void setError(String name) {
            if (data.containsKey(name)) {
//                Entry entry = data.get(name);
//                entry.errors++;
//                System.out.println(entry);
            } else {
                Entry entry = new Entry(name, null);
                //entry.errors++;
                data.put(name, entry);
            }
        }
        
        /**
         * Return all Entry objects for the given names, as far as they are
         * available. This includes all Entry objects, even those that don't
         * contain an id (that would be from errored requests, or if the name
         * doesn't exist at all in the API).
         * 
         * @param usernames
         * @return 
         */
        public synchronized Map<String, Entry> get(Collection<String> usernames) {
            Map<String, Entry> result = new HashMap<>();
            for (String name : usernames) {
                if (data.containsKey(name)) {
                    result.put(name, data.get(name));
                }
            }
            return result;
        }
        
        public synchronized boolean hasId(String name) {
            return data.containsKey(name) && data.get(name).id != null;
        }
        
        /**
         * Returns true if the id for this name should be requested again, so if
         * it hasn't been requested yet, or only few errors occured so far.
         * 
         * @param name
         * @return 
         */
        public synchronized boolean shouldRequest(String name) {
            if (!data.containsKey(name)) {
                return true;
            }
            Entry entry = data.get(name);
            return entry.id == null && entry.errors < 10;
        }
        
    }
    
    private static class Entry {
        
        private final String name;
        private final String id;
        
        private volatile boolean notFound;
        private int errors;
        
        public Entry(String name, String id) {
            this.name = name;
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public String getId() {
            return id;
        }
        
        public boolean notFound() {
            return notFound;
        }
        
        public int errorCount() {
            return errors;
        }
        
        @Override
        public String toString() {
            return name+"/"+id+"/"+(notFound ? "n/a" : "")+"/"+errors;
        }
    }
    
}
