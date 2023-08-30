
package chatty.util;

import chatty.Chatty;
import chatty.Chatty.PathType;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * General purpose static methods.
 * 
 * @author tduva
 */
public class MiscUtil {
    
    private static final Logger LOGGER = Logger.getLogger(MiscUtil.class.getName());

    /**
     * Copy the given text to the clipboard.
     * 
     * @param text 
     */
    public static void copyToClipboard(String text) {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents(new StringSelection(text), null);
    }
    
    public static boolean openFile(Path folder, Component parent) {
        return openFile(folder.toString(), parent);
    }
    
    public static boolean openFile(File file, Component parent) {
        return openFile(file.toString(), parent);
    }
    
    public static boolean openFile(String file, Component parent) {
        try {
            Desktop.getDesktop().open(new File(file));
        } catch (Exception ex) {
            if (parent != null) {
                JOptionPane.showMessageDialog(parent, "Opening file/folder failed.\n"+ex.getLocalizedMessage());
            }
            return false;
        }
        return true;
    }
    
    public static boolean openFilePrompt(String path, Component parent) {
        int chosenOption = JOptionPane.showOptionDialog(parent,
                path,
                "Open in default application?",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, new String[]{"Open File", "Cancel"}, 0);
        if (chosenOption == 0) {
            return MiscUtil.openFile(path, parent);
        }
        return false;
    }
    
