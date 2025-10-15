# Docker Environment for StateMachine

Docker environment for development and testing of the StateMachine project without installing dependencies on the host machine.

## Prerequisites

Only Docker and Docker Compose:
```bash
# Verify installation
docker --version
docker-compose --version
```

## Available Services

### 1. Test - Run all tests
```bash
cd docker
docker-compose up test
```

### 2. Dev - Interactive development environment
```bash
cd docker
docker-compose up -d dev
docker-compose exec dev bash

# Inside the container:
mvn test                    # Run tests
mvn compile                 # Compile
mvn clean package           # Create JAR
```

### 3. Test Single - Run a specific test
```bash
cd docker
TEST_CLASS=StateMachineTest docker-compose up test-single

# Other examples:
TEST_CLASS=TransitionTest docker-compose up test-single
TEST_CLASS=StateMachineContextTest docker-compose up test-single
```

### 4. Coverage - Generate coverage report
```bash
cd docker
docker-compose up coverage

# Report will be in: ../target/site/jacoco/index.html
```

### 5. Build - Compile and create JAR
```bash
cd docker
docker-compose up build

# JAR will be in: ../target/nexum-1.0.0.jar
```

## Quick Commands

### Run tests
```bash
cd docker
docker-compose up test
```

### Interactive development
```bash
cd docker
docker-compose up -d dev
docker-compose exec dev bash

# Available commands in container:
mvn test                           # Tests
mvn test -Dtest=StateMachineTest  # Specific test
mvn compile                        # Compile
mvn clean                          # Clean
tree src test                      # View structure
```

### Stop containers
```bash
docker-compose down
```

### Rebuild images
```bash
docker-compose build --no-cache
```

### Clean everything (containers, images, volumes)
```bash
docker-compose down -v
docker system prune -a
```

## Structure

```
docker/
├── Dockerfile           # Image with Java 11 and Maven
├── docker-compose.yml   # Services configuration
└── README.md           # This guide
```

## Volumes

Volumes are configured for:
- **Source code**: Immediate changes without rebuild
- **Maven cache**: Speed up subsequent builds
- **Target**: Access to compiled files and reports

## Environment Variables

- `MAVEN_OPTS`: JVM options for Maven (default: `-Xmx512m`)
- `TEST_CLASS`: Test class to run (for test-single)

## Usage Examples

### Typical development workflow

1. **Start dev environment**:
```bash
cd docker
docker-compose up -d dev
```

2. **Enter the container**:
```bash
docker-compose exec dev bash
```

3. **Work in the container**:
```bash
# Edit files (with host editor)
# Run tests
mvn test

# Specific test
mvn test -Dtest=StateMachineTest

# Compile
mvn compile
```

4. **Stop when done**:
```bash
exit
docker-compose down
```

### Run tests quickly

```bash
cd docker
docker-compose up test
```

### Generate coverage report

```bash
cd docker
docker-compose up coverage
# Open: ../target/site/jacoco/index.html
```

### Build the project

```bash
cd docker
docker-compose up build
# JAR in: ../target/nexum-1.0.0.jar
```

## Troubleshooting

### Tests fail
```bash
# Rebuild the image
docker-compose build --no-cache test

# Check logs
docker-compose logs test
```

### Permission issues
```bash
# Add user to docker group
sudo usermod -aG docker $USER
# Logout and login
```

### Corrupted Maven cache
```bash
# Remove the volume
docker-compose down -v
docker volume rm statemachine-maven-cache
```

### Container won't start
```bash
# Check logs
docker-compose logs dev

# Check status
docker-compose ps
```

## Performance

### First execution
- Base image download: ~2-3 minutes
- Maven dependencies download: ~1-2 minutes
- Project build: ~30 seconds

### Subsequent executions
- Container startup: ~5 seconds
- Test execution: ~10-20 seconds

### Optimizations
- Persistent Maven cache (volume)
- Cached Docker layers
- Pre-downloaded dependencies

## Notes

- **Hot reload**: File changes are immediate (mounted volumes)
- **Isolation**: No installation on host machine
- **Portability**: Works on any system with Docker
- **Reproducibility**: Identical environment for all developers

## Useful Commands

```bash
# See active containers
docker-compose ps

# See logs
docker-compose logs -f test

# Run custom command
docker-compose run --rm dev mvn clean compile

# Access running container
docker-compose exec dev bash

# Remove everything
docker-compose down -v --rmi all
```

## CI/CD Integration

Example for GitHub Actions:
```yaml
- name: Run tests in Docker
  run: |
    cd docker
    docker-compose up test
```

Example for GitLab CI:
```yaml
test:
  image: docker/compose:latest
  script:
    - cd docker
    - docker-compose up test