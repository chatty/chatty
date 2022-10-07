
package chatty;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gets an {@code InetSocketAddress} from resolving a host and a port, selecting
 * those first that may be more likely to connect based on previous error
 * reports (if more than one IP/port is available).
 * 
 * @author tduva
 */
public class AddressManager {
    
    private static final Logger LOGGER = Logger.getLogger(AddressManager.class.getName());
    
    /**
     * Socket Addressses that couldn't be connected to. This may be cleared
     * (possibly only partly), so this doesn't mean all failed connection
     * attempts are in here. It is only used to determine the next hopefully
     * best address to connect to.
     */
    private final Set<InetSocketAddress> errors = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * Gets an {@code InetSocketAddress} based on the given host (a single host)
     * and the list of ports (one or several comma-separated ports). It uses the
     * first host/port that hasn't been added to the errors list via
     * {@code addError()}, or start from the beginning again if all possible
     * addresses are in the errors list.
     * 
     * @param host The host to connect to.
     * @param portsString The port(s) to connect to.
     * @return The {@code InetSocketAddress} or {@code null} if either the host
     * or ports are invalid
     * @throws java.net.UnknownHostException If the host could not be resolved
     */
    public InetSocketAddress getAddress(String host, String portsString) throws UnknownHostException {
        List<Integer> ports = parsePorts(portsString);
        if (ports.isEmpty()) {
            LOGGER.warning("No port to connect to found: "+portsString);
            return null;
        }
        if (host == null || host.isEmpty()) {
            LOGGER.warning("No host to connect to provided.");
            return null;
        }
        InetAddress[] ips = InetAddress.getAllByName(host);

        // For testing
        //ips = InetAddress.getAllByName("199.9.250.239");
        // Try to find a socket address that isn't in the error list
        Set<InetSocketAddress> tried = new HashSet<>();
        for (InetAddress ip : ips) {
            for (int port : ports) {
                InetSocketAddress address = new InetSocketAddress(ip, port);
                if (!errors.contains(address)) {
                    return address;
                }
                tried.add(address);
            }
        }
        LOGGER.info("Tried all available sockets.. trying from the start.");
        clearErrorsForAddresses(tried);
        return tried.iterator().next();
    }
    
    /**
     * Tells the manager that connecting to this address failed, which means
     * other addresses (if available) are tried first for the next attempt.
     * 
     * @param address 
     */
    public void addError(InetSocketAddress address) {
        errors.add(address);
    }
    
    /**
     * Removes all given {@code InetSocketAddress} objects from the errors list,
     * so they can be tried again.
     * 
     * @param addresses The list of {@code InetSocketAddress} objects to remove
     */
    private void clearErrorsForAddresses(Set<InetSocketAddress> addresses) {
        errors.removeAll(addresses);
    }
    
    /**
     * Parses a String containing one or more ports and returns a {@code List}
     * of them. Port numbers can be separated by any non-numeric character
     * (although it is recommended to separate by comma).
     * 
     * @param ports
     * @return A {@code List} or {@code Integer} objects representing the ports,
     * which may be empty if no port could be found
     */
    public static List<Integer> parsePorts(String ports) {
        String[] split = ports.split("[^0-9]");
        List<Integer> parsedPorts = new ArrayList<>();
        for (String portString : split) {
            if (!portString.isEmpty()) {
                try {
                    parsedPorts.add(Integer.parseInt(portString));
                } catch (NumberFormatException ex) {
                    // Do nothing if the port is invalid (e.g. larger than int)
                }
            }
        }
        return parsedPorts;
    }
    
    // Testing stuff
    public static void main(String[] args) {
        AddressManager m = new AddressManager();
        try {
            for (int i=0;i<60;i++) {
                InetSocketAddress addr = m.getAddress("irc.chat.twitch.tv", "6697,6667,443,80");
                System.out.println(addr);
                m.addError(addr);
            }
            
        } catch (UnknownHostException ex) {
            Logger.getLogger(AddressManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
