
package chatty;

import java.util.Arrays;

/**
 * Main class that sets the working directory to the JAR directory. This is
 * because the Chatty.exe created by jpackage will no longer force the working
 * directory to the "app" directory, so it would break backwards compatibility.
 * 
 * @author tduva
 */
public class Chatty2 {

    public static void main(String[] args) {
        String[] modified = Arrays.copyOf(args, args.length+1);
        modified[args.length] = "-appwdir";
        Chatty.main(modified);
    }
    
}
