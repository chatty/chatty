
package chatty.util.commands;

import chatty.util.UrlRequest;
import chatty.util.UrlRequest.FullResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Perform a UrlRequest.
 * 
 * @author tduva
 */
public class Request implements Item {

    private final boolean isRequired;
    private final Item url;
    private final List<Item> options;
    
    Request(Item url, List<Item> options, boolean isRequired) {
        this.url = url;
        this.options = options;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        // URL
        String urlString = url.replace(parameters);
        if (!Item.checkReq(isRequired, urlString)) {
            return null;
        }
        
        // Options
        List<String> optionsString = new ArrayList<>();
        for (Item option : options) {
            String optionString = option.replace(parameters);
            if (!Item.checkReq(isRequired, optionString)) {
                return null;
            }
            optionsString.add(optionString);
        }
        boolean outputError = optionsString.contains("error");
        boolean trim = optionsString.contains("trim");
        
        // General errors
        if (parameters.get("allow-request") == null) {
            return outputError ? "Request not allowed in this context" : error();
        }
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            return outputError ? "URL scheme must be http or https" : error();
        }
        
        try {
            UrlRequest request = new UrlRequest(urlString);
            request.setLabel("Custom Command");
            FullResult result = request.sync();
            String resultText = result.getResult();
            if (resultText == null) {
                if (outputError) {
                    if (result.getResponseCode() != 0) {
                        return "Request error: " + result.getResponseCode();
                    }
                    return "Request error: " + result.getError();
                }
                return error();
            }
            if (resultText.isEmpty() && isRequired) {
                return null;
            }
            return trim ? resultText.trim() : resultText;
        }
        catch (Exception ex) {
            return outputError ? ex.getClass().getSimpleName()+": "+ex.getLocalizedMessage() : error();
        }
    }
    
    private String error() {
        if (isRequired) {
            return null;
        }
        return "";
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        // The caller can use this to check how replacements should be performed
        if (prefix.equals("-async-")) {
            return Item.getIdentifiersWithPrefix(prefix, url, options, "-async-");
        }
        return Item.getIdentifiersWithPrefix(prefix, url, options);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, url, options);
    }
    
    @Override
    public String toString() {
        return "Request " + url + "(" + options + ")";
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
        final Request other = (Request) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.url, other.url)) {
            return false;
        }
        if (!Objects.equals(this.options, other.options)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (this.isRequired ? 1 : 0);
        hash = 47 * hash + Objects.hashCode(this.url);
        hash = 47 * hash + Objects.hashCode(this.options);
        return hash;
    }

}
