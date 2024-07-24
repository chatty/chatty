
package chatty.util;

/**
 *
 * @author tduva
 */
public class MathUtil {

    /**
     * Division with integers with rounding up. Check if this has the desired
     * behaviour for negative values if needed.
     * 
     * @param a
     * @param b
     * @return 
     */
    public static int divRoundUp(int a, int b) {
        return a / b + (a % b == 0 ? 0 : 1);
    }
    
}
