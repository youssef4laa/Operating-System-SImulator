# Mutex Subsystem Implementation Guide

## Overview
The mutex (mutual exclusion) subsystem ensures that only one process can access critical shared resources at a time. This implementation includes three mutexes for protecting three critical resources in the operating system.

## Components

### 1. **Mutex.java** - Core Mutex Implementation
The `Mutex` class provides binary semaphore functionality for protecting critical sections.

#### Key Features:
- **Mutual Exclusion**: Only one process can hold the mutex at a time
- **FIFO Blocking Queue**: Processes wait in FIFO order
- **Ownership Tracking**: Tracks which process owns the mutex
- **Blocking Mechanism**: Processes that can't acquire the mutex are blocked
- **Logging**: Detailed logging of all mutex operations

#### Key Methods:
```java
// Attempt to acquire the mutex (semWait)
public boolean acquire(PCB p, Scheduler scheduler)

// Release the mutex (semSignal)
public void release(Scheduler scheduler, PCB currentProcess)

// Check if mutex is locked
public boolean isLocked()

// Get the owning process
public PCB getOwner()

// Get resource name
public String getResourceName()
```

#### Important Notes:
- When a process tries to acquire a locked mutex, it's added to the wait queue and blocked
- When a process releases the mutex, the first waiting process in the queue is unblocked
- If no processes are waiting, the mutex becomes fully unlocked

### 2. **MutexManager.java** - Centralized Mutex Management
The `MutexManager` class manages all three system mutexes and provides a unified interface.

#### Key Features:
- **Centralized Management**: Manages all three mutexes from one place
- **Resource Lookup**: Easy retrieval of mutex by resource name
- **Statistics Tracking**: Tracks how many times each mutex has been acquired
- **Status Reporting**: Provides detailed status of all mutexes
- **Logging Control**: Verbose logging can be enabled/disabled

#### The Three Mutexes:

| Mutex Name | Resource Type | Usage |
|-----------|---------------|-------|
| `mutexUserOutput` | Screen Output | Protecting `print` and `printFromTo` operations |
| `mutexUserInput` | User Input | Protecting `assign x input` operations |
| `mutexFile` | File Access | Protecting `readFile` and `writeFile` operations |

#### Key Methods:
```java
// Get specific mutexes
public Mutex getUserOutputMutex()
public Mutex getUserInputMutex()
public Mutex getFileMutex()

// Acquire/release by name
public boolean acquire(String resourceName, PCB process, Scheduler scheduler)
public void release(String resourceName, PCB currentProcess, Scheduler scheduler)

// Status and statistics
public String getMutexStatus()
public void printStatistics()
public boolean hasBlockedProcesses()
public int getTotalBlockedProcesses()
```

## Usage Flow

### When a Process Needs a Resource:

1. **Execute semWait Instruction**
   ```
   semWait userOutput
   ```

2. **Interpreter Calls Mutex Acquire**
   - `mutexManager.acquire("userOutput", currentProcess, scheduler)`

3. **Mutex Acquire Operation**
   - If mutex is free → acquire immediately, process continues
   - If mutex is in use → block process, add to wait queue

4. **Process Execution**
   - If acquired: process executes the resource operation (print, readFile, etc.)
   - If blocked: process moves to blocked queue, another process is scheduled

5. **Execute semSignal Instruction**
   ```
   semSignal userOutput
   ```

6. **Mutex Release Operation**
   - If processes waiting → unblock first process, transfer ownership
   - If no processes waiting → unlock completely

### Example Usage in Program:

```
# Program to print with mutex protection
semWait userOutput
print x
semSignal userOutput
```

## Integration with Components

### Scheduler Integration
- Initializes MutexManager
- Manages blocked queue when processes are blocked on mutexes
- Handles process state transitions (Ready ↔ Blocked)
- Maintains scheduler time

### Interpreter Integration
- Handles `semWait` and `semSignal` instructions
- Calls mutex acquire/release operations
- Manages process blocking when waiting for resources
- Prevents program counter increment if process is blocked

