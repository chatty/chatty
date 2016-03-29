
package chatty.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author tduva
 */
public class ProcessManager {
    
    private static final AtomicInteger lastId = new AtomicInteger();
    private static final Map<Integer, Proc> processes = Collections.synchronizedMap(new TreeMap<>());
    
    public static String command(String input) {
        if (input == null || input.isEmpty()) {
            return "Invalid";
        }
        String[] split = input.split(" ", 2);
        String command = split[0];
        String parameter = null;
        if (split.length == 2) {
            parameter = split[1];
        }
        if (command.equals("exec")) {
            execute(parameter, "Custom");
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
    
    public static void execute(String command, String label) {
        final int id = lastId.incrementAndGet();
        Proc proc = new Proc(command, new Proc.ProcListener() {

            @Override
            public void processStarted(Proc p) {
                processes.put(id, p);
            }

            @Override
            public void message(Proc p, String message) {
            }

            @Override
            public void processFinished(Proc p, int exitValue) {
                processes.remove(id, p);
            }
        }, label+"/"+id);
        proc.start();
    }
    
    public static boolean kill(int id) {
        Proc proc = processes.get(id);
        if (proc != null) {
            proc.kill();
            return true;
        }
        return false;
    }
    
    public static Collection<String> getList() {
        Collection<String> result = new ArrayList<>();
        for (int id : processes.keySet()) {
            Proc p = processes.get(id);
            result.add(id+": "+p.toString());
        }
        return result;
    }
}
