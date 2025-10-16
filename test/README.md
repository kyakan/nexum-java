# Nexum Unit Tests

This directory contains comprehensive unit and integration tests for the Nexum library.

## Test Structure

```
test/it/disionira/nexum/
├── NexumContextTest.java      # Tests for context management
├── TransitionTest.java                # Tests for transitions
├── NexumTest.java              # Tests for state machine core
├── NexumExceptionTest.java     # Tests for exception handling
└── NexumIntegrationTest.java   # Real-world scenario tests
```

## Test Coverage

### NexumContextTest
Tests the context class that holds state and data:
- Initial state management
- State transitions and history
- Data storage and retrieval (typed and untyped)
- Error tracking
- Context clearing and resetting

### TransitionTest
Tests transition behavior:
- Simple transitions
- Transitions with guards (conditional logic)
- Transitions with actions
- Event matching
- Data propagation through transitions

### NexumTest
Tests the core state machine functionality:
- State machine initialization and startup
- Event firing and state transitions
- State handlers (onEnter, onExit, handleEvent)
- Listeners and notifications
- Error handling
- Thread safety
- Reset functionality

### NexumExceptionTest
Tests exception handling:
- Exception creation with various constructors
- State and event context in exceptions
- Error message formatting
- Cause preservation

### NexumIntegrationTest
Real-world scenario tests demonstrating:
- **Traffic Light**: Simple cyclic state machine
- **Door with Lock**: State machine with guards (key validation)
- **Order Processing**: Complex workflow with state handlers
- **Media Player**: State machine with actions and context data
- **Vending Machine**: Guards for business logic (balance checking)
- **Connection with Retry**: Error handling and retry logic

## Running Tests

### Using Maven

Run all tests:
```bash
mvn test
```

Run specific test class:
```bash
mvn test -Dtest=NexumTest
```

Run with coverage:
```bash
mvn test jacoco:report
```

### Using IDE

Most Java IDEs (IntelliJ IDEA, Eclipse, VS Code with Java extensions) can run JUnit 5 tests directly:
1. Right-click on a test class or method
2. Select "Run Test" or "Debug Test"

## Test Requirements

- Java 11 or higher
- JUnit 5.10.0
- Maven 3.6+ (for building)

## Writing New Tests

When adding new tests:

1. **Follow naming conventions**: Test methods should clearly describe what they test
2. **Use @DisplayName**: Provide human-readable test descriptions
3. **Test one thing**: Each test should verify a single behavior
4. **Use appropriate assertions**: Choose the right assertion for clarity
5. **Clean up**: Use @BeforeEach and @AfterEach for setup/teardown

Example:
```java
@Test
@DisplayName("Should transition from IDLE to RUNNING on START event")
void testSimpleTransition() throws NexumException {
    Nexum
        .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
        .start();
    
    Nexum.fireEvent(TestEvent.START);
    
    assertEquals(TestState.RUNNING, Nexum.getCurrentState());
}
```

## Test Patterns Used

- **Arrange-Act-Assert (AAA)**: Clear test structure
- **Builder Pattern**: Fluent API for state machine configuration
- **Test Doubles**: Using lambdas for guards and actions
- **State Verification**: Checking state after operations
- **Behavior Verification**: Using flags and lists to verify callbacks

## Common Test Scenarios

### Testing Guards
```java
Nexum.addTransition(FROM, TO, EVENT,
    (ctx, event, data) -> {
        // Guard logic
        return condition;
    });
```

### Testing Actions
```java
Nexum.addTransition(FROM, TO, EVENT, null,
    (ctx, event, data) -> {
        // Action logic
        ctx.put("key", value);
    });
```

### Testing State Handlers
```java
StateHandler<State, Event> handler = new StateHandler<>() {
    @Override
    public State getState() { return STATE; }
    
    @Override
    public void onEnter(NexumContext<State> ctx, State from, Event event) {
        // Entry logic
    }
};
```

### Testing Listeners
```java
Nexum.addListener((from, to, event) -> {
    // Listener logic
});
```

## Troubleshooting

### Tests not running
- Ensure JUnit 5 dependencies are in pom.xml
- Check Java version (must be 11+)
- Verify test directory structure matches package names

### Compilation errors
- Run `mvn clean compile` to rebuild
- Check that source files are in `src/` and tests in `test/`

### Flaky tests
- Thread safety tests may occasionally fail due to timing
- Increase timeouts if needed
- Ensure proper synchronization in concurrent tests