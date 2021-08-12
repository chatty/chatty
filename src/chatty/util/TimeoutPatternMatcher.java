
package chatty.util;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates a Matcher object with the text wrapped into a special class that will
 * throw an exception when the text is accessed past the timeout time. This is
 * supposed to cancel long Matcher operations, such as catastrophic
 * backtracking. This only works if the Matcher implementation actually accesses
 * the given text itself regularly, instead of e.g. making it's own copy.
 *
 * Based on https://stackoverflow.com/a/11348374
 *
 * @author tduva
 */
public class TimeoutPatternMatcher {

    /**
     * Creates a Matcher that will (probably) throw a MatcherTimeoutException if
     * the Matcher is active past the given timeout.
     *
     * @param regex The regex
     * @param text The text to match against
     * @param timeoutMillis The timeout in milliseconds
     * @return A Matcher
     */
    public static Matcher create(String regex, String text, long timeoutMillis) {
        Pattern pattern = Pattern.compile(regex);
        return create(pattern, text, timeoutMillis);
    }

    /**
     * Creates a Matcher that will (probably) throw a MatcherTimeoutException if
     * the Matcher is active past the given timeout.
     *
     * @param pattern The Pattern object
     * @param text The text to match against
     * @param timeoutMillis The timeout in milliseconds
     * @return A Matcher
     */
    public static Matcher create(Pattern pattern, String text, long timeoutMillis) {
        CharSequence charSequence = new TimeoutCharSequence(text,
                System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis));
        return pattern.matcher(charSequence);
    }

    /**
     * Throws a MatcherTimeoutException if charAt() is accessed past the
     * timeout.
     */
    private static class TimeoutCharSequence implements CharSequence {

        private final CharSequence inner;
        private final long timeoutTime;
        private int count;

        public TimeoutCharSequence(CharSequence inner, long timeoutTime) {
            super();
            this.inner = inner;
            this.timeoutTime = timeoutTime;
        }

        @Override
        public char charAt(int index) {
            count++;
            if (count % 100 == 0) {
                // Only check more expensive (getting time) comparison sometimes
                if (timeoutTime - System.nanoTime() < 0) {
                    throw new MatcherTimeoutException();
                }
            }
            return inner.charAt(index);
        }

        @Override
        public int length() {
            return inner.length();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new TimeoutCharSequence(inner.subSequence(start, end), timeoutTime);
        }

        @Override
        public String toString() {
            return inner.toString();
        }
    }
    
    public static class MatcherTimeoutException extends RuntimeException {
        
        public MatcherTimeoutException() {
            super("Regex took too long to match");
        }
        
    }

}