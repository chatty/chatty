
package chatty.util.dnd;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tduva
 */
public class Test {
    
    private static int counter = 0;
    private static DockManager m;
    private static DockLayout savedLayout;
    private static String savedLayoutJson;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            m = new DockManager(new DockListener() {
                @Override
                public void activeContentChanged(DockPopout window, DockContent content, boolean focusChange) {
                    if (window == null) {
                        frame.setTitle(content.getTitle());
                    }
                    else {
                        window.setTitle(content.getTitle());
                    }
                }

                @Override
                public void popoutClosed(DockPopout window, List<DockContent> contents) {
                    
                }

                @Override
                public void popoutOpened(DockPopout popout, DockContent content) {
                    
                }

                @Override
                public void contentAdded(DockContent content) {
                    
                }

                @Override
                public void contentRemoved(DockContent content) {
                    
                }

                @Override
                public void popoutClosing(DockPopout popout) {
                    m.closePopout(popout);
                }
            });
//            m.setSetting(DockSetting.Type.TAB_PLACEMENT, "left");
            m.setSetting(DockSetting.Type.DIVIDER_SIZE, 7);
            m.setSetting(DockSetting.Type.POPOUT_PARENT, frame);
            frame.add(m.getBase(), BorderLayout.CENTER);
            
            JMenuBar menubar = new JMenuBar();
            JMenu menu = new JMenu("Menu");
            Action action = new AbstractAction("Test") {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    add();
                }
            };
            menu.add(new JMenuItem(action));
            Action action2 = new AbstractAction("Test") {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    m.setSetting(DockSetting.Type.POPOUT_TYPE, DockSetting.PopoutType.FRAME);
                }
            };
            menu.add(new JMenuItem(action2));
            
            menu.add(new JMenuItem(new AbstractAction("Save Layout") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    savedLayout = m.getLayout();
                    savedLayoutJson = JSONArray.toJSONString(m.getLayout().toList());
                    for (DockContent c : m.getContents()) {
                        System.out.println(c.getId()+" "+savedLayout.getPath(c.getId()));
                    }
                    System.out.println(savedLayout);
                    System.out.println(savedLayout.toList());
                    System.out.println(DockLayout.fromList(savedLayout.toList()));
                }
            }));
            menu.add(new JMenuItem(new AbstractAction("Load Layout") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        DockLayout layout = savedLayout;
                        
                        // Load from JSON test
                        JSONParser parser = new JSONParser();
                        JSONArray list = (JSONArray) parser.parse(savedLayoutJson);
                        System.out.println(savedLayoutJson);
                        layout = DockLayout.fromList(list);
                        
                        List<DockContent> currentContents = m.getContents();
                        m.loadLayout(layout);
                        for (DockContent c : currentContents) {
                            c.setTargetPath(layout.getPath(c.getId()));
                            m.addContent(c);
                        }
                    }
                    catch (ParseException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }));
            menu.add(new JCheckBoxMenuItem(new AbstractAction("Keep Empty") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
                    System.out.println(item.isSelected());
                    m.setSetting(DockSetting.Type.KEEP_EMPTY, item.isSelected());
                }
            }));
            
            menubar.add(menu);
            frame.setJMenuBar(menubar);
            
            frame.setSize(1200, 500);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
            for (int i=0;i<6;i++) {
                add();
            }
            DockContent c = add();
            m.setActiveContent(c);
        });
    }
    
    private static DockContent add() {
        counter++;
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JTextPane text = new JTextPane();
        panel.add(text, BorderLayout.CENTER);
        text.setText("Test Text " + counter);
        DockContentContainer content = new DockContentContainer("Test " + counter, new JScrollPane(panel), m);
        content.setId("c"+counter);
        text.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                content.setTitle(text.getText());
                if (text.getText().equals("test")) {
                    m.addContent(content);
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                
            }
        });
        m.addContent(content);
        return content;
    }
    
}
