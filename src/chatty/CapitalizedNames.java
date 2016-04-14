
package chatty;

import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.TwitchApi;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class CapitalizedNames {
    
    private static final Logger LOGGER = Logger.getLogger(CapitalizedNames.class.getName());
    
    private final Path FILE = Paths.get(Chatty.getUserDataDirectory(), "capitalizedNames");
    private final Charset CHARSET = Charset.forName("UTF-8");
    
    private final int DELAY = 60*1000;
    
    /**
     * The listeners that will be informed if the capitalization of a name has
     * changed.
     */
    private final Set<CapitalizedNamesListener> listeners = new HashSet<>();
    
    /**
     * Names that have capitalization saved for them, either from the file or
     * just requested. A map with a key as name is used for easier lookup of
     * names.
     */
    private final Map<String, CapitalizedName> names = new HashMap<>();
    
    /**
     * Names that were requested, but the request failed.
     */
    private final Set<String> erroredNames = new HashSet<>();
    
    /**
     * Names that are supposed to be requested. Either because they don't have
     * capitalization yet, or their capitalization should be refreshed. A map
     * with the key as name is used for easier lookup and removeal of names.
     */
    private final Map<String, NameForRequest> queuedForRequest = new HashMap<>();
    
    private final TwitchApi api;
    
    private boolean saved;
    
    public CapitalizedNames(TwitchApi api) {
        this.api = api;
        
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                requestName();
            }
        }, DELAY, DELAY);
        
        loadFromFile();
    }
    
    private synchronized void loadFromFile() {
        try (BufferedReader reader = Files.newBufferedReader(FILE, CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] split = line.split(" ");
                    String name = split[0];
                    long time = -1;
                    if (split.length == 2) {
                        try {
                            time = Long.parseLong(split[1]);
                        } catch (NumberFormatException ex) {
                            time = -1;
                        }
                    }
                    boolean fixed = time == -1;
                    if (split.length == 3) {
                        if (split[2].equals("f")) {
                            fixed = true;
                        }
                    }
                    names.put(StringUtil.toLowerCase(name), new CapitalizedName(name, time, fixed));
                }
            }
            LOGGER.info("Loaded "+names.size()+" names from: "+FILE);
        } catch (IOException ex) {
            LOGGER.warning("Error loading names: "+ex);
        }
    }
    
    public synchronized void saveToFileOnce() {
        if (!saved) {
            saveToFile();
        }
    }
    
    public synchronized void saveToFile() {
        try (BufferedWriter writer = Files.newBufferedWriter(FILE, CHARSET)) {
            for (CapitalizedName name : names.values()) {
                writer.write(name.capitalizedName);
                writer.write(" ");
                writer.write(String.valueOf(name.time));
                if (name.fixed) {
                    writer.write(" ");
                    writer.write("f");
                }
                writer.newLine();
            }
            LOGGER.info("Saved "+names.size()+" names to: "+FILE);
            saved = true;
        } catch (IOException ex) {
            LOGGER.warning("Error saving names: "+ex);
        }
    }
    
    /**
     * Unsynchronized, everything that should be synchronized on this is in
     * pollNameForRequest()
     */
    private void requestName() {
        String name = pollNameForRequest();
        if (name != null) {
            api.requestUserInfo(name);
        }
    }
    
    /**
     * Get the capitalized name for a name. If no capatalized name exists for
     * the given name or it should be refreshed, it is added to the queue to be
     * requested.
     *
     * @param name The name to get the capitalized name for
     * @return The capitalized name, or null if none exists or the name is
     * invalid
     */
    public synchronized String getName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        CapitalizedName exisiting = names.get(name);
        if (exisiting == null) {
            addNameForRequest(name, 0);
            return null;
        } else {
            int refreshPriority = exisiting.refreshPriority();
            //System.out.println("RP"+refreshPriority+" "+name);
            if (refreshPriority > 0) {
                //System.out.println("Added name for refresh "+name+" "+refreshPriority);
                addNameForRequest(name, -(10 - refreshPriority));
            }
        }
        return exisiting.capitalizedName;
    }
    
    /**
     * Increase the activity of the given name, if it is currently in the queue
     * to be requested.
     * 
     * @param name 
     */
    public synchronized void activity(String name) {
        if (queuedForRequest.containsKey(name)) {
            queuedForRequest.get(name).increasePriority();
        }
    }

    /**
     * Adds the name to the queue to be requested, unless it already has
     * capitalization or the request failed before. If already in the queue, it
     * increases the activity for that name.
     *
     * @param name 
     */
    private void addNameForRequest(String name, int initialPriority) {
        if (!erroredNames.contains(name)) {
            NameForRequest existing = queuedForRequest.get(name);
            if (existing == null) {
                queuedForRequest.put(name, new NameForRequest(name, initialPriority));
            } else {
                existing.increasePriority();
            }
        }
    }

    /**
     * Gets the name with the highest priority from the request queue.
     * 
     * @return 
     */
    private synchronized String pollNameForRequest() {
        long start = System.currentTimeMillis();
        if (!queuedForRequest.isEmpty()) {
            List<NameForRequest> list = new ArrayList<>(queuedForRequest.values());
            
            Collections.sort(list);
            //System.out.println(list.size()+" "+list);
            String name = list.get(0).name;
            queuedForRequest.remove(name);
            //System.out.println(System.currentTimeMillis() - start);
            LOGGER.info("["+(System.currentTimeMillis() - start)+"] Names Cached: "+names.size()+" Queued: "
                    +list.size()+" "+Helper.subList(list, 0, 10));
            if (!erroredNames.contains(name)) {
                return name;
            }
        }
        return null;
    }
    
    /**
     * Sets a capitalization for a name. Informs the listeners of the change if
     * the new capitalization is different from the current one.
     * 
     * @param name All lowercase name
     * @param capitalizedName Capitalized name
     */
    public synchronized void setName(String name, String capitalizedName) {
        setName(name, capitalizedName, false);
    }
    
    /**
     * Sets a capitalization for a name. Informs the listeners of the change if
     * the new capitalization is different from the current one.
     * 
     * @param name All lowercase name
     * @param capitalizedName Capitalized name
     * @param fixed Whether this capitalization should be fixed (manually set)
     */
    public synchronized void setName(String name, String capitalizedName,
            boolean fixed) {
        queuedForRequest.remove(name);
        if (capitalizedName == null) {
            erroredNames.add(name);
        } else {
            CapitalizedName current = names.get(name);
            if (current == null || !current.fixed || fixed) {
                CapitalizedName updated = new CapitalizedName(capitalizedName, System.currentTimeMillis(), fixed);
                names.put(name, updated);
                if (!updated.equals(current)) {
                    informListeners(name, capitalizedName);
                }
            }
        }
    }
    
    private void informListeners(String name, String capitalizedName) {
        for (CapitalizedNamesListener listener : listeners) {
            listener.setName(name, capitalizedName);
        }
    }
    
    public synchronized void addListener(CapitalizedNamesListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    public String commandRefreshCase(String parameter) {
        if (parameter == null) {
            return "Usage: /refreshCase <name>";
        } else {
            String name = parameter.split(" ")[0];
            if (!Helper.validateStream(name)) {
                return "Invalid name.";
            } else {
                boolean fixed = false;
                // Synchronize here, so API call isn't in the lock (which may
                // or may not cause problems)
                synchronized(this) {
                    CapitalizedName cached = names.get(StringUtil.toLowerCase(name));
                    if (cached != null && cached.fixed) {
                        names.remove(StringUtil.toLowerCase(name));
                        name = cached.capitalizedName;
                        fixed = true;
                    }
                }
                api.requestUserInfo(StringUtil.toLowerCase(name));
                if (fixed) {
                    return "Capitalization was manually set to " + name + " (resetting to default now)";
                } else {
                    return "Refreshing capitalization for " + name;
                }
            }
        }
    }
    
    public synchronized String commandSetCase(String parameter) {
        if (parameter == null) {
            return "Usage: /setCase <capitalized_name>";
        } else {
            String name = parameter.split(" ")[0];
            if (!Helper.validateStream(name)) {
                return "Invalid name.";
            } else {
                setName(StringUtil.toLowerCase(name), name, true);
                return "Setting fixed capitalization to " + name;
            }
        }
    }
    
    public synchronized String commandGetCase(String parameter) {
        if (parameter == null) {
            return "Usage: /getCase <name>";
        }
        String name = parameter.split(" ")[0];
        if (!Helper.validateStream(name)) {
            return "Invalid name.";
        } else {
            CapitalizedName cached = names.get(StringUtil.toLowerCase(name));
            if (cached == null) {
                return "No capitalization set for "+name;
            }
            String timeInfo = cached.time == -1 ? "set at unknown time"
                    : "set "+DateTime.agoSingleVerbose(cached.time)+" ago, "+DateTime.formatFullDatetime(cached.time);
            return "Capitalization for " + name + " is "+cached.capitalizedName+" ("+(cached.fixed ? "manually " : "")+timeInfo+")";
        }
    }
    
    
    /**
     * Listener that can be implemented by classes that want to be informed
     * about changes in capitalization for names.
     */
    public static interface CapitalizedNamesListener {
        
        /**
         * When the capitalization for a name changes, either because it got one
         * at all, or it changed.
         * 
         * @param name All lowercase name
         * @param capitalizedName The name with correct case
         */
        public void setName(String name, String capitalizedName);
    }
    
    /**
     * A name that alaready has capitalization, with the time the capitalization
     * was set, so it can be determined when it should be refreshed.
     */
    private static class CapitalizedName {
        
        private static final int WEEK = 1000*60*60*24*7;
        
        public final String capitalizedName;
        public final long time;
        public final boolean fixed;
        
        public CapitalizedName(String capitalizedName, long time, boolean fixed) {
            this.capitalizedName = capitalizedName;
            this.time = time;
            this.fixed = fixed;
        }
        
        @Override
        public String toString() {
            return capitalizedName;
        }
        
        /**
         * How important refreshing this should be. An integer between 0 and 10,
         * with 0 meaning it shouldn't be refreshed at all.
         * 
         * @return 
         */
        public int refreshPriority() {
            if (!fixed && time >= 0) {
                int weeksOld = (int) ((System.currentTimeMillis() - time) / WEEK);
                if (weeksOld > 8) {
                    return 10;
                } else if (weeksOld > 4) {
                    return 5;
                } else if (weeksOld > 2) {
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + Objects.hashCode(this.capitalizedName);
            return hash;
        }

        /**
         * Two objects are considered equal if they have the same capitalized
         * name (equals()).
         * 
         * @param obj
         * @return 
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CapitalizedName other = (CapitalizedName) obj;
            if (!Objects.equals(this.capitalizedName, other.capitalizedName)) {
                return false;
            }
            return true;
        }
    }
    
    /**
     * A name for request, with a priority how important the request should be.
     * The priority can be used to sort the objects.
     */
    private static class NameForRequest implements Comparable {
        
        public final String name;
        private int priority;
        
        /**
         * Create a new object with the name (all lowercase) and an initial
         * priority.
         * 
         * @param name
         * @param initialPriority 
         */
        public NameForRequest(String name, int initialPriority) {
            this.name = name;
            this.priority = initialPriority;
        }
        
        public void increasePriority() {
            priority++;
        }
        
        public int getPriority() {
            return priority;
        }
        
        @Override
        public String toString() {
            return name+"("+priority+")";
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 47 * hash + Objects.hashCode(this.name);
            return hash;
        }

        /**
         * Objects are considered equal if they have the same name for request.
         * 
         * @param obj
         * @return 
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NameForRequest other = (NameForRequest) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
        }

        /**
         * Sort according to priortiy (higher priority is "smaller").
         * 
         * @param o
         * @return 
         */
        @Override
        public int compareTo(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return -1;
            }
            return ((NameForRequest)o).priority - priority;
        }
        
    }
    
}
