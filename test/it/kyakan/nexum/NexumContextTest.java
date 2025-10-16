package it.kyakan.nexum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NexumContext
 */
@DisplayName("NexumContext Tests")
class NexumContextTest {
    
    private enum TestState {
        IDLE, RUNNING, STOPPED
    }
    
    private NexumContext<TestState> context;
    
    @BeforeEach
    void setUp() {
        context = new NexumContext<>(TestState.IDLE);
    }
    
    @Test
    @DisplayName("Should initialize with correct initial state")
    void testInitialState() {
        assertEquals(TestState.IDLE, context.getCurrentState());
        assertNull(context.getPreviousState());
    }
    
    @Test
    @DisplayName("Should update current state and track previous state")
    void testStateTransition() {
        context.setCurrentState(TestState.RUNNING);
        
        assertEquals(TestState.RUNNING, context.getCurrentState());
        assertEquals(TestState.IDLE, context.getPreviousState());
    }
    
    @Test
    @DisplayName("Should update transition time on state change")
    void testTransitionTime() {
        long timeBefore = System.currentTimeMillis();
        context.setCurrentState(TestState.RUNNING);
        long timeAfter = System.currentTimeMillis();
        
        long transitionTime = context.getLastTransitionTime();
        assertTrue(transitionTime >= timeBefore && transitionTime <= timeAfter);
    }
    
    @Test
    @DisplayName("Should store and retrieve data")
    void testDataStorage() {
        context.put("key1", "value1");
        context.put("key2", 42);
        
        assertEquals("value1", context.get("key1"));
        assertEquals(42, context.get("key2"));
    }
    
    @Test
    @DisplayName("Should retrieve typed data")
    void testTypedDataRetrieval() {
        context.put("string", "test");
        context.put("number", 123);
        
        String stringValue = context.get("string", String.class);
        Integer numberValue = context.get("number", Integer.class);
        
        assertEquals("test", stringValue);
        assertEquals(123, numberValue);
    }
    
    @Test
    @DisplayName("Should return null for wrong type")
    void testTypedDataRetrievalWrongType() {
        context.put("string", "test");
        
        Integer wrongType = context.get("string", Integer.class);
        assertNull(wrongType);
    }
    
    @Test
    @DisplayName("Should check if key exists")
    void testContainsKey() {
        context.put("existing", "value");
        
        assertTrue(context.contains("existing"));
        assertFalse(context.contains("nonexistent"));
    }
    
    @Test
    @DisplayName("Should remove data")
    void testRemoveData() {
        context.put("key", "value");
        assertTrue(context.contains("key"));
        
        Object removed = context.remove("key");
        assertEquals("value", removed);
        assertFalse(context.contains("key"));
    }
    
    @Test
    @DisplayName("Should clear all data")
    void testClearData() {
        context.put("key1", "value1");
        context.put("key2", "value2");
        
        context.clear();
        
        assertFalse(context.contains("key1"));
        assertFalse(context.contains("key2"));
        assertTrue(context.getData().isEmpty());
    }
    
    @Test
    @DisplayName("Should return immutable copy of data")
    void testGetData() {
        context.put("key1", "value1");
        context.put("key2", "value2");
        
        var data = context.getData();
        assertEquals(2, data.size());
        
        // Modifying returned map should not affect context
        data.put("key3", "value3");
        assertFalse(context.contains("key3"));
    }
    
    @Test
    @DisplayName("Should store and retrieve last error")
    void testLastError() {
        assertNull(context.getLastError());
        
        Exception error = new RuntimeException("Test error");
        context.setLastError(error);
        
        assertEquals(error, context.getLastError());
    }
    
    @Test
    @DisplayName("Should clear last error")
    void testClearLastError() {
        Exception error = new RuntimeException("Test error");
        context.setLastError(error);
        assertNotNull(context.getLastError());
        
        context.clearLastError();
        assertNull(context.getLastError());
    }
    
    @Test
    @DisplayName("Should generate meaningful toString")
    void testToString() {
        String str = context.toString();
        
        assertTrue(str.contains("currentState=IDLE"));
        assertTrue(str.contains("previousState=null"));
        assertTrue(str.contains("dataSize=0"));
        assertTrue(str.contains("hasError=false"));
    }
    
    @Test
    @DisplayName("Should track multiple state transitions")
    void testMultipleTransitions() {
        context.setCurrentState(TestState.RUNNING);
        assertEquals(TestState.RUNNING, context.getCurrentState());
        assertEquals(TestState.IDLE, context.getPreviousState());
        
        context.setCurrentState(TestState.STOPPED);
        assertEquals(TestState.STOPPED, context.getCurrentState());
        assertEquals(TestState.RUNNING, context.getPreviousState());
    }
}