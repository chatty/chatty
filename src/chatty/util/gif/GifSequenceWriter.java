
package chatty.util.gif;

// 
//  GifSequenceWriter.java
//  
//  Created by Elliot Kroo on 2009-04-25.
//
// This work is licensed under the Creative Commons Attribution 3.0 Unported
// License. To view a copy of this license, visit
// http://creativecommons.org/licenses/by/3.0/ or send a letter to Creative
// Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.

/**
 * Changes made by tduva:
 * 
 * ----------
 * Changed:
 * graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
 * 
 * To:
 * graphicsControlExtensionNode.setAttribute("disposalMethod", "restoreToBackgroundColor");
 * 
 * as suggested in http://stackoverflow.com/questions/777947/creating-animated-gif-with-imageio/789723#comment26719121_789723
 * ----------
 * Modified to add per-frame delay.
 * ----------
 * Further modified to choose disposal method based on whether the image has
 * transparent pixels. Some other refactoring.
 */

import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import java.awt.image.*;
import java.io.*;
import java.util.Iterator;

public class GifSequenceWriter {
  protected ImageWriter gifWriter;
  protected ImageWriteParam imageWriteParam;
  protected IIOMetadata imageMetaData;
  private final String metaFormatName;
  private final boolean hasTransparency;
  
  /**
   * Creates a new GifSequenceWriter
   * 
   * @param outputStream the ImageOutputStream to be written to
   * @param imageType one of the imageTypes specified in BufferedImage
   * @param hasTransparency whether the gif has any transparent pixels
   * @param loopContinuously wether the gif should loop repeatedly
   * @throws IIOException if no gif ImageWriters are found
   *
   * @author Elliot Kroo (elliot[at]kroo[dot]net)
   */
  public GifSequenceWriter(
      ImageOutputStream outputStream,
      int imageType,
      boolean loopContinuously,
      boolean hasTransparency) throws IIOException, IOException {
    // my method to create a writer
    gifWriter = getWriter(); 
    imageWriteParam = gifWriter.getDefaultWriteParam();
    ImageTypeSpecifier imageTypeSpecifier =
      ImageTypeSpecifier.createFromBufferedImageType(imageType);

    imageMetaData =
      gifWriter.getDefaultImageMetadata(imageTypeSpecifier,
      imageWriteParam);

    metaFormatName = imageMetaData.getNativeMetadataFormatName();

    IIOMetadataNode root = (IIOMetadataNode)imageMetaData.getAsTree(metaFormatName);

    IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
    commentsNode.setAttribute("CommentExtension", "Created by MAH");

    IIOMetadataNode appEntensionsNode = getNode(
      root,
      "ApplicationExtensions");

    IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

    child.setAttribute("applicationID", "NETSCAPE");
    child.setAttribute("authenticationCode", "2.0");

    int loop = loopContinuously ? 0 : 1;

    child.setUserObject(new byte[]{ 0x1, (byte) (loop & 0xFF), (byte)
      ((loop >> 8) & 0xFF)});
    appEntensionsNode.appendChild(child);

    imageMetaData.setFromTree(metaFormatName, root);

    gifWriter.setOutput(outputStream);

    gifWriter.prepareWriteSequence(null);
    
    this.hasTransparency = hasTransparency;
  }
  
    /**
     * Add a frame to the GIF. The frame delay is capped (see
     * {@link capDelay(int)}).
     * 
     * @param img The image to add
     * @param frameDelay The frame delay (capped)
     * @throws IOException
     */
    public void writeToSequence(RenderedImage img, int frameDelay) throws IOException {
        writeToSequence(img, frameDelay, true);
    }
  
    /**
     * Add a frame to the GIF. The frame delay is capped when capDelay is true
     * (see {@link capDelay(int)}).
     * 
     * @param img The image to add
     * @param frameDelay The frame delay
     * @param capDelay Whether to cap the frame delay
     * @throws IOException 
     */
    public void writeToSequence(RenderedImage img, int frameDelay, boolean capDelay) throws IOException {
        if (capDelay) {
            frameDelay = capDelay(frameDelay);
        }
        setGceOptions(frameDelay);
        gifWriter.writeToSequence(
                new IIOImage(
                        img,
                        null,
                        imageMetaData),
                imageWriteParam);
    }
  
