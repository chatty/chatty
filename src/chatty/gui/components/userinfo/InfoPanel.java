
package chatty.gui.components.userinfo;

import chatty.Helper;
import chatty.User;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.util.colors.HtmlColors;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.ChannelInfo;
import chatty.util.api.Follower;
import chatty.util.api.TwitchApi;
import chatty.util.commands.CustomCommand;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 *
 * @author tduva
 */
public class InfoPanel extends JPanel {
    
    private final UserInfo owner;

    private final JPanel panel1 = new JPanel();
    private final JPanel panel2 = new JPanel();

    private final JLabel numberOfLines = new JLabel("");
    private final JLabel colorInfo = new JLabel("Color: #123456");
    
    private final JLabel createdAt = new JLabel("Loading..");
    private final JLabel followers = new JLabel();
    private final JLabel firstSeen = new JLabel();
    private final JLabel followedAt = new JLabel();

    private User currentUser;
    private ChannelInfo currentChannelInfo;
    private Follower currentFollower;
    
    public InfoPanel(UserInfo owner, ContextMenuListener listener) {
        this.owner = owner;
        
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 2));
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 1));
        
        panel1.add(numberOfLines);
        panel1.add(firstSeen);
        panel1.add(followedAt);
        
        panel2.add(colorInfo);
        panel2.add(followers);
        panel2.add(createdAt);
        
        //firstSeen.setComponentPopupMenu(new DataContextMenu("userid", listener));
        followedAt.setComponentPopupMenu(new DataContextMenu("following", listener));
        createdAt.setComponentPopupMenu(new DataContextMenu("account", listener));

        setLayout(new GridBagLayout());
        add(panel1, Util.makeGbc(0, 0, 1, 1));
        add(panel2, Util.makeGbc(0, 1, 1, 1));
        
        Timer updateTimer = new Timer(60*1000, e -> updateTimes(false));
        updateTimer.setRepeats(true);
        updateTimer.start();
    }
    
    public void update(User user) {
        if (user != currentUser) {
            currentUser = user;
            showInfo();
        }
        numberOfLines.setText("Messages: "+user.getNumberOfMessages());
        updateColor();
        updateTimes(true);
    }
    
    private void updateTimes(boolean force) {
        if (!owner.isVisible() && !force) {
            return;
        }
        if (currentUser != null) {
            firstSeen.setText(String.format("First seen: %s ago",
                    formatAgoTime(currentUser.getCreatedAt())));
            firstSeen.setToolTipText(String.format("<html>First seen: %s ago (%s)"
                    + "<br /><br />"
                    + "(Could mean the first message or when the user first joined, during this Chatty session)",
                    formatAgoTimeVerbose(currentUser.getCreatedAt()),
                    DateTime.formatFullDatetime(currentUser.getCreatedAt())));
        }
    }
    
    private void updateColor() {
        Color color = currentUser.getColor();
        Color correctedColor = currentUser.getCorrectedColor();
        
        String colorNamed = HtmlColors.getNamedColorString(color);
        String correctedColorNamed = HtmlColors.getNamedColorString(correctedColor);
        
        String colorCode = HtmlColors.getColorString(color);
        String correctedColorCode = HtmlColors.getColorString(correctedColor);
        
        String colorText;
        String colorTooltipText;

        if (currentUser.hasCustomColor()) {
            Color plainColor = currentUser.getPlainColor();
            colorText = "Color: "+colorNamed+"**";
            colorTooltipText = "Custom Color: "+colorCode
                    +" (Original: "+HtmlColors.getNamedColorString(plainColor)+"/"
                    + HtmlColors.getColorString(plainColor)+")";
        } else if (currentUser.hasDefaultColor()) {
            colorText = "Color: "+colorNamed+"*";
            colorTooltipText = "Color: "+colorCode+" (default)";
        } else if (currentUser.hasCorrectedColor() && !colorCode.equals(correctedColorCode)) {
            colorText = "Color: "+correctedColorNamed+" ("+colorNamed+")";
            colorTooltipText = "Corrected Color: "+correctedColorCode
                    +" (Original: "+colorNamed+"/"+colorCode+")";
        } else {
            colorText = "Color: "+colorNamed;
            colorTooltipText = "Color: "+colorCode;
        }
        colorInfo.setText(colorText);
        colorInfo.setToolTipText(colorTooltipText);
    }
    
    private void showInfo() {
        // Channel Info
        ChannelInfo requestedInfo = owner.getChannelInfo();
        currentChannelInfo = null;
        if (requestedInfo == null) {
            createdAt.setText(Language.getString("userDialog.loading"));
            createdAt.setToolTipText(null);
            followers.setText(null);
            panel2.setToolTipText(null);
        } else {
            setChannelInfo(requestedInfo);
        }
        
        // Follower Info
        Follower follow = owner.getFollowInfo(false);
        currentFollower = null;
        if (follow == null) {
            followedAt.setText(Language.getString("userDialog.loading"));
            followedAt.setToolTipText(null);
        } else {
            setFollowInfo(follow, TwitchApi.RequestResultCode.SUCCESS);
        }
        
        // For button containing $(followage) and such
        owner.updateButtons();
    }
    
    public void setChannelInfo(ChannelInfo info) {
        if (info != null) {
            currentChannelInfo = info;
            createdAt.setText(Language.getString("userDialog.registered",
                    formatAgoTime(info.createdAt)));
    //        createdAt.setToolTipText(Language.getString("userDialog.registered.tip",
    //                DateTime.formatFullDatetime(info.createdAt)));
            followers.setText(Language.getString("userDialog.followers",
                    Helper.formatViewerCount(info.followers)));
            String tooltip = String.format("<html>"
                    + "Registered: %6$s ago (%7$s)<br />"
                    + "ID: %8$s<br />"
                    + "Type: %9$s<br />"
                    + "<br />"
                    + "Title: %2$s<br />"
                    + "Category: %3$s<br />"
                    + "Views: %4$s<br />"
                    + "Followers: %5$s<br />"
                    + "<br />"
                    + "%1$s<br />"
                    + "<br />"
                    + "(Info may not be entirely up-to-date)",
                    !StringUtil.isNullOrEmpty(info.description)
                            ? StringUtil.addLinebreaks(Helper.htmlspecialchars_encode(info.description), 70, true)
                            : "No description",
                    info.status,
                    info.game,
                    Helper.formatViewerCount(info.views),
                    Helper.formatViewerCount(info.followers),
                    formatAgoTimeVerbose(info.createdAt),
                    DateTime.formatFullDatetime(info.createdAt),
                    info.id,
                    !StringUtil.isNullOrEmpty(info.broadcaster_type)
                            ? StringUtil.firstToUpperCase(info.broadcaster_type)
                            : "Regular");
            followers.setToolTipText(tooltip);
            createdAt.setToolTipText(tooltip);

            // Should mostly already be set, but just in case
            if (currentUser.getId() == null) {
                currentUser.setId(info.id);
            }
            // For button containing $(followage) and such
            owner.updateButtons();
        } else {
            createdAt.setText(Language.getString("userDialog.error"));
        }
    }

    public void setFollowInfo(Follower follower, TwitchApi.RequestResultCode result) {
        if (result == TwitchApi.RequestResultCode.SUCCESS && follower.follow_time != -1) {
            followedAt.setText(Language.getString("userDialog.followed",
                    formatAgoTime(follower.follow_time)));
            followedAt.setToolTipText(Language.getString("userDialog.followed.tip",
                    formatAgoTimeVerbose(follower.follow_time),
                    DateTime.formatFullDatetime(follower.follow_time)));
            currentFollower = follower;
        } else if (result == TwitchApi.RequestResultCode.NOT_FOUND) {
            followedAt.setText(Language.getString("userDialog.notFollowing"));
            followedAt.setToolTipText(null);
        } else {
            followedAt.setText(Language.getString("userDialog.error"));
            followedAt.setToolTipText(null);
        }
        // For button containing $(followage) and such
        owner.updateButtons();
    }
    
    private static String formatAgoTime(long time) {
        return DateTime.formatAccountAgeCompact(time);
    }
    
    private static String formatAgoTimeVerbose(long time) {
        return DateTime.formatAccountAgeVerbose(time);
    }
    
    protected String getFollowAge() {
        if (currentFollower != null) {
            return formatAgoTimeVerbose(currentFollower.follow_time);
        }
        return null;
    }
    
    protected String getFollowDate() {
        if (currentFollower != null) {
            return DateTime.formatFullDatetime(currentFollower.follow_time);
        }
        return null;
    }
    
    protected String getAccountAge() {
        if (currentChannelInfo != null) {
            return formatAgoTimeVerbose(currentChannelInfo.createdAt);
        }
        return null;
    }
    
    protected String getAccountDate() {
        if (currentChannelInfo != null) {
            return DateTime.formatFullDatetime(currentChannelInfo.createdAt);
        }
        return null;
    }
    
    protected String getChannelInfoTooltipText() {
        if (currentChannelInfo != null) {
            return createdAt.getToolTipText().replace("<br />", "\n").replace("<html>", "");
        }
        return null;
    }
    
    /**
     * Indiciate that the follow age data is being loaded.
     */
    protected void setRefreshingFollowAge() {
        followedAt.setText(Language.getString("userDialog.loading"));
    }
    
    private static class DataContextMenu extends ContextMenu {

        private final ContextMenuListener listener;
        
        public DataContextMenu(String type, ContextMenuListener listener) {
            this.listener = listener;
            
            if (type.equals("userid")) {
                addItem("copyUserId", "Copy User ID");
            } else if (type.equals("following")) {
                addItem("sendFollowAge", "Send Follow Age message");
                addItem("copyFollowAge", "Copy Follow Age");
                addSeparator();
                addItem("refresh", "Refresh");
            } else if (type.equals("account")) {
                addItem("sendAccountAge", "Send Account Age message");
                addItem("copyAccountAge", "Copy Account Age");
                addSeparator();
                addItem("copyUserId", "Copy ID");
                addItem("copyChannelInfo", "Copy Full Info (Tooltip)");
            }
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            listener.menuItemClicked(e);
        }

    }
    
    protected static final CustomCommand COMMAND_FOLLOW_AGE =
            CustomCommand.parse("$$(nick) has been following for $$(followage)");
    protected static final CustomCommand COMMAND_ACCOUNT_AGE =
            CustomCommand.parse("$$(nick) has been registered for $$(accountage)");
}
