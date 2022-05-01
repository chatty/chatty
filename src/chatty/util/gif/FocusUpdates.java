
package chatty.util.gif;

import chatty.util.BatchAction;
import chatty.util.settings.Settings;
import java.awt.KeyboardFocusManager;

/**
 * Update animation state based on the settings and whether an app window is
 * active.
 *
 * @author tduva
 */
public class FocusUpdates {

    private static final String PAUSE_SETTING = "animationPause";
    private static final String PAUSE_FRAME_SETTING = "animationPauseFrame";
    
    public static void set(Settings settings) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusedWindow", e -> {
            /**
             * When focus switches aways from Chatty the "new value" is null,
             * however that also happens when switching focus between Chatty
             * windows, with a second event aftewards with the newly focused
             * window in the "new value", so delay updating settings a bit to
             * wait for the second event.
             */
            BatchAction.queue(FocusUpdates.class, 100, false, true, () -> {
                update(settings, e.getNewValue() != null);
            });
        });
        
        settings.addSettingChangeListener((setting, type, value) -> {
            if (setting.equals(PAUSE_SETTING)) {
                /**
                 * Window always counts as active for this update, which might
                 * not always be correct (e.g. changing setting with a global
                 * hotkey), but should be good enough for now.
                 */
                update(settings, true);
            }
        });
    }
    
    private static void update(Settings settings, boolean active) {
        boolean alwaysOff = settings.getLong(PAUSE_SETTING) == 0;
        boolean activityOff = !active && settings.getLong(PAUSE_SETTING) == 1;
        int pauseState = -1;
        if (alwaysOff || activityOff) {
            pauseState = (int) settings.getLong(PAUSE_FRAME_SETTING);
        }
        AnimatedImage.setAnimationPause(pauseState);
    }
    
}