    private void setGceOptions(int frameDelay) throws IOException {
        IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(
                root,
                "GraphicControlExtension");

        /**
         * Choose disposal method so that now unwanted transparent pixels appear
         * in images that contain no transparent pixels at all. There is
         * probably a better way to do this, but it seems to work well enough
         * for now.
         */
        graphicsControlExtensionNode.setAttribute(
                "disposalMethod",
                hasTransparency ? "restoreToBackgroundColor" : "doNotDispose");
        graphicsControlExtensionNode.setAttribute(
                "userInputFlag",
                "FALSE");
        graphicsControlExtensionNode.setAttribute(
                "transparentColorFlag",
                "FALSE");
        graphicsControlExtensionNode.setAttribute(
                "transparentColorIndex",
                "0");
        graphicsControlExtensionNode.setAttribute(
                "delayTime",
                Integer.toString(frameDelay / 10));

        imageMetaData.setFromTree(metaFormatName, root);
    }
  
  /**
   * Close this GifSequenceWriter object. This does not close the underlying
   * stream, just finishes off the GIF.
   */
  public void close() throws IOException {
    gifWriter.endWriteSequence();    
  }

  /**
   * Returns the first available GIF ImageWriter using 
   * ImageIO.getImageWritersBySuffix("gif").
   * 
   * @return a GIF ImageWriter object
   * @throws IIOException if no GIF image writers are returned
   */
  private static ImageWriter getWriter() throws IIOException {
    Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
    if(!iter.hasNext()) {
      throw new IIOException("No GIF Image Writers Exist");
    } else {
      return iter.next();
    }
  }

  /**
   * Returns an existing child node, or creates and returns a new child node (if 
   * the requested node does not exist).
   * 
   * @param rootNode the <tt>IIOMetadataNode</tt> to search for the child node.
   * @param nodeName the name of the child node.
   * 
   * @return the child node, if found or a new node created with the given name.
   */
  private static IIOMetadataNode getNode(
      IIOMetadataNode rootNode,
      String nodeName) {
    int nNodes = rootNode.getLength();
    for (int i = 0; i < nNodes; i++) {
      if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName)
          == 0) {
        return((IIOMetadataNode) rootNode.item(i));
      }
    }
    IIOMetadataNode node = new IIOMetadataNode(nodeName);
    rootNode.appendChild(node);
    return(node);
  }

//  public static void main(String[] args) throws Exception {
//    if (args.length > 1) {
//      // grab the output image type from the first image in the sequence
//      BufferedImage firstImage = ImageIO.read(new File(args[0]));
//
//      // create a new BufferedOutputStream with the last argument
//      ImageOutputStream output = 
//        new FileImageOutputStream(new File(args[args.length - 1]));
//      
//      // create a gif sequence with the type of the first image, 1 second
//      // between frames, which loops continuously
//      GifSequenceWriter writer = 
//        new GifSequenceWriter(output, firstImage.getType(), 1, false);
//      
//      // write out the first image to our sequence...
//      writer.writeToSequence(firstImage);
//      for(int i=1; i<args.length-1; i++) {
//        BufferedImage nextImage = ImageIO.read(new File(args[i]));
//        writer.writeToSequence(nextImage);
//      }
//      
//      writer.close();
//      output.close();
//    } else {
//      System.out.println(
//        "Usage: java GifSequenceWriter [list of gif files] [output file]");
//    }
//  }
  
    /**
     * Create a GifSequenceWriter automatically getting the image type from the
     * given image, looping repeatedly and checking the image for transparencey.
     * 
     * @param output The image output stream
     * @param image A frame of the GIF, usually the first
     * @return The GifSequenceWriter
     * @throws IOException 
     */  
    public static GifSequenceWriter create(ImageOutputStream output, BufferedImage image) throws IOException {
        return new GifSequenceWriter(output, image.getType(), true, hasTransparency(image));
    }
    
    public static boolean hasTransparency(BufferedImage image) {
        if (image.getColorModel().hasAlpha()) {
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int alpha = (image.getRGB(x, y) >> 24) & 0xFF;
                    if (alpha == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Browsers usually seem to turn a delay of 0/100 or 1/100 into 10/100, so
     * do the same.
     * 
     * @see <a href="https://www.deviantart.com/humpy77/journal/Frame-Delay-Times-for-Animated-GIFs-240992090">How browsers handle frame delays (Web)</a>
     * 
     * @param delay Time between frames in ms
     * @return
     */
    public static int capDelay(int delay) {
        if (delay <= 10) {
            return 100;
        }
        return delay;
    }
    
}
