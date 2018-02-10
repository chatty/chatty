
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.lang.Language;
import chatty.util.StringUtil;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 *
 * @author tduva
 */
public class FontChooser extends JDialog implements InputListListener,
        ListSelectionListener, ActionListener {

    // Lists
    private final InputList fontList;
    private final InputList fontSizeList;
    
    private static final String PREVIEW_TEXT = 
              "<html><body style=\"margin: 4px;\">"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ<br />"
            + "abcdefghijklmnopqrstuvwxyz<br />"
            + "1234567890<br />"
            + "ÄÖÜäöüôàúåãë.,-\\/#+-µ[:]{}$";
    
    // Preview
    private final FontPreviewLabel preview = new FontPreviewLabel(PREVIEW_TEXT);
    
    private final JTextField test = new JTextField();

    // Buttons
    private final JButton ok = new JButton(Language.getString("dialog.button.ok"));
    private final JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
    
    // Close Action
    private int closeAction;
    
    public static final int ACTION_OK = 1;
    public static final int ACTION_CANCEL = 0;

    // Font
    private Font font;
    private static final int FALLBACK_FONT_SIZE = 14;
    private String[] fontSizes = new String[]{
        "8", "9", "10", "11", "12", "13", "14", "15", "16", "18", "20", "22",
        "24", "26", "28", "30", "32", "36", "48", "60", "72"
    };
    
    // Reference
    private final Dialog owner;
    
    /**
     * Creates a FontChooser with the given Dialog as owner.
     * 
     * @param owner 
     */
    public FontChooser(Dialog owner) {
        super(owner, Language.getString("settings.chooseFont.title"), true);
        this.owner = owner;

        setLayout(new GridBagLayout());
        setMinimumSize(new Dimension(300,500));
        
        ok.addActionListener(this);
        cancel.addActionListener(this);
        
        /*
         * Font selection (Panel)
         */
        JPanel fontSelection = new JPanel(new GridLayout(1,2));
        
        // Create and fill font list
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getAvailableFontFamilyNames();

        fontList = new InputList(fonts);   
        
        // Create and fill font size list
        fontSizeList = new InputList(fontSizes, new IntegerVerifier());
        
        fontSelection.add(fontList);
        fontSelection.add(fontSizeList);
        fontSelection.setBorder(BorderFactory.createTitledBorder(Language.getString("settings.chooseFont.selectFont")));
        
        // List listeners
        fontList.addInputListListener(this);
        fontSizeList.addInputListListener(this);
        
        /*
         * Font preview (Panel)
         */
        JPanel fontPreview = new JPanel(new BorderLayout());
        fontPreview.add(preview);
        fontPreview.setBorder(BorderFactory.createTitledBorder(Language.getString("settings.chooseFont.preview")));
        
        /*
         * Font test (Panel)
         */
        JPanel fontTest = new JPanel(new BorderLayout());
        fontTest.add(test);
        test.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreviewText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreviewText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePreviewText();
            }
        });
        fontTest.setBorder(BorderFactory.createTitledBorder(Language.getString("settings.chooseFont.enterText")));
        
        /*
         * Add everything to the dialog
         */
        // Panels
        GridBagConstraints gbc;
        gbc = makeGbc(0,0,2,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0.7;
        add(fontSelection,gbc);
        
        gbc = makeGbc(0,1,2,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0.3;
        add(fontPreview,gbc);
        
        gbc = makeGbc(0,2,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        add(fontTest,gbc);
        
        // Buttons
        gbc = makeGbc(0,3,1,1);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.5;
        add(ok,gbc);
        
        gbc = makeGbc(1,3,1,1);
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.WEST;
        add(cancel,gbc);
        
        pack();
    }
    
    public String getFontName() {
        return font.getName();
    }
    
    public Integer getFontSize() {
        return font.getSize();
    }
    
    /**
     * Open the dialog with the given initial settings.
     * 
     * @param defaultFont
     * @param defaultFontSize
     * @return 
     */
    public int showDialog(String defaultFont, int defaultFontSize) {
        fontList.setValue(defaultFont);
        fontSizeList.setValue(new Integer(defaultFontSize).toString());
        closeAction = ACTION_CANCEL;
        setLocationRelativeTo(owner);
        setVisible(true);
        return closeAction;
    }
    
    /**
     * Creates a new Font based on the current settings and also updates the
     * preview.
     * 
     * The created Font is not only used for the preview, but also to return
     * it when requested from the caller.
     */
    private void updateFont() {
        String fontName = fontList.getValue();
        Integer fontSize = FALLBACK_FONT_SIZE;
        try {
            fontSize = Integer.parseInt(fontSizeList.getValue());
        } catch (NumberFormatException ex) {
            // Just use the fallback font size
        }
        font = new Font(fontName,Font.PLAIN,fontSize);
        preview.setFont(font);
    }
    
    private void updatePreviewText() {
        preview.setText(PREVIEW_TEXT+"<br />"+test.getText());
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        updateFont();
    }

    @Override
    public void valueChanged(String value) {
        updateFont();
    }

    /**
     * When a Button was pressed, close Dialog, change closeAction when the
     * Ok-Button was pressed.
     * 
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == ok) {
            closeAction = ACTION_OK;
        }
        setVisible(false);
    }
    
    static class FontPreviewLabel extends JLabel {
        
        FontPreviewLabel(String previewText) {
            setPreferredSize(new Dimension(300,100));
            //setBackground(Color.WHITE);
            //setForeground(Color.black);
            //setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            //setOpaque(true);
            setText(previewText);
        }
        
    }
    
    /**
     * A JList combined with a JTextField.
     */
    static class InputList extends JPanel implements ListSelectionListener,
            ActionListener, DocumentListener {
        
        private JList<String> list;
        private JTextField input;
        private InputListListener listener;
        
        InputList(String[] data) {
            this(data, null);
        }
        
        InputList(String[] data, InputVerifier inputVerifier) {

            // List
            list = new JList<>(data);
            list.addListSelectionListener(this);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane listScroll = new JScrollPane(list);

            // Text input
            input = new JTextField();
            input.setInputVerifier(inputVerifier);
            input.addActionListener(this);
            input.getDocument().addDocumentListener(this);

            setLayout(new BorderLayout());
            
            /*
             * Add everything to the panel
             */
            add(input, BorderLayout.NORTH);
            add(listScroll);
        }
        
        public void addInputListListener(InputListListener listener) {
            this.listener = listener;
        }


        
        public String getValue() {
            return input.getText();
        }
        
        public void setValue(String value) {
            list.clearSelection();
            list.setSelectedValue(value, true);
            input.setText(value);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            valueChanged(input.getText());
        }
        
        private void valueChanged(String newValue) {
            if (listener != null) {
                listener.valueChanged(newValue);
            }
        }
        
        /**
         * Sets the text of the input field based on the current selected value
         * of the list.
         */
        private void updateTextInput() {
            String newValue = list.getSelectedValue();
            if (newValue != null && !newValue.isEmpty()) {
                input.setText(newValue);
                valueChanged(newValue);
            }
        }
        
        /**
         * Given on the current text input, scroll to the appropriate entry or
         * select it if it's an exact match.
         */
        private void updateListSelection() {
            String inputValue = StringUtil.toLowerCase(input.getText());
            if (inputValue.isEmpty()) {
                return;
            }
            int size = list.getModel().getSize();
            for (int i=0;i < size;i++) {
                String value = list.getModel().getElementAt(i);
                
                if (value.equalsIgnoreCase(inputValue)) {
                    list.setSelectedValue(value, true);
                    valueChanged(value);
                    break;
                }
                if (StringUtil.toLowerCase(value).startsWith(inputValue)) {
                    scrollToValue(value);
                    break;
                }
            }
        }

        private void scrollToValue(String value) {
            list.setSelectedValue(value, true);
            list.clearSelection();
        }
        
        /*
         * Text input listeners
         */
        @Override
        public void insertUpdate(DocumentEvent e) {
            updateListSelection();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateListSelection();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
        }
        
        /*
         * List selection changed
         */
        @Override
        public void valueChanged(ListSelectionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    updateTextInput();
                }
            });
        }
        
    }
    
    static class IntegerVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent input) {
            JTextField component = (JTextField)input;
            try {
                Integer.parseInt(component.getText());
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        
    }
    
}

interface InputListListener {

    void valueChanged(String value);
}