
package chatty.gui.components;

import chatty.Chatty;
import chatty.Helper;
import chatty.Logging;
import chatty.util.DateTime;
import chatty.util.ElapsedTime;
import chatty.util.LogUtil;
import chatty.util.MiscUtil;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.LogRecord;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author tduva
 */
public class ErrorMessage extends JDialog {

    public static final int CONTINUE = 0;
    public static final int QUIT = 1;
    
    private static final int ERROR_LIMIT = 20;
    
    private static final String TOP_MESSAGE = "<html><body width='300px'>An unexpected error has occured.";
    
    private static final String MESSAGE = "<html><body width='300px'>"
            + "You can help to fix "
            + "the error by [url:https://docs.google.com/forms/d/1pCc1xcWHOK1JPetQNc_N3boijnm3srcPH8PcfyVYG_U/viewform reporting it]."
            + " Please include a copy of the report message below and a short description of what you were "
            + "doing when the error occured.<br /><br />"
            + "Alternatively to the report page you can also send the report via "
            + "[url:mailto:chattyclient@gmail.com E-Mail]. "
            + "You can use something like [url:https://pastebin.com Pastebin.com] "
            + "to send the text as a link.<br /><br />"
            + "Please also check if there already is an update available for "
            + "Chatty, since often bugs are fixed in newer versions.";
    
    private static final String MINIMIZED_MESSAGE = "<html><body width='300px'>"
            + "An unexpected error has occured. ([local:maximize Show more..])";
    
    private static final int MINIMIZE_AFTER = 30*1000;
    
    private final LinkedList<String> errors = new LinkedList<>();
    private int errorCount;
    
    private final GridBagConstraints contentGbc;
    
    private final LinkLabel topMessage;
    private final LinkLabel message;
    private final LinkLabel minimizedMessage;
    private final JButton continueProgram = new JButton("Continue");
    private final JButton quitProgram = new JButton("Quit Program");
    private final JButton copyText = new JButton("Copy to clipboard");
    private final JTextArea debugMessage = new JTextArea();
    private final Frame parent;

    private final JPanel normal;
    private final JPanel minimized;
    
    private final Timer minimizeTimer;
    private final ElapsedTime openedET = new ElapsedTime();

    private int result;

    public ErrorMessage(Frame parent, LinkLabelListener linkLabelListener) {
        super(parent);
        this.parent = parent;
        setTitle("Error");
        setAlwaysOnTop(true);
        setAutoRequestFocus(false);
        setModal(true);
        debugMessage.setLineWrap(false);
        debugMessage.setEditable(false);
        message = new LinkLabel(MESSAGE, linkLabelListener);
        
        minimizeTimer = new Timer(5000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                update();
            }
        });
        
        LinkLabelListener localListener = new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                if (ref.equals("minimize")) {
                    minimize();
                } else if (ref.equals("maximize")) {
                    maximize();
                }
            }
        };
        topMessage = new LinkLabel(TOP_MESSAGE, localListener);
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                stopTimer();
            }
        });
        
        setLayout(new GridBagLayout());
        
        contentGbc = makeGbc(0, 0, 2, 1);
        contentGbc.fill = GridBagConstraints.BOTH;
        contentGbc.weightx = 1;
        contentGbc.weighty = 1;
        contentGbc.insets = new Insets(0, 0, 0, 0);
        
        
        minimized = new JPanel();
        minimizedMessage = new LinkLabel(MINIMIZED_MESSAGE, localListener);
        minimized.add(minimizedMessage);
        
        normal = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        ImageIcon icon = new ImageIcon(ErrorMessage.class.getResource("dialog-warning.png"));
        setIconImage(icon.getImage());
        
        gbc = makeGbc(0, 0, 2, 1);
        gbc.insets.bottom = 0;
        normal.add(topMessage, gbc);
        
        gbc = makeGbc(0, 1, 2, 1);
        normal.add(message, gbc);
        
        gbc = makeGbc(1, 2, 1, 1);
        copyText.setMargin(new Insets(0,10,-1,10));
        gbc.insets = new Insets(5, 0, 0, 10);
        gbc.anchor = GridBagConstraints.EAST;
        normal.add(copyText, gbc);
        
        gbc = makeGbc(0, 3, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        JScrollPane scroll = new JScrollPane(debugMessage);
        scroll.setPreferredSize(new Dimension(300,100));
        normal.add(scroll, gbc);
        
        gbc = makeGbc(0, 1, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.weightx = 0.5;
        add(continueProgram, gbc);
        
        gbc = makeGbc(1, 1, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 8, 10, 0);
        gbc.weightx = 0.5;
        add(quitProgram, gbc);
        
        ActionListener actionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == continueProgram) {
                    close(CONTINUE);
                } else if (e.getSource() == quitProgram) {
                    close(QUIT);
                } else if (e.getSource() == copyText) {
                    MiscUtil.copyToClipboard(debugMessage.getText());
                }
            }
        };
        
        continueProgram.addActionListener(actionListener);
        quitProgram.addActionListener(actionListener);
        copyText.addActionListener(actionListener);
        
        add(normal, contentGbc);
        finishDialog();
    }
    
    public int show(LogRecord error, LinkedList<LogRecord> previous, int openChans) {
        if (errorCount >= ERROR_LIMIT) {
            setTitle(errorCount+" Errors (stopped recording)");
        } else {
            addError(error, previous, openChans);
        }
        
        if (!isVisible()) {
            setFocusableWindowState(false);
            maximize();
            setLocationRelativeTo(parent);
        }
        
        result = CONTINUE;
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                setFocusableWindowState(true);
                debugMessage.scrollRectToVisible(new Rectangle());
            }
        });
        openedET.set();
        if (!isFocused()) {
            stopTimer();
            minimizeTimer.restart();
        }
        setVisible(true);
        return result;
    }
    
    private void addError(LogRecord error, LinkedList<LogRecord> previous,
            int openChans) {
        errorCount++;
        String errorText = makeErrorText(error, previous);
        errors.add(errorText);
        if (errorCount == 1) {
            setTitle("Error");
            debugMessage.setText(String.format(Locale.ROOT, "Error Report // %s / %s / %s / %s / Chans: %d\n\n",
                    DateTime.fullDateTime(),
                    Chatty.chattyVersion(),
                    Helper.systemInfo(),
                    LogUtil.getMemoryUsage(),
                    openChans));
        } else {
            setTitle(errorCount+" Errors");
        }
        debugMessage.append(errorText);
    }
    
    public static String makeErrorText(LogRecord error, LinkedList<LogRecord> previous) {
        StringBuilder b = new StringBuilder();
        for (LogRecord r : previous) {
            // Should never be null, but since this may contain null and it
            // apparently happened before..
            if (r != null) {
                b.append(Logging.formatRecordCompact(r));
            }
        }
        b.append(Logging.formatRecordCompact(error));
        b.append("\n\n");
        return b.toString();
    }
    
    private void finishDialog() {
        setMinimumSize(null);
        pack();
        setMinimumSize(getPreferredSize());
    }
    
    private void minimize() {
        remove(normal);
        add(minimized, contentGbc);
        finishDialog();
    }
    
    private void maximize() {
        remove(minimized);
        add(normal, contentGbc);
        finishDialog();
    }
    
    private void update() {
        if (isVisible()) {
            //System.out.println("update");
            long leftToMinimize = (MINIMIZE_AFTER - openedET.millisElapsed()) / 1000;
            if (leftToMinimize <= 0) {
                minimize();
                stopTimer();
            } else {
                topMessage.setText(TOP_MESSAGE+" ([local:minimize Minimize dialog] in "+leftToMinimize+"s)");
            }
        }
    }
    
    private void stopTimer() {
        if (minimizeTimer != null) {
            minimizeTimer.stop();
            topMessage.setText(TOP_MESSAGE+" ("+"[local:minimize Minimize dialog]"+")");
        }
    }
    
    private void close(int action) {
        result = action;
        errors.clear();
        errorCount = 0;
        setVisible(false);
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(10,10,10,10);
        return gbc;
    }
    
}
