# OS Simulator GUI - User Guide

## Overview

The OS Simulator GUI is a comprehensive JavaFX-based visualization tool for the CSEN 602 Operating System Simulator project. It provides real-time visualization of system state, process execution, memory management, and scheduling algorithms.

## Features

### 1. **Real-Time System State Display**
- **Memory Visualization**: 40-word memory grid showing:
  - Empty cells (gray)
  - Process Control Blocks (blue)
  - Instructions/Data (green)
  - Other data types (orange)
  
- **Queue Display**: Visual representation of:
  - Ready Queue (processes waiting to execute)
  - Blocked Queue (processes waiting for resources)
  - Finished Queue (completed processes)

### 2. **Process Information Panel**
Shows details of the currently executing process:
- Process ID
- Current State (Running, Ready, Blocked, Finished)
- Current Instruction
- Program Counter (PC)
- Memory Boundaries
- Arrival Time
- Remaining Time
- Symbol Table with variable addresses

### 3. **Timeline & Statistics**
- **Clock Cycles**: Track simulation time
- **Instructions Executed**: Total instruction count
- **Memory Usage**: Current memory allocation
- **Process Counts**: Ready, Blocked, and Finished process counts
- **Timeline Log**: Event history

### 4. **System Monitoring Tabs**
- **Process Tab**: Current process details
- **Timeline Tab**: Execution timeline and statistics
- **Mutexes Tab**: Status of 3 system mutexes:
  - File Access
  - User Input
  - User Output
- **Statistics Tab**: System call statistics and success rates

### 5. **Debug Console**
- Real-time capture of all system output
- Color-coded messages (green for normal, red for errors)
- Timestamped log entries
- Scrollable history (up to 1000 lines)
- Includes:
  - Process creation/termination
  - Memory allocation/deallocation
  - System call invocations
  - Scheduler decisions
  - Mutex operations

## User Interface Layout

```
┌─────────────────────────────────────────────────────────────────┐
│ OS Simulator GUI - CSEN 602 (Spring 2026)                       │
│ Algorithm: [RR/HRRN/MLFQ] | Mode: Step/Auto | Speed: 0.1-3.0x │
├──────────────────────┬──────────────────────┬────────────────────┤
│                      │                      │                    │
│   Memory & Queues    │  Debug Console       │  Process Info      │
│   ────────────────   │  ──────────────      │  (Tabs)            │
│                      │                      │                    │
│   • Memory Grid      │  [Real-time logging] │  • Process Tab     │
│   • Ready Queue      │                      │  • Timeline Tab    │
│   • Blocked Queue    │  [≤ 1000 lines]      │  • Mutexes Tab     │
│                      │                      │  • Stats Tab       │
├──────────────────────┴──────────────────────┴────────────────────┤
│ [Initialize] [Start] [Step] [Pause] [Resume] [Reset]             │
│             Status: Ready | Active | Paused | Completed          │
└───────────────────────────────────────────────────────────────────┘
```

## Usage Guide

### Getting Started

1. **Launch the Application**:
   ```bash
   cd os
   bash build-gui.sh
   ```

2. **Initialize Simulation**:
   - Click the **Initialize** button
   - Select algorithm from dropdown if needed (default: RR)
   - Check Debug Console for initialization messages

3. **Choose Execution Mode**:
   - **Step-Through Mode** (recommended for learning):
     - Click **Start** to prepare
     - Click **Step** to execute one instruction at a time
   - **Automatic Mode**:
     - Select "Auto" radio button
     - Adjust speed with slider (0.1x = very slow, 3.0x = very fast)
     - Click **Start** to begin
     - Monitor execution in Debug Console

### Navigation

#### Algorithm Selection
- Dropdown at top displays available algorithms
- Changing algorithm resets simulation
- Options: RR, HRRN, MLFQ

#### Execution Controls
- **Initialize**: Set up simulation with selected algorithm
- **Start**: Begin execution
- **Step**: Execute one instruction (Step-Through mode only)
- **Pause**: Halt execution (Automatic mode)
- **Resume**: Continue from pause point
- **Reset**: Clear all state and return to initial state

#### View Controls
- **Tabs on right panel**: Switch between different views
- **Scroll areas**: Scroll through memory, queues, and logs
- **Splitters**: Resize panels by dragging dividers

### Debug Console Tips

The Debug Console shows all system activity:

```
[HH:MM:SS.ms] Message text
[HH:MM:SS.ms] Process P1 created
[HH:MM:SS.ms] Instruction executed: print Hello
[ERROR] Operation failed
```

**Tips**:
- Red text = Errors
- Green text = Success messages
- Timestamp helps correlate with clock cycles

### Monitoring Execution

#### In Step-Through Mode:
1. Click **Step** button
2. Observe changes in:
   - Memory grid (new allocations highlighted)
   - Queue displays (process movement)
   - Current process panel (PC advances)
   - Debug console (instruction logged)
3. Repeat for detailed analysis

#### In Automatic Mode:
1. Set speed (faster = 2.0x, slower = 0.5x)
2. Watch real-time updates to all panels
3. Click **Pause** to stop at any point
4. Click **Step** to continue one instruction at a time
5. Click **Resume** to return to automatic mode

