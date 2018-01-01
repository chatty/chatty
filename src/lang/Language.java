
package lang;

import chatty.Chatty;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.net.URL;
import java.net.URISyntaxException;
import org.apache.commons.io.*;

public class Language
{
    private List<String> LANGUAGE_PACKS = new ArrayList<String>();
    private String CURRENT_LANGUAGE = "en_US";
    private Map<String, String> STRINGS = new HashMap<String, String>();

    /**
     * Searches for all available language packs inside the JAR archive, or IDE project working directory.
     */
    public Language()
    {
        final String path = "lang/lp";
        final File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

        if (jarFile.isFile()) // Run with JAR file
        {
            final JarFile jar;
            try {
                jar = new JarFile(jarFile);
            } catch (IOException e) {
                return;
            }
            final Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
            while (entries.hasMoreElements())
            {
                final String name = entries.nextElement().getName();
                if (name.startsWith(path + "/") && name.endsWith(".lp")) // filter according to the path
                {
                    String lp = new File(name).getName();
                    lp = lp.substring(0, lp.lastIndexOf('.'));
                    LANGUAGE_PACKS.add(lp);
                }
            }
            try {
                jar.close();
            } catch (IOException e) {
            }
        }

        /****************
        /* UNTESTED !!! *
         ****************/
        else // Run with IDE
        {
            final URL url = Chatty.class.getResource("/" + path);
            if (url != null)
            {
                try {
                    final File apps = new File(url.toURI());
                    for (File app : apps.listFiles())
                        LANGUAGE_PACKS.add(app.getName());
                } catch (URISyntaxException e) {
                    // never happens
                }
            }
        }
    }

    public List<String> languagePacks()
    {
        return this.LANGUAGE_PACKS;
    }

    private void _Clear()
    {
        this.STRINGS.clear();
        this.CURRENT_LANGUAGE = "en_US";
    }

    /**
     * Tries to the set the requested language.
     * Falls back to builtin en_US on failure.
     */
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
                this._Clear();
                return;
            }
            String lp = null;
            try {
                lp = IOUtils.toString(reader);
            } catch (IOException e) {
                this._Clear();
                lp = null;
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
                STRINGS.put(s.substring(0, s.indexOf(' ')),   // key
                       s.substring(s.indexOf(' ') + 1)); // value (string)
                System.out.print("Language: added entry: "+s+"\n");
            }
            this.CURRENT_LANGUAGE = lang;
        }
        else
        {
            this._Clear();
        }
    }

    public Boolean hasLanguagePack(String lp)
    {
        return LANGUAGE_PACKS.contains(lp);
    }

    public String languageCode()
    {
        return this.CURRENT_LANGUAGE;
    }

    private Boolean hasStringForKey(String key)
    {
        return STRINGS.containsKey(key);
    }

    private String stringForKey(String key)
    {
        return STRINGS.get(key);
    }

    public String GET(String key, String def)
    {
        if (this.hasStringForKey(key))
            return this.stringForKey(key);
        return def;
    }
}
