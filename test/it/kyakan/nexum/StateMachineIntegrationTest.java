package it.kyakan.nexum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for StateMachine demonstrating real-world scenarios
 */
@DisplayName("StateMachine Integration Tests")
class StateMachineIntegrationTest {
    
    // Enums for Traffic Light test
    enum TrafficLightState { RED, YELLOW, GREEN }
    enum TrafficLightEvent { TIMER }
    
    // Enums for Door test
    enum DoorState { CLOSED, OPEN, LOCKED }
    enum DoorEvent { OPEN, CLOSE, LOCK, UNLOCK }
    
    // Enums for Order Processing test
    enum OrderState { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }
    enum OrderEvent { CONFIRM, SHIP, DELIVER, CANCEL }
    
    // Enums for Media Player test
    enum PlayerState { STOPPED, PLAYING, PAUSED }
    enum PlayerEvent { PLAY, PAUSE, STOP }
    
    // Enums for Vending Machine test
    enum VendingState { IDLE, COIN_INSERTED, DISPENSING }
    enum VendingEvent { INSERT_COIN, SELECT_ITEM, DISPENSE_COMPLETE }
    
    // Enums for Connection test
    enum ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
    enum ConnectionEvent { CONNECT, SUCCESS, FAILURE, DISCONNECT }
    
    /**
     * Example: Traffic Light State Machine
     */
    @Test
    @DisplayName("Traffic Light State Machine")
    void testTrafficLightStateMachine() throws StateMachineException {
        
        StateMachine<TrafficLightState, TrafficLightEvent> trafficLight = 
            new StateMachine<>(TrafficLightState.RED);
        
        List<TrafficLightState> stateHistory = new ArrayList<>();
        
        trafficLight
            .addTransition(TrafficLightState.RED, TrafficLightState.GREEN, TrafficLightEvent.TIMER)
            .addTransition(TrafficLightState.GREEN, TrafficLightState.YELLOW, TrafficLightEvent.TIMER)
            .addTransition(TrafficLightState.YELLOW, TrafficLightState.RED, TrafficLightEvent.TIMER)
            .addListener((from, to, event) -> {
                if (to != null) stateHistory.add(to);
            })
            .start();
        
        // Simulate traffic light cycle
        trafficLight.fireEvent(TrafficLightEvent.TIMER); // RED -> GREEN
        trafficLight.fireEvent(TrafficLightEvent.TIMER); // GREEN -> YELLOW
        trafficLight.fireEvent(TrafficLightEvent.TIMER); // YELLOW -> RED
        
        assertEquals(TrafficLightState.RED, trafficLight.getCurrentState());
        assertEquals(4, stateHistory.size()); // Initial RED + 3 transitions
        assertEquals(TrafficLightState.RED, stateHistory.get(0));
        assertEquals(TrafficLightState.GREEN, stateHistory.get(1));
        assertEquals(TrafficLightState.YELLOW, stateHistory.get(2));
        assertEquals(TrafficLightState.RED, stateHistory.get(3));
    }
    
    /**
     * Example: Door State Machine with Guards
     */
    @Test
    @DisplayName("Door State Machine with Lock")
    void testDoorStateMachine() throws StateMachineException {
        StateMachine<DoorState, DoorEvent> door = new StateMachine<>(DoorState.CLOSED);
        
        door
            .addTransition(DoorState.CLOSED, DoorState.OPEN, DoorEvent.OPEN)
            .addTransition(DoorState.OPEN, DoorState.CLOSED, DoorEvent.CLOSE)
            .addTransition(DoorState.CLOSED, DoorState.LOCKED, DoorEvent.LOCK)
            .addTransition(DoorState.LOCKED, DoorState.CLOSED, DoorEvent.UNLOCK,
                (ctx, event, data) -> {
                    // Guard: requires correct key
                    String key = (String) data;
                    return "correct-key".equals(key);
                })
            .start();
        
        // Open and close door
        door.fireEvent(DoorEvent.OPEN);
        assertEquals(DoorState.OPEN, door.getCurrentState());
        
        door.fireEvent(DoorEvent.CLOSE);
        assertEquals(DoorState.CLOSED, door.getCurrentState());
        
        // Lock door
        door.fireEvent(DoorEvent.LOCK);
        assertEquals(DoorState.LOCKED, door.getCurrentState());
        
        // Try to unlock with wrong key
        assertThrows(StateMachineException.class, () -> {
            door.fireEvent(DoorEvent.UNLOCK, "wrong-key");
        });
        assertEquals(DoorState.LOCKED, door.getCurrentState());
        
        // Unlock with correct key
        door.fireEvent(DoorEvent.UNLOCK, "correct-key");
        assertEquals(DoorState.CLOSED, door.getCurrentState());
    }
    
