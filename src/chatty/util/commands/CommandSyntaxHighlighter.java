
package chatty.util.commands;

import chatty.util.SyntaxHighlighter;
import chatty.util.colors.ColorCorrectionNew;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

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
//        CustomCommand command = CustomCommand.parse(input);
//        Map<Integer, String> parts = command.literals.getParts();
//        fillIndices(parts, input);
//        if (command.hasError()) {
//            int errorOffset = command.getErrorOffset() >= input.length() ? input.length() - 1 : command.getErrorOffset();
//            items.removeIf(item -> item.start >= errorOffset || item.end >= errorOffset);
//            add(errorOffset, errorOffset + 1, Type.ERROR);
//        }
    }
    
    public List<chatty.util.commands.Item> fillIndices(Map<Integer, String> parts, String input) {
        List<chatty.util.commands.Item> result = new ArrayList<>();
        int lastStart = -1;
        int lastEnd = -1;
        for (Map.Entry<Integer, String> part : parts.entrySet()) {
            int start = part.getKey();
            int end = part.getKey() + part.getValue().length();
//            System.out.println(start+" "+end+" "+part.getValue());
            if (lastStart == -1) {
                if (start > 0) {
                    add(0, start);
                }
            }
            else {
                add(lastEnd, start);
            }
            lastStart = start;
            lastEnd = end;
        }
//        System.out.println(lastEnd);
        if (lastEnd != -1 && lastEnd < input.length()) {
            add(lastEnd, input.length());
        }
        if (lastEnd == -1) {
            add(0, input.length());
        }
        return result;
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
    
    private static JTextComponent create(Color foreground, Color background) {
        JTextArea text = new JTextArea();
        text.setForeground(foreground);
        text.setBackground(background);
        text.setLineWrap(true);
        text.setColumns(80);
        text.setRows(3);
        text.setFont(Font.decode("Monospaced"));
        SyntaxHighlighter.install(text, new CommandSyntaxHighlighter());
        text.setText("$(username) $1- $\"abc\" \\$(username) $replace($1-,$'abc',replace,reg)\n\n");
        return text;
    }

}
