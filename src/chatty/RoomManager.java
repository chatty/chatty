
package chatty;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class RoomManager {
    
    /**
     * Rooms by channel name of the room.
     */
    private final Map<String, Room> roomsByChannel = new HashMap<>();
    
    private final Map<String, Set<Room>> roomsByOwner = new HashMap<>();
    
    private final RoomUpdatedListener listener;
    
    public RoomManager(RoomUpdatedListener listener) {
        this.listener = listener;
    }
    
    public synchronized void addRooms(Collection<Room> newRooms) {
        if (newRooms != null) {
            for (Room r : newRooms) {
                addRoom(r);
            }
        }
    }
    
    /**
     * Add only those rooms that don't have a Room object stored here yet.
     * 
     * @param newRooms 
     */
    public synchronized void addRoomsIfNone(Collection<Room> newRooms) {
        if (newRooms != null) {
            for (Room r : newRooms) {
                if (!roomsByChannel.containsValue(r)) {
                    addRoom(r);
                }
            }
        }
    }
    
    public synchronized void addRoom(Room r) {
        if (r == null) {
            return;
        }
        Room previous = roomsByChannel.put(r.getChannel(), r);
        if (previous != r) {
            listener.roomUpdated(r);
        }
        if (r.hasOwnerChannel()) {
            if (!roomsByOwner.containsKey(r.getOwnerChannel())) {
                roomsByOwner.put(r.getOwnerChannel(), new HashSet<>());
            }
            roomsByOwner.get(r.getOwnerChannel()).add(r);
        }
    }
    
    /**
     * Return all Room objects that are associated with the given ownerChannel,
     * including the ownerChannel itself, if a Room object for that is stored.
     * 
     * @param ownerChannel
     * @return 
     */
    public synchronized Collection<Room> getRoomsByOwner(String ownerChannel) {
        Collection<Room> result = roomsByOwner.get(ownerChannel);
        if (result != null) {
            return new HashSet<>(result);
        }
        return new HashSet<>();
    }
    
    public synchronized Room getRoom(String channel) {
        if (channel == null) {
            return null;
        }
        if (!roomsByChannel.containsKey(channel)) {
            addRoom(Room.createRegular(channel));
        }
        //System.out.println(channel+" "+roomsByChannel.get(channel));
        return roomsByChannel.get(channel);
    }
    
    public static interface RoomUpdatedListener {
        public void roomUpdated(Room room);
    }
    
}
