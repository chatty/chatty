
package chatty.util.commands;

import chatty.util.SyntaxHighlighter;
import chatty.util.colors.ColorCorrectionNew;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.text.ParseException;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author tduva
 */
public class CommandSyntaxHighlighter extends SyntaxHighlighter {

    @Override
    public void update(String input) {
        clear();
        try {
            new Parser(input, "$", "\\").parse(this);
        }
        catch (ParseException ex) {
            int errorOffset = ex.getErrorOffset();
            if (errorOffset == input.length()) {
                errorOffset = input.length() - 1;
            }
            int errorOffset2 = errorOffset;
            items.removeIf(item -> item.start >= errorOffset2 || item.end >= errorOffset2);
            add(errorOffset, errorOffset + 1, Type.ERROR);
        }
    }
    
    public static void main(String[] args) {
        JFrame dialog = new JFrame();
        dialog.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
        dialog.setLocationRelativeTo(null);
        dialog.add(create(new Color(230, 230, 230), new Color(60, 60, 60)), BorderLayout.NORTH);
        dialog.add(create(new Color(60, 60, 60), new Color(245, 245, 245)), BorderLayout.SOUTH);
        dialog.pack();
        dialog.setVisible(true);
        
        System.out.println(ColorCorrectionNew.matchLightness(Color.BLUE, Color.BLACK, 0.8f));
    }
    
    private static JComponent create(Color foreground, Color background) {
        JTextArea text = new JTextArea();
        text.setForeground(foreground);
        text.setBackground(background);
        text.setLineWrap(true);
        text.setColumns(80);
        text.setRows(3);
        text.setFont(Font.decode("Monospaced"));
        SyntaxHighlighter.install(text, new CommandSyntaxHighlighter());
        text.setText("$(username) $1- $\"abc\" \\$(username) $replace($1-,$'abc',replace,reg)");
        return new JScrollPane(text);
    }

}