    /**
     * Parses the command line arguments from the main method into a Map.
     * Arguments that start with a dash "-" are interpreted as key, everything
     * after as value (until the next key or end of the arguments). This means
     * that argument values can contain spaces, but they can not contain an
     * argument starting with "-" (which would be interpreted as the next key).
     * If a key occurs more than once, the value of the last one is used.
     * 
     * Example:
     * -cd -channel test -channel zmaskm, sirstendec -connect
     * 
     * Returns the Map:
     * {cd="",
     *  channel="zmaskm, sirstendec",
     *  connect=""
     * }
     * 
     * @param args The commandline arguments from the main method
     * @return The map with argument keys and values
     */
    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> result = new HashMap<>();
        String key = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                // Put key in result, but also remember for next iteration
                key = arg.substring(1);
                // Overwrites possibly existing key, so only last one with this
                // name is saved
                result.put(key, "");
            } else if (key != null) {
                // Append current value (not a key) to last found key
                // Trim in case previous value was empty
                String newValue = (result.get(key)+" "+arg).trim();
                result.put(key, newValue);
            }
        }
        return result;
    }
    
    /**
     * Attempt to move the file atomically, and if that fails try regular file
     * replacing.
     * 
     * @param from The file to move
     * @param to The target filename, which will be overwritten if it already
     * exists
     * @throws java.io.IOException
     */
    public static void moveFile(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, ATOMIC_MOVE);
        } catch (IOException ex) {
            // Based on the Files.move() docs it may throw an IOException when
            // the target file already exists (implementation specific), so try
            // alternate move on that instead of AtomicMoveNotSupportedException
            LOGGER.info("ATOMIC_MOVE failed: "+ex);
            Files.move(from, to, REPLACE_EXISTING);
        }
    }
    
    /**
     * Delete all files and directories in the given directory.
     * 
     * The prefix can be used if all the files to delete share a common prefix,
     * to ensure that only the correct files are deleted.
     * 
     * If the deleteDir parameter is true, then the given directory itself is
     * also deleted, otherwise only subdirectories are deleted.
     * 
     * If a file or directory fails to be deleted (for directories this often
     * occurs because it is not empty), then the rest of the process will still
     * continue and no error is thrown.
     * 
     * @param dir The directory to act upon
     * @param prefix Files need to have this prefix to be deleted
     * @param deleteDir If true, delete the given directory itself as well
     * @return The number of files successfully deleted
     */
    public static int deleteInDir(File dir, String prefix, boolean deleteDir) {
        int count = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.isDirectory()) {
                        count += deleteInDir(file, prefix, true);
                    } else if (file.getName().startsWith(prefix)) {
                        if (files[i].delete()) {
                            count++;
                        }
                    }
                }
            }
        }
        if (deleteDir) {
            dir.delete();
        }
        return count;
    }
    
    /**
     * Returns the StackTrace of the given Throwable as a String.
     * 
     * @param e
     * @return 
     */
    public static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    public static final boolean OS_WINDOWS = checkOS("Windows");
    public static final boolean OS_LINUX = checkOS("Linux");
    public static final boolean OS_MAC = checkOS("Mac");
    
    private static boolean checkOS(String check) {
        String os = System.getProperty("os.name");
        return os.startsWith(check);
    }
    
    /**
     * Returns System.nanoTime() as milliseconds and can thus only be used to
     * compare two values to eachother to get elapsed time that is not dependent
     * on system clock time.
     * 
     * @return Some elapsed time in milliseconds
     */
    public static long ems() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
    
    public static boolean biton(int value, int i) {
        return (value & (1 << i)) != 0;
    }
    
    public static Image rotateImage(Image image) {
        BufferedImage bi;
        if (image instanceof BufferedImage) {
            bi = (BufferedImage)image;
        } else {
            bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
        AffineTransform tx;
        AffineTransformOp op;
        tx = AffineTransform.getScaleInstance(-1, -1);
        tx.translate(-image.getWidth(null), -image.getHeight(null));
        op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(bi, null);
    }
    
    /**
     * Split up a set into several sets, so that each one only has at most limit
     * entries.
     * 
     * @param <T>
     * @param original The input, will not be modified
     * @param limit The limit (limits smaller than 1 will be turned into 1)
     * @return A list of sets
     */
    public static <T> List<Set<T>> splitSetByLimit(Set<T> original, int limit) {
        if (limit <= 0) {
            limit = 1;
        }
        List<Set<T>> result = new ArrayList<>();
        Iterator<T> it = original.iterator();
        while (it.hasNext()) {
            Set<T> part = new HashSet<>();
            for (int i=0;i<limit;i++) {
                if (!it.hasNext()) {
                    break;
                }
                part.add(it.next());
            }
            result.add(part);
        }
        return result;
    }
    
    /**
     * Add items from the source Set to the target Set until the target contains
     * limit entries or the source is exhausted.
     * 
     * @param <T>
     * @param source The Set to take items from (only read), must be non-null
     * @param target The Set to add items to, must be non-null
     * @param limit The amount of items that are at most allowed to be in target
     */
    public static <T> void addLimited(Set<T> source, Set<T> target, int limit) {
        for (T item : source) {
            if (target.size() >= limit) {
                return;
            }
            target.add(item);
        }
    }
    
    public static boolean isBitEnabled(int value, int bit) {
        return (value & bit) != 0;
    }
    
    public static boolean isBitEnabled(long value, long bit) {
        return (value & bit) != 0;
    }
    
    /**
     * Return the Set contained in the Map for the given key. If no Set exists
     * for the key yet, a new HashSet is automatically created and added to the
     * Map.
     * 
     * @param <K> The Map key type
     * @param <V> The Set value type
     * @param map The Map
     * @param mapKey The Map key
     * @return 
     */
    public static <K, V> Set<V> getSetFromMap(Map<K, Set<V>> map, K mapKey) {
        if (!map.containsKey(mapKey)) {
            map.put(mapKey, new HashSet<>());
        }
        return map.get(mapKey);
    }
    
    public static boolean exportText(String fileName, String text, boolean append) {
        Path file = Chatty.getPathCreate(PathType.EXPORT).resolve(fileName).toAbsolutePath().normalize();
        if (!file.startsWith(Chatty.getPath(PathType.EXPORT))) {
            LOGGER.warning("Invalid filename (may contain '..'?): " + fileName);
            return false;
        }
        try {
            OpenOption[] options = new OpenOption[]{
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            };
            if (append) {
                options = new OpenOption[]{
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
                };
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file, Charset.forName("UTF-8"), options)) {
                writer.write(text);
            }
            LOGGER.info(String.format("Written text to file: %s [%s]",
                    file, StringUtil.shortenTo(text, 10)));
            return true;
        }
        catch (IOException ex) {
            LOGGER.warning("Error writing text to file: " + ex);
            return false;
        }
    }

    public static String intern(String input) {
        if (input == null) {
            return null;
        }
        return input.intern();
    }
    
    public static boolean isNumTrue(Object object) {
        return object instanceof Number && ((Number) object).intValue() == 1;
    }
    
}
