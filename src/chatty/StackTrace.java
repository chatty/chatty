package chatty;
import java.io.StringWriter;
import java.io.PrintWriter;

public class StackTrace {
    public static String getStacktrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    public static String getStacktraceForLogging(Throwable t) {
        if (t != null) {
            try {
                return "\n:"+getStacktrace(t);
            } catch (Exception ex) {
                return "\n:Error getting stacktrace";
            }
        }
        return "";
    }
     
}
