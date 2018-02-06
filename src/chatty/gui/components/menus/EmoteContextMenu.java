
package chatty.gui.components.menus;

import chatty.Chatty;
import chatty.Helper;
import static chatty.gui.components.menus.ContextMenuHelper.ICON_IMAGE;
import static chatty.gui.components.menus.ContextMenuHelper.ICON_WEB;
import chatty.util.StringUtil;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.Emoticons;
import java.awt.event.ActionEvent;

/**
 * Shows information about the emote that was right-clicked on.
 * 
 * @author tduva
 */
public class EmoteContextMenu extends ContextMenu {
    
    private static Emoticons emoteManager;
    private final ContextMenuListener listener;
    private final EmoticonImage emoteImage;
    
    public EmoteContextMenu(EmoticonImage emoteImage, ContextMenuListener listener) {
        Emoticon emote = emoteImage.getEmoticon();
        this.listener = listener;
        this.emoteImage = emoteImage;
        
        if (emote.subType == Emoticon.SubType.CHEER) {
            addItem("cheer","Cheering Emote");
        } else {
            addItem("code", StringUtil.shortenTo(emote.code, 40, 28));
            if (emote.type == Emoticon.Type.EMOJI && emote.stringId != null) {
                addItem("codeEmoji", emote.stringId);
            }
        }
        addItem("emoteImage", emoteImage.getSizeString(), ICON_IMAGE);
        if (emote.numericId != Emoticon.ID_UNDEFINED) {
            addItem("emoteId", "ID: "+emote.numericId, ICON_WEB);
        }
        
        // Non-Twitch Emote Information
        if (emote.type != Emoticon.Type.TWITCH) {
            addSeparator();
            if (emote.type == Emoticon.Type.FFZ) {
                addItem("ffzlink", "FrankerFaceZ Emote", ICON_WEB);
            } else if (emote.type == Emoticon.Type.BTTV) {
                addItem("bttvlink", "BetterTTV Emote", ICON_WEB);
            } else if (emote.type == Emoticon.Type.CUSTOM) {
                addItem("", "Custom Emote");
            } else if (emote.type == Emoticon.Type.EMOJI) {
                addItem("", "Emoji ("+emote.creator+")");
            }
            if (emote.creator != null) {
                addItem("emoteCreator", "Emote by: " + emote.creator);
            }
            
            // Info
            if (emote.subType == Emoticon.SubType.EVENT) {
                for (String info : emote.getInfos()) {
                    addItem("", "Featured " + info);
                }
            } else {
                for (String info : emote.getInfos()) {
                    if (!info.equals(emote.stringId)) {
                        addItem("", info);
                    }
                }
            }
            
            addStreamSubmenu(emote);
        }
        
        if (emote.type == Emoticon.Type.NOT_FOUND_FAVORITE) {
            addItem("", "Not found favorite");
        }
        
        // Emoteset information
        if (emote.emoteSet > Emoticon.SET_GLOBAL) {
            addSeparator();
            if (Emoticons.isTurboEmoteset(emote.emoteSet)) {
                addItem("twitchturbolink", "Turbo Emoticon");
            } else if (!emote.hasStreamSet() && emote.hasEmotesetInfo()) {
                addItem("", emote.getEmotesetInfo()+" Emoticon");
            } else {
                addItem("", "Subscriber Emoticon");
                addStreamSubmenu(emote);
            }
            addItem("", "Emoteset: "+emote.emoteSet+
                    (emote.hasEmotesetInfo() && emote.hasStreamSet() ? " ("+emote.getEmotesetInfo()+")" : ""));
        }
        if (emote.emoteSet == Emoticon.SET_UNKNOWN) {
            addSeparator();
            addItem("", "Emoteset: unknown");
        }
        
        addSeparator();
        addItem("emoteDetails", "Show Details");
        
        addSeparator();
        addItem("ignoreEmote", "Ignore");
        if (emote.subType != Emoticon.SubType.CHEER) {
            if (!emote.hasStreamRestrictions()) {
                if (emoteManager.isFavorite(emote)) {
                    addItem("unfavoriteEmote", "UnFavorite");
                } else {
                    addItem("favoriteEmote", "Favorite");
                }
            }
        }

        if (Chatty.DEBUG) {
            addItem("", String.valueOf(System.identityHashCode(emote)));
            addItem("", emoteImage.getImageIcon().getDescription());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.emoteMenuItemClicked(e, emoteImage);
        }
    }
    
    public static void setEmoteManager(Emoticons emotes) {
        emoteManager = emotes;
    }
    
    private void addStreamSubmenu(Emoticon emote) {
        if (emote.hasStreamSet() && Helper.validateStream(emote.getStream())) {
            String subMenu = emote.getStream();
            addItem("stream", "Twitch Stream", subMenu);
            addItem("join", "Join " + Helper.toValidChannel(emote.getStream()), subMenu);
            addSeparator(subMenu);
            addItem("showChannelEmotes", "Show Emotes", subMenu);
        }
    }
    
}
