
package chatty.gui.components.menus;

import chatty.Chatty;
import chatty.Helper;
import static chatty.gui.components.menus.ContextMenuHelper.ICON_IMAGE;
import static chatty.gui.components.menus.ContextMenuHelper.ICON_WEB;
import chatty.lang.Language;
import chatty.util.StringUtil;
import chatty.util.TwitchEmotesApi;
import chatty.util.TwitchEmotesApi.EmotesetInfo;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticons;
import chatty.util.api.CachedImage;
import java.awt.event.ActionEvent;

/**
 * Shows information about the emote that was right-clicked on.
 * 
 * @author tduva
 */
public class EmoteContextMenu extends ContextMenu {
    
    private static final Object unique = new Object();
    
    private static Emoticons emoteManager;
    private final ContextMenuListener listener;
    private final CachedImage<Emoticon> emoteImage;
    
    public EmoteContextMenu(CachedImage<Emoticon> emoteImage, ContextMenuListener listener) {
        Emoticon emote = emoteImage.getObject();
        this.listener = listener;
        this.emoteImage = emoteImage;
        
        if (emote.subType == Emoticon.SubType.CHEER) {
            addItem("cheer","Cheering Emote");
        } else {
            addItem("code", StringUtil.shortenTo(emote.code, 40, 28));
            if (emote.type == Emoticon.Type.EMOJI && emote.stringId != null) {
                if (emote.stringIdAlias != null) {
                    addItem("codeEmoji", emote.stringId+" ("+emote.stringIdAlias+")");
                } else {
                    addItem("codeEmoji", emote.stringId);
                }
            }
        }
        addItem("emoteImage", emoteImage.getSizeString(), ICON_IMAGE);
        switch (emote.type) {
            case TWITCH:
            case FFZ:
            case BTTV:
            case SEVENTV:
                addItem("emoteId", "ID: "+StringUtil.shortenTo(emote.stringId, 14), ICON_WEB);
                break;
        }
        
        // Non-Twitch Emote Information
        if (emote.type != Emoticon.Type.TWITCH) {
            addSeparator();
            switch (emote.type) {
                case FFZ:
                    addItem("ffzlink", "FrankerFaceZ Emote", ICON_WEB);
                    break;
                case BTTV:
                    addItem("bttvlink", "BetterTTV Emote", ICON_WEB);
                    break;
                case SEVENTV:
                    addItem("seventvlink", "7TV Emote", ICON_WEB);
                    break;
                case CUSTOM:
                    addItem("", "Custom Emote");
                    break;
                case CUSTOM2:
                    addItem("", "Local Emote");
                    break;
                case EMOJI:
                    addItem("", "Emoji");
                    break;
            }
            if (emote.creator != null) {
                addItem("emoteCreator", Language.getString("emoteCm.emoteBy", emote.creator));
            }
            
            // Info
            if (emote.subType == Emoticon.SubType.EVENT) {
                for (String info : emote.getInfos()) {
                    addItem("", "Featured " + info);
                }
            } else {
                for (String info : emote.getInfos()) {
                    addItem("", info);
                }
            }
            
            addStreamSubmenu(emote);
        }
        
        if (!emote.hasGlobalEmoteset()) {
            addSeparator();
            addItem("", TwitchEmotesApi.getEmoteType(emote));
        }

        addSeparator();
        addItem("emoteDetails", Language.getString("emoteCm.showDetails"));
        
        addSeparator();
        if (!emoteManager.isEmoteIgnored(emote, 0)) {
            addItem("ignoreEmote", Language.getString("emoteCm.ignore"));
        }
        else {
            addItem("ignoreEmote", Language.getString("emoteCm.changeIgnore"));
        }
        if (emote.subType != Emoticon.SubType.CHEER
                && ((emote.emoteset != null && !emote.emoteset.isEmpty())
                || emote.type != Emoticon.Type.TWITCH)) {
            if (emoteManager.isFavorite(emote)) {
                addItem("unfavoriteEmote", Language.getString("emoteCm.unfavorite"));
            }
            else {
                addItem("favoriteEmote", Language.getString("emoteCm.favorite"));
            }
        }
        
        if (emoteManager.canAddCustomLocal(emote)) {
            if (!emoteManager.isCustomLocal(emote)) {
                addItem("addCustomLocalEmote", "Add local emote");
            }
            else {
                addItem("removeCustomLocalEmote", "Remove local emote");
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
        if (emote.hasStreamSet() && Helper.isValidStream(emote.getStream())) {
            String subMenu = emote.getStream();
            addItem("stream", "Twitch Stream", subMenu);
            addItem("join", Language.getString("userCm.join", emote.getStream()), subMenu);
            addSeparator(subMenu);
            addItem("showChannelEmotes", Language.getString("emoteCm.showEmotes"), subMenu);
        }
    }
    
}
