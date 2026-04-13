# Mutex Usage Examples

This document shows how to properly use mutexes in your OS simulation programs.

## Example 1: Protected Console Output

**Scenario**: Multiple processes printing to the screen

```
assign greeting "Hello, World!"
semWait userOutput
print greeting
semSignal userOutput
```

**What happens**:
1. Process acquires userOutput mutex
2. Prints greeting to screen
3. Releases mutex
4. Other processes waiting on userOutput can now print

**Why it matters**: Without the mutex, other processes could print simultaneously, causing garbled output.

---

## Example 2: Protected File Operations

**Scenario**: One process writing, another reading the same file

```
# Writer Process
assign filename "data.txt"
assign data "Important Information"
semWait file
writeFile data.txt Important Information
semSignal file
```

```
# Reader Process
assign filename "data.txt"
semWait file
readFile data.txt
print filename
semSignal file
```

**What happens**:
1. First process acquires file mutex
2. Writes data or reads file
3. Releases mutex
4. Second process waits for file to be available
5. Gets access to consistent, complete file data

---

## Example 3: Protected User Input

**Scenario**: Process asking user for input

```
semWait userInput
assign userResponse input
semSignal userInput
print userResponse
```

**What happens**:
1. Process acquires userInput mutex
2. Prompts user and reads input
3. Releases mutex
4. Other processes can now get user input

**Why it matters**: Prevents multiple processes from trying to read input simultaneously

---

## Example 4: Coordinated Access Pattern

**Scenario**: Process needs multiple operations without interruption

```
# Step 1: Get data from user
semWait userInput
assign number1 input
semSignal userInput

# Step 2: Process the data
assign result number1

# Step 3: Display result
semWait userOutput
print result
semSignal userOutput
```

**Important**: 
- The `result` calculation is NOT protected (doesn't need to be)
- Only actual resource access (input, output, file) needs protection
- Minimize time mutex is held

---

## Example 5: The printFromTo Pattern

**Scenario**: Printing a range of numbers protected

```
assign start 5
assign end 10
semWait userOutput
printFromTo start end
semSignal userOutput
```

**What happens**:
1. Acquires mutex
2. Prints all numbers in range atomically (5, 6, 7, 8, 9, 10)
3. No other process can interrupt the output
4. Releases mutex

---

## Example 6: Sequential File Processing

**Scenario**: Read, process, then write (with proper synchronization)

```
# Read phase
semWait file
readFile input.txt
assign content input.txt
semSignal file

# Process phase (no lock needed)
# ... do processing ...

# Write phase
semWait file
writeFile output.txt content
semSignal file
```

**Why both operations need locks**:
- Another process might read during your write (sees inconsistent state)
- Another process might write while you're reading (gets corrupt data)

---

## Example 7: Common Mistake - Unprotected Access

❌ **INCORRECT**:
```
writeFile data.txt "critical data"  # No mutex!
```

✅ **CORRECT**:
```
semWait file
writeFile data.txt "critical data"
semSignal file
```

---

## Example 8: Common Mistake - Keeping Lock Too Long

❌ **INEFFICIENT**:
```
semWait userOutput
assign result number1 * number2  # Expensive calculation
assign result result * 100
print result
semSignal userOutput
```

✅ **EFFICIENT**:
```
assign result number1 * number2  # Do calculation without lock
assign result result * 100
semWait userOutput
print result
semSignal userOutput
```

**Reason**: Only hold the lock while accessing the actual resource

---

## Example 9: Multiple Write Operations

**Scenario**: Log file with multiple processes

```
# Process 1
semWait file
writeFile log.txt "Process 1 completed"
semSignal file

# Process 2
semWait file
writeFile log.txt "Process 2 completed"
semSignal file

# Process 3
semWait file
writeFile log.txt "Process 3 completed"
semSignal file
```

**Result**: Each process waits its turn, file stays consistent

---

## Example 10: Input with Validation

**Scenario**: Get input and print confirmation

```
semWait userInput
assign userValue input
semSignal userInput

assign message "You entered:"
semWait userOutput
print message
print userValue
semSignal userOutput
```

**Key point**: 
- Input is protected with userInput mutex
- Output is protected with userOutput mutex
- Processing (message construction) happens unprotected

---

## Mutex Acquisition Timing

### Quick Operations (< 10 instructions)
```
semWait resource
perform operation     # Very fast
semSignal resource
```

### Medium Operations (< 100 instructions)
```
semWait resource
perform operation     # Some processing
semSignal resource
```

### Long Operations
```
# Phase 1: Get data (with lock)
semWait resource
read data
semSignal resource

# Phase 2: Process (without lock)
process data

# Phase 3: Output (with lock)
semWait resource
write result
semSignal resource
```

---

## Deadlock Prevention

### Information About Your System:
✅ **Deadlock-free by design** because:
- Each semWait/semSignal pair is independent
- No process waits for multiple mutexes in sequence
- FIFO ordering prevents circular waiting

### Safe Pattern:
```
semWait resource1
use resource1
semSignal resource1

# Later, different operation
semWait resource2
use resource2
semSignal resource2
```

### Pattern to Avoid (but won't deadlock here):
```
# This is OK in our system,
# but in real OS could deadlock:
semWait resource1
semWait resource2
use both
semSignal resource2
semSignal resource1
```

---

## Testing Your Mutex Implementation

### Test Pattern 1: Basic Acquire/Release
```
semWait userOutput
print "Resource acquired"
semSignal userOutput
```
Expected: One message printed

### Test Pattern 2: Sequential Access
```
# Process 1
semWait userOutput
print "Process 1"
semSignal userOutput

# Process 2
semWait userOutput
print "Process 2"
semSignal userOutput
```
Expected: Two messages, never overlapped

### Test Pattern 3: Stress Test
```bash
# Run with all three processes
# Each using different mutexes
# Watch the blocking queue in real-time
```
Expected: Processes block and unblock smoothly

---

## Debugging Mutex Issues

### Check Current Status
- Review mutex status output after each clock cycle
- Look for processes in blocked queue
- Verify owner process

### Add Debug Output
```
semWait userOutput  # Should acquire immediately or block
# If blocks, debug why owning process hasn't released
print "About to use resource"
semSignal userOutput  # Should release
```

### Common Issues
1. **Process stuck in blocked queue**: 
   - Verify semSignal is being called
   - Check if owner process is still running

2. **Acquire count not matching**:
   - Count semWait calls
   - Count semSignal calls
   - Should be equal

3. **Output appears interleaved**:
   - Missing semWait/semSignal around output
   - Multiple processes printing without mutex

---

## Summary

**Remember**:
- ✅ Always protect resource operations with semWait/semSignal
- ✅ Acquire just before needed, release immediately after
- ✅ Initialize before use, clean up afterward
- ✅ Don't hold locks longer than necessary
- ✅ Pair every semWait with a semSignal
- ❌ Don't forget mutexes (causes race conditions)
- ❌ Don't hold mutexes too long (reduces parallelism)
- ❌ Don't acquire non-existent resources
