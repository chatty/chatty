
package lang;

import java.io.*;
import org.apache.commons.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class Language
{
    // FIXME: get directory listing instead (jar:///lang/lp)
    private static final List<String> LanguagePacks = Arrays.asList(
        // default (built-in) language: en_US
        "de_AT", // German (Austria)
        "de_DE", // German (Germany)
        "ja_JP"  // Japanese (Japan)
    );

    private Map<String, String> LP = new HashMap<String, String>();

    public Language()
    {
    }

    // falls back to builtin en_US on failure
    public void setLanguage(String lang)
    {
        if (this.hasLanguagePack(lang))
        {
            // open language pack from JAR and read it into a string
            BufferedReader reader;
            try {
                InputStream in = getClass().getResourceAsStream("/lang/lp/" + lang + ".lp"); 
                reader = new BufferedReader(new InputStreamReader(in));
            } catch (Exception e) {
                this.LP.clear();
                return;
            }
            String lp = null;
            try {
                lp = IOUtils.toString(reader);
            } catch (IOException e) {
                this.LP.clear();
                lp = "";
                return;
            }

            // iterate over all entries in the language pack
            for (String s : lp.split("\\r?\\n"))
            {
                // comments: # //
                if (s.isEmpty() ||
                    s.startsWith("#") ||
                    s.startsWith("//"))
                    continue;
                LP.put(s.substring(0, s.indexOf(' ')),   // key
                       s.substring(s.indexOf(' ') + 1)); // value (string)
                System.out.print("Language: added entry: "+s+"\n");
            }
        }
        else
        {
            this.LP.clear();
        }
    }

    public Boolean hasLanguagePack(String lp)
    {
        return LanguagePacks.contains(lp);
    }

    private Boolean hasStringForKey(String key)
    {
        return LP.containsKey(key);
    }

    private String stringForKey(String key)
    {
        return LP.get(key);
    }

    public String GET(String key, String def)
    {
        if (this.hasStringForKey(key))
            return this.stringForKey(key);
        return def;
    }
}
