
package chatty.gui.laf;

import chatty.util.MiscUtil;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatPropertiesLaf;
import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.logging.Logger;
import javax.swing.UIManager;

/**
 *
 * @author tduva
 */
public class FlatLafUtil {
    
    public final static int TAB_SELECTED_BACKGROUND = 1 << 0;
    public final static int TAB_SEP = 1 << 1;
    public final static int TAB_SEP_FULL = 1 << 2;
    
    private static final Logger LOGGER = Logger.getLogger(FlatLafUtil.class.getName());

    protected static void loadFlatLaf(String baseTheme, LaF.LaFSettings settings) throws Exception {
        Properties pCustom = new Properties();
        pCustom.load(new ByteArrayInputStream(settings.flatProperties.getBytes(Charset.forName("ISO-8859-1"))));
        boolean loadChattyProperties = !pCustom.getOrDefault("chattyProperties", "true").equals("false");
        
        Properties p = new Properties();
        if (loadChattyProperties) {
            p.load(FlatLafUtil.class.getResourceAsStream("FlatLaf.properties"));
        }
        if (MiscUtil.isBitEnabled(settings.flatTabs, TAB_SELECTED_BACKGROUND)) {
            p.put("TabbedPane.selectedBackground", "if(@optionalSelectedTabBackground, @optionalSelectedTabBackground, mix(@selectionBackground,$TabbedPane.background,15%))");
        }
        p.put("TabbedPane.showTabSeparators", String.valueOf(MiscUtil.isBitEnabled(settings.flatTabs, TAB_SEP)));
        p.put("TabbedPane.tabSeparatorsFullHeight", String.valueOf(MiscUtil.isBitEnabled(settings.flatTabs, TAB_SEP_FULL)));
        p.put("TitlePane.menuBarEmbedded", String.valueOf(settings.flatEmbeddedMenu));
        p.put("TitlePane.useWindowDecorations", String.valueOf(settings.flatStyledWindow));
        p.put("@baseTheme", baseTheme);
        p.putAll(pCustom);
        FlatPropertiesLaf lookAndFeel = new FlatPropertiesLaf("Customized Flat LaF", p) {
            
            @Override
            public void provideErrorFeedback(Component component) {
                if (settings.errorSound) {
                    super.provideErrorFeedback(component);
                }
            }
            
        };
        UIManager.setLookAndFeel(lookAndFeel);
    }
    
}
