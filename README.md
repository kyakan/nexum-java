# State Machine Package

A generic and flexible state machine implementation in Java for Android.

## Main Components

### 1. Nexum<S, E>
The main class that manages the state machine. It's generic and can work with any type of state and event.

### 2. NexumContext<S>
Maintains the current state and provides a key-value store for additional data during transitions.

### 3. Transition<S, E>
Represents a transition from one state to another, with support for:
- Guard conditions (conditions that must be satisfied)
- Actions (actions to execute during the transition)

### 4. StateHandler<S, E>
Interface to handle state-specific logic:
- `onEnter()` - called when entering the state
- `onExit()` - called when exiting the state
- `onUpdate()` - called periodically while in the state
- `handleEvent()` - handles events without causing transitions

### 5. Event<T>
Interface for events that can carry data.

### 6. NexumException
Specific exception for state machine errors.

## Usage Example

```java
// 1. Define states (can be an Enum or String)
enum DeviceState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    BUSY,
    ERROR
}

// 2. Define events
enum DeviceEvent {
    CONNECT,
    DISCONNECT,
    START_OPERATION,
    OPERATION_COMPLETE,
    ERROR_OCCURRED
}

// 3. Create the state machine
Nexum<DeviceState, DeviceEvent> sm = new Nexum<>(DeviceState.DISCONNECTED);

// 4. Add transitions
sm.addTransition(DeviceState.DISCONNECTED, DeviceState.CONNECTING, DeviceEvent.CONNECT)
  .addTransition(DeviceState.CONNECTING, DeviceState.CONNECTED, DeviceEvent.CONNECT)
  .addTransition(DeviceState.CONNECTED, DeviceState.BUSY, DeviceEvent.START_OPERATION)
  .addTransition(DeviceState.BUSY, DeviceState.CONNECTED, DeviceEvent.OPERATION_COMPLETE)
  .addTransition(DeviceState.CONNECTED, DeviceState.DISCONNECTED, DeviceEvent.DISCONNECT);

// 5. Add transitions with guard conditions
sm.addTransition(
    DeviceState.CONNECTING, 
    DeviceState.CONNECTED, 
    DeviceEvent.CONNECT,
    (context, event, data) -> {
        // Guard: check if connection is valid
        return context.get("connectionValid", Boolean.class) == Boolean.TRUE;
    }
);

// 6. Add transitions with actions
sm.addTransition(
    DeviceState.CONNECTED,
    DeviceState.BUSY,
    DeviceEvent.START_OPERATION,
    null, // no guard
    (context, event, data) -> {
        // Action: save operation in context
        context.put("operation", data);
        context.put("startTime", System.currentTimeMillis());
    }
);

// 7. Register state handlers
sm.registerStateHandler(new StateHandler<DeviceState, DeviceEvent>() {
    @Override
    public DeviceState getState() {
        return DeviceState.CONNECTED;
    }
    
    @Override
    public void onEnter(NexumContext<DeviceState> context, 
                       DeviceState fromState, DeviceEvent event) {
        System.out.println("Device connected!");
        context.put("connectedAt", System.currentTimeMillis());
    }
    
    @Override
    public void onExit(NexumContext<DeviceState> context, 
                      DeviceState toState, DeviceEvent event) {
        System.out.println("Leaving connected state");
    }
});

// 8. Add listeners
sm.addListener(new Nexum.NexumListener<DeviceState, DeviceEvent>() {
    @Override
    public void onStateChanged(DeviceState fromState, DeviceState toState, DeviceEvent event) {
        System.out.println("State changed: " + fromState + " -> " + toState + " (event: " + event + ")");
    }
    
    @Override
    public void onError(Exception error) {
        System.err.println("State machine error: " + error.getMessage());
    }
});

// 9. Start the state machine
sm.start();

// 10. Fire events
try {
    sm.fireEvent(DeviceEvent.CONNECT);
    sm.fireEvent(DeviceEvent.START_OPERATION, "firmware_update");
    sm.fireEvent(DeviceEvent.OPERATION_COMPLETE);
} catch (NexumException e) {
    e.printStackTrace();
}

// 11. Access the context
NexumContext<DeviceState> context = sm.getContext();
String operation = context.get("operation", String.class);
Long startTime = context.get("startTime", Long.class);

// 12. Check current state
DeviceState currentState = sm.getCurrentState();
```

## Example with Existing NexumStatus

```java
// Use existing enum
Nexum<NexumStatus, String> sm = 
    new Nexum<>(NexumStatus.DISCONNECTED);

sm.addTransition(NexumStatus.DISCONNECTED, NexumStatus.CONNECTED, "connect")
  .addTransition(NexumStatus.CONNECTED, NexumStatus.BUSY, "start_work")
  .addTransition(NexumStatus.BUSY, NexumStatus.CONNECTED, "finish_work");

sm.start();
sm.fireEvent("connect");
```

## Features

