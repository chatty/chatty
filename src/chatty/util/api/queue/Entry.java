
package chatty.util.api.queue;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author tduva
 */
public class Entry implements Comparable<Entry> {
    
    private static final AtomicLong seq = new AtomicLong();
    
    public final long entryNum;
    public final int priority;
    
    public final Request request;
    public final ResultListener listener;
    
    public Entry(int priority, Request request, ResultListener listener) {
        this.entryNum = seq.getAndIncrement();
        this.priority = priority;
        this.request = request;
        this.listener = listener;
    }

    @Override
    public int compareTo(Entry o) {
        if (priority == o.priority) {
            return -Long.compare(entryNum, o.entryNum);
        }
        return Integer.compare(priority, o.priority);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Entry other = (Entry) obj;
        if (this.priority != other.priority) {
            return false;
        }
        if (!Objects.equals(this.request, other.request)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + this.priority;
        hash = 53 * hash + Objects.hashCode(this.request);
        return hash;
    }
    
}
