
package chatty.util.gif;

/**
 * Stores pixels in an int array in the AARRGGBB format.
 * 
 * @author tduva
 */
public class ARGBBuffer {

    private final int[] buffer;
    private int pos;
    
    public ARGBBuffer(int size) {
        this.buffer = new int[size];
    }
    
    public ARGBBuffer(int[] buffer) {
        this.buffer = buffer;
    }
    
    /**
     * Add a pixel at the current position, increases position by one.
     * 
     * @param r red
     * @param g green
     * @param b blue
     * @param a alpha
     */
    public void addRGBA(byte r, byte g, byte b, byte a) {
        int argb = (((a       ) << 24) |
                    ((r & 0xff) << 16) |
                    ((g & 0xff) <<  8) |
                    ((b & 0xff)      ));
        buffer[pos] = argb;
        pos++;
    }
    
    /**
     * Get the buffer directly.
     * 
     * @return 
     */
    public int[] get() {
        return buffer;
    }
    
    /**
     * Reset the position where pixels are added to the beginning.
     */
    public void reset() {
        pos = 0;
    }
    
}