- **Generic**: Works with any type of state and event (Enum, String, custom classes)
- **Thread-safe**: Uses locks to ensure safety in multi-threaded environments
- **Flexible**: Supports guard conditions and actions
- **Extensible**: State handlers for state-specific logic
- **Observable**: Listeners to monitor state changes and errors
- **Context**: Key-value store to maintain data during transitions
- **Error handling**: Robust error handling with specific exceptions
- **Timer Support**: Schedule events to be fired after a delay or periodically

## Notes

- The state machine must be started with `start()` before firing events
- Transitions are evaluated in the order they were added
- Guard conditions allow you to control whether a transition can be executed
- Actions are executed during the transition, after onExit and before onEnter
- The context can be used to pass data between states and transitions
- Timer events can be scheduled using a custom `TimerService` implementation

## Testing

The project includes a comprehensive suite of unit and integration tests.

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=NexumTest

# Run tests with coverage report
mvn test jacoco:report
```

### Test Structure

```
test/it/kyakan/nexum/
├── NexumContextTest.java      # Tests for context management
├── TransitionTest.java                # Tests for transitions
├── NexumTest.java              # Tests for state machine
├── NexumExceptionTest.java     # Tests for error handling
└── NexumIntegrationTest.java   # Real-world scenario tests
```

### Test Coverage

The tests cover:
- ✅ State management and transitions
- ✅ Guard conditions and actions
- ✅ State handlers (onEnter, onExit, handleEvent)
- ✅ Listeners and notifications
- ✅ Error handling
- ✅ Thread safety
- ✅ Context data management
- ✅ Real-world scenarios (traffic light, door with lock, orders, media player, etc.)
- ✅ Timer-based event scheduling

For more details, see [`test/README.md`](test/README.md).

## Build and Dependencies

### Requirements

- Java 11 or higher
- Maven 3.6+

### Dependencies

The project uses:
- JUnit 5.10.0 for testing
- No runtime dependencies (standalone library)

### Build

```bash
# Compile the project
mvn compile

# Compile and test
mvn test

# Create JAR
mvn package
```

## CI/CD

This project uses GitHub Actions for continuous integration and automated releases.

### Continuous Integration

Every push and pull request triggers:
- Build on Java 11, 17, and 21
- Automated testing
- Artifact generation

### Automated Releases

To create a new release:

```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

This will automatically:
- Update version in pom.xml
- Build and test the project
- Generate changelog from commits
- Create GitHub release with JAR artifact
- Add installation instructions

For detailed release instructions, see [`.github/RELEASE.md`](.github/RELEASE.md).

## Timer Support

The state machine supports scheduling events to be fired after a delay or periodically. This is useful for implementing timeouts, periodic checks, or delayed actions.

### Using TimerService

To use timer functionality, you need to provide an implementation of the `TimerService` interface:

```java
public interface TimerService {
    void scheduleOnce(Runnable task, long delay, TimeUnit unit);
    void schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit);
    void cancel();
}
```

### Example Implementations

#### Java Standard Library Implementation

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JavaTimerService implements TimerService {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void scheduleOnce(Runnable task, long delay, TimeUnit unit) {
        executor.schedule(task, delay, unit);
    }

    @Override
    public void schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
        executor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    @Override
    public void cancel() {
        executor.shutdown();
    }
}
```

#### Android Implementation

```java
import android.os.Handler;
import android.os.Looper;

public class AndroidTimerService implements TimerService {
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void scheduleOnce(Runnable task, long delay, TimeUnit unit) {
        handler.postDelayed(task, unit.toMillis(delay));
    }

    @Override
    public void schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
        // Note: This is a simplified implementation
        // For true periodic execution, consider using a Handler with repeated postDelayed
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                task.run();
                handler.postDelayed(this, unit.toMillis(period));
            }
        }, unit.toMillis(initialDelay));
    }

    @Override
    public void cancel() {
        handler.removeCallbacksAndMessages(null);
    }
}
```

### Using Timer in State Machine

```java
// 1. Create a timer service implementation
TimerService timerService = new JavaTimerService();

// 2. Create state machine with timer service
Nexum<DeviceState, DeviceEvent> sm = new Nexum<>(DeviceState.DISCONNECTED, timerService);

// 3. Schedule events
sm.scheduleEvent(DeviceEvent.CONNECT, 5, TimeUnit.SECONDS); // Fire CONNECT after 5 seconds
sm.schedulePeriodicEvent(DeviceEvent.HEARTBEAT, 0, 1, TimeUnit.MINUTES); // Fire HEARTBEAT every minute

// 4. Start the state machine
sm.start();
```

### Notes

- The timer service is optional. If not provided, timer-related methods will throw `IllegalStateException`.
- You can set or change the timer service at any time using `setTimerService()`.
- Remember to call `cancel()` on your timer service when it's no longer needed to free resources.

## License

See [LICENSE](LICENSE) file for details.
