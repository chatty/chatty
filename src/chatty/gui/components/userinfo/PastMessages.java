
package chatty.gui.components.userinfo;

import chatty.User;
import chatty.util.DateTime;
import chatty.util.MiscUtil;
import chatty.util.RepeatMsgHelper;
import chatty.util.StringUtil;
import chatty.util.Timestamp;
import chatty.util.colors.ColorCorrectionNew;
import chatty.util.settings.Settings;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;

/**
 *
 * @author tduva
 */
public class PastMessages extends JTextArea {
    
    private Timestamp timestampFormat = new Timestamp("[HH:mm:ss]", "");
    
    private String currentMessageIdMessage;
    
    private final RepeatMsgHelper repeatHelper;
    private final Settings settings;
    
    private final Map<Integer, Integer> highlights = new HashMap<>();
    private int highlightStart;
    private final DefaultHighlighter.DefaultHighlightPainter highlightPainter;
    
    // These values are used in the settings, so they must not be changed
    public final static int CURRENT_MSG = 1 << 0;
    public final static int REPEATED_MSG = 1 << 1;
    public final static int MOD_ACTION = 1 << 2;
    public final static int AUTO_MOD = 1 << 3;
    public final static int LOW_TRUST = 1 << 4;
    
    public PastMessages(RepeatMsgHelper repeat, Settings settings) {
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        this.repeatHelper = repeat;
        this.settings = settings;
        highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(ColorCorrectionNew.offset(getBackground(), 0.8f));
    }
    
    public String getCurrentMessage() {
        return currentMessageIdMessage;
    }
    
    public void update(User user, String currentMessageId) {
        setText(null);
        if (user != null) {
            highlights.clear();
            setText(makeLines(user, currentMessageId));
            
            for (Map.Entry<Integer, Integer> entry : highlights.entrySet()) {
                try {
                    getHighlighter().addHighlight(entry.getKey(), entry.getValue(), highlightPainter);
                }
                catch (BadLocationException ex) {
                    // Ignore
                }
            }
        }
    }
    
    private void startHighlight(int pos, int type) {
        if (MiscUtil.isBitEnabled((int)settings.getLong("userMessagesHighlight"), type)) {
            highlightStart = pos;
        }
    }
    
    private void endHighlight(int pos, int type) {
        if (MiscUtil.isBitEnabled((int)settings.getLong("userMessagesHighlight"), type)
                && pos > highlightStart) {
            highlights.put(highlightStart, pos);
        }
    }
    
