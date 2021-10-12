
package chatty.gui.components.updating;

import chatty.Chatty;
import chatty.util.Debugging;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class RunUpdater {
    
    private static final Logger LOGGER = Logger.getLogger(RunUpdater.class.getName());
    
    public static void run(Path installerPath, Path installDir, Path jarPath,
            Path chattyExe, Path javawExe, String[] chattyArgs, boolean debug) throws IOException {
        if (!Files.exists(installerPath)) {
            throw new IOException("Installer not found: "+installerPath);
        }
        if (jarPath == null) {
            throw new IOException("JAR not identified");
        }
//        if (jarPath.toString().contains("a")) {
//            throw new IOException("Quite long error message and whatnot maybe with path: "+jarPath.toString());
//        }
        
        List<String> command = new ArrayList<>();
        
        // Installer Executable
        String installerExe = installerPath.toAbsolutePath().toString();
        command.add(quote(installerExe));
        command.add(makeParam("DIR", installDir.toAbsolutePath().toString()));
        command.add(makeParam("TYPE", "update"));
        if (debug) {
            command.add(makeParam("LOG", installerExe+".log"));
        }
        
        // Decide parameters for restarting Chatty
        List<String> restartArgs = new ArrayList<>();
        if (chattyExe != null && javawExe == null) {
            // Chatty.exe
            command.add(makeParam("runChattyPath", chattyExe.toString()));
        } else if (javawExe != null) {
            // java.exe
            command.add(makeParam("runChattyPath", javawExe.toString()));
            restartArgs.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
            restartArgs.add("-jar");
            restartArgs.add(jarPath.toString());
        }
        if (chattyArgs != null) {
            restartArgs.addAll(Arrays.asList(chattyArgs));
        }
        // Common arguments
        command.add(makeParam("runChattyWdir", removeBackslashSuffix(Chatty.getWorkingDirectory())));
        String restartParams = makeJavaParam("runChattyParam", restartArgs);
        if (restartParams == null) {
            throw new IOException("Could not build restart parameters: "
                    +Debugging.filterToken(restartArgs.toString()));
        }
        command.add(restartParams);
        
        // Let's do it!
        ProcessBuilder pb = new ProcessBuilder(command);
        LOGGER.info("Starting: "+Debugging.filterToken(command.toString()));
        pb.start();
    }

    /**
     * Surround input with ["] if input contains spaces.
     * 
     * @param input
     * @return Changed String, or original String if no spaces found in input
     */
    public static String quote(String input) {
        if (!input.contains(" ")) {
            return input;
        }
        return "\""+input+"\"";
    }
    
    /**
     * Remove backslash at the end of a path to prevent issues with some Java
     * versions adding double quotes, which won't work for launching the setup.
     * It might be better to just let ProcessBuilder do all of the escaping,
     * without adding any quote beforehand, but even then a backslash at the end
     * leaves a double-backslash when launching the setup, which I'm not sure if
     * it can cause issues.
     * 
     * @param input
     * @return 
     */
    private static String removeBackslashSuffix(String input) {
        if (input.endsWith("\\")) {
            return input.substring(0, input.length() - 1);
        }
        return input;
    }
    
    /**
     * Make a parameter for InnoSetup, quoting the whole thing if necessary.
     * 
     * @param param
     * @param value
     * @return 
     */
    private static String makeParam(String param, String value) {
        return quote("/"+param+"="+value);
    }
    
    public static String makeJavaParam(String param, List<String> input) {
        StringBuilder b = new StringBuilder();
        for (String p : input) {
            String escaped = replaceQuotes(escapeForJava(p));
            if (escaped == null) {
                return null;
            }
            if (b.length() > 0) {
                b.append(" ");
            }
            b.append(escaped);
        }
        return quote("/"+param+"="+b.toString());
    }
    
    /**
     * Replace all ["] characters with [`], unless there is already a [`]
     * character.
     * 
     * @param input
     * @return Changed String, or null if [`] found in input
     */
    public static String replaceQuotes(String input) {
        if (input.contains("`")) {
            return null;
        }
        return input.replace('"', '`');
    }
    
    public static String escapeForJava(String input) {
        if (!input.matches(".*[ \n\\s\"].*")) {
            return input;
        }
        int numBackslashesBefore = 0;
        StringBuilder b = new StringBuilder().append('"');
        for (int i=0; i<input.length(); i++) {
            char c = input.charAt(i);
            
            if (c == '\\') {
                // Count how many [\] there are in case it's relevant for the
                // following character
                numBackslashesBefore++;
            } else if (c == '"') {
                // Double [\] before ["] to escape them, so that it's possible
                // to add another one to escape ["]
                b.append(String.join("", Collections.nCopies(numBackslashesBefore*2+1, "\\")));
                numBackslashesBefore = 0;
                b.append('"');
            } else {
                // No special character, just add counted [\] normally
                b.append(String.join("", Collections.nCopies(numBackslashesBefore, "\\")));
                numBackslashesBefore = 0;
                b.append(c);
            }
        }
        // Escape backslashes at the end, so the closing ["] is not escaped
        b.append(String.join("", Collections.nCopies(numBackslashesBefore*2, "\\")));
        b.append('"');
        return b.toString();
    }
    
    
    public static void main(String[] args) {
        String[] args2 = new String[]{"-token", "abc"};
        Path installerPath = Paths.get("J:\\Chatty_0.16-b2_win_setup.exe");
        Path jarPath = Paths.get("J:\\chatty install\\ParamTest.jar");
        Path chattyExe = null;
        Path javawExe = Paths.get("C:\\Program Files (x86)\\Java\\jre1.8.0_201\\bin\\javaw.exe");
        try {
            run(installerPath, jarPath.getParent(), jarPath, chattyExe, javawExe, args2, true);
        } catch (IOException ex) {
            Logger.getLogger(RunUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
}
