
package chatty.gui.components;

import chatty.gui.DockedDialogHelper;
import chatty.gui.DockedDialogManager;
import chatty.gui.MainGui;
import chatty.gui.components.textpane.ModLogInfo;
import chatty.util.DateTime;
import chatty.util.api.pubsub.ModeratorActionData;
import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockContentContainer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 *
 * @author tduva
 */
public class ModerationLog extends JDialog {
    
    private static final int MAX_NUMBER_LINES = 100;
    
    private final JTextArea log;
    private final JScrollPane scroll;
    
    private final DockedDialogHelper helper;
    
    private final Map<String, List<String>> cache = new HashMap<>();
    
    private String currentChannel;
    private String currentLoadedChannel;
    
    private final Set<String> unread = new HashSet<>();

    public ModerationLog(MainGui owner, DockedDialogManager dockedDialogs) {
        super(owner);
        log = createLogArea();
        
        scroll = new JScrollPane(log);
        scroll.setPreferredSize(new Dimension(300, 200));
        add(scroll, BorderLayout.CENTER);
        
        DockContent content = dockedDialogs.createStyledContent(scroll, "Mod Actions", "-modlog-");
        helper = dockedDialogs.createHelper(new DockedDialogHelper.DockedDialog() {
            @Override
            public void setVisible(boolean visible) {
                ModerationLog.super.setVisible(visible);
            }

            @Override
            public boolean isVisible() {
                return ModerationLog.super.isVisible();
            }

            @Override
            public void addComponent(Component comp) {
                add(comp, BorderLayout.CENTER);
            }

            @Override
            public void removeComponent(Component comp) {
                remove(comp);
            }

            @Override
            public Window getWindow() {
                return ModerationLog.this;
            }

            @Override
            public DockContent getContent() {
                return content;
            }
        });
        helper.installContextMenu(log);
        
        setTitle("Moderator Actions");
        
        pack();
    }
    
    @Override
    public void setVisible(boolean visible) {
        helper.setVisible(visible, true);
    }

    @Override
    public boolean isVisible() {
        return helper.isVisible();
    }
    
    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (helper != null) {
            helper.getContent().setLongTitle(title);
        }
    }

    private static JTextArea createLogArea() {
        // Caret to prevent scrolling
        DefaultCaret caret = new DefaultCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        JTextArea text = new JTextArea();
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setCaret(caret);
        text.setBorder(BorderFactory.createEmptyBorder(1, 2, 3, 2));
        return text;
    }
    
    public void setChannel(String channel) {
        if (helper.isContentVisible()) {
            unread.remove(channel);
        }
        if (channel != null && !channel.equals(currentChannel)) {
            if (unread.contains(channel)) {
                helper.setNewMessage();
            }
            else {
                helper.resetNewMessage();
            }
            currentChannel = channel;
            setTitle("Moderation Actions ("+channel+")");

            if (isVisible()) {
                setDataToCurrent();
            }
        }
    }
    
    private void setDataToCurrent() {
        if (currentChannel != null && !currentChannel.equals(currentLoadedChannel)) {
            currentLoadedChannel = currentChannel;
            List<String> cached = cache.get(currentChannel);
            if (cached != null) {
                StringBuilder b = new StringBuilder();
                String linebreak = "";
                for (String line : cached) {
                    b.append(linebreak);
                    b.append(line);
                    linebreak = "\n";
                }
                log.setText(b.toString());
                scrollDown();
            } else {
                log.setText(null);
            }
        }
    }
    
    public void add(ModeratorActionData data) {
        if (data.stream == null) {
            return;
        }
        String channel = data.stream;
        
        String line = String.format("[%s] <%s> /%s %s",
                DateTime.currentTime(),
                data.created_by,
                data.moderation_action,
                ModLogInfo.makeArgsText(data));
        
        if (channel.equals(currentLoadedChannel)) {
            printLine(log, line);
            // Only set for current channel (automatically checks for visible)
            helper.setNewMessage();
        }
        // Should be added for non-active channels, so on switch it can be set
        if (!helper.isContentVisible()) {
            unread.add(channel);
        }
        
        if (!cache.containsKey(channel)) {
            cache.put(channel, new ArrayList<>());
        }
        cache.get(channel).add(line);
        if (cache.get(channel).size() > MAX_NUMBER_LINES) {
            cache.get(channel).remove(0);
        }
    }
    
    private void printLine(JTextArea text, String line) {
        try {
            Document doc = text.getDocument();
            String linebreak = doc.getLength() > 0 ? "\n" : "";
            doc.insertString(doc.getLength(), linebreak+line, null);
            JScrollBar bar = scroll.getVerticalScrollBar();
            boolean scrollDown = bar.getValue() > bar.getMaximum() - bar.getVisibleAmount() - 4;
            if (scrollDown) {
                scrollDown();
            }
            clearSomeChat(doc);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private void scrollDown() {
        scroll.validate();
        scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
            }
        });
    }
    
    /**
     * Removes some lines from the given Document so it won't exceed the maximum
     * number of lines.
     * 
     * @param doc 
     */
    public void clearSomeChat(Document doc) {
        int count = doc.getDefaultRootElement().getElementCount();
        if (count > MAX_NUMBER_LINES) {
            removeFirstLines(doc, 10);
        }
    }
    
    /**
     * Removes the given number of lines from the given Document.
     * 
     * @param doc
     * @param amount 
     */
    private void removeFirstLines(Document doc, int amount) {
        if (amount < 1) {
            amount = 1;
        }
        Element firstToRemove = doc.getDefaultRootElement().getElement(0);
        Element lastToRemove = doc.getDefaultRootElement().getElement(amount - 1);
        int startOffset = firstToRemove.getStartOffset();
        int endOffset = lastToRemove.getEndOffset();
        try {
            doc.remove(startOffset,endOffset);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
   }
    
    public void showDialog() {
        setVisible(true);
        setDataToCurrent();
    }
}
