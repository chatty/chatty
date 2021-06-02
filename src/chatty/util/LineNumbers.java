
package chatty.util;

import chatty.gui.GuiUtil;
import chatty.util.colors.ColorCorrectionNew;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

/**
 * Add line numbers to a JTextComponent:
 *
 * <pre>
 * JScrollPane scroll = new JScrollPane(textComponent);
 * scroll.setRowHeaderView(new LineNumbers(textComponent));
 * </pre>
 * 
 * Only tested with JTextArea.
 * 
 * Partly based on: https://tips4java.wordpress.com/2009/05/23/text-component-line-number/
 * 
 * (The About page states: "We assume no responsibility for the code. You are
 * free to use and/or modify and/or distribute any or all code posted on the
 * Java Tips Weblog without restriction. A credit in the code comments would be
 * nice, but not in any way mandatory.")
 *
 * @author tduva
 */
public class LineNumbers extends JPanel {

    private static final int MIN_DIGITS = 2;
    private static final int HEIGHT = 20000000;
    private static final int PADDING_LEFT = 4;
    private static final int PADDING_RIGHT = 5;
    
    private final JTextComponent comp;
    
    public LineNumbers(JTextComponent comp) {
        this.comp = comp;
        
        comp.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                documentUpdated();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                documentUpdated();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                documentUpdated();
            }
        });
        comp.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if (evt.getPropertyName() == null) {
                return;
            }
            switch (evt.getPropertyName()) {
                case "font":
                    setFont(comp.getFont());
                    updateSize();
                    break;
                case "background":
                case "foregorund":
                    updateColors();
                    break;
            }
        });
        setFont(comp.getFont());
        updateColors();
        setBorder(BorderFactory.createEmptyBorder(0, PADDING_LEFT, 0, PADDING_RIGHT));
        updateSize();
    }

    /**
     * This may be called when an overall repaint is necessary or e.g. when the
     * JScrollPane is scrolled. Most stuff in the associated text component
     * (e.g. selecting text, flashing cursor) won't trigger a repaint, since
     * this is in a separate component.
     *
     * @param g 
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Document doc = comp.getDocument();
        
        Insets insets = getInsets();
        int availableWidth = getWidth() - insets.left - insets.right;
        int descent = g.getFontMetrics().getDescent();
        
        // Clip
        Rectangle clip = g.getClipBounds();
        int startOffset = comp.viewToModel(new Point(0, clip.y));
        int endOffset = comp.viewToModel(new Point(clip.x+clip.width, clip.y + clip.height));
        int startLine = doc.getDefaultRootElement().getElementIndex(startOffset);
        
        // Go through all lines that need to be painted
        for (int i = startLine; i < doc.getDefaultRootElement().getElementCount(); i++) {
            Element row = doc.getDefaultRootElement().getElement(i);
            if (row.getStartOffset() > endOffset) {
                break;
            }
            try {
                Rectangle r = comp.modelToView(row.getStartOffset());
                String number = String.valueOf(i + 1);
                int numberWidth = g.getFontMetrics().stringWidth(number);
                int pos = availableWidth - numberWidth + insets.left;
                g.drawString(number, pos, r.y + r.height - descent);
            }
            catch (BadLocationException ex) {
                // Just don't draw
            }
        }
    }
    
    private int prevHeight;
    
    private void documentUpdated() {
        /**
         * Original comment: "View of the component has not been updated at the
         * time the DocumentEvent is fired"
         */
        SwingUtilities.invokeLater(() -> {
            try {
                int endPos = comp.getDocument().getLength();
                Rectangle rect = comp.modelToView(endPos);

                if (rect != null && rect.y != prevHeight) {
                    updateSize();
                    getParent().repaint();
                    prevHeight = rect.y;
                }
            }
            catch (BadLocationException ex) {
                // Do nothing
            }
        });
    }
    
    private void updateColors() {
        // Slightly change the text component's foreground color for the numbers
        setForeground(ColorCorrectionNew.offset(comp.getForeground(), 0.65f));
        setBackground(comp.getBackground());
    }
    
    private int prevDigitCount;
    private Font prevFont;
    
    private void updateSize() {
        Element root = comp.getDocument().getDefaultRootElement();
        int lines = root.getElementCount();
        int digitCount = Math.max(String.valueOf(lines).length(), MIN_DIGITS);
        Font font = getFont();
        
        if (prevDigitCount != digitCount || prevFont != font) {
            prevDigitCount = digitCount;
            prevFont = font;
            
            FontMetrics fontMetrics = getFontMetrics(font);
            int width = fontMetrics.charWidth('0') * digitCount;
            Insets insets = getInsets();
            int preferredWidth = insets.left + insets.right + width;

            Dimension d = getPreferredSize();
            d.setSize(preferredWidth, HEIGHT);
            setPreferredSize(d);
            setSize(d);
        }
    }
    
    /**
     * For testing
     * 
     * @param args 
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setLocationRelativeTo(null);
            
            JTextArea text = new JTextArea();
            frame.setLayout(new GridBagLayout());
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setMargin(new Insets(2, 2, 2, 2));
            text.setFont(Font.decode(Font.MONOSPACED));
            StringBuilder b = new StringBuilder();
            for (int i=0; i<100; i++) {
                b.append("test text"+i);
                if (ThreadLocalRandom.current().nextInt(4) == 0) {
                    b.append("\n");
                }
            }
            text.setText(b.toString());
            text.setMargin(new Insets(1, 3, 1, 3));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setRowHeaderView(new LineNumbers(text));
//            text.setBackground(Color.BLACK);
//            text.setForeground(Color.WHITE);
//            text.setFont(Font.decode("Verdana 14"));
            GridBagConstraints gbc;
            gbc = GuiUtil.makeGbc(0, 1, 3, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(5, 7, 5, 7);
            frame.add(scroll, gbc);
            
            frame.setSize(400, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
    
}
