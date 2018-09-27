
package chatty.gui.components.textpane;

import chatty.Chatty;
import chatty.util.Debugging;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * Replaces some Views by custom ones to change display behaviour.
 * 
 * @author tduva
 */
class MyEditorKit extends StyledEditorKit {

    private final StyledViewFactory factory;
    
    public MyEditorKit(boolean startAtBottom) {
        this.factory = new StyledViewFactory(startAtBottom);
    }
    
    @Override
    public ViewFactory getViewFactory() {
        return factory;
    }
    
    public Collection<MyIconView> getByImage(Image image) {
        return factory.getByImage(image);
    }
    
    public void clearImages() {
        factory.clearImages();
    }
    
    public void clearImage(long imageId) {
        Debugging.edt();
        factory.clearImage(imageId);
    }
    
    public void changeImage(Image oldImage, Image newImage) {
        factory.changeImage(oldImage, newImage);
    }
 
    static class StyledViewFactory implements ViewFactory {

        private final boolean startAtBottom;
        
        /**
         * Store image views by image, so that the view and thus location can be
         * retrieved for repainting animated GIFs. The updates through the
         * ImageObserver don't have to come on the EDT, so synchronizing access
         * to this is probably necessary.
         */
        private final Map<Image, Map<Long, MyIconView>> imageViews = new HashMap<>();
        
        StyledViewFactory(boolean startAtBottom) {
            this.startAtBottom = startAtBottom;
        }
        
        public void changeImage(Image oldImage, Image newImage) {
            synchronized(imageViews) {
                Map<Long, MyIconView> data = imageViews.get(oldImage);
                if (data != null) {
    //                Chatty.println("changeImage to "+newImage);
                    imageViews.put(newImage, data);
                    imageViews.remove(oldImage);
                }
            }
        }
        
//        public Set<StoredImageView> getByImage(Image image) {
//            synchronized(imageViews) {
//                return imageViews.get(image);
//            }
//        }
        
        public Collection<MyIconView> getByImage(Image image) {
            synchronized(imageViews) {
                Map<Long, MyIconView> data = imageViews.get(image);
                if (data != null) {
                    return new ArrayList<>(data.values());
                }
                return null;
            }
        }
        
        public void clearImages() {
            synchronized(imageViews) {
                imageViews.clear();
            }
        }
        
        public void clearImage(long imageId) {
            synchronized(imageViews) {
//                Iterator<Set<StoredImageView>> imagesIt = imageViews.values().iterator();
//                while (imagesIt.hasNext()) {
//                    Set<StoredImageView> d = imagesIt.next();
//                    Iterator<StoredImageView> it = d.iterator();
//                    while (it.hasNext()) {
//                        if (it.next().id == imageId) {
//                            it.remove();
//                            if (Debugging.isEnabled("gifd")) {
//                                Debugging.println(String.format(
//                                        "Removed image %d (remaining: %s)",
//                                        imageId, d.toString()));
//                            }
//                            
//                        }
//                    }
//                    if (d.isEmpty()) {
//                        imagesIt.remove();
//                    }
//                }
                Iterator<Map<Long, MyIconView>> it = imageViews.values().iterator();
                while (it.hasNext()) {
                    Map<Long, MyIconView> d = it.next();
                    if (d.remove(imageId) != null) {
                        if (Debugging.isEnabled("gifd", "gifd2")) {
                            Debugging.println(String.format(
                                    "Removed image %d (remaining: %s)",
                                    imageId, d.toString()));
                        }
                        if (d.isEmpty()) {
                            it.remove();
                        }
                    }
                }
            }
        }
        
        private void addImageView(Image image, MyIconView view, long id) {
            synchronized(imageViews) {
                if (!imageViews.containsKey(image)) {
                    imageViews.put(image, new HashMap<>());
                }
//                StoredImageView t = new StoredImageView(view, id);
//                imageViews.get(image).remove(t);
//                imageViews.get(image).add(t);
                imageViews.get(image).put(id, view);
            }
        }

        @Override
        public View create(Element elem) {
            Debugging.edt();
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new WrapLabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new MyParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new ChatBoxView(elem, View.Y_AXIS, startAtBottom);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    Long id = (Long)elem.getAttributes().getAttribute(ChannelTextPane.Attribute.IMAGE_ID);
                    MyIconView view = new MyIconView(elem);
                    if (id != null) {
                        ImageIcon icon = (ImageIcon) StyleConstants.getIcon(elem.getAttributes());
                        Image image = icon.getImage();
                        addImageView(image, view, id);
                        if (Debugging.isEnabled("gifd", "gifd2")) {
                            Debugging.println(String.format(
                                    "Added image %d (now %d)",
                                    id, imageViews.size()));
                        }
                    }
                    return view;
                }
            }
            return new LabelView(elem);
        }

    }
    
}
