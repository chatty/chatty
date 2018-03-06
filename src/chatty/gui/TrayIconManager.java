
package chatty.gui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * Allows adding/removing of a tray icon with a popup menu. It is added
 * automatically if a tray notification is shown (displayInfo()) and can also
 * be shown/hidden manually.
 * 
 * @author tduva
 */
public class TrayIconManager {
    
    private static final Logger LOGGER = Logger.getLogger(TrayIconManager.class.getName());
    
    private final SystemTray tray;
    private final TrayIcon trayIcon;
    private final PopupMenu popup;
    
    private boolean iconAdded;
    
    public TrayIconManager(Image image) {
        if (SystemTray.isSupported()) {
            tray = SystemTray.getSystemTray();
            
            popup = new PopupMenu();
            MenuItem showItem = new MenuItem("Show");
            showItem.setActionCommand("show");
            popup.add(showItem);
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.setActionCommand("exit");
            popup.add(exitItem);

            trayIcon = new TrayIcon(image, "Chatty");
            trayIcon.setImageAutoSize(true);
            trayIcon.setPopupMenu(popup);
        } else {
            tray = null;
            trayIcon = null;
            popup = null;
        }
    }
    
    /**
     * Displays a tray icon info message.
     * 
     * @param title The title to use
     * @param message The main message text to use
     */
    public void displayInfo(String title, String message) {
        if (trayIcon != null) {
            addIcon();
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.NONE);
        }
    }
    
    /**
     * Sets the tooltip of the tray icon. Does nothing if the tray is not
     * available.
     *
     * @param text The text to set the tooltip to
     */
    public void setTooltipText(String text) {
        if (trayIcon != null) {
            trayIcon.setToolTip(text);
        }
    }
    
    /**
     * Adds an ActionListener to the tray icon and the tray popoup menu. Does
     * nothing if the tray is not available.
     * 
     * @param listener The ActionListener to add to the icon and menu
     */
    public void addActionListener(ActionListener listener) {
        if (trayIcon != null) {
            trayIcon.addActionListener(listener);
            popup.addActionListener(listener);
        }
    }
    
    /**
     * Adds the tray icon if the tray is available and the icon isn't already
     * added.
     */
    private void addIcon() {
        if (tray != null && !iconAdded && !isIconAdded()) {
            try {
                tray.add(trayIcon);
                iconAdded = true;
            } catch (AWTException ex) {
                LOGGER.warning("Error adding tray icon: " + ex.getLocalizedMessage());
            }
        }
    }

    /**
     * Removes the tray icon if the tray is available and the icon is currently
     * added.
     */
    private void removeIcon() {
        if (iconAdded && tray != null) {
            tray.remove(trayIcon);
            iconAdded = false;
        }
    }
    
    private boolean isIconAdded() {
        for (TrayIcon icon : tray.getTrayIcons()) {
            if (icon == trayIcon) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Adds or removes the tray icon. If the icon is already visible, then
     * setting is as visible does nothing (and the same applies for making it
     * invisible).
     *
     * @param visible true to show the icon, false to hide it
     */
    public void setIconVisible(boolean visible) {
        if (visible) {
            addIcon();
        } else {
            removeIcon();
        }
    }
    
    /**
     * Test if SystemTray is supported and the TrayIcon is actually currently
     * added.
     * 
     * @return 
     */
    public boolean isAvailable() {
        return SystemTray.isSupported() && isIconAdded();
    }
    
}