    private String makeLines(User user, String currentMessageId) {
        StringBuilder b = new StringBuilder();
        if (user.linesCleared()) {
            b.append("<some lines cleared due to user inactivity>\n");
        }
        if (user.maxLinesExceeded()) {
            b.append("<only last ");
            b.append(user.getMaxNumberOfLines());
            b.append(" lines are saved>\n");
        }
        String currentMsgText = user.getMessageText(currentMessageId);
        String lowTrustInfo = null;
        int messageCountSinceLowTrustInfo = 0;
        List<User.Message> messages = user.getMessages();
        int currentDay = 0;
        for (User.Message m : messages) {
            messageCountSinceLowTrustInfo++;
            // Date separator
            LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(m.getTime()), ZoneId.systemDefault());
            if (date.getDayOfMonth() != currentDay) {
                currentDay = date.getDayOfMonth();
                b.append("- ");
                b.append(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)));
                b.append(" -");
                b.append("\n");
            }
            // Messages
            if (m instanceof User.TextMessage) {
                User.TextMessage tm = (User.TextMessage)m;
                
                if (tm.lowTrust != null) {
                    if (lowTrustInfo == null
                            || !lowTrustInfo.equals(tm.lowTrust.makeInfo())
                            || messageCountSinceLowTrustInfo > 4) {
                        lowTrustInfo = tm.lowTrust.makeInfo();
                        b.append(timestampFormat.make(m.getTime(), user.getRoom())).append("I ");
                        startHighlight(b.length(), LOW_TRUST);
                        b.append(lowTrustInfo);
                        endHighlight(b.length(), LOW_TRUST);
                        b.append("\n");
                        messageCountSinceLowTrustInfo = 0;
                    }
                }
                
                int simPercentage = 0;
                if (!StringUtil.isNullOrEmpty(currentMessageId)
                        && currentMessageId.equals(tm.id)) {
                    startHighlight(b.length(), CURRENT_MSG);
                    b.append(">");
                    endHighlight(b.length(), CURRENT_MSG);
                    //singleMessage.setText(SINGLE_MESSAGE_CHECK+" ("+StringUtil.shortenTo(tm.text, 14)+")");
                    currentMessageIdMessage = tm.text;
                }
                else if (currentMsgText != null) {
                    simPercentage = repeatHelper.getPercentage(user, tm.text, currentMsgText);
                }
                b.append(timestampFormat.make(m.getTime(), user.getRoom()));
                if (tm.lowTrust != null) {
                    startHighlight(b.length(), LOW_TRUST);
                    switch (tm.lowTrust.treatment) {
                        case ACTIVE_MONITORING:
                            b.append("[M]");
                            break;
                        case RESTRICTED:
                            b.append("[R]");
                            break;
                    }
                    endHighlight(b.length(), LOW_TRUST);
                }
                if (simPercentage > 0) {
                    startHighlight(b.length() + 1, REPEATED_MSG);
                    b.append(" [").append(simPercentage).append("%]");
                    endHighlight(b.length(), REPEATED_MSG);
                }
                if (tm.action) {
                    b.append("* ");
                } else {
                    b.append(" ");
                }
                b.append(tm.text);
                b.append("\n");
            }
            else if (m instanceof User.BanMessage) {
                User.BanMessage bm = (User.BanMessage)m;
                b.append(timestampFormat.make(m.getTime(), user.getRoom())).append(">");
                startHighlight(b.length(), MOD_ACTION);
                if (bm.duration > 0) {
                    b.append("Timed out (").append(bm.duration).append("s)");
                }
                else {
                    b.append("Banned permanently");
                }
                endHighlight(b.length(), MOD_ACTION);
                if (bm.id != null) {
                    b.append(" (single message)");
                }
                if (bm.reason != null && !bm.reason.isEmpty()) {
                    b.append(" [").append(bm.reason).append("]");
                }
                if (bm.by != null) {
                    b.append(" (@").append(bm.by).append(")");
                }
                b.append("\n");
            }
            else if (m instanceof User.UnbanMessage) {
                User.UnbanMessage ubm = (User.UnbanMessage)m;
                b.append(timestampFormat.make(m.getTime(), user.getRoom())).append(">");
                startHighlight(b.length(), MOD_ACTION);
                if (ubm.type == User.UnbanMessage.TYPE_UNBAN) {
                    b.append("Unbanned");
                } else if (ubm.type == User.UnbanMessage.TYPE_UNTIMEOUT) {
                    b.append("Timeout removed");
                }
                endHighlight(b.length(), MOD_ACTION);
                b.append(" (@").append(ubm.by).append(")");
                b.append("\n");
            }
            else if (m instanceof User.MsgDeleted) {
                User.MsgDeleted md = (User.MsgDeleted)m;
                b.append(timestampFormat.make(m.getTime(), user.getRoom())).append(">");
                startHighlight(b.length(), MOD_ACTION);
                b.append("Message deleted: ").append(md.msg);
                endHighlight(b.length(), MOD_ACTION);
                if (md.by != null) {
                    b.append(" (@").append(md.by).append(")");
                }
                b.append("\n");
            }
            else if (m instanceof User.SubMessage) {
                User.SubMessage sm = (User.SubMessage)m;
                
                int simPercentage = 0;
                if (!StringUtil.isNullOrEmpty(currentMessageId)
                        && currentMessageId.equals(sm.id)) {
                    startHighlight(b.length(), CURRENT_MSG);
                    b.append(">");
                    endHighlight(b.length(), CURRENT_MSG);
                    //singleMessage.setText(SINGLE_MESSAGE_CHECK+" ("+StringUtil.shortenTo(tm.text, 14)+")");
                    currentMessageIdMessage = sm.attached_message;
                }
                else if (currentMsgText != null) {
                    simPercentage = repeatHelper.getPercentage(user, sm.attached_message, currentMsgText);
                }
                
                b.append(timestampFormat.make(m.getTime(), user.getRoom()));
                
                if (simPercentage > 0) {
                    startHighlight(b.length() + 1, REPEATED_MSG);
                    b.append(" [").append(simPercentage).append("%]");
                    endHighlight(b.length(), REPEATED_MSG);
                }
                
                b.append("$ ");
                b.append(sm.system_msg);
                if (!sm.attached_message.isEmpty()) {
                    b.append(" [").append(sm.attached_message).append("]");
                }
                b.append("\n");
            }
            else if (m instanceof User.InfoMessage) {
                User.InfoMessage sm = (User.InfoMessage)m;
                b.append(timestampFormat.make(m.getTime(), user.getRoom())).append("I ");
                b.append(sm.full_text);
                b.append("\n");
            }
            else if (m instanceof User.WarnMessage) {
                User.WarnMessage wm = (User.WarnMessage)m;
                b.append(timestampFormat.make(m.getTime(), user.getRoom())).append("! ");
                if (wm.by == null && wm.reason == null) {
                    b.append("Warning acknowledged");
                }
                else {
                    b.append("Warned by ").append(wm.by).append(" (").append(wm.reason).append(")");
                }
                b.append("\n");
            }
            else if (m instanceof User.ModAction) {
                User.ModAction ma = (User.ModAction)m;
                b.append(timestampFormat.make(m.getTime(), user.getRoom())).append(">");
                b.append("ModAction: /");
                b.append(ma.commandAndParameters);
                b.append("\n");
            }
            else if (m instanceof User.AutoModMessage) {
                User.AutoModMessage ma = (User.AutoModMessage)m;
                if (!StringUtil.isNullOrEmpty(currentMessageId)
                        && currentMessageId.equals(ma.id)) {
                    b.append(">");
                }
                b.append(timestampFormat.make(m.getTime(), user.getRoom())).append(">");
                startHighlight(b.length(), AUTO_MOD);
                b.append("Filtered by AutoMod");
                endHighlight(b.length(), AUTO_MOD);
                if (!StringUtil.isNullOrEmpty(ma.reason)) {
                    b.append(" [").append(ma.reason).append("]");
                }
                b.append(": ").append(ma.message);
                b.append("\n");
            }
        }
        // Remove last newline
        if (b.length() > 0 && b.charAt(b.length() - 1) == '\n') {
            b.deleteCharAt(b.length() - 1);
        }
        return b.toString();
    }

    public void setTimestampFormat(Timestamp timestampFormat) {
        if (timestampFormat != null) {
            this.timestampFormat = timestampFormat;
        }
    }
    
}
