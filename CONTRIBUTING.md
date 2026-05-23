# Contributing to OS Simulator

Thank you for your interest in contributing to the OS Simulator project! This document provides guidelines and instructions for contributing.

## Code of Conduct

We are committed to providing a welcoming and inclusive environment for all contributors. Please be respectful and constructive in all interactions.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally
3. **Create a branch** for your feature or fix
4. **Make your changes** and test thoroughly
5. **Submit a pull request** with a clear description

## Development Setup

### Prerequisites
- Java 11 or higher (17+ recommended)
- JavaFX SDK 17 or higher
- Git

### Building the Project

```bash
# Navigate to the os directory
cd os

# Build the project
bash build-gui.sh

# Or manually compile
javac -d bin -cp "$JAVAFX_PATH_ENV/lib/*:." src/os/*.java
```

### Running Tests

```bash
# Run in CLI mode with test program
java -cp bin os.Main Program1.txt

# Run GUI mode
bash build-gui.sh
```

## Code Style Guidelines

### Java Code Style
- Use **4 spaces** for indentation (no tabs)
- Follow **camelCase** for variable and method names
- Use **PascalCase** for class names
- Keep lines under **100 characters** where possible
- Add meaningful comments for complex logic

### Naming Conventions
```java
// Class names
public class MemoryManager { }

// Method names (camelCase, descriptive)
private void allocateMemory(int size) { }

// Constant names (UPPER_SNAKE_CASE)
private static final int DEFAULT_TIME_QUANTUM = 4;

// Variable names (camelCase)
int currentProcessId = 0;
```

### Comments
- Use `//` for single-line comments
- Use `/* */` for multi-line comments
- Write comments that explain *why*, not *what*
- Keep comments up-to-date with code changes

### Example
```java
// Good comment - explains why
// Use MLFQ algorithm for better response time
ProcessScheduler scheduler = new MLFQScheduler();

// Avoid - explains obvious implementation
// Get the current process
Process current = processes.get(0);
```

## Commit Guidelines

### Commit Message Format
```
[Type] Brief description (50 chars max)

Longer explanation if needed (72 chars per line)
- Bullet points OK
- Reference issues: Closes #123
```

### Types
- `[Feature]` - New feature
- `[Fix]` - Bug fix
- `[Docs]` - Documentation only
- `[Refactor]` - Code refactoring
- `[Test]` - Adding or updating tests
- `[Perf]` - Performance improvement

### Examples
```
[Feature] Add process priority preemption to MLFQ

Implements age-based priority adjustment to prevent
starvation of lower-priority processes.

[Fix] Correct memory allocation boundary check

Fixes issue where memory allocation could exceed
available space due to off-by-one error in size
validation.

[Docs] Update GUI quick reference guide

Added section on advanced debugging features and
included screenshots.
```

## Pull Request Process

### Before Submitting
1. **Update documentation** - Modify README.md, relevant guides, or add new docs
2. **Test thoroughly** - Run the simulator with your changes
3. **Check code style** - Follow the style guidelines above
4. **Keep commits clean** - Make logical, atomic commits
5. **Update changelog** - Add entry to MLFQ_MUTEX_CHANGELOG.md

### PR Description Template
```markdown
## Description
Briefly describe what this PR does.

## Type of Change
- [ ] New feature
- [ ] Bug fix
- [ ] Documentation
- [ ] Performance improvement
- [ ] Refactoring

## Testing
Describe how you tested your changes:
- [ ] Tested with Round Robin scheduler
- [ ] Tested with HRRN scheduler
- [ ] Tested with MLFQ scheduler
- [ ] GUI components working correctly

## Documentation
- [ ] Updated relevant markdown files
- [ ] Updated code comments
- [ ] Added/updated CHANGELOG entry

## Screenshots (if applicable)
Show before/after if this is a visual change.

## Checklist
- [ ] Code follows style guidelines
- [ ] Comments added for complex logic
- [ ] No new warnings generated
- [ ] All tests pass
- [ ] Documentation updated
```

## Areas for Contribution

### High Priority
- [ ] Additional scheduling algorithms (Earliest Deadline First, etc.)
- [ ] Performance optimizations
- [ ] Enhanced memory visualization
- [ ] Additional system calls
- [ ] Improved error handling

### Medium Priority
- [ ] Better documentation with examples
- [ ] Additional test programs
- [ ] GUI usability improvements
- [ ] Code refactoring for clarity
- [ ] Platform compatibility testing

### Documentation
- [ ] Tutorial articles
- [ ] Video walkthroughs
- [ ] Algorithm explanations
- [ ] FAQ expansion
- [ ] Example programs

## Reporting Issues

### Bug Reports
Include:
- **Title**: Clear, specific description
- **Environment**: Java version, OS, JavaFX version
- **Steps to Reproduce**: Numbered steps
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happens
- **Screenshots**: If applicable
- **Error Messages**: Full stack traces

### Feature Requests
Include:
- **Description**: Clear explanation of feature
- **Use Case**: Why it's needed
- **Proposed Solution**: How to implement
- **Alternatives**: Other approaches considered

## Review Process

1. **Maintainer Review** - Code review by project maintainers
2. **Feedback** - Address any requested changes
3. **Approval** - PR approved by maintainer
4. **Merge** - Changes merged to main branch

Feedback will be constructive and focused on code quality and project goals.

## Project Structure

```
os/
├── src/os/
│   ├── Main.java              # Entry point
│   ├── Interpreter.java       # Core engine
│   ├── MemoryManager.java     # Memory subsystem
│   ├── ProcessScheduler.java  # Scheduling
│   ├── MutexManager.java      # Synchronization
│   ├── SystemCallHandler.java # System calls
│   └── GUI*.java              # GUI components
├── bin/                       # Compiled classes
└── build-gui.sh              # Build script
```

## Useful Commands

```bash
# Compile without running
javac -d bin -cp "$JAVAFX_PATH_ENV/lib/*:." src/os/*.java

# Clean build
rm -rf bin && mkdir -p bin && bash build-gui.sh

# Run with debugging
java -cp bin -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 os.Main

# Check Java version
java -version

# Check JAVAFX_PATH
echo $JAVAFX_PATH_ENV
```

## Questions?

- **How do I...?** → Check [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)
- **I found a bug** → Create an issue with details
- **Feature idea** → Open a discussion or issue
- **Technical question** → See [README_GUI.md](README_GUI.md)

## Recognition

Contributors will be recognized in:
- This file (contributors list)
- Release notes
- Project documentation

Thank you for contributing! 🎉
