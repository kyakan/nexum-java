package it.kyakan.nexum;

/**
 * Simple implementation of the Event interface.
 * This class provides a basic event with type and optional data.
 * 
 * @param <T> The type of the event
 */
public class SimpleEvent<T> implements Event<T> {
    
    private final T type;
    private final Object data;
    private final long timestamp;
    
    /**
     * Create a new simple event
     * @param type The event type
     */
    public SimpleEvent(T type) {
        this(type, null);
    }
    
    /**
     * Create a new simple event with data
     * @param type The event type
     * @param data The event data
     */
    public SimpleEvent(T type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public T getType() {
        return type;
    }
    
    @Override
    public Object getData() {
        return data;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "SimpleEvent{" +
                "type=" + type +
                ", hasData=" + (data != null) +
                ", timestamp=" + timestamp +
                '}';
    }
}