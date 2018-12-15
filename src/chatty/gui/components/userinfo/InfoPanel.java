
package chatty.gui.components.userinfo;

import chatty.Helper;
import chatty.User;
import chatty.util.colors.HtmlColors;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import static chatty.gui.components.userinfo.Util.makeGbc;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.api.ChannelInfo;
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
    
    private final JLabel firstSeen = new JLabel("");
    private final JLabel numberOfLines = new JLabel("");
    private final JLabel colorInfo = new JLabel("Color: #123456");
    
    private final JLabel createdAt = new JLabel("Loading..");
    private final JLabel followers = new JLabel();
    
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
        revalidate();
        owner.finishDialog();
        infoAdded = true;
    }
    
    private void removeInfo() {
        remove(panel2);
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
        if (requestedInfo == null) {
            addInfo();
            createdAt.setText(Language.getString("userDialog.loading"));
            createdAt.setToolTipText(null);
            followers.setText(null);
        } else {
            addInfo();
            setChannelInfo(requestedInfo);
        }
    }
    
    public void setChannelInfo(ChannelInfo info) {
        if (infoAdded) {
            createdAt.setText(Language.getString("userDialog.registered", DateTime.formatAccountAge(info.createdAt, DateTime.Formatting.VERBOSE)));
            createdAt.setToolTipText(Language.getString("userDialog.registered.tip", DateTime.formatFullDatetime(info.createdAt)));
            followers.setText(" "+Language.getString("userDialog.followers", Helper.formatViewerCount(info.followers)));
        }
    }
    
}
