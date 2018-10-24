
package chatty.gui.components.userinfo;

import chatty.Helper;
import chatty.User;
import chatty.gui.HtmlColors;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import static chatty.gui.components.userinfo.Util.makeGbc;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.api.ChannelInfo;
import chatty.util.api.Follower;
import chatty.util.api.TwitchApi;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
    private final JPanel panel3 = new JPanel();

    private final JLabel firstSeen = new JLabel("");
    private final JLabel numberOfLines = new JLabel("");
    private final JLabel colorInfo = new JLabel("Color: #123456");
    
    private final JLabel createdAt = new JLabel("Loading..");
    private final JLabel followers = new JLabel();
    private final JLabel userID = new JLabel();
    private final JLabel followedAt = new JLabel();

    private User currentUser;
    private boolean infoAdded;
    
    public InfoPanel(UserInfo owner) {
        this.owner = owner;
        
        panel1.add(numberOfLines);
        panel1.add(firstSeen);
        panel1.add(colorInfo);
        
        LinkLabel link = new LinkLabel("[open:details More..]", new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                toggleInfo();
            }
        });
        panel1.add(link);
        
        panel2.add(createdAt);
        panel2.add(followers);
        panel3.add(userID);
        panel3.add(followedAt);

        setLayout(new GridBagLayout());
        
        add(panel1, Util.makeGbc(0, 0, 1, 1));
    }
    
    public void update(User user) {
        if (user != currentUser && infoAdded) {
            showInfo();
        }
        currentUser = user;
        firstSeen.setText(" First seen: "+DateTime.format(user.getCreatedAt()));
        firstSeen.setToolTipText("First seen (this session only): "+DateTime.formatFullDatetime(user.getCreatedAt()));
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

    private void addInfo() {
        if (infoAdded) {
            return;
        }
        GridBagConstraints gbc = makeGbc(0, 7, 3, 1);
        gbc.insets = new Insets(-8, 5, 0, 5);
        add(panel2, gbc);
        gbc.gridy = 16;
        gbc.insets = new Insets(-4, 5, 0, 5);
        add(panel3, gbc);
        revalidate();
        owner.finishDialog();
        infoAdded = true;
    }
    
    private void removeInfo() {
        remove(panel2);
        remove(panel3);
        revalidate();
        owner.finishDialog();
        infoAdded = false;
    }
    
    private void toggleInfo() {
        if (infoAdded) {
            removeInfo();
        } else {
            showInfo();
        }
    }
    
    private void showInfo() {
        ChannelInfo requestedInfo = owner.getChannelInfo();
        Follower follow = owner.getFollow();
        addInfo();
        if (requestedInfo == null) {
            createdAt.setText(Language.getString("userDialog.loading"));
            createdAt.setToolTipText(null);
            followers.setText(null);
        } else {
            setChannelInfo(requestedInfo);
        }
        if (follow == null) {
            followedAt.setText(Language.getString("userDialog.loading"));
            followedAt.setToolTipText(null);
        } else {
            setFollowInfo(follow, TwitchApi.RequestResultCode.SUCCESS);
        }
    }
    
    public void setChannelInfo(ChannelInfo info) {
        if (infoAdded) {
            createdAt.setText(Language.getString("userDialog.registered", DateTime.formatAccountAge(info.createdAt, DateTime.Formatting.VERBOSE)));
            createdAt.setToolTipText(Language.getString("userDialog.registered.tip", DateTime.formatFullDatetime(info.createdAt)));
            followers.setText(" "+Language.getString("userDialog.followers", Helper.formatViewerCount(info.followers)));
            userID.setText(" "+Language.getString("userDialog.id", info.id));
        }
    }

    public void setFollowInfo(Follower follow, TwitchApi.RequestResultCode result) {
        if (infoAdded) {
            if (result == TwitchApi.RequestResultCode.SUCCESS) {
                followedAt.setText(" "+Language.getString("userDialog.followed", DateTime.formatAccountAge(follow.time, DateTime.Formatting.VERBOSE)));
                followedAt.setToolTipText(Language.getString("userDialog.followed.tip", DateTime.formatFullDatetime(follow.time)));
            } else if (result == TwitchApi.RequestResultCode.NOT_FOUND) {
                followedAt.setText(Language.getString("userDialog.notFollowing"));
                followedAt.setToolTipText(null);
            } else {
                followedAt.setText(Language.getString("userDialog.error"));
                followedAt.setToolTipText(null);
            }
        }
    }
}
