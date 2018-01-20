
package chatty.gui;

import chatty.util.StringUtil;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A Color with one or more names. If it has more than one name, it has a
 * primary name that is returned by {@see getName()} and can be used for display
 * for example.
 * 
 * @author tduva
 */
public class NamedColor extends Color {
    
    private final String name;
    private final Set<String> names;
    private final Set<String> namesLowercase;
    
    /**
     * Constructs a new color with the given name and rgb values.
     * 
     * @param name The name of the color
     * @param r The red component of the color
     * @param g The green component of the color
     * @param b The blue component of the color
     */
    public NamedColor(String name, int r, int g, int b) {
        super(r, g, b);
        if (name == null) {
            name = "";
        }
        this.name = name;
        names = new HashSet<>();
        names.add(name);
        namesLowercase = new HashSet<>();
        namesLowercase.add(StringUtil.toLowerCase(name));
    }
    
    /**
     * Constructs a new color with the given names and rgb values. The first
     * name in the array is used as the primary name.
     * 
     * @param namesArray The String array of names, should not contain null
     * values
     * @param r The red component of the color
     * @param g The green component of the color
     * @param b The blue component of the color
     */
    public NamedColor(String[] namesArray, int r, int g, int b) {
        super(r, g, b);
        names = new HashSet<>();
        names.addAll(Arrays.asList(namesArray));
        namesLowercase = new HashSet<>();
        for (String thisName : namesArray) {
            namesLowercase.add(StringUtil.toLowerCase(thisName));
        }
        if (namesArray.length == 0) {
            name = "";
        } else {
            name = namesArray[0];
        }
    }
    
    /**
     * Get the primary name of this color.
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    
    /**
     * Check if this color is associated with the given name (case-insensitive).
     * 
     * @param nameToCheck The name to check against
     * @return true if this color contains the given name, false otherwise
     */
    public boolean hasName(String nameToCheck) {
        return namesLowercase.contains(StringUtil.toLowerCase(nameToCheck));
    }
    
    /**
     * A String representation of this {@code NamedColor} which contains a
     * representation of the {@code Color} and the names associated with it.
     * 
     * @return 
     */
    @Override
    public String toString() {
        return super.toString() + names;
    }
    
    /**
     * Returns a comma-seperated String of the RGB components of this color.
     * 
     * @return 
     */
    public String getRgbString() {
        return getRed()+","+getGreen()+","+getBlue();
    }
    
}
