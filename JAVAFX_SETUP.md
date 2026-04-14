# JavaFX Setup Guide for OS Simulator

## Overview
The Operating System Simulator GUI requires JavaFX 17+ to run. This guide will help you set up JavaFX on your system.

## System Requirements
- Java 11 or higher (Java 17+ recommended)
- JavaFX 17 or higher
- At least 4GB RAM
- Modern web browser (for any web-based components)

## Installation Instructions

### Option 1: Using Homebrew (Recommended for macOS)

1. **Install Homebrew** (if not already installed):
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

2. **Install JavaFX via Homebrew**:
   ```bash
   brew install javafx-sdk
   ```

3. **Set Environment Variable**:
   ```bash
   export JAVAFX_PATH_ENV=$(brew --cellar javafx-sdk)/*/libexec
   ```
   
   To make this permanent, add to your shell configuration file (`~/.zprofile`, `~/.bashrc`, etc.):
   ```bash
   export JAVAFX_PATH_ENV=$(brew --cellar javafx-sdk)/*/libexec
   ```

### Option 2: Manual Installation (All Platforms)

1. **Download JavaFX SDK**:
   - Visit: https://gluonhq.com/products/javafx/
   - Download the latest version (17.0.1 or later)
   - Choose the version matching your OS (macOS, Windows, Linux)

2. **Extract to a known location**:
   ```bash
   # On macOS/Linux
   mkdir -p ~/.javafx
   unzip javafx-sdk-*.zip -d ~/.javafx/
   ```

3. **Set Environment Variable**:
   ```bash
   export JAVAFX_PATH_ENV=$HOME/.javafx/javafx-sdk-<version>
   ```

4. **Make it permanent**:
   Add the above line to your shell configuration file (`~/.zprofile`, `~/.bashrc`, etc.)

### Option 3: Using Docker (Alternative)

If you prefer a containerized environment:

```dockerfile
FROM openjdk:17-slim
RUN apt-get update && apt-get install -y \
    openjfx && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .
RUN javac -d bin src/os/*.java
CMD ["java", "-cp", "bin", "os.OSSimulatorGUI"]
```

## Verification

After installation, verify JavaFX is properly set up:

```bash
# Check if environment variable is set
echo $JAVAFX_PATH_ENV

# Verify JavaFX files exist
ls -la $JAVAFX_PATH_ENV/lib/
```

You should see files like:
- `javafx-base-*.jar`
- `javafx-controls-*.jar`
- `javafx-fxml-*.jar`
- `javafx-graphics-*.jar`

## Building and Running the GUI

### Quick Start (if JavaFX is installed):

```bash
cd /Users/youssef/Code/Operating\ Systems/OSProject/os

# Compile
bash build-gui.sh
```

### Manual Build:

```bash
cd os

# Compile
javac \
  --module-path $JAVAFX_PATH_ENV/lib \
  --add-modules javafx.controls,javafx.fxml \
  -d bin \
  src/os/*.java

# Run
java \
  --module-path $JAVAFX_PATH_ENV/lib \
  --add-modules javafx.controls,javafx.fxml \
  -cp bin \
  os.OSSimulatorGUI
```

## Troubleshooting

### Issue: "JavaFX SDK not found"

**Solution:**
1. Verify installation location
2. Set `JAVAFX_PATH_ENV` environment variable
3. Check that the path exists: `ls $JAVAFX_PATH_ENV/lib/`

### Issue: "module javafx.controls not found"

**Solution:**
- Ensure you're using the `--module-path` flag during compilation
- Verify JavaFX SDK version (should be 17+)

### Issue: "Class not found" errors

**Solution:**
- Regenerate the build system:
  ```bash
  cd os
  rm -rf bin
  mkdir -p bin
  bash build-gui.sh
  ```

### Issue: GUI doesn't display on macOS

**Solution:**
Some versions of Java on macOS have display issues. Try:
```bash
java \
  --module-path $JAVAFX_PATH_ENV/lib \
  --add-modules javafx.controls,javafx.fxml \
  -cp bin \
  -XstartOnFirstThread \
  os.OSSimulatorGUI
```

## GUI Features

Once running, the GUI provides:

✅ **Real-time System State Visualization**
- Memory layout with color-coded allocations
- Ready and Blocked process queues
- Current process information

✅ **Easy Debugging**
- Debug console with captured system output
- Timeline showing all events
- Execution control buttons

✅ **Scheduling Algorithm Selection**
- Round Robin (RR)
- Highest Response Ratio Next (HRRN)
- Multi-Level Feedback Queue (MLFQ)

✅ **Execution Modes**
- Step-through mode for detailed analysis
- Automatic mode with adjustable speed
- Pause/Resume controls

✅ **System Monitoring**
- Mutex status display
- System call statistics
- Memory usage tracking
- Clock cycle counter

## Alternative: CLI Mode

If you don't want to use the GUI, run in command-line mode:

```bash
cd os
javac -d bin src/os/*.java
java -cp bin os.Main -cli
```

This will run the simulation without GUI, with output to console.

## Performance Notes

- **Recommended**: Run on machines with 4GB+ RAM
- **Optimal**: Java 17+ for best performance
- **Speed Control**: Use the GUI speed slider to slow down for analysis

## Getting Help

If you encounter issues:

1. Check that `$JAVAFX_PATH_ENV` is set correctly
2. Verify Java version: `java --version`
3. Look at `build.log` after running build script
4. Check console output for specific error messages

## References

- JavaFX Official: https://www.javafxonline.com/
- Gluon JavaFX SDK: https://gluonhq.com/products/javafx/
- Java Modules: https://docs.oracle.com/en/java/javase/17/
