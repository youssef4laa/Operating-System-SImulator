# OS Simulator GUI - Implementation & User Guide

## 📋 Table of Contents
1. [Overview](#overview)
2. [Features](#features)
3. [System Architecture](#system-architecture)
4. [Installation](#installation)
5. [Usage](#usage)
6. [Components](#components)
7. [Debugging](#debugging)
8. [Troubleshooting](#troubleshooting)

## Overview

The Operating System Simulator GUI is a comprehensive JavaFX-based visualization tool designed for the CSEN 602 Operating Systems course. It provides **real-time visualization** and **easy debugging** of a multi-process OS simulator that implements:

- ✅ 3 scheduling algorithms (RR, HRRN, MLFQ)
- ✅ Process management with PCBs
- ✅ Memory management (40 words)
- ✅ 3 system mutexes for resource control
- ✅ 6 system calls
- ✅ Real-time debug output
- ✅ Step-by-step execution

## Features

### 🎨 Visual Components

1. **Memory Visualization**
   - 40-word grid with color coding
   - Real-time updates
   - Shows PCBs, instructions, and data
   - Memory allocation tracking

2. **Queue Management**
   - Ready Queue display (processes waiting to run)
   - Blocked Queue display (waiting for resources)
   - Finished Queue (completed processes)
   - Visual cards with Process IDs and states

3. **Current Process Panel**
   - Process ID and state
   - Program counter
   - Memory boundaries
   - Arrival time and remaining time
   - Full instruction list and symbol table

4. **Debug Console**
   - Real-time system output capture
   - Timestamped log entries
   - Color-coded messages
   - Up to 1000 line history
   - Full system visibility

5. **Execution Timeline**
   - Clock cycle counter
   - Instructions executed counter
   - Memory usage display
   - Process count statistics
   - Event log

6. **System Monitoring**
   - Mutex status (3 resources)
   - System call statistics
   - Success/failure rates
   - Resource owner tracking

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    OSSimulatorGUI (JavaFX)                  │
│                    Main application window                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ├──> SimulationEngine
                           │    └─ Manages execution state
                           │    └─ Handles step-by-step logic
                           │    └─ Loads processes from files
                           │
                           ├──> DebugConsole
                           │    └─ Captures System.out
                           │    └─ Displays with timestamps
                           │    └─ Redirects stderr
                           │
                           ├──> Scheduler
                           │    └─ Process selection
                           │    └─ Algorithm implementation
                           │    └─ Queue management
                           │
                           ├──> Memory
                           │    └─ 40-word memory model
                           │    └─ Allocation/deallocation
                           │    └─ Memory protection
                           │
                           └──> Visualization Panels
                                ├─ MemoryVisualization
                                ├─ QueueVisualization
                                ├─ CurrentProcessPanel
                                ├─ TimelinePanel
                                ├─ MutexStatusPanel
                                └─ SystemCallStatsPanel
```

### Class Hierarchy

```
SimulationEngine
├─ Manages simulation state
├─ Controls execution flow
├─ Loads processes
├─ Implements step logic
└─ Notifies GUI of changes

DebugConsole (extends VBox)
├─ Captures print streams
├─ Formats with timestamps
├─ Manages line limit
└─ Provides clear() method

OSSimulatorGUI (extends Application)
├─ Creates UI layout
├─ Manages panels
├─ Handles user input
├─ Updates displays
└─ Manages timeline animation

Visualization Panels
├─ MemoryVisualization - Memory grid display
├─ QueueVisualization - Queue display
├─ CurrentProcessPanel - Process details
├─ TimelinePanel - Statistics
├─ MutexStatusPanel - Mutex state
└─ SystemCallStatsPanel - System call stats
```

## Installation

### Prerequisites
- Java 11 or higher (Java 17+ recommended)
- JavaFX 17 or higher
- 4GB RAM minimum

### Step 1: Install JavaFX

**macOS with Homebrew (Recommended):**
```bash
brew install javafx-sdk
export JAVAFX_PATH_ENV=$(brew --cellar javafx-sdk)/*/libexec
```

**Manual Installation:**
1. Download from: https://gluonhq.com/products/javafx/
2. Extract to `~/.javafx/javafx-sdk/`
3. Set: `export JAVAFX_PATH_ENV=$HOME/.javafx/javafx-sdk`

See [JAVAFX_SETUP.md](JAVAFX_SETUP.md) for detailed instructions.

### Step 2: Build the Project

```bash
cd /path/to/OSProject/os
bash build-gui.sh
```

## Usage

### Quick Start

```bash
# In the os/ directory
bash build-gui.sh
```

### Basic Workflow

1. **Initialize First**
   - Click [Initialize] button
   - Select algorithm if needed
   - Check Debug Console for messages

2. **Choose Execution Mode**
   - **Step Mode** (default): Click "Step" button for each instruction
   - **Auto Mode**: Set speed slider, click "Start"

3. **Monitor Execution**
   - Watch Memory grid update
   - Observe queue changes
   - Read Debug Console output
   - Check Timeline statistics

4. **Pause and Inspect**
   - Pause at any time
   - Use Step mode for fine-grained analysis
   - Check process details in tabs

### Example Session

```
╔═════════════════════════════════════════════════════════════╗
║ Step-by-Step Learning Session                              ║
╚═════════════════════════════════════════════════════════════╝

1. Launch GUI
   $ bash build-gui.sh
   
2. Initialize
   • Click [Initialize]
   • Algorithm: RR (default)
   • Debug Console shows:
     "[SYSCALL] Simulation Initialized"
   
3. Start Execution
   • Click [Start]
   • Status → "Running"
   
4. Execute Instructions
   • Click [Step]
   • Observe:
     - Memory grid updates
     - PC advances in current process
     - Debug Console logs instruction
   • Repeat [Step]
   
5. Pause & Examine
   • Click [Pause] if in Auto mode
   • Check Timeline Tab for stats
   • Review Process Tab details
   
6. Continue or Reset
   • Click [Resume] to continue
   • Click [Reset] to restart
```

## Components

### OSSimulatorGUI
**Purpose**: Main application window and UI layout
**Key Features**:
- Creates all UI panels
- Manages execution control
- Updates displays
- Handles events
- Integrates SimulationEngine

**Main Methods**:
```java
start(Stage) - Initialize application
createMainLayout() - Build UI
startExecution() - Begin simulation
executeOneStep() - Execute one instruction
pauseExecution() - Pause simulation
resetSimulation() - Clear everything
updateAll() - Refresh all displays
```

### SimulationEngine
**Purpose**: Manages simulation state and execution
**Key Features**:
- Step-based execution model
- Process creation and lifecycle
- Clock cycle tracking
- State persistence
- Listener notifications

**Key Methods**:
```java
initialize(String algorithm) - Setup simulation
start() - Begin execution
step() - Execute one instruction
pause() - Pause execution
reset() - Clear state
```

### DebugConsole
**Purpose**: Captures and displays system output
**Key Features**:
- Redirects System.out and System.err
- Timestamps every message
- Color-codes output
- Auto-limits to 1000 lines
- Provides real-time visibility

**Example Output**:
```
[08:35:14.223] Simulation Initialized
[08:35:14.224] Algorithm: RR
[08:35:14.225] Creating Process 1
[08:35:14.226] Process P1 allocated memory [0-12]
[08:35:14.227] [Clock 0] Process P1 executing
[08:35:14.228]   Instruction: assign x 5
```

### Visualization Panels

#### MemoryVisualization
```
[0]: PCB(P1)  [1]: "assign x"  [2]: "print x"  ...
[8]: Empty    [9]: Empty      [10]: "semWait"  ...
```

#### QueueVisualization
```
Ready Queue (2 processes)
┌─────┐ ┌─────┐
│ P1  │ │ P2  │
│ RUN │ │ RDY │
└─────┘ └─────┘
```

#### CurrentProcessPanel
- Shows process ID, state, PC
- Symbol table
- Remaining instructions

#### TimelinePanel
- Clock cycles, instruction count
- Memory usage
- Process queue sizes
- Event log

#### MutexStatusPanel
- 3 resource mutexes
- Owner information
- Wait queue size

#### SystemCallStatsPanel
- Call counts per system call
- Success/failure rates
- Percentage metrics

## Debugging

### Using Step Mode for Analysis

Step mode is the best way to understand execution flow:

```
┌──────────────────────────────────────────┐
│ Step-by-Step Debugging Workflow          │
├──────────────────────────────────────────┤
│ 1. Initialize                            │
│ 2. Switch to Step Mode (default)         │
│ 3. Click [Step]                          │
│    ├─ Memory updates                     │
│    ├─ PC advances                        │
│    ├─ Debug Console logs instruction    │
│    └─ Panels refresh                     │
│ 4. Observe and analyze                   │
│ 5. Click [Step] again                    │
│ 6. Repeat until done or error           │
└──────────────────────────────────────────┘
```

### Interpreting Debug Output

Each line shows:
```
[HH:MM:SS.ms] <message>

Examples:
[14:23:45.123] Process P1 created
[14:23:45.124] Instruction: print x
[14:23:45.125] semWait userOutput → P1 blocked
[ERROR] Memory allocation failed
```

### Key Output Messages

**Process Creation**:
```
Creating Process 1 from Program1.txt
Process P1 allocated memory [0-12]
```

**Instruction Execution**:
```
[Clock 0] Process P1 executing, PC: 0
  Instruction: assign x 5
```

**Resource Operations**:
```
semWait userOutput → Acquiring resource
semSignal userOutput → Releasing resource
Blocked Queue: [P2]
Ready Queue: [P1, P3]
```

**Completion**:
```
Process P1 completed
All processes completed
Total Clock Cycles: 45
Instructions Executed: 127
```

## Troubleshooting

### Problem: GUI Won't Start

**Symptom**: 
```
Module javafx.controls not found
```

**Solution**:
1. Verify JavaFX installation: `ls $JAVAFX_PATH_ENV/lib/`
2. Check environment variable: `echo $JAVAFX_PATH_ENV`
3. See [JAVAFX_SETUP.md](JAVAFX_SETUP.md) for detailed setup

### Problem: Processes Don't Execute

**Symptom**: Ready Queue stays empty or Process doesn't advance PC

**Causes**:
- Didn't click [Initialize]
- Program files missing
- Memory allocation failed

**Solutions**:
1. Click [Initialize] before [Start]
2. Check program files exist: `ls Program*.txt`
3. Look for errors in Debug Console
4. Try [Reset] and initialize again

### Problem: Memory Doesn't Update

**Symptom**: Memory grid shows no changes despite execution

**Causes**:
- In Auto mode but not really running
- UI not refreshing
- Execution already complete

**Solutions**:
1. Switch to Step mode
2. Click [Step] explicitly
3. Check status label shows "Running"
4. Check Debug Console for progress
5. Try [Reset] and start fresh

### Problem: Simulation Runs Too Fast

**Symptom**: Can't see what's happening in Auto mode

**Solutions**:
1. Switch to Step mode (use [Step] button)
2. Reduce speed slider to 0.1x - 0.5x
3. Use [Pause] frequently
4. Widen Debug Console window to see output

### Problem: Get "Not enough contiguous memory"

**Symptom**: Process creation fails with memory error

**Causes**:
- Memory fragmentation
- Already have processes using memory
- Process size too large

**Solutions**:
1. Click [Reset] to clear memory
2. Start fresh simulation
3. In Step mode, watch memory allocation carefully
4. Check Timeline tab for actual memory usage

### Problem: Mutex Seems Stuck

**Symptom**: Process blocked forever, never resumes

**Causes**:
- Missing semSignal to release resource
- Process blocked but semSignal executed
- Deadlock situation

**Debug Steps**:
1. Check Mutexes Tab for resource owner
2. Look in Debug Console for semWait/semSignal
3. Check Blocked Queue to see stuck processes
4. Review program files for matching wait/signal

## Performance Optimization

### For Fast Machines
- Use Auto mode with 2.0x - 3.0x speed
- Watch Timeline tab for aggregate data
- Good for testing many scenarios

### For Detailed Analysis
- Use Step mode
- Take notes on Debug Console output
- Use Timeline for verification
- Expand panels to see more detail

### For Classroom Demo
- Use Auto mode with 1.0x speed
- Keep Debug Console visible
- Use larger monitor resolution
- Advance slowly through key points
- Use Step mode for explanations

## Files Modified for GUI

```
New Files:
├─ DebugConsole.java         (real-time output capture)
├─ SimulationEngine.java      (simulation state management)
├─ TimelinePanel.java         (timeline display)
├─ OSSimulatorGUI.java        (rewritten - new architecture)
├─ Main.java                  (updated - GUI entry point)
├─ JAVAFX_SETUP.md            (installation guide)
├─ GUI_USER_GUIDE.md          (comprehensive user guide)
├─ GUI_QUICK_REFERENCE.md     (at-a-glance reference)
└─ README_GUI.md              (this file)

Modified Files:
├─ Scheduler.java             (added selectNextProcess)
├─ Memory.java                (added getUsedWords, allocate)
└─ build-gui.sh               (updated for flexibility)

Existing Files (Unchanged):
├─ Interpreter.java
├─ Parser.java
├─ Scheduler.java             (core logic unchanged)
├─ SystemCall.java
├─ Mutex.java
├─ MutexManager.java
└─ ... (other project files)
```

## Keyboard Controls (Future Enhancement)

Currently all operations use mouse clicks. A future enhancement could add:
- **Space Bar**: Execute Step
- **P**: Pause/Resume
- **R**: Reset
- **S**: Start
- **I**: Initialize
- **Q**: Quit

## Command-Line Alternative

If you prefer not to use the GUI:

```bash
cd os
javac -d bin src/os/*.java
java -cp bin os.Main -cli
```

This runs the simulator in console mode without GUI.

## System Requirements Summary

| Component | Requirement | Notes |
|-----------|-------------|-------|
| Java | 11+ (17+ optimal) | JDK or OpenJDK |
| JavaFX | 17+ | Must match Java version |
| Memory | 4GB minimum | 8GB recommended |
| Display | 1400x900 minimum | Larger better for readability |
| OS | Any (macOS/Linux/Windows) | Tested on macOS |

## Support & Resources

1. **Installation Issues**: See [JAVAFX_SETUP.md](JAVAFX_SETUP.md)
2. **Usage Questions**: See [GUI_USER_GUIDE.md](GUI_USER_GUIDE.md)
3. **Quick Reference**: See [GUI_QUICK_REFERENCE.md](GUI_QUICK_REFERENCE.md)
4. **Project Spec**: See project description PDF
5. **Code Issues**: Check Debug Console first

## Future Enhancement Ideas

1. **Additional Visualizations**
   - Gantt chart of process scheduling
   - Memory fragmentation visualization
   - Resource dependency graph

2. **Advanced Controls**
   - Modify process arrival times
   - Adjust time quantum
   - Change memory size
   - Pause on breakpoints

3. **Recording & Playback**
   - Record execution session
   - Save to file
   - Playback for analysis
   - Export execution timeline

4. **Comparison Mode**
   - Run multiple algorithms side-by-side
   - Compare metrics
   - Visualize differences

5. **Extended Features**
   - Keyboard shortcuts
   - Configuration profiles
   - Dark/Light themes
   - Window layouts

## Summary

The OS Simulator GUI provides:
- ✅ Real-time visualization of OS behavior
- ✅ Easy debugging with capture output
- ✅ Support for all 3 scheduling algorithms
- ✅ Complete memory and process state visibility
- ✅ Step-by-step execution for learning
- ✅ Rich statistical monitoring

Perfect for understanding operating system concepts and debugging multi-process applications!

---

**Version**: 2.0  
**Course**: CSEN 602 - Operating Systems  
**Semester**: Spring 2026  
**Institution**: German University in Cairo
