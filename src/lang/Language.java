
package lang;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.Set;

public class Language
{
    private Boolean hasStringForKey(String key)
    {
        return false;
    }

    private String stringForKey(String key)
    {
        return "";
    }

    public String GET(String key, String def)
    {
        if (this.hasStringForKey(key))
            return this.stringForKey(key);
        return def;
    }
}
