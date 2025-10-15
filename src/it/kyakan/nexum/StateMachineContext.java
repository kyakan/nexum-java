package it.kyakan.nexum;

import java.util.HashMap;
import java.util.Map;

/**
 * Context class that holds the state and data for the state machine.
 * This class maintains the current state and provides a key-value store for additional data.
 * 
 * @param <S> The type of the state (typically an Enum or String)
 */
public class StateMachineContext<S> {
    private S currentState;
    private S previousState;
    private final Map<String, Object> data;
    private long lastTransitionTime;
    private Exception lastError;
    
    /**
     * Create a new context with the given initial state
     * @param initialState The initial state
     */
    public StateMachineContext(S initialState) {
        this.currentState = initialState;
        this.previousState = null;
        this.data = new HashMap<>();
        this.lastTransitionTime = System.currentTimeMillis();
    }
    
    /**
     * Get the current state
     * @return The current state
     */
    public S getCurrentState() {
        return currentState;
    }
    
    /**
     * Set the current state (internal use only)
     * @param state The new state
     */
    void setCurrentState(S state) {
        this.previousState = this.currentState;
        this.currentState = state;
        this.lastTransitionTime = System.currentTimeMillis();
    }
    
    /**
     * Get the previous state
     * @return The previous state or null if no previous state
     */
    public S getPreviousState() {
        return previousState;
    }
    
    /**
     * Get the timestamp of the last state transition
     * @return Timestamp in milliseconds
     */
    public long getLastTransitionTime() {
        return lastTransitionTime;
    }
    
    /**
     * Store data in the context
     * @param key The key
     * @param value The value
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }
    
    /**
     * Retrieve data from the context
     * @param key The key
     * @return The value or null if not found
     */
    public Object get(String key) {
        return data.get(key);
    }
    
    /**
     * Retrieve typed data from the context
     * @param key The key
     * @param type The expected type
     * @param <T> The type parameter
     * @return The value cast to the specified type or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Check if the context contains a key
     * @param key The key to check
     * @return true if the key exists
     */
    public boolean contains(String key) {
        return data.containsKey(key);
    }
    
    /**
     * Remove data from the context
     * @param key The key to remove
     * @return The removed value or null if not found
     */
    public Object remove(String key) {
        return data.remove(key);
    }
    
    /**
     * Clear all data from the context
     */
    public void clear() {
        data.clear();
    }
    
    /**
     * Get all data as an immutable map
     * @return Map of all context data
     */
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }
    
    /**
     * Set the last error that occurred
     * @param error The error
     */
    public void setLastError(Exception error) {
        this.lastError = error;
    }
    
    /**
     * Get the last error that occurred
     * @return The last error or null if no error
     */
    public Exception getLastError() {
        return lastError;
    }
    
    /**
     * Clear the last error
     */
    public void clearLastError() {
        this.lastError = null;
    }
    
    @Override
    public String toString() {
        return "StateMachineContext{" +
                "currentState=" + currentState +
                ", previousState=" + previousState +
                ", dataSize=" + data.size() +
                ", lastTransitionTime=" + lastTransitionTime +
                ", hasError=" + (lastError != null) +
                '}';
    }
}