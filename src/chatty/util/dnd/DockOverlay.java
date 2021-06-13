
package chatty.util.dnd;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;

/**
 * Each base has it's own overlay, which handles updating various things when a
 * drop occurs as well as drawing information about the current drop location.
 * 
 * @author tduva
 */
public class DockOverlay extends JPanel {
    
    private final DockBase base;
    
    /**
     * The current drop rectangle to paint, may be null.
     */
    private Rectangle paintRect;
    
    /**
     * The current drop info, updated continuously when a drag occurs. May be
     * null.
     */
    private DockDropInfo dropInfo;
    
    /**
     * The current import info, updated continuously when a drag occurs.
     */
    private DockImportInfo importInfo;
    
    private Color fillColor = new Color(64, 64, 64, 64);
    private Color lineColor = Color.DARK_GRAY;
    
    public DockOverlay(DockBase base) {
        this.base = base;
        
        // Stop drawing rectangle when mouse moves outside of area
        Timer timer = new Timer(100, e -> {
            if (paintRect != null && getMousePosition() == null) {
                paintRect = null;
                repaint();
            }
        });
        timer.start();
        
        /**
         * Handles all imports, but asks the base and thus child components
         * what kind of drop should occur. Handling all imports in a central
         * place allows easy updating and drawing on this overlay.
         */
        setTransferHandler(new TransferHandler() {

            @Override
            public boolean canImport(TransferHandler.TransferSupport info) {
                return updateImport(info);
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport info) {
                // Seems redundant since canImport() already checks this
//                DockTransferable dtf = getTransferable(info);
//                if (dtf == null) {
//                    return false;
//                }
                if (canImport(info)) {
                    // At this point, dropInfo should never be null
                    DockTransferable dtf = DockUtil.getTransferable(info);
                    paintRect = null;
                    repaint();
                    base.requestStopDrag(dtf);
//                    System.out.println("IMPORT!" + dtf.content);
                    try {
                        dropInfo.dropComponent.drop(new DockTransferInfo(dropInfo, dtf));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return true;
                }
                return false;
            }

        });
    }
    
    public void setFillColor(Color color) {
        if (color != null) {
            this.fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 90);
        }
    }
    
    public void setLineColor(Color color) {
        if (color != null) {
            this.lineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 180);
        }
    }
    
    /**
     * Updates the drop info variables based on the current info provided by the
     * drag&drop system, by asking components whether a drop can occur.
     * 
     * @param info
     * @return 
     */
    private boolean updateImport(TransferHandler.TransferSupport info) {
//        System.out.println("UPDATE_IMPORT "+info);
        DockTransferable dtf = DockUtil.getTransferable(info);
        if (dtf == null) {
            return false;
        }
//        System.out.println("!!!!!!!!!!!!!!!!!"+dtf.content);

        // Ask other components which drop can occur at the current location
        DockImportInfo importInfoUpdated = new DockImportInfo(info, dtf);
        DockDropInfo dropInfoUpdated = base.findDrop(importInfoUpdated);
        
        // Update some stuff based on the info received
        if (dropInfoUpdated != null) {
            Rectangle rect = dropInfoUpdated.rect;
            paintRect = SwingUtilities.convertRectangle(dropInfoUpdated.dropComponent.getComponent(), rect, DockOverlay.this);
        }
        else {
            paintRect = null;
        }
        this.dropInfo = dropInfoUpdated;
        this.importInfo = importInfoUpdated;
        repaint();
        return dropInfoUpdated != null;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        if (paintRect != null) {
            g.setColor(fillColor);
            g.fillRect(paintRect.x, paintRect.y, paintRect.width, paintRect.height);
            g.setColor(lineColor);
            g.drawRect(paintRect.x, paintRect.y, paintRect.width, paintRect.height);
        }
        if (importInfo != null) {
            Graphics2D g2d = (Graphics2D)g;
//            String title = "[Moving "+importInfo.tf.content.getTitle()+"]";
//            if (dropInfo != null) {
//                if (dropInfo.location == Location.TAB) {
//                    title += " Add as Tab";
//                }
//                else {
//                    title += " Add into new split";
//                }
//            }
//            if (paintRect == null) {
//                title += " Abort";
//            }
//            
//            int width = g.getFontMetrics().stringWidth(title);
//            int height = g.getFontMetrics().getHeight();
//            int x = getWidth() / 2 - width / 2;
//            int y = getHeight() / 2;
//            g.setColor(FILL_COLOR);
//            g.fillRect(x - 2, y - 2, width + 4, height + 4);
//            g.setColor(Color.BLUE);
//            g.drawRect(x - 2, y - 2, width + 4, height + 4);
//            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g.drawString(title, x, y + (int)(height / 1.5));
            
            if (importInfo.tf.image != null && getMousePosition() != null) {
                Point p = importInfo.info.getDropLocation().getDropPoint();
//                int imgWidth = importInfo.tf.image.getWidth(null);
                int imgHeight = importInfo.tf.image.getHeight(null);
                Composite origComp = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g.drawImage(importInfo.tf.image, p.x + 14, p.y - imgHeight / 3, this);
                g2d.setComposite(origComp);
            }
        }
    }
    
}
