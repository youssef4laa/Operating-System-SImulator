# OS Simulator with GUI

A comprehensive **Operating System Simulator** implemented in Java with a modern JavaFX GUI. Simulate CPU scheduling algorithms, memory management, process synchronization, and system calls in real-time.

## Features

✨ **Core Functionality**
- **Multiple Scheduling Algorithms**: Round Robin (RR), Highest Response Ratio Next (HRRN), Multi-Level Feedback Queue (MLFQ)
- **Memory Management**: Real-time visualization of memory allocation and process layout
- **Process Synchronization**: Mutex and semaphore implementation with deadlock detection
- **System Calls**: Complete system call subsystem with interrupt handling
- **GUI Visualization**: Interactive real-time visualization of OS components

🎨 **GUI Features**
- Real-time memory grid visualization
- Process queue monitoring
- Debug console with timestamped output
- Step-by-step execution mode for educational purposes
- Automatic execution mode for testing
- Algorithm comparison capabilities
- Timeline and statistics tracking

📊 **Educational Value**
- Learn how CPU scheduling affects process execution
- Understand memory management and allocation strategies
- Explore mutual exclusion and synchronization primitives
- Study system call mechanisms and interrupt handling
- Visualize context switching and process states

## Quick Start

### Prerequisites
- **Java**: 11 or higher (17+ recommended)
- **JavaFX**: 17 or higher
- **Memory**: 4GB minimum
- **Screen**: 1400x900 minimum resolution

### Installation

#### 1. Install JavaFX

**macOS (with Homebrew - Recommended)**:
```bash
brew install javafx-sdk
export JAVAFX_PATH_ENV=$(brew --cellar javafx-sdk)/*/libexec
```

