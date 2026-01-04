# Contributing to Cadence

Thank you for considering contributing to Cadence.

## Development Setup

### Prerequisites
- Java 21 or higher
- Gradle 8.x (or use included wrapper)

### Building
```bash
git clone https://github.com/SpaceLeam/cadence.git
cd cadence
./gradlew build
```

### Running Tests
```bash
./gradlew test
```

### Running Tests with Coverage
```bash
./gradlew test jacocoTestReport
```
Coverage report will be at `build/reports/jacoco/test/html/index.html`

## Code Style

- Use 4 spaces for indentation
- Follow standard Java naming conventions
- Add Javadoc for public APIs
- Keep methods focused and small

## Submitting Changes

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass (`./gradlew test`)
6. Commit with clear message
7. Push to your fork
8. Open a Pull Request

## Reporting Issues

When reporting bugs, please include:
- Java version
- Steps to reproduce
- Expected vs actual behavior
- Stack trace (if applicable)
