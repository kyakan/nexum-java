package it.kyakan.nexum;

/**
 * Exception thrown when state machine operations fail.
 * This exception can wrap underlying causes and provide context about the failure.
 */
public class NexumException extends Exception {
    
    private final Object currentState;
    private final Object event;
    
    /**
     * Create a new state machine exception
     * @param message The error message
     */
    public NexumException(String message) {
        super(message);
        this.currentState = null;
        this.event = null;
    }
    
    /**
     * Create a new state machine exception with a cause
     * @param message The error message
     * @param cause The underlying cause
     */
    public NexumException(String message, Throwable cause) {
        super(message, cause);
        this.currentState = null;
        this.event = null;
    }
    
    /**
     * Create a new state machine exception with state and event context
     * @param message The error message
     * @param currentState The current state when the error occurred
     * @param event The event that caused the error
     */
    public NexumException(String message, Object currentState, Object event) {
        super(message);
        this.currentState = currentState;
        this.event = event;
    }
    
    /**
     * Create a new state machine exception with full context
     * @param message The error message
     * @param cause The underlying cause
     * @param currentState The current state when the error occurred
     * @param event The event that caused the error
     */
    public NexumException(String message, Throwable cause, Object currentState, Object event) {
        super(message, cause);
        this.currentState = currentState;
        this.event = event;
    }
    
    /**
     * Get the state when the error occurred
     * @return The current state or null if not available
     */
    public Object getCurrentState() {
        return currentState;
    }
    
    /**
     * Get the event that caused the error
     * @return The event or null if not available
     */
    public Object getEvent() {
        return event;
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (currentState != null) {
            sb.append(" [state=").append(currentState).append("]");
        }
        if (event != null) {
            sb.append(" [event=").append(event).append("]");
        }
        return sb.toString();
    }
}