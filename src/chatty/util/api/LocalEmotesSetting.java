
package chatty.util.api;

import chatty.gui.MainGui;
import chatty.gui.components.LinkLabel;
import chatty.util.settings.AdvancedSetting;
import chatty.util.settings.Settings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;

/**
 *
 * @author tduva
 */
public class LocalEmotesSetting extends AdvancedSetting<Emoticon> {

    private final MainGui gui;
    
    public LocalEmotesSetting(Settings settings, MainGui gui) {
        super(settings, "localEmotes", false);
        this.gui = gui;
    }
    
    @Override
    public Emoticon toObject(Object input) {
        List<String> list = (List) input;
        String emoteId = list.get(0);
        String emoteCode = list.get(1);
        return createEmote(emoteId, emoteCode);
    }

    @Override
    public Object fromObject(Emoticon emote) {
        List result = new ArrayList();
        result.add(emote.stringId);
        result.add(emote.code);
        return result;
    }
    
    private Emoticon createEmote(String id, String code) {
        Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.CUSTOM2, code, null);
        b.setStringId(id);
        return b.build();
    }
    
    @Override
    public void add(Emoticon emote) {
        add(new HashSet<>(Arrays.asList(new Emoticon[]{emote})));
    }
    
    @Override
    public void remove(Emoticon emote) {
        remove(new HashSet<>(Arrays.asList(new Emoticon[]{emote})));
    }
    
    public void add(Set<Emoticon> emotes) {
        if (checkAction("Add", emotes)) {
            for (Emoticon emote : emotes) {
                super.setAdd(createEmote(emote.stringId, emote.code));
            }
            notifyChanges();
        }
    }
    
    public void remove(Set<Emoticon> emotes) {
        if (checkAction("Remove", emotes)) {
            for (Emoticon emote : emotes) {
                super.remove(createEmote(emote.stringId, emote.code));
            }
            notifyChanges();
        }
    }
    
    @Override
    public void setData(Collection<Emoticon> data) {
        super.setData(data);
        notifyChanges();
    }
    
    private boolean checkAction(String action, Collection<Emoticon> emotes) {
        LinkLabel label = new LinkLabel(String.format(
                "%s %d Local Emotes?<br /><br />(Read help: [help-settings:EmoticonsLocal Local Emotes])",
                action, emotes.size()),
                gui.getLinkLabelListener());
        int result = JOptionPane.showConfirmDialog(gui, label,
                "Local Emotes",
                JOptionPane.YES_NO_OPTION);
        return result == 0;
    }
    
}
