
package chatty.util.dnd;

import java.awt.Color;

/**
 * Provides constants and functions for use with
 * {@link DockChild#setSetting(DockSetting.Type, Object) DockChild#setSetting()}.
 *
 * @author tduva
 */
public class DockSetting {
    
    /**
     * The setting type defines what values are accepted and what the setting
     * changes.
     */
    public enum Type {
        /**
         * Sets {@link javax.swing.JTabbedPane#setTabPlacement(int)}.
         */
        TAB_PLACEMENT,
        
        /**
         * Sets {@link javax.swing.JTabbedPane#setTabLayoutPolicy(int)}.
         */
        TAB_LAYOUT,
        
        /**
         * Sets where added tabs are inserted, accepts {@link TabOrder} values.
         */
        TAB_ORDER,
        
        /**
         * Set a custom DockContent comparator. If non-null the TAB_ORDER
         * setting is ignored.
         */
        TAB_COMPARATOR,
        
        /**
         * Enables changing tabs by using the mousewheel while the mouse is
         * over it. Accepts booleans.
         */
        TAB_SCROLL,
        
        /**
         * Extends upon {@link TAB_SCROLL} to allow mousewheel tab scrolling
         * anywhere the scroll event is accepted. Accepts booleans.
         */
        TAB_SCROLL_ANYWHERE,
        
        /**
         * Close Tabs with Middle Mouse Button.
         */
        TAB_CLOSE_MMB,
        
        /**
         * When a tab is closed switch to the previously selected tab, if
         * applicable. Otherwise it does whatever the default behaviour of the
         * tab pane is.
         */
        TAB_CLOSE_SWITCH_TO_PREV,
        
        /**
         * The fill {@link Color} of the box that shows where a drag&drop
         * movement accepts a drop.
         */
        FILL_COLOR,
        
        /**
         * The line {@link Color} of the box that shows where a drag&drop
         * movement accepts a drop.
         */
        LINE_COLOR,
        
        DIVIDER_SIZE,
        
        KEEP_EMPTY,
        
        POPOUT_TYPE,
        
        POPOUT_TYPE_DRAG,
        
        POPOUT_ICONS,
        
        POPOUT_PARENT,
        
        DEBUG
    }
    
    public enum TabOrder {
        /**
         * Inserts added tabs at the end.
         */
        INSERTION,
        
        /**
         * Inserts new tabs in alphabetic order based on the name of the added
         * Component. If tabs have been reordered manually, then tabs are
         * inserted before the first tab whose name would be greater than the
         * new one.
         * 
         * <p>Ordering is done ignoring case.</p>
         */
        ALPHABETIC
    }
    
    public enum PopoutType {
        FRAME, DIALOG, NONE
    }
    
    public static boolean getBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }
    
    public static String getString(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        return "";
    }
    
    public static Color getColor(Object value) {
        if (value instanceof Color) {
            return (Color) value;
        }
        return null;
    }
    
    public static Integer getInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return -1;
    }
    
}
