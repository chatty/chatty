
package chatty.util.api.queue;

/**
 *
 * @author tduva
 */
public interface ResultListener {
    
    public void result(Result r);
    
    public static class Result {
        
        public final String text;
        public final int responseCode;
        public final String errorText;
        
        public Result(String result, int responseCode, String errorMessage) {
            this.text = result;
            this.responseCode = responseCode;
            this.errorText = errorMessage;
        }
        
    }
    
}