**macOS/Linux/Windows (Manual)**:
1. Download from [GluonHQ](https://gluonhq.com/products/javafx/)
2. Extract to a location
3. Set environment variable:
   ```bash
   export JAVAFX_PATH_ENV=/path/to/javafx-sdk
   ```

Make the export permanent by adding it to your shell config (`~/.bashrc`, `~/.zprofile`, etc.).

#### 2. Build and Run

```bash
cd os
bash build-gui.sh
```

The GUI will launch automatically.

#### 3. Quick Test

1. Click **[Initialize]** button
2. Click **[Start]** button
3. Click **[Step]** button to execute one instruction
4. Observe changes in memory, queues, and debug console

## Project Structure

```
OSProject/
├── os/
│   ├── src/os/                    # Java source code
│   │   ├── Main.java              # Entry point
│   │   ├── Interpreter.java       # OS simulation engine
│   │   ├── MemoryManager.java     # Memory management
│   │   ├── ProcessScheduler.java  # CPU scheduling
│   │   ├── MutexManager.java      # Synchronization
│   │   ├── SystemCallHandler.java # System call handling
│   │   ├── GUIController.java     # GUI logic
│   │   └── ...                    # Other components
│   ├── bin/                       # Compiled class files (generated)
│   └── build-gui.sh               # Build script
│
├── README.md                      # This file
├── START_HERE.md                  # Quick start guide
├── GUI_QUICK_REFERENCE.md         # GUI buttons and features
├── GUI_USER_GUIDE.md              # Comprehensive GUI documentation
├── README_GUI.md                  # Technical GUI architecture
├── JAVAFX_SETUP.md                # JavaFX installation help
├── DOCUMENTATION_INDEX.md         # Documentation guide
│
├── MLFQ_MUTEX_IMPLEMENTATION.md   # MLFQ & Mutex details
├── MLFQ_MUTEX_COMPLETION_SUMMARY.md
├── MLFQ_MUTEX_CHANGELOG.md        # Implementation history
├── SYSTEM_CALL_IMPLEMENTATION.md  # System call subsystem
├── MUTEX_IMPLEMENTATION.md        # Mutex implementation details
└── .gitignore                     # Git ignore rules
```

## Documentation

Start with these files in order:

1. **[START_HERE.md](START_HERE.md)** - Quick 5-minute setup guide
2. **[GUI_QUICK_REFERENCE.md](GUI_QUICK_REFERENCE.md)** - Button descriptions and quick tips
3. **[GUI_USER_GUIDE.md](GUI_USER_GUIDE.md)** - Detailed feature guide and workflows
4. **[README_GUI.md](README_GUI.md)** - Technical architecture details
5. **[DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)** - Complete documentation guide

### Feature-Specific Documentation

- **Scheduling**: See MLFQ_MUTEX_IMPLEMENTATION.md
- **Memory Management**: See README_GUI.md
- **System Calls**: See SYSTEM_CALL_IMPLEMENTATION.md
- **Synchronization**: See MUTEX_IMPLEMENTATION.md
- **JavaFX Setup Issues**: See JAVAFX_SETUP.md

## Usage

### GUI Mode (Recommended)
```bash
cd os
bash build-gui.sh
```

### CLI Mode (Console Only)
```bash
cd os
java -cp bin os.Main -cli
```

### Running Specific Programs
The simulator can execute test programs:
```bash
java -cp bin os.Main Program1.txt
```

## Scheduling Algorithms

### 1. Round Robin (RR)
- Time slice (quantum) based scheduling
- Processes take turns in a circular queue
- Fair distribution of CPU time

### 2. Highest Response Ratio Next (HRRN)
- Minimizes average waiting time
- Calculates priority based on response ratio
- Non-preemptive scheduling

### 3. Multi-Level Feedback Queue (MLFQ)
- Multiple priority queues
- Dynamic priority adjustment
- Balances responsiveness and fairness

## System Components

### Memory Management
- Dynamic memory allocation
- Process layout visualization
- Memory fragmentation tracking
- Allocation/deallocation operations

### Process Scheduling
- Process creation and termination
- Context switching
- Priority management
- Waiting queue management

### Synchronization (Mutex)
- Mutual exclusion primitives
- Lock acquisition and release
- Deadlock detection
- Priority inheritance support

### System Calls
- Standard system call interface
- Interrupt handling
- System call tracing
- Performance metrics

## Troubleshooting

### "JavaFX not found" Error
```bash
# Check environment variable
echo $JAVAFX_PATH_ENV

# Should show: /path/to/javafx-sdk

# If empty, set it:
export JAVAFX_PATH_ENV=$(brew --cellar javafx-sdk)/*/libexec
```

### "Module javafx.controls not found"
- Reinstall JavaFX (see Installation section)
- Verify JAVAFX_PATH_ENV is set correctly

### Build Fails
```bash
# Clear previous builds
rm -rf bin

# Make script executable
chmod +x build-gui.sh

# Try again
bash build-gui.sh
```

### GUI Won't Launch
- Check Java version: `java --version` (should be 11+)
- Check screen resolution (minimum 1400x900)
- Check available memory

For more help, see [JAVAFX_SETUP.md](JAVAFX_SETUP.md) or [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md).

## Development

### Building from Source
```bash
cd os
javac -d bin -cp "$JAVAFX_PATH_ENV/lib/*:." src/os/*.java
java --module-path $JAVAFX_PATH_ENV/lib --add-modules javafx.controls -cp bin os.Main
```

### Code Structure
- **Main.java**: Entry point and initialization
- **Interpreter.java**: Core simulation engine
- **MemoryManager.java**: Memory subsystem
- **ProcessScheduler.java**: Scheduling logic
- **MutexManager.java**: Synchronization primitives
- **GUIController.java**: GUI event handling
- **Memory Visualization**: Real-time graphics components

### Adding New Features
1. Extend the appropriate manager class
2. Add GUI controls in GUIController
3. Update documentation
4. Test with multiple scheduling algorithms

## System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| Java | 11 | 17+ |
| JavaFX | 17 | Latest |
| RAM | 4GB | 8GB+ |
| Screen | 1400x900 | 1920x1080+ |
| OS | macOS, Linux, Windows | Any |

## Performance

The simulator handles:
- Up to 100+ processes simultaneously
- Real-time visualization at 60 FPS
- Complex synchronization scenarios
- Long execution traces

## Contributing

This is an educational project. Contributions and improvements are welcome:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Update documentation
5. Submit a pull request

## License

[Specify your license here - e.g., MIT, GPL, etc.]

## Author

Created by Yossif Alaa

## Support

- **Questions about features?** → Check [GUI_QUICK_REFERENCE.md](GUI_QUICK_REFERENCE.md)
- **Installation issues?** → See [JAVAFX_SETUP.md](JAVAFX_SETUP.md)
- **Looking for something?** → [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)
- **Want to understand internals?** → [README_GUI.md](README_GUI.md)

## Changelog

See [MLFQ_MUTEX_CHANGELOG.md](MLFQ_MUTEX_CHANGELOG.md) for detailed version history.

### Recent Updates
- ✅ Complete GUI implementation with JavaFX
- ✅ MLFQ scheduling algorithm
- ✅ Mutex and synchronization primitives
- ✅ System call subsystem
- ✅ Memory visualization
- ✅ Debug console with real-time output

---

**Ready to simulate?** Start with [START_HERE.md](START_HERE.md) 🚀
