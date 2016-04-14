
package chatty.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class BitEncoderTest {

    /**
     * Test of encode/decode methods, of class BitEncoder.
     */
    @Test
    public void testEncodeDecode() {
        System.out.println("encode/decode");
        int[] testValues = new int[]{10,32};
        BitEncoder encodeInstance = new BitEncoder(testValues);
        
        long noValueSet = encodeInstance.encode();
        encodeInstance.setValue(0, 10);
        long firstValueSet = encodeInstance.encode();
        
        BitEncoder decodeInstance = new BitEncoder(testValues);
        
        decodeInstance.decode(noValueSet);
        assertEquals(0, decodeInstance.getValue(0));
        
        decodeInstance.decode(firstValueSet);
        assertEquals(10, decodeInstance.getValue(0));
        
        System.out.println("two sets of values");
        int[] testValues2 = new int[]{0,-5};
        BitEncoder encodeInstance2 = new BitEncoder(testValues, testValues2);
        
        encodeInstance2.setValue(1, -5);
        long twoSets = encodeInstance2.encode();
        
        BitEncoder decodeInstance2 = new BitEncoder(testValues, testValues2);
        decodeInstance2.decode(twoSets);
        
        assertEquals(decodeInstance2.getValue(1), -5);
    }
    
    /**
     * Test without value sets.
     */
    @Test
    public void testEmpty() {
        BitEncoder instance = new BitEncoder();
        instance.encode();
        instance.decode(0);
    }
    
}
