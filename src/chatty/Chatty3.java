
package chatty;

import java.util.Arrays;

/**
 * Main class for a ChattyPortable.exe created by jpackage. Both sets the
 * working directory to the JAR directory (see Chatty2) and sets the settings
 * directory to a directory next to the JAR.
 * 
 * @author tduva
 */
public class Chatty3 {
    
    public static void main(String[] args) {
        String[] modified = Arrays.copyOf(args, args.length+2);
        modified[args.length] = "-portable";
        modified[args.length+1] = "-appwdir";
        Chatty.main(modified);
    }
    
}
