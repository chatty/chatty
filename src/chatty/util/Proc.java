
package chatty.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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
public class Proc extends Thread {

    private final static Logger LOGGER = Logger.getLogger(Proc.class.getName());

    private final String command;
    private final ProcListener listener;
    private Process process;
    private final String label;
    private final long created;

    /**
     * Create a new process.
     * 
     * @param command The command and parameters
     * @param listener The listener to monitor the process state and output
     * @param label Label for debug output
     */
    public Proc(String command, ProcListener listener, String label) {
        this.command = command;
        this.listener = listener;
        this.label = label;
        this.created = System.currentTimeMillis();
    }
    
    public String getCommand() {
        return command;
    }
    
    public String getLabel() {
        return label;
    }

    @Override
    public void run() {
        if (command == null || command.isEmpty()) {
            return;
        }
        String[] cmd = split(command);
        try {
            Runtime rt = Runtime.getRuntime();
            Process p = rt.exec(cmd);
            this.process = p;
            LOGGER.info(String.format("[%s] Process %s started. [%s][%s]",
                    label, id(), command, StringUtil.join(cmd)));
            listener.processStarted(this);

            // Read both output streams (output of the process, so input), so
            // the process keeps running and to output it's output
            InputStreamHelper errorStream = new InputStreamHelper(p.getErrorStream());
            InputStreamHelper inputStream = new InputStreamHelper(p.getInputStream());

            errorStream.start();
            inputStream.start();

            int exitValue = p.waitFor();
            errorStream.join(1000);
            inputStream.join(1000);
            listener.processFinished(this, exitValue);
            LOGGER.info(String.format("[%s] Process %s finished.",
                    label, id()));
        } catch (IOException ex) {
            listener.message(this, "Error: " + ex);
            LOGGER.warning(String.format(
                    "[%s] Error starting process / %s [%s][%s]",
                    label, ex, command, StringUtil.join(cmd)));
        } catch (InterruptedException ex) {
            listener.message(this, "Error: " + ex);
            LOGGER.warning(String.format(
                    "[%s] %s", label, ex));
        }
    }

    private String id() {
        return Integer.toHexString(process.hashCode());
    }

    public void kill() {
        LOGGER.info(String.format("[%s] Killing Process %s", label, id()));
        process.destroy();
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", label, command, DateTime.agoSingleVerbose(created));
    }

    public static interface ProcListener {

        public void processStarted(Proc process);

        public void message(Proc process, String message);

        public void processFinished(Proc process, int exitValue);
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
                    listener.message(Proc.this, line);
                    LOGGER.info(String.format("[%s] (%s): %s", label, id(), line));
                }
            } catch (IOException ex) {
                listener.message(Proc.this, "Error: " + ex);
                LOGGER.warning(String.format("[%s] (%s): Error reading stream / %s",
                        label, id(), ex));
            }
        }
    }

    /**
     * Splits up a line of text into tokens by spaces, ignoring spaces for parts
     * that are surrounded by quotes. Quotes can be escaped by prepending a
     * backslash (\").
     *
     * <p>
     * This can be used for splitting up parameters.</p>
     *
     * @param input The line of text to tokenize
     * @return An array of tokens (tokens in quotes may be empty)
     */
    public static String[] split(String input) {
        List<String> result = new ArrayList<>();
        Matcher m = Pattern.compile("\"((?:\\\\\"|[^\"])+)\"|((?:\\\\\"|[^\"\\s])+)").matcher(input);
        while (m.find()) {
            if (m.group(1) != null) {
                // Remove escaping characters for quotes (\" to ")
                result.add(m.group(1).replace("\\\"", "\""));
            } else {
                result.add(m.group(2).replace("\\\"", "\""));
            }
        }
        return result.toArray(new String[result.size()]);
    }
    
    public static void main(String[] args) {
        String message = "Is there even anything over \"here\"?".replace("\"", "\\\"");
        String test = "notify-send \\\"Title\\\" \""+message+"\"";
        String[] split = split(test);
        System.out.println(test);
        System.out.println(Arrays.asList(split));
    }
}
