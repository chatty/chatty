
package chatty.util;

/**
 * Provide a list of possible values and this encodes which one is currently
 * set in one number. This is possible for more than one sets of values.
 * 
 * If first valueDef is [1,2,3,9] and second is [0,1]
 * 
 * first value = 3
 * second value = 0
 * [1,0][9,3,2,1]
 *  0 1  0 1 0 0 = 20
 * 
 * @author tduva
 */
public class BitEncoder {
    
    private final int[][] valueDef;
    private final int[] values;

    public BitEncoder(int[]... valueDef) {
        this.valueDef = valueDef;
        this.values = new int[valueDef.length];
    }
    
    public void setValue(int id, int value) {
        values[id] = value;
    }
    
    public int getValue(int id) {
        return values[id];
    }
    
    public long encode() {
        int result = 0;
        int count = 0;
        for (int id=0;id<valueDef.length;id++) {
            for (int i=0;i<valueDef[id].length;i++) {
                if (valueDef[id][i] == values[id]) {
                    result += Math.pow(2, count);
                }
                count++;
            }
        }
        return result;
    }
    
    public void decode(long value) {
        int count = 0;
        for (int id=0;id<valueDef.length;id++) {
            for (int i = 0; i < valueDef[id].length; i++) {
                long bitmask = (long) Math.pow(2, count);
                if ((value & bitmask) == bitmask) {
                    values[id] = valueDef[id][i];
                }
                count++;
            }
        }
    }
    
}