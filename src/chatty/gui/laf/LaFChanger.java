
package chatty.gui.laf;

import chatty.gui.laf.LaF.LaFSettings;
import java.awt.Component;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class LaFChanger {

    private static final Logger LOGGER = Logger.getLogger(LaFChanger.class.getName());
    
    private static LaFSettings settingsBefore;
    private static String loggedError;
    private static Component parentComponent;
    
    /**
     * Set a new Look&Feel, but revert to previous if an error occurs and output
     * error message.
     * 
     * @param settings
     * @param parentComponent 
     */
    public static void changeLookAndFeel(LaFSettings settings, Component parentComponent) {
        LaFChanger.settingsBefore = LaF.getSettings();
        LaFChanger.loggedError = null;
        LaFChanger.parentComponent = parentComponent;
        
        // Try to set Look&Feel, during which Flat LaF may log errors.
        String caughtError = LaF.setLookAndFeel(settings);
        if (caughtError == null && loggedError == null) {
            /**
             * Only update when there is no error, so it doesn't cause issues
             * when reverting is necessary. It threw a NPE otherwise, not sure
             * if that's because stuff wasn't initialized correctly or because
             * of changing LaF again directly after updating.
             */
            LaF.updateLookAndFeel();
        }
        else {
            revertLookAndFeelWithError(caughtError != null ? caughtError : loggedError);
        }
        
        /**
         * Show popup every time after setting LaF with the option to revert. It
         * could also automatically revert after a few seconds, for example if
         * colors are weird making it difficult to read/click stuff (not
         * implemented here yet). But might be a bit too annoying for every
         * small change.
         */
//        SwingUtilities.invokeLater(() -> {
//            int result = JOptionPane.showConfirmDialog(null, settings, "Keep Look&Feel settings?", JOptionPane.YES_NO_OPTION);
//            if (result == 1) {
//                LaFSettings s = LaF.getPreviousSettings();
//                System.out.println(s.lafCode);
//                SwingUtilities.invokeLater(() -> {
//                    LaF.setLookAndFeel(s);
//                    LaF.updateLookAndFeel();
//                });
//            }
//        });
    }
    
    /**
     * Called when the Flat LaF logs an error without throwing an exception, for
     * example when an invalid color code fails to parse.
     * 
     * @param error 
     */
    public static void loggedFlatLookAndFeelError(String error) {
        LaFChanger.loggedError = error;
    }
    
    private static void revertLookAndFeelWithError(String error) {
        LOGGER.info("Reverting LaF due to error: "+error);
        SwingUtilities.invokeLater(() -> {
            LaF.setLookAndFeel(settingsBefore);
            LaF.updateLookAndFeel();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(parentComponent,
                                              error,
                                              "Failed to apply new Look&Feel settings",
                                              JOptionPane.WARNING_MESSAGE);
            });
        });
    }
    
}
