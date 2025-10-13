package it.disionira.sm;

/**
 * Interface representing an event that can trigger state transitions.
 * Events can carry data and are used to trigger state changes in the state machine.
 * 
 * @param <T> The type of the event (typically an Enum)
 */
public interface Event<T> {
    /**
     * Get the event type
     * @return The event type
     */
    T getType();
    
    /**
     * Get optional data associated with this event
     * @return Event data or null if no data
     */
    default Object getData() {
        return null;
    }
    
    /**
     * Get the timestamp when this event was created
     * @return Event timestamp in milliseconds
     */
    default long getTimestamp() {
        return System.currentTimeMillis();
    }
}