## Interpreting Displays

### Memory Grid
- **[0]: Empty** = No data
- **[5]: PCB(P1)** = Process Control Block for Process 1 (blue)
- **[10]: "print x"** = Instruction (green)
- **[15]: 42** = Data value (orange)

### Queue Display
```
Ready Queue (3 processes)
┌─────┐ ┌─────┐ ┌─────┐
│ P1  │ │ P2  │ │ P3  │
│ RUN │ │ RDY │ │ RDY │
│PC:5 │ │PC:0 │ │PC:0 │
└─────┘ └─────┘ └─────┘
```

### Timeline Panel
Shows:
```
Clock Cycle: 15        Instructions: 42
Memory: 28/40 words    Ready: 1  Blocked: 1  Finished: 1
```

## Scheduling Algorithm Explanation

### Round Robin (RR)
- Each process gets 2 instructions per time slice
- Processes rotate in Ready Queue
- Fair allocation of CPU time
- Good for multi-user systems

### Highest Response Ratio Next (HRRN)
- Selects process with highest response ratio
- Formula: (Waiting Time + Burst Time) / Burst Time
- Non-preemptive (runs to completion or block)
- Minimizes average turnaround time

### Multi-Level Feedback Queue (MLFQ)
- 4 priority queues (Q0 to Q3)
- Q0 gets most CPU time (quantum = 1)
- Q1 gets medium time (quantum = 2)
- Q2 gets less time (quantum = 4)
- Q3 gets least time (quantum = 8)
- Lower priority on starvation

## Troubleshooting

### GUI Won't Start
**Problem**: "Module not found" error
- **Solution**: Ensure JavaFX is properly installed
- Check `JAVAFX_PATH_ENV` environment variable
- See JAVAFX_SETUP.md for installation help

### Simulation Won't Execute
**Problem**: Processes don't move or execute
- **Solution**: 
  1. Click **Initialize** button
  2. Check Debug Console for error messages
  3. Verify program files exist (Program1.txt, etc.)

### Slow Performance
**Problem**: GUI is laggy or unresponsive
- **Solution**:
  1. Reduce speed (lower values: 0.5x, 0.2x)
  2. Use Step mode instead of Automatic
  3. Close other applications
  4. Increase Java heap size: `java -Xmx2G -cp bin os.OSSimulatorGUI`

### Memory Not Updating
**Problem**: Memory panel doesn't show changes
- **Solution**: 
  1. Make sure you're in Step mode and clicking "Step"
  2. Check that processes were created successfully
  3. Look for errors in Debug Console

## Keyboard Shortcuts (Future Enhancement)

Once implemented:
- **Space**: Step execution
- **P**: Pause/Resume
- **R**: Reset
- **S**: Start
- **Ctrl+C**: Quick exit

## Performance Tips

1. **For Learning**: Use Step mode with speed = 1.0x
2. **For Testing**: Use Auto mode with speed = 2.0x to 3.0x
3. **For Debugging**: Use Step mode with Debug Console visible
4. **For Demo**: Use Auto mode with medium speed (1.0x)

## System Call Tracking

The GUI monitors system calls:
- **print**: Output to console
- **input**: User input from keyboard
- **readFile**: Read file from disk
- **writeFile**: Write file to disk
- **readMemory**: Access process memory
- **writeMemory**: Modify process memory

Statistics shown in **Statistics Tab**.

## Mutex Monitoring

Three mutexes are tracked:
1. **File Access**: Controls file I/O operations
2. **User Input**: Controls keyboard input
3. **User Output**: Controls screen output

Status shows:
- 🟢 **Free**: Resource available
- 🔴 **Locked**: Resource in use
- **Owner**: Process holding the mutex
- **Waiting**: Processes waiting for resource

## Advanced Features

### Memory Swapping
- When memory is full, least recently used process is swapped
- Swapped processes remain in scheduler
- Automatically swapped back when needed
- Disk format shown in Debug Console

### Process Priority (MLFQ)
- Higher priority processes run first
- Priority decreases with starvation time
- Lower queues use Round Robin scheduling

### Deadlock Detection
- System monitors mutex dependencies
- Alerts if potential deadlock detected
- Shows which processes are involved

## Keyboard Shortcuts (Manual)

You can manually control execution using these button clicks:
- **Initialize**: Prepare simulation
- **Start**: Begin execution
- **Step**: Execute next instruction
- **Pause**: Stop execution
- **Resume**: Continue from pause
- **Reset**: Clear everything

## File Format

Program files should follow the syntax specified in the project requirements:
- `print variable`
- `assign variable value`
- `writeFile filename data`
- `readFile filename`
- `printFromTo start end`
- `semWait resource`
- `semSignal resource`

## Exiting

Click the **X** button in the top-right corner to exit the application. The application will:
1. Stop any running execution
2. Clean up resources
3. Close all threads
4. Exit gracefully

## Additional Resources

- Project Spec: See project description PDF
- JavaFX Setup: See JAVAFX_SETUP.md
- System Architecture: See documentation files
- Code Examples: Check Program*.txt files
