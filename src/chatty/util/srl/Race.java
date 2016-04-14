
package chatty.util.srl;

import chatty.Helper;
import java.util.Collection;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Represents a single SRL race.
 * 
 * @author tduva
 */
public class Race {
    
    public static final int ENTRY_OPEN = 1;
    public static final int ENTRY_CLOSED = 2;
    public static final int IN_PROGRESS = 3;
    public static final int COMPLETE = 4;
    public static final int RACE_OVER = 5;
    
    public final String game;
    public final String id;
    public final int state;
    public final String statetext;
    public final long time;
    public final String goal;
    private final Collection<Race.Entrant> entrants;
    
    public Race(String id, String game, String goal, int state, String statetext, long time) {
        this.id = id;
        this.game = game;
        
        // Replace for URLs in the goal
        // TODO: Should probably be more general in case other stuff has to
        // be replaced as well
        this.goal = Helper.htmlspecialchars_decode(goal);
        
        this.state = state;
        this.statetext = statetext;
        this.time = time*1000;
        entrants = new TreeSet<>();
    }
    
    public void addEntrant(Race.Entrant entrant) {
        entrants.add(entrant);
    }
    
    public Collection<Race.Entrant> getEntrants() {
        return entrants;
    }
    
    @Override
    public String toString() {
        return game+" - "+goal+" ("+statetext+")";
    }
    
    /**
     * The race is equal if it has the same id.
     * 
     * @param other
     * @return 
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Race) {
            Race otherRace = (Race)other;
            return id.equals(otherRace.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.id);
        return hash;
    }

    /**
     * Represents an entrant in the race.
     */
    public static class Entrant implements Comparable<Entrant> {

        public final String name;
        public final String statetext;
        public final int place;
        public final long time;
        public final String twitch;
        public final int points;
        public final String message;
        
        public Entrant(String name, String statetext, int place, long time,
                String twitch, int points, String message) {
            this.name = name;
            this.statetext = statetext;
            this.place = place;
            this.time = time;
            this.twitch = twitch;
            this.points = points;
            this.message = message != null ? message : "";
        }
        
        @Override
        public String toString() {
            return name+" ("+statetext+"/"+twitch+")";
        }

        /**
         * Sort by place (which should also have a certain order from the SRL
         * request if not finished yet)
         * 
         * @param o
         * @return 
         */
        @Override
        public int compareTo(Entrant o) {
            if (place > o.place) {
                return 1;
            } else if (place < o.place) {
                return -1;
            }
            return name.compareTo(o.name);
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof Entrant)) {
                return false;
            }
            return name.equals(((Entrant)other).name);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + Objects.hashCode(this.name);
            return hash;
        }
    }
}
