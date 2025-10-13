package it.disionira.sm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StateMachineException
 */
@DisplayName("StateMachineException Tests")
class StateMachineExceptionTest {
    
    private enum TestState {
        IDLE, RUNNING
    }
    
    private enum TestEvent {
        START, STOP
    }
    
    @Test
    @DisplayName("Should create exception with message only")
    void testMessageOnlyConstructor() {
        StateMachineException exception = new StateMachineException("Test error");
        
        assertEquals("Test error", exception.getMessage());
        assertNull(exception.getCurrentState());
        assertNull(exception.getEvent());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("Should create exception with message and cause")
    void testMessageAndCauseConstructor() {
        RuntimeException cause = new RuntimeException("Root cause");
        StateMachineException exception = new StateMachineException("Test error", cause);
        
        assertEquals("Test error", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getCurrentState());
        assertNull(exception.getEvent());
    }
    
    @Test
    @DisplayName("Should create exception with state and event context")
    void testStateAndEventConstructor() {
        StateMachineException exception = 
            new StateMachineException("Test error", TestState.IDLE, TestEvent.START);
        
        assertTrue(exception.getMessage().contains("Test error"));
        assertTrue(exception.getMessage().contains("state=IDLE"));
        assertTrue(exception.getMessage().contains("event=START"));
        assertEquals(TestState.IDLE, exception.getCurrentState());
        assertEquals(TestEvent.START, exception.getEvent());
    }
    
    @Test
    @DisplayName("Should create exception with full context")
    void testFullContextConstructor() {
        RuntimeException cause = new RuntimeException("Root cause");
        StateMachineException exception = 
            new StateMachineException("Test error", cause, TestState.RUNNING, TestEvent.STOP);
        
        assertTrue(exception.getMessage().contains("Test error"));
        assertTrue(exception.getMessage().contains("state=RUNNING"));
        assertTrue(exception.getMessage().contains("event=STOP"));
        assertEquals(TestState.RUNNING, exception.getCurrentState());
        assertEquals(TestEvent.STOP, exception.getEvent());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    @DisplayName("Should format message with state only")
    void testMessageWithStateOnly() {
        StateMachineException exception = 
            new StateMachineException("Error occurred", TestState.IDLE, null);
        
        String message = exception.getMessage();
        assertTrue(message.contains("Error occurred"));
        assertTrue(message.contains("state=IDLE"));
        assertFalse(message.contains("event="));
    }
    
    @Test
    @DisplayName("Should format message with event only")
    void testMessageWithEventOnly() {
        StateMachineException exception = 
            new StateMachineException("Error occurred", null, TestEvent.START);
        
        String message = exception.getMessage();
        assertTrue(message.contains("Error occurred"));
        assertTrue(message.contains("event=START"));
        assertFalse(message.contains("state="));
    }
    
    @Test
    @DisplayName("Should handle null state and event gracefully")
    void testNullStateAndEvent() {
        StateMachineException exception = 
            new StateMachineException("Error occurred", null, null);
        
        assertEquals("Error occurred", exception.getMessage());
        assertNull(exception.getCurrentState());
        assertNull(exception.getEvent());
    }
    
    @Test
    @DisplayName("Should be throwable")
    void testThrowable() {
        assertThrows(StateMachineException.class, () -> {
            throw new StateMachineException("Test exception");
        });
    }
    
    @Test
    @DisplayName("Should preserve cause stack trace")
    void testCauseStackTrace() {
        RuntimeException cause = new RuntimeException("Root cause");
        StateMachineException exception = new StateMachineException("Wrapper", cause);
        
        assertNotNull(exception.getCause());
        assertEquals("Root cause", exception.getCause().getMessage());
    }
    
    @Test
    @DisplayName("Should work with string states and events")
    void testStringStateAndEvent() {
        StateMachineException exception = 
            new StateMachineException("Error", "StringState", "StringEvent");
        
        assertTrue(exception.getMessage().contains("state=StringState"));
        assertTrue(exception.getMessage().contains("event=StringEvent"));
        assertEquals("StringState", exception.getCurrentState());
        assertEquals("StringEvent", exception.getEvent());
    }
}