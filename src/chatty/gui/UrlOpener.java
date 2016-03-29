
package chatty.gui;

import chatty.util.MiscUtil;
import chatty.util.ProcessManager;
import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Opens the given URL in the default browser, with or without prompt.
 * 
 * @author tduva
 */
public class UrlOpener {
    
    private static final Logger LOGGER = Logger.getLogger(UrlOpener.class.getName());
    
    /**
     * The maximum number of chars before an url is split up into several lines
     * when displaying it in a prompt.
     */
    private final static int MAX_URL_LENGTH = 80;
    
    /**
     * Save whether to currently use a prompt by default.
     */
    private static boolean prompt = true;
    
    /**
     * Changes whether to use a prompt by default.
     * 
     * @param usePrompt 
     */
    public static void setPrompt(boolean usePrompt) {
        prompt = usePrompt;
    }
    
    private static boolean customCommandEnabled;
    private static String customCommand;
    
    public static void setCustomCommandEnabled(boolean enabled) {
        customCommandEnabled = enabled;
    }
    
    public static void setCustomCommand(String command) {
        customCommand = command;
    }
    
    /**
     * Open a single URL with a prompt if enabled.
     * 
     * @param parent The Component that will be used as parent for the prompt.
     * @param url The URL as a String.
     * @return true if the given url was opened successfully, false otherwise
     */
    public static boolean openUrlPrompt(Component parent, String url) {
        return openUrlPrompt(parent, url, false);
    }
    
    /**
     * Open a single URL with a prompt if enabled or if the prompt is forced
     * by the parameter.
     * 
     * @param parent The Component that will be used as parent for the prompt.
     * @param url The URL as a String.
     * @param forcePrompt Whether to force a prompt or use the default setting.
     * @return true if the given url was opened successfully, false otherwise
     */
    public static boolean openUrlPrompt(Component parent, String url,
            boolean forcePrompt) {
        if (url == null) {
            return false;
        }
        List<String> list = new ArrayList<>();
        list.add(url);
        return openUrlsPrompt(parent, list, forcePrompt);
    }
    
    /**
     * Directly open the given URL in the default browser.
     * 
     * @param url The URL as a String.
     * @return true if the url was opened successfully, false if an error
     * occured, e.g. if the url was empty or invalid
     */
    public static boolean openUrl(String url) {
        if (url == null) {
            return false;
        }
        URI parsed;
        try {
            parsed = new URI(url);
        } catch (URISyntaxException ex) {
            LOGGER.warning("Invalid URI format: " + ex);
            return false;
        }
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
                && !customCommandEnabled) {
            try {
                Desktop.getDesktop().browse(parsed);
                return true;
            } catch (IOException ex) {
                LOGGER.warning("Error opening URL: "+ex);
            }
        } else {
            return openUrlNative(url);
        }
        return false;
    }
    
    private static boolean openUrlNative(String url) {
        String command = null;
        if (customCommandEnabled && customCommand != null && !customCommand.isEmpty()) {
            command = customCommand+" "+url;
        }
        else if (MiscUtil.OS_WINDOWS) {
            command = "explorer "+url;
        }
        else if (MiscUtil.OS_LINUX) {
            command = "xdg-open "+url;
        }
        else if (MiscUtil.OS_MAC) {
            command = "open "+url;
        }
        if (command != null) {
            ProcessManager.execute(command, "URL");
            return true;
        }
        return false;
    }
    
    /**
     * Open several URLs by using a prompt if it's enabled.
     * 
     * @param parent The Component that will be used as parent for the prompt.
     * @param urls The list of URLs as Strings.
     */
    public static void openUrlsPrompt(Component parent, List<String> urls) {
        openUrlsPrompt(parent, urls, false);
    }
    
    /**
     * Opens several URLs by using a prompt if it's enabled or if a prompt is
     * forced by the parameter.
     * 
     * @param parent The Component that should be the parent of a prompt
     * @param urls The list of URLs as Strings
     * @param forcePrompt Always show a prompt, even if it's not the default
     *  setting
     * @return true if at least one of the given URLs was opened successfully
     */
    public static boolean openUrlsPrompt(Component parent, List<String> urls,
            boolean forcePrompt) {
        if (urls.isEmpty()) {
            return false;
        }
        if (!forcePrompt && !prompt) {
            return openUrls(urls);
        }
        switch (showUrlsPrompt(parent, urls)) {
            case 0: return openUrls(urls);
            case 1: MiscUtil.copyToClipboard(urls.get(0));
        }
        return true;
    }

    /**
     * Opens one or more URLs specified in the given list in the default
     * browser.
     *
     * @param urls The list of URLs to open
     * @return true if all of the given URLs were opened successfully, false
     * otherwise
     */
    public static boolean openUrls(List<String> urls) {
        boolean result = true;
        for (String url : urls) {
            if (!openUrl(url)) {
                result = false;
            }
        }
        return result;
    }
    
    /**
     * Actually show the dialog that contain the given URLs and give the user
     * the option to open the URL, copy it or cancel the dialog.
     * 
     * @param parent The Component that will be used as parent for the prompt.
     * @param urls The list of URLs as Strings
     * @return 0 if the URL should be opened, 1 if it should be copied, 2 if
     *  nothing should be done
     */
    private static int showUrlsPrompt(Component parent, List<String> urls) {
        // Make text
        String text = "<html><body style='width: 100px;'>";
        for (String url : urls) {
            url = splitUrl(url);
            text += url + "<br />";
        }
        // Make options
        String okOption = "Open URL";
        if (urls.size() > 1) {
            okOption = "Open "+urls.size()+" URLs";
        }
        String[] options = {okOption, "Cancel"};
        if (urls.size() == 1) {
            options = new String[]{okOption, "Copy URL", "Cancel"};
        }
        // Show dialog
        int chosenOption = JOptionPane.showOptionDialog(parent,
                text,
                "Open in default browser?",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, 1);
        
        return chosenOption;
    }
    
    /**
     * Split up the given url if it is longer than the max length, by adding a
     * space in between, so it can be linebroken at that point.
     *
     * @param url The url to split up
     * @return The url with added spaces if it exceeded max length
     */
    private static String splitUrl(String url) {
        if (url.length() > MAX_URL_LENGTH) {
            return url.substring(0, MAX_URL_LENGTH)+" "+splitUrl(url.substring(MAX_URL_LENGTH));
        }
        return url;
    }
    
}
