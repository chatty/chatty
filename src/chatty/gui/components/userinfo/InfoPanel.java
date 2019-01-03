
package chatty.gui.components.userinfo;

import chatty.Helper;
import chatty.User;
import chatty.util.colors.HtmlColors;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.api.ChannelInfo;
import chatty.util.api.Follower;
import chatty.util.api.TwitchApi;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
    private final JLabel userID = new JLabel();
    private final JLabel followedAt = new JLabel();

    private User currentUser;
    private ChannelInfo currentChannelInfo;
    private Follower currentFollower;
    
    public InfoPanel(UserInfo owner) {
        this.owner = owner;
        
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 2));
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 2));
        
        panel1.add(numberOfLines);
        panel1.add(colorInfo);
        panel1.add(followedAt);
        
        panel2.add(userID);
        panel2.add(followers);
        panel2.add(createdAt);

        setLayout(new GridBagLayout());
        add(panel1, Util.makeGbc(0, 0, 1, 1));
        add(panel2, Util.makeGbc(0, 1, 1, 1));
    }
    
    public void update(User user) {
        if (user != currentUser) {
            showInfo();
        }
        currentUser = user;
        numberOfLines.setText(" Messages: "+user.getNumberOfMessages());
        updateColor();
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
        Follower follow = owner.getFollowInfo();
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
        currentChannelInfo = info;
        createdAt.setText(Language.getString("userDialog.registered",
                formatAgoTime(info.createdAt)));
        createdAt.setToolTipText(Language.getString("userDialog.registered.tip",
                DateTime.formatFullDatetime(info.createdAt)));
        followers.setText(Language.getString("userDialog.followers",
                Helper.formatViewerCount(info.followers)));
        userID.setText(Language.getString("userDialog.id", info.id));
        panel2.setToolTipText(String.format("<html><em>Channel Info</em><br />"
                + "Title: %s<br />"
                + "Game: %s<br />"
                + "Views: %s<br />"
                + "(Info may not be entirely up-to-date)",
                info.status, info.game, Helper.formatViewerCount(info.views)));
        
        // Should mostly already be set, but just in case
        if (currentUser.getId() == null) {
            currentUser.setId(info.id);
        }
        // For button containing $(followage) and such
        owner.updateButtons();
    }

    public void setFollowInfo(Follower follower, TwitchApi.RequestResultCode result) {
        if (result == TwitchApi.RequestResultCode.SUCCESS && follower.time != -1) {
            followedAt.setText(Language.getString("userDialog.followed",
                    formatAgoTime(follower.time)));
            followedAt.setToolTipText(Language.getString("userDialog.followed.tip",
                    DateTime.formatFullDatetime(follower.time)));
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
        return DateTime.formatAccountAge(time, DateTime.Formatting.VERBOSE,
                DateTime.Formatting.LAST_ONE_EXACT);
    }
    
    protected String getFollowAge() {
        if (currentFollower != null) {
            return formatAgoTime(currentFollower.time);
        }
        return null;
    }
}
