
package chatty.gui.components.settings;

import chatty.gui.components.LinkLabelListener;

/**
 * Interface for a simple String editor that shows a dialog where the user can
 * enter and edit a text.
 * 
 * @author tduva
 */
public interface StringEditor {

    /**
     * Open the dialog used to edit the String, with OK and Cancel buttons.
     * 
     * @param title Short description of what is being edited, shown as title
     * or somewhere else in the dialog
     * @param preset The value to fill in, may be empty, but not null
     * @param info Help text or general info shown below the input field
     * @return The edited value if dialog was clossed by the OK button, null
     * otherwise
     */
    public String showDialog(String title, String preset, String info);

    /**
     * Set the listener for links specified in the info text (LinkLabel format).
     * 
     * @param listener 
     */
    public void setLinkLabelListener(LinkLabelListener listener);

}