    /**
     * Example: Order Processing State Machine
     */
    @Test
    @DisplayName("Order Processing State Machine")
    void testOrderProcessingStateMachine() throws StateMachineException {
        StateMachine<OrderState, OrderEvent> order = new StateMachine<>(OrderState.PENDING);
        
        // Track order processing steps
        List<String> processingLog = new ArrayList<>();
        
        StateHandler<OrderState, OrderEvent> confirmedHandler = new AbstractStateHandler<OrderState, OrderEvent>(OrderState.CONFIRMED) {
            @Override
            public void onEnter(StateMachineContext<OrderState> context, OrderState fromState, OrderEvent event) {
                processingLog.add("Order confirmed - preparing for shipment");
                context.put("confirmationTime", System.currentTimeMillis());
            }
        };
        
        StateHandler<OrderState, OrderEvent> shippedHandler = new AbstractStateHandler<OrderState, OrderEvent>(OrderState.SHIPPED) {
            @Override
            public void onEnter(StateMachineContext<OrderState> context, OrderState fromState, OrderEvent event) {
                processingLog.add("Order shipped - tracking number generated");
                context.put("trackingNumber", "TRACK-12345");
            }
        };
        
        order
            .registerStateHandler(confirmedHandler)
            .registerStateHandler(shippedHandler)
            .addTransition(OrderState.PENDING, OrderState.CONFIRMED, OrderEvent.CONFIRM)
            .addTransition(OrderState.CONFIRMED, OrderState.SHIPPED, OrderEvent.SHIP)
            .addTransition(OrderState.SHIPPED, OrderState.DELIVERED, OrderEvent.DELIVER)
            .addTransition(OrderState.PENDING, OrderState.CANCELLED, OrderEvent.CANCEL)
            .addTransition(OrderState.CONFIRMED, OrderState.CANCELLED, OrderEvent.CANCEL)
            .start();
        
        // Process order
        order.fireEvent(OrderEvent.CONFIRM);
        assertEquals(OrderState.CONFIRMED, order.getCurrentState());
        assertTrue(order.getContext().contains("confirmationTime"));
        
        order.fireEvent(OrderEvent.SHIP);
        assertEquals(OrderState.SHIPPED, order.getCurrentState());
        assertEquals("TRACK-12345", order.getContext().get("trackingNumber"));
        
        order.fireEvent(OrderEvent.DELIVER);
        assertEquals(OrderState.DELIVERED, order.getCurrentState());
        
        assertEquals(2, processingLog.size());
        assertTrue(processingLog.get(0).contains("confirmed"));
        assertTrue(processingLog.get(1).contains("shipped"));
    }
    
    /**
     * Example: Player State Machine with Actions
     */
    @Test
    @DisplayName("Media Player State Machine")
    void testMediaPlayerStateMachine() throws StateMachineException {
        StateMachine<PlayerState, PlayerEvent> player = new StateMachine<>(PlayerState.STOPPED);
        
        final int[] playCount = {0};
        
        player
            .addTransition(PlayerState.STOPPED, PlayerState.PLAYING, PlayerEvent.PLAY,
                null,
                (ctx, event, data) -> {
                    playCount[0]++;
                    ctx.put("position", 0);
                })
            .addTransition(PlayerState.PLAYING, PlayerState.PAUSED, PlayerEvent.PAUSE,
                null,
                (ctx, event, data) -> {
                    Integer position = ctx.get("position", Integer.class);
                    ctx.put("pausedAt", position);
                })
            .addTransition(PlayerState.PAUSED, PlayerState.PLAYING, PlayerEvent.PLAY,
                null,
                (ctx, event, data) -> {
                    Integer pausedAt = ctx.get("pausedAt", Integer.class);
                    ctx.put("position", pausedAt);
                })
            .addTransition(PlayerState.PLAYING, PlayerState.STOPPED, PlayerEvent.STOP)
            .addTransition(PlayerState.PAUSED, PlayerState.STOPPED, PlayerEvent.STOP)
            .start();
        
        // Play media
        player.fireEvent(PlayerEvent.PLAY);
        assertEquals(PlayerState.PLAYING, player.getCurrentState());
        assertEquals(1, playCount[0]);
        
        // Pause
        player.fireEvent(PlayerEvent.PAUSE);
        assertEquals(PlayerState.PAUSED, player.getCurrentState());
        assertTrue(player.getContext().contains("pausedAt"));
        
        // Resume
        player.fireEvent(PlayerEvent.PLAY);
        assertEquals(PlayerState.PLAYING, player.getCurrentState());
        
        // Stop
        player.fireEvent(PlayerEvent.STOP);
        assertEquals(PlayerState.STOPPED, player.getCurrentState());
    }
    
