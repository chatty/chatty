
package chatty.gui.components;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.menus.TextSelectionMenu;
import chatty.lang.Language;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author tduva
 */
public class JoinDialog extends JDialog {
    
    private final JTextField channels = new JTextField(20);
    
    private final JButton joinButton = new JButton(Language.getString("join.button.join"));
    private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
    
    private final JButton favoritesButton = new JButton(Language.getString("join.button.favoritesHistory"));
    
    private boolean join = false;
    
    public JoinDialog(final MainGui owner) {
        super(owner);
        setTitle(Language.getString("join.title"));
        setResizable(false);
        setModal(true);
        
        setLayout(new GridBagLayout());
        
        channels.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        });
        GuiUtil.installLengthLimitDocumentFilter(channels, 8000, false);
        TextSelectionMenu.install(channels);
        
        GridBagConstraints gbc;
        
        gbc = makeGbc(0,0,1,1);
        JLabel label = new JLabel(Language.getString("join.channel"));
        label.setLabelFor(channels);
        add(label, gbc);
        
        gbc = makeGbc(1,0,2,1);
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        add(channels, gbc);
        
        gbc = makeGbc(1,1,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2,5,5,5);
        favoritesButton.setPreferredSize(new Dimension(100,20));
        favoritesButton.setMnemonic(KeyEvent.VK_F);
        add(favoritesButton, gbc);
        
        gbc = makeGbc(1,2,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10,5,5,5);
        gbc.weightx = 0.8;
        add(joinButton, gbc);
        
        gbc = makeGbc(2,2,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10,5,5,5);
        add(cancelButton, gbc);
        
        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == joinButton || e.getSource() == channels) {
                    join = true;
                    setVisible(false);
                } else if (e.getSource() == cancelButton) {
                    setVisible(false);
                } else if (e.getSource() == favoritesButton) {
                    Set<String> selectedFavorites = owner.chooseFavorites(JoinDialog.this, "");
                    channels.setText(Helper.buildStreamsString(selectedFavorites));
                }
            }
            
        };
        
        joinButton.addActionListener(listener);
        cancelButton.addActionListener(listener);
        favoritesButton.addActionListener(listener);
        channels.addActionListener(listener);
        
        pack();
    }
    
    public Set<String> showDialog() {
        channels.setText("");
        join = false;
        changed();
        channels.requestFocusInWindow();
        setVisible(true);
        if (!join) {
            channels.setText("");
        }
        return getChannels();
    }
    
    private Set<String> getChannels() {
        String chans = channels.getText();
        return Helper.parseChannelsFromString(chans, false);
    }
    
    private void changed() {
        Set<String> chans = getChannels();
        boolean empty = chans.isEmpty();
        joinButton.setEnabled(!empty);
        joinButton.setText(Language.getString("join.button.join", chans.size()));
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(3,3,3,3);
        return gbc;
    }
    
}
