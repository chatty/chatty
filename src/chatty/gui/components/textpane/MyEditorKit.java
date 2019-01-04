
package chatty.gui.components.textpane;

import chatty.util.Debugging;
import java.awt.Image;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
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

    private static final Logger LOGGER = Logger.getLogger(MyEditorKit.class.getName());
    
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
    
    public void debug() {
        factory.updateDebugInfo();
    }
 
    static class StyledViewFactory implements ViewFactory {
        
        private final boolean startAtBottom;
        
        /**
         * Store image views by image, so that the view and thus location can be
         * retrieved for repainting animated GIFs. The updates through the
         * ImageObserver don't have to come on the EDT, so synchronizing access
         * to this is probably necessary.
         */
        private final Map<Image, Map<Long, WeakReference<MyIconView>>> imageViews = new HashMap<>();
        
        StyledViewFactory(boolean startAtBottom) {
            this.startAtBottom = startAtBottom;
        }
        
        public void changeImage(Image oldImage, Image newImage) {
            synchronized(imageViews) {
                Map<Long, WeakReference<MyIconView>> data = imageViews.get(oldImage);
                if (data != null) {
                    imageViews.put(newImage, data);
                    imageViews.remove(oldImage);
                }
            }
        }
        
        public Collection<MyIconView> getByImage(Image image) {
            synchronized(imageViews) {
                Map<Long, WeakReference<MyIconView>> data = imageViews.get(image);
                if (data != null) {
                    List<MyIconView> result = new ArrayList<>();
                    Iterator<Entry<Long,WeakReference<MyIconView>>> it = data.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<Long, WeakReference<MyIconView>> entry = it.next();
                        MyIconView v = entry.getValue().get();
                        if (v == null) {
                            it.remove();
                            LOGGER.warning("Removed reference for image "+entry.getKey());
                        } else {
                            result.add(v);
                        }
                    }
                    return result;
                }
                return null;
            }
        }
        
        public void clearImages() {
            synchronized(imageViews) {
                imageViews.clear();
                updateDebugInfo();
            }
        }
        
        public void clearImage(long imageId) {
            synchronized(imageViews) {
                Iterator<Map<Long, WeakReference<MyIconView>>> it = imageViews.values().iterator();
                while (it.hasNext()) {
                    Map<Long, WeakReference<MyIconView>> d = it.next();
                    if (d.remove(imageId) != null) {
                        if (Debugging.isEnabled("gifd", "gifd2")) {
                            Debugging.println(String.format(
                                    "Removed image %d (remaining: %s)",
                                    imageId, d.toString()));
                        }
                    }
                    Iterator<WeakReference<MyIconView>> it2 = d.values().iterator();
                    while (it2.hasNext()) {
                        if (it2.next().get() == null) {
                            it2.remove();
                            LOGGER.warning("Removed reference for image");
                        }
                    }
                    if (d.isEmpty()) {
                        it.remove();
                    }
                }
                updateDebugInfo();
            }
        }
        
        private void addImageView(Image image, MyIconView view, long id) {
            synchronized(imageViews) {
                if (!imageViews.containsKey(image)) {
                    imageViews.put(image, new HashMap<>());
                }
                imageViews.get(image).put(id, new WeakReference<>(view));
            }
        }
        
        private void updateDebugInfo() {
            if (!Debugging.isEnabled("gifd0")) {
                return;
            }
            synchronized(imageViews) {
                int totalViews = 0;
                int hiddenViews = 0;
                int animated = 0;
                for (Map<Long, WeakReference<MyIconView>> e : imageViews.values()) {
                    for (WeakReference<MyIconView> ref : e.values()) {
                        MyIconView view = ref.get();
                        totalViews++;
                        if (view != null) {
                            if (!view.getShouldRepaint()) {
                                hiddenViews++;
                            }
                            if (view.getAttributes().containsAttribute(ChannelTextPane.Attribute.ANIMATED, true)) {
                                animated++;
                            }
                        }
                    }
                }
                Debugging.printlnTimed("imageViews"+this.hashCode(),
                        String.format("images: %d views: %d animated: %d hidden: %d",
                        imageViews.size(), totalViews, animated, hiddenViews));
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
                        updateDebugInfo();
                    }
                    return view;
                }
            }
            return new LabelView(elem);
        }

    }
    
}