    /**
     * Example: Vending Machine State Machine
     */
    @Test
    @DisplayName("Vending Machine State Machine")
    void testVendingMachineStateMachine() throws StateMachineException {
        StateMachine<VendingState, VendingEvent> vendingMachine = 
            new StateMachine<>(VendingState.IDLE);
        
        vendingMachine
            .addTransition(VendingState.IDLE, VendingState.COIN_INSERTED, VendingEvent.INSERT_COIN,
                null,
                (ctx, event, data) -> {
                    Integer amount = (Integer) data;
                    ctx.put("balance", amount);
                })
            .addTransition(VendingState.COIN_INSERTED, VendingState.DISPENSING, VendingEvent.SELECT_ITEM,
                (ctx, event, data) -> {
                    // Guard: check if balance is sufficient
                    Integer balance = ctx.get("balance", Integer.class);
                    Integer itemPrice = (Integer) data;
                    return balance != null && itemPrice != null && balance >= itemPrice;
                },
                (ctx, event, data) -> {
                    Integer balance = ctx.get("balance", Integer.class);
                    Integer itemPrice = (Integer) data;
                    ctx.put("change", balance - itemPrice);
                })
            .addTransition(VendingState.DISPENSING, VendingState.IDLE, VendingEvent.DISPENSE_COMPLETE,
                null,
                (ctx, event, data) -> {
                    ctx.remove("balance");
                    ctx.remove("change");
                })
            .start();
        
        // Insert coin
        vendingMachine.fireEvent(VendingEvent.INSERT_COIN, 100);
        assertEquals(VendingState.COIN_INSERTED, vendingMachine.getCurrentState());
        assertEquals(100, vendingMachine.getContext().get("balance"));
        
        // Try to select expensive item (should fail)
        assertThrows(StateMachineException.class, () -> {
            vendingMachine.fireEvent(VendingEvent.SELECT_ITEM, 150);
        });
        
        // Select affordable item
        vendingMachine.fireEvent(VendingEvent.SELECT_ITEM, 75);
        assertEquals(VendingState.DISPENSING, vendingMachine.getCurrentState());
        assertEquals(25, vendingMachine.getContext().get("change"));
        
        // Complete dispensing
        vendingMachine.fireEvent(VendingEvent.DISPENSE_COMPLETE);
        assertEquals(VendingState.IDLE, vendingMachine.getCurrentState());
        assertFalse(vendingMachine.getContext().contains("balance"));
    }
    
    /**
     * Example: Connection State Machine with Retry Logic
     */
    @Test
    @DisplayName("Connection State Machine with Retry")
    void testConnectionStateMachine() throws StateMachineException {
        StateMachine<ConnectionState, ConnectionEvent> connection = 
            new StateMachine<>(ConnectionState.DISCONNECTED);
        
        connection
            .addTransition(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING, ConnectionEvent.CONNECT,
                null,
                (ctx, event, data) -> {
                    ctx.put("retryCount", 0);
                })
            .addTransition(ConnectionState.CONNECTING, ConnectionState.CONNECTED, ConnectionEvent.SUCCESS)
            .addTransition(ConnectionState.CONNECTING, ConnectionState.ERROR, ConnectionEvent.FAILURE,
                null,
                (ctx, event, data) -> {
                    Integer retries = ctx.get("retryCount", Integer.class);
                    ctx.put("retryCount", retries + 1);
                })
            .addTransition(ConnectionState.ERROR, ConnectionState.CONNECTING, ConnectionEvent.CONNECT,
                (ctx, event, data) -> {
                    // Guard: allow retry if count < 3
                    Integer retries = ctx.get("retryCount", Integer.class);
                    return retries != null && retries < 3;
                })
            .addTransition(ConnectionState.CONNECTED, ConnectionState.DISCONNECTED, ConnectionEvent.DISCONNECT)
            .start();
        
        // Attempt connection
        connection.fireEvent(ConnectionEvent.CONNECT);
        assertEquals(ConnectionState.CONNECTING, connection.getCurrentState());
        
        // Simulate failure
        connection.fireEvent(ConnectionEvent.FAILURE);
        assertEquals(ConnectionState.ERROR, connection.getCurrentState());
        assertEquals(1, connection.getContext().get("retryCount"));
        
        // Retry
        connection.fireEvent(ConnectionEvent.CONNECT);
        assertEquals(ConnectionState.CONNECTING, connection.getCurrentState());
        
        // Success
        connection.fireEvent(ConnectionEvent.SUCCESS);
        assertEquals(ConnectionState.CONNECTED, connection.getCurrentState());
        
        // Disconnect
        connection.fireEvent(ConnectionEvent.DISCONNECT);
        assertEquals(ConnectionState.DISCONNECTED, connection.getCurrentState());
    }
}