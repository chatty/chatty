
package chatty.gui.components;

import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.lang.Language;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.Timer;

/**
 * Dialog to search text in the chat.
 * 
 * @author tduva
 */
public class SearchDialog extends JDialog {
    
    private static final Color COLOR_NORMAL = new JLabel().getBackground();
    private static final Color COLOR_NO_RESULT = new Color(255,165,80);
    
    private static final int NO_RESULT_COLOR_TIME = 300;
    
    private final Timer timer;
    private final JTextField searchText = new JTextField(20);
    private final JButton searchButton = new JButton(Language.getString("searchDialog.button.search"));
    //private final JCheckBox highlightAll = new JCheckBox("Highlight all occurences");
    
    private Channel chan;
    
    private static final Map<Window, SearchDialog> created = new HashMap<>();
    
    public static void showSearchDialog(Channel channel, MainGui g, Window owner) {
        SearchDialog dialog = created.get(owner);
        if (dialog == null) {
            dialog = new SearchDialog(g, owner);
            dialog.setLocationRelativeTo(owner);
            GuiUtil.installEscapeCloseOperation(dialog);
            created.put(owner, dialog);
        }
        dialog.setChannel(channel);
        dialog.setVisible(true);
        // I don't think this really changes anything, but just in case
        dialog.pack();
    }
    
    private SearchDialog(final MainGui g, final Window owner) {
        super(owner);
        setTitle(Language.getString("searchDialog.title"));
        setLayout(new GridBagLayout());
 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        
        add(searchText, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        GuiUtil.smallButtonInsets(searchButton);
        searchButton.setFocusable(false);
        add(searchButton, gbc);

        timer = new Timer(NO_RESULT_COLOR_TIME, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                searchText.setBackground(COLOR_NORMAL);
            }
        });
        timer.setRepeats(false);
        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!g.search(chan, searchText.getText())) {
                    searchText.setBackground(COLOR_NO_RESULT);
                    timer.restart();
                }
            }
        };
        searchText.addActionListener(listener);
        searchButton.addActionListener(listener);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                g.resetSearch(chan);
                searchText.setText(null);
                searchText.setBackground(COLOR_NORMAL);
            }
        });
        
        pack();
    }
    
    public void setChannel(Channel chan) {
        this.chan = chan;
    }
    
}
