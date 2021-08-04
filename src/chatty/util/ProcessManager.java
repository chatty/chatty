
package chatty.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Provides functions to start processes and store a list of them.
 * 
 * @author tduva
 */
public class ProcessManager {
    
    // Used to make an id for currently running processes
    private static final AtomicInteger lastId = new AtomicInteger();
    
    // Store currently running processes
    private static final Map<Integer, Proc> processes
            = Collections.synchronizedMap(new TreeMap<Integer, Proc>());
    
    /**
     * Handle input from the /proc command.
     * 
     * @param input The parameters to the command
     * @param messageListener Receives output from the process (can be null)
     * @return Message back to the user
     */
    public static String command(String input, Consumer<String> messageListener) {
        if (input == null || input.isEmpty()) {
            return "Invalid input.";
        }
        String[] split = input.split(" ", 2);
        String command = split[0];
        String parameter = null;
        if (split.length == 2) {
            parameter = split[1];
        }
        if (command.equals("exec")) {
            execute(parameter, "Custom", null);
            return "Trying to start process.";
        }
        else if (command.equals("execEcho")) {
            execute(parameter, "Custom", messageListener);
            return "Trying to start process.";
        }
        else if (command.equals("kill")) {
            try {
                if (kill(Integer.parseInt(parameter))) {
                    return "Trying to kill process.";
                }
                return "No process with this id.";
            } catch (NumberFormatException ex) {
                return "Invalid parameter.";
            }
        }
        else if (command.equals("list")) {
            Collection<String> list = getList();
            StringBuilder result = new StringBuilder("Currently "+list.size()+" processes.");
            for (String p : list) {
                result.append("\n");
                result.append(p);
            }
            return result.toString();
        }
        return "Invalid input.";
    }
    
    /**
     * Execute the given process and parameters.
     * 
     * @param command The process and parameters (split up by spaces, except
     * for parts surrounded by quotes, \" to escape quotes)
     * @param label For debug output
     * @param messageListener Receives output from the process (can be null)
     */
    public static void execute(String command, String label, Consumer<String> messageListener) {
        final int id = lastId.incrementAndGet();
        Proc proc = new Proc(command, new Proc.ProcListener() {

            @Override
            public void processStarted(Proc p) {
                processes.put(id, p);
            }

            @Override
            public void message(Proc p, String message) {
                if (messageListener != null) {
                    messageListener.accept(p.getLabel()+": "+message);
                }
            }

            @Override
            public void processFinished(Proc p, int exitValue) {
                processes.remove(id);
            }
        }, label+"/"+id);
        proc.start();
    }
    
    /**
     * Try to kill the process with the given id, if running.
     * 
     * @param id The id as created in execute()
     * @return true if the id was of a running process, false otherwise (doesn't
     * give any indiciation if killing the process actually worked)
     */
    public static boolean kill(int id) {
        Proc proc = processes.get(id);
        if (proc != null) {
            proc.kill();
            return true;
        }
        return false;
    }
    
    /**
     * Return a list of human-readable information on all running processes.
     * 
     * @return 
     */
    public static Collection<String> getList() {
        Collection<String> result = new ArrayList<>();
        for (int id : processes.keySet()) {
            Proc p = processes.get(id);
            result.add(id+": "+p.toString());
        }
        return result;
    }
}
