
package chatty.lang;

import chatty.gui.components.settings.MainSettings;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author tduva
 */
public class LanguageTest {
    
    /**
     * Test all strings with MessageFormat, to find out if they are formatted
     * correctly.
     * 
     * It tests all the languages that can be set in the Settings Dialog, using
     * the same method for retrieving the ResourceBundle as is used in
     * production (including fallbacks), which means that all strings that are
     * actually used in production should be tested.
     *
     * @throws Exception 
     */
    @Test
    public void test() throws Exception {
        // Get all the languages that can be set in the Settings Dialog
        Collection<String> languages = new HashSet<>(MainSettings.getLanguageOptions().keySet());
        languages.add("asdas");
        //languages.remove("tr");
        //String[] languages = new String[]{"adas", "en", "es", "ja", "ru", "zh_TW"};
        
        boolean failed = false;
        for (String lang : languages) {
            // Get the bundle as Chatty would normally get it
            ResourceBundle bundle = Language.getBundleForLanguage(lang);
            //System.out.println(bundle.getLocale()+"------------------------------------------"+lang);
            for (String key : bundle.keySet()) {
                //System.out.println(key);
                try {
                    // Just use any parameter to check the format, a number
                    // because it should work for everything (hopefully)
                    MessageFormat.format(bundle.getString(key), 1);
                } catch (Exception ex) {
                    // Don't fail immediately, output all errors first
                    // (will output errors in parent strings as well)
                    failed = true;
                    System.out.println(String.format("Error in '%s' [%s = %s] (%s)",
                            lang, key, bundle.getString(key), ex.toString()));
                }
            }
        }
        if (failed) {
            Assert.fail("MessageFormat error");
        }
    }
    
    /**
     * Find strings in translations that don't have the same amount of format
     * parameters as in the master bundle (English). This only outputs any
     * mismatching strings instead of failing the test, because there could be
     * good reasons for the amount of parameters not being exactly the same.
     */
    @Test
    public void findMissingParameters() {
        String masterLang = "en_US";
        Collection<String> languages = new HashSet<>(MainSettings.getLanguageOptions().keySet());
        ResourceBundle masterBundle = Language.getBundleForLanguage(masterLang);
        for (String lang : languages) {
            ResourceBundle bundle = Language.getBundleForLanguage(lang);
            for (String key : bundle.keySet()) {
                /**
                 * If a key was removed or renamed it may still be in a not yet
                 * updated language file, but not in the master file
                 */
                if (masterBundle.containsKey(key)) {
                    String masterItem = masterBundle.getString(key);
                    String item = bundle.getString(key);
                    MessageFormat mmf = new MessageFormat(masterItem);
                    MessageFormat mf = new MessageFormat(item);
                    if (mmf.getFormats().length != mf.getFormats().length) {
                        System.out.println(String.format("Parameter count mismatch: %s\n %s: %s\n %s: %s",
                                key, masterLang, masterItem, lang, item));
                    }
                }
            }
        }
    }
    
}
