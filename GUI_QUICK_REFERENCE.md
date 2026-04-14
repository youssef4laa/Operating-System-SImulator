# GUI Quick Reference

## Buttons (Left to Right)

| Button | Function | Mode | Notes |
|--------|----------|------|-------|
| **Initialize** | Set up simulation | All | Must click before Start (unless already running) |
| **Start** | Begin execution | All | Prepares simulation if not initialized |
| **Step** | Execute 1 instruction | Step Only | Use for detailed analysis |
| **Pause** | Stop execution | Auto Only | Pauses automatic mode |
| **Resume** | Continue execution | Auto Only | Resumes from pause point |
| **Reset** | Clear everything | All | Returns to initial state, clears debug log |

## Controls (Top Panel)

```
Algorithm: [Dropdown]           Set scheduling algorithm (RR/HRRN/MLFQ)
Mode: [Radio] Step   [Auto]    Choose execution mode
Speed: [Slider] 0.1x ←→ 3.0x  Adjust execution speed
```

## Display Panels (Left to Right)

### Left Panel: System State
```
├─ Memory (40 words) - Color-coded grid
│  ├─ Blue = PCB
│  ├─ Green = Instructions
│  └─ Orange = Data
├─ Ready Queue - Processes waiting to run
└─ Blocked Queue - Processes waiting for resources
```

### Center Panel: Debug Console
```
Real-time system output with timestamps
├─ [HH:MM:SS.ms] Process created
├─ [HH:MM:SS.ms] Instruction executed
└─ [ERROR] Error messages in red
```

### Right Panel: Information Tabs
```
├─ Process Tab - Current process details
├─ Timeline Tab - Execution statistics
├─ Mutexes Tab - Resource status
└─ Statistics Tab - System call metrics
```

## Status Indicators

**Status Label (Bottom Right)**
- 🔵 Ready = Waiting to start
- 🟢 Running = Execution in progress
- 🟠 Paused = Temporarily halted
- 🟢 Completed = All done
- 🔴 Error = Something failed

## Memory Colors

| Color | Meaning |
|-------|---------|
| Gray | Empty space |
| Blue | Process Control Block (PCB) |
| Green | Instruction/Code |
| Orange | Data/Variables |

## Typical Workflow

### For Step-by-Step Analysis:
```
1. Click [Initialize]
2. Select algorithm from dropdown
3. Set mode to "Step"
4. Click [Start]
5. Click [Step] repeatedly
6. Watch Debug Console and panels update
7. Click [Reset] to start over
```

### For Automatic Observation:
```
1. Click [Initialize]
2. Set mode to "Auto"
3. Adjust speed slider (0.5x for detailed, 2.0x for fast)
4. Click [Start]
5. Watch execution in real-time
6. Click [Pause] to stop
7. Click [Step] for single steps or [Resume] to continue auto
```

## Key Observations

**Ready Queue**
- Shows processes waiting to execute
- Order depends on algorithm chosen
- Shrinks as processes block or finish

**Blocked Queue**
- Shows processes waiting for resources
- Increases when semWait is called
- Decreases when semSignal is called

**Memory Grid**
- Color changes indicate allocation/deallocation
- Blue cells show PCB locations
- Green cells show where instructions are stored

**Debug Console**
- Most important for understanding execution flow
- Shows exact timing of all events
- Errors appear in red text
- Scroll to see history

## Algorithm Differences

| Algorithm | Speed | Fairness | Best For |
|-----------|-------|----------|----------|
| **RR** | ⭐⭐ | ⭐⭐⭐ | Learning, fair systems |
| **HRRN** | ⭐⭐⭐ | ⭐⭐ | Quick average turnaround |
| **MLFQ** | ⭐⭐⭐ | ⭐⭐⭐ | Priority systems |

## Common Issues & Quick Fixes

| Issue | Try This |
|-------|----------|
| Process doesn't execute | Click [Initialize] |
| Memory doesn't update | Switch to Step mode |
| Console is too full | Not an issue - it auto-limits to 1000 lines |
| Simulation runs too fast | Reduce speed slider |
| Simulation freezes | It might be complete - check Debug Console |
| Program won't start | Check JAVAFX_SETUP.md |

## Keyboard Tips (Future)

Currently all actions use mouse clicks. Future versions may support:
- Space bar = Step
- 'P' = Pause/Resume
- 'R' = Reset

## Memory Swapping

If memory becomes full:
1. System finds least-used process in Ready Queue
2. Swaps it to disk
3. Process remains in scheduler
4. Automatically swapped back when needed
5. Watch Debug Console for swap messages

## Mutex Operations

**semWait** (Acquire):
- Adds process to resource's wait queue
- Moves process to Blocked Queue
- Sets owner if resource is free

**semSignal** (Release):
- Releases resource from owner
- Wakes up first waiting process
- Moves process back to Ready Queue

Monitor in **Mutexes Tab**.

## Statistics Tab Meanings

| Metric | Meaning |
|--------|---------|
| **Total** | How many times this call was made |
| **Success** | How many succeeded |
| **Failure** | How many failed |
| **% Success** | Success rate percentage |

## Exiting the Application

Click the ❌ button in the top-right corner:
1. Current execution stops
2. Resources are cleaned up
3. Application closes
4. All state is lost (unless you save separately)

## Pro Tips

1. **Use Step Mode for Learning** - See exactly what happens
2. **Watch Debug Console** - It tells you everything
3. **Pause Before Looking** - Makes it easier to read output
4. **Use Timeline Tab** - Shows summary statistics
5. **Check Mutexes Tab** - See what resources are locked
6. **Change Speed** - Slow down (0.5x) for demos, speed up (2.0x) for verification
7. **Color Matters** - Green successful, Red errors, Blue process blocks
8. **Reset Often** - Easy way to try different algorithms

## Legend

```
P1, P2, P3 = Process IDs
PC = Program Counter (instruction pointer)
RDY = Ready state
RUN = Running state
BLK = Blocked state
FIN = Finished state
[addr] = Memory address
```

## Support

Having issues? Check these in order:
1. JAVAFX_SETUP.md - Installation help
2. GUI_USER_GUIDE.md - Detailed instructions
3. Project specification PDF - System requirements
4. Debug Console - Shows actual errors and events
