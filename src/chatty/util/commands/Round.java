
package chatty.util.commands;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class Round implements Item {

    private final boolean isRequired;
    private final Item input;
    private final Item decimalsItem;
    private final Item roundingModeItem;
    private final Item minDecimalsItem;
    
    public Round(Item input, Item decimals, Item roundingMode, Item minDecimals, boolean isRequired) {
        this.input = input;
        this.decimalsItem = decimals;
        this.roundingModeItem = roundingMode;
        this.minDecimalsItem = minDecimals;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String numString = input.replace(parameters);
        if (!Item.checkReq(isRequired, numString)) {
            return null;
        }
        double num;
        try {
            num = Double.parseDouble(numString);
        }
        catch (NumberFormatException ex) {
            return numString;
        }
        int decimals = getInt(decimalsItem, parameters);
        if (decimals == -1) {
            return null;
        }
        int minDecimals = getInt(minDecimalsItem, parameters);
        if (minDecimals == -1) {
            return null;
        }
        String roundingMode = "";
        if (roundingModeItem != null) {
            roundingMode = roundingModeItem.replace(parameters);
            if (!Item.checkReq(isRequired, roundingMode)) {
                return null;
            }
        }
        return round(num, decimals, getRoundingMode(roundingMode), minDecimals);
    }
    
    private int getInt(Item item, Parameters parameters) {
        if (item == null) {
            return 0;
        }
        String temp = item.replace(parameters);
        if (!Item.checkReq(isRequired, temp)) {
            return -1;
        }
        try {
            return Integer.parseInt(temp);
        }
        catch (NumberFormatException ex) {
            return 0;
        }
    }
    
    private static RoundingMode getRoundingMode(String input) {
        switch (input) {
            case "floor":
                return RoundingMode.FLOOR;
            case "ceil":
                return RoundingMode.CEILING;
            case "up":
                return RoundingMode.UP;
            case "down":
                return RoundingMode.DOWN;
            case "half-down":
                return RoundingMode.HALF_DOWN;
            default:
                return RoundingMode.HALF_UP;
        }
    }
    
    public static String round(double input, int decimals, RoundingMode mode, int minDecials) {
        DecimalFormat format = new DecimalFormat("#.####################",
                DecimalFormatSymbols.getInstance(Locale.ROOT));
        format.setMinimumFractionDigits(minDecials);
        return format.format(BigDecimal.valueOf(input).setScale(decimals, mode));
    }
    
    @Override
    public String toString() {
        return String.format("Round %s(%s/%s/%s)",
                input,
                opt(decimalsItem), opt(roundingModeItem), opt(minDecimalsItem));
    }
    
    private static String opt(Object item) {
        return item != null ? item.toString() : "-";
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, input, decimalsItem, roundingModeItem);
    }

    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, input, decimalsItem, roundingModeItem);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Round other = (Round) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.input, other.input)) {
            return false;
        }
        if (!Objects.equals(this.decimalsItem, other.decimalsItem)) {
            return false;
        }
        if (!Objects.equals(this.roundingModeItem, other.roundingModeItem)) {
            return false;
        }
        return Objects.equals(this.minDecimalsItem, other.minDecimalsItem);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.isRequired ? 1 : 0);
        hash = 97 * hash + Objects.hashCode(this.input);
        hash = 97 * hash + Objects.hashCode(this.decimalsItem);
        hash = 97 * hash + Objects.hashCode(this.roundingModeItem);
        hash = 97 * hash + Objects.hashCode(this.minDecimalsItem);
        return hash;
    }

}
