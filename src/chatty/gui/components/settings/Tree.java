
package chatty.gui.components.settings;

import chatty.gui.components.settings.SettingsDialog.Page;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

/**
 * Helper class for the JTree that is used to switch between setting pages.
 * 
 * @author tduva
 */
public class Tree {
    
    public static JTree createTree(Map<Page, List<Page>> nodes) {
        
        // Create nodes structure based on Map
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        for (Page parent : nodes.keySet()) {
            DefaultMutableTreeNode category = new DefaultMutableTreeNode(parent);
            root.add(category);
            List<Page> subNodes = nodes.get(parent);
            for (Page child : subNodes) {
                category.add(new DefaultMutableTreeNode(child));
            }
        }
        
        // Create and configure tree
        JTree tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);
        
        // Disable icons and use default renderer
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            
            // Border to give text some more space
            private final Border BORDER = BorderFactory.createEmptyBorder(0, 3, 0, 1);
            
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {
                Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    label.setBorder(BORDER);
                }
                return c;
            }
        };
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        tree.setCellRenderer(renderer);
        
        // Expand all branches
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        
        // Select closest node on click, to allow more leniency with selecting
        tree.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                int row = tree.getClosestRowForLocation(e.getX(), e.getY());
                if (row != -1) {
                    tree.setSelectionRow(row);
                }
            }
        });
        
        // Prevent collapsing of nodes completely
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {

            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                throw new ExpandVetoException(event);
            }
        });
        
        return tree;
    }
    
    /**
     * Select node based on the associated user object.
     * 
     * @param tree
     * @param object 
     */
    public static void setSelected(JTree tree, Object object) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject() != null
                    && node.getUserObject().equals(object)) {
                tree.setSelectionPath(new TreePath(node.getPath()));
            }
        }
    }
    
}
