
package chatty.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Start a process with the given command and send the resulting output to the
 * listener.
 * 
 * @author tduva
 */
public class Livestreamer extends Thread {
    
    private final static Logger LOGGER = Logger.getLogger(Livestreamer.class.getName());
    
    private final String command;
    private final LivestreamerListener listener;
    private Process process;
    
    public Livestreamer(String command, LivestreamerListener listener) {
        this.command = command;
        this.listener = listener;
    }
    
    @Override
    public void run() {
        try {
            Runtime rt = Runtime.getRuntime();
            String[] cmd = split(command);
            Process process = rt.exec(cmd);
            this.process = process;
            LOGGER.info("Livestreamer: Process "+id()+" started. ["+filterToken(command)+"]");
            listener.processStarted(command);
            
            // Read both output streams (output of the process, so input), so
            // the process keeps running and to output it's output
            InputStreamHelper errorStream = new InputStreamHelper(process.getErrorStream());
            InputStreamHelper inputStream = new InputStreamHelper(process.getInputStream());
            
            errorStream.start();
            inputStream.start();
            
            int exitValue = process.waitFor();
            errorStream.join(1000);
            inputStream.join(1000);
            listener.processFinished(exitValue);
            LOGGER.info("Livestreamer: Process "+id()+" finished.");
        } catch (IOException ex) {
            listener.message("Error: "+ex);
            LOGGER.warning("Livestreamer: Error starting process / "+ex);
        } catch (InterruptedException ex) {
            listener.message("Error: "+ex);
            LOGGER.warning("Livestreamer: "+ex);
        }
    }
    
    private String id() {
        return Integer.toHexString(process.hashCode());
    }
    
    public void kill() {
        LOGGER.info("Livestreamer: Killing Process "+id());
        process.destroy();
    }
    
    public static interface LivestreamerListener {
        public void processStarted(String command);
        public void message(String message);
        public void processFinished(int exitValue);
    }
    
    /**
     * Reads the given stream in it's own thread and outputs it.
     */
    private class InputStreamHelper extends Thread {
        
        private final InputStream input;
        
        InputStreamHelper(InputStream input) {
            this.input = input;
        }
        
        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    listener.message(line);
                    LOGGER.info("Livestreamer ("+id()+"): "+line);
                }
            } catch (IOException ex) {
                listener.message("Error: "+ex);
                LOGGER.warning("Livestreamer ("+id()+"): Error reading stream / "+ex);
            }
        }
    }
    
//    private static String join(String[] cmd) {
//        StringBuilder b = new StringBuilder();
//        for (String c : cmd) {
//            if (!c.isEmpty()) {
//                if (b.length() > 0) {
//                    b.append(" ");
//                }
//                b.append("\"");
//                b.append(c);
//                b.append("\"");
//            }
//        }
//        return b.toString();
//    }
    
    /**
     * Splits up a line of text into tokens by spaces, ignoring spaces for parts
     * that are surrounded by brackets.
     * 
     * <p>This can be used for splitting up parameters.</p>
     * 
     * @param input The line of text to tokenize
     * @return An array of tokens (tokens in brackets may be empty)
     */
    private static String[] split(String input) {
        List<String> result = new ArrayList<>();
        Matcher m = Pattern.compile("\"([^\"]*)\"|([^\"\\s]+)").matcher(input);
        while (m.find()) {
            if (m.group(1) != null) {
                result.add(m.group(1));
            } else {
                result.add(m.group(2));
            }
        }
        //System.out.println(result);
        return result.toArray(new String[result.size()]);
    }
    
    public static String filterToken(String input) {
        return input.replaceAll("--twitch-oauth-token \\w+", "--twitch-oauth-token <token>");
    }
    
    public static final void main(String[] args) {
        //split("\"a b c\" -h   test");
        System.out.println(filterToken("--twitch-oauth-token abcfwf --fwaf"));
    }
    
}
