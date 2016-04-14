
package chatty.gui.components.textpane;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;

/**
 * Adds a way to refresh the (whole) document.
 * 
 * This is currently used to display Icons after they are fully loaded, although
 * there should be a better way to do this.
 * 
 * @author tduva
 */
class MyDocument extends DefaultStyledDocument {
    
    public void refresh() {
        refresh(0, getLength());
    }
    
    public void refresh(int offset, int len) {
        DefaultDocumentEvent changes = new AbstractDocument.DefaultDocumentEvent(offset,len, DocumentEvent.EventType.CHANGE);
        Element root = getDefaultRootElement();
        Element[] removed = new Element[0];
        Element[] added = new Element[0];
        changes.addEdit(new ElementEdit(root, 0, removed, added));
        changes.end();
        fireChangedUpdate(changes);
    }

}