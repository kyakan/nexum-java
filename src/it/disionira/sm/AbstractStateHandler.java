package it.disionira.sm;

/**
 * Abstract base class for StateHandler that provides a constructor to set the state.
 * This simplifies the creation of state handlers by eliminating the need to implement getState().
 * 
 * @param <S> The type of the state
 * @param <E> The type of the event
 */
public abstract class AbstractStateHandler<S, E> implements StateHandler<S, E> {
    
    private final S state;
    
    /**
     * Constructor that sets the state for this handler
     * @param state The state this handler is for
     */
    public AbstractStateHandler(S state) {
        this.state = state;
    }
    
    /**
     * Get the state this handler is for
     * @return The state
     */
    @Override
    public final S getState() {
        return state;
    }
}