### SystemCall Integration
- File operations (readFile, writeFile) are protected by fileMutex
- User input operations are protected by userInputMutex
- Output operations are protected by userOutputMutex
- System calls assume the caller has already acquired the appropriate mutex

## Process States Related to Mutexes

When a process interacts with mutexes, it can be in these states:

| State | Description |
|-------|-------------|
| Ready | Process is ready to run, not waiting for any resource |
| Running | Process is currently executing |
| Blocked | Process is waiting to acquire a mutex (in wait queue) |
| Finished | Process has completed execution |

## Blocking Behavior

### When a Process Gets Blocked:
1. Process status changes to "Blocked"
2. Process is added to the mutex's wait queue
3. Process is added to scheduler's blocked queue
4. Scheduler skips this process and runs another ready process
5. Program counter is NOT incremented (process will retry the same instruction)

### When a Process Gets Unblocked:
1. Status changes to "Ready"
2. Process is removed from blocked queue
3. Process is added to ready queue
4. Process will retry the semWait instruction again
5. This time, it should succeed (ownership transferred to it)

## Output and Logging

The mutex subsystem provides detailed logging:

```
[MUTEX] Process 1 acquired mutex 'userOutput' (count: 1)
[MUTEX] Process 2 blocked on mutex 'userOutput' (waiting for process 1)
[MUTEX] Wait queue size: 1
[MUTEX] Released to Process 2 from mutex 'userOutput' (next in queue)
[MUTEX] Mutex 'userOutput' is now unlocked
```

At the end of execution:
```
========== MUTEX STATISTICS ==========
userOutput acquire count: 15
userInput acquire count: 3
file acquire count: 8
======================================
```

## Important Design Decisions

1. **Non-Reentrant**: Each mutex is non-reentrant. A process cannot re-acquire a mutex it already owns (though we allow it to return immediately without blocking).

2. **FIFO Ordering**: Processes are unblocked in FIFO order (fairness).

3. **Ownership Tracking**: We track the current owner to prevent unauthorized releases and for error detection.

4. **Blocking on Program Boundary**: When a process blocks on a semWait, the program counter is not incremented, so the process will retry when it becomes ready again.

5. **Atomicity**: The acquire/release operations are atomic at the Java level (single-threaded simulation) but in real OS would need synchronization.

## Error Handling

The system handles several error cases:

- **Unknown Resource**: semWait/semSignal with unknown resource name
- **Wrong Owner Release**: Attempting to release a mutex owned by another process
- **Duplicate in Wait Queue**: Prevents adding the same process twice

## Testing Recommendations

1. **Single Process**: Test with one process to verify basic acquire/release
2. **Multiple Processes**: Test with processes competing for the same resource
3. **Different Resources**: Verify that different mutexes work independently
4. **Blocking Scenarios**: Test scenarios where processes block and wake up
5. **State Verification**: Check that blocked queue, ready queue, and mutex states are correct

## Key Files

- `/os/src/os/Mutex.java` - Core mutex implementation
- `/os/src/os/MutexManager.java` - Centralized mutex management
- `/os/src/os/Scheduler.java` - Scheduler with mutex support (modified)
- `/os/src/os/Interpreter.java` - Instruction execution with mutex operations
- `/os/src/os/SystemCall.java` - System calls protected by mutexes

## Troubleshooting

### Process Remains Blocked Forever
- Check if the owning process is still running
- Verify semSignal is being called
- Check blocking queue to see which processes are blocked

### Deadlock Scenarios
- Current implementation prevents deadlock by:
  - Using only one resource per instruction
  - FIFO ordering ensures no circular waiting
  - No nested mutex acquisitions

### Incorrect Acquire Count
- Verify that all acquire operations are matched by release operations
- Check the mutex statistics output
- Look for mismatched semWait/semSignal pairs in programs

## Summary

The mutex subsystem provides robust mutual exclusion for three critical resources. It maintains proper blocking/unblocking of processes and provides detailed logging for debugging and understanding the system's behavior.
