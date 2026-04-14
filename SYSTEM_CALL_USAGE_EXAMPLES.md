# System Call Usage Examples

This document provides practical examples of how to use each system call in your OS simulation programs.

## Example 1: Simple File Write

**Program: WriteExample.txt**
```
assign filename "output.txt"
assign data "Hello from Process 1"
semWait file
writeFile output.txt Hello from Process 1
semSignal file
```

**What happens**:
1. Variable `filename` is created with value "output.txt"
2. Variable `data` is created
3. Process acquires file mutex (may wait if another process holds it)
4. System Call 2 executes: creates/overwrites the file with data
5. Mutex is released, allowing other processes to access files

**Output**:
```
[SYSCALL] writeFile: wrote 22 bytes to 'output.txt'
[SYSCALL] File written: output.txt
```

---

## Example 2: File Read and Print

**Program: ReadAndPrint.txt**
```
assign filename "input.txt"
semWait file
readFile input.txt
semSignal file
assign content input.txt
semWait userOutput
print content
semSignal userOutput
```

**What happens**:
1. File mutex acquired
2. System Call 1 executes: reads file contents
3. File mutex released
4. Content stored in variable (now safe to access)
5. Output mutex acquired
6. System Call 3 executes: prints content atomically
7. Output mutex released

**Output**:
```
[SYSCALL] readFile: read 5 lines from 'input.txt'
[SYSCALL] print: 'File contents here...'
```

---

## Example 3: Protected Console Output

**Program: ProtectedOutput.txt**
```
assign greeting "Process 1 says hello"
semWait userOutput
print greeting
semSignal userOutput
```

**Why mutex is needed**:
- Without mutex: Output from multiple processes could be interleaved
  ```
  Process 1 Process 2 says says hello
  ```
- With mutex: Output is atomic
  ```
  Process 1 says hello
  Process 2 says goodbye
  ```

**When used with other processes**:
```
Thread 1: Process 1 says hello
Thread 2: Process 2 says goodbye
(clean, no interleaving)
```

---

## Example 4: User Input with Storage

**Program: UserInput.txt**
```
semWait userInput
assign userValue input
semSignal userInput

assign message "You entered: "
semWait userOutput
print message
print userValue
semSignal userOutput
```

**Execution Flow**:
1. Acquire input mutex
2. System Call 4 executes: prompts "Please enter a value: "
3. User types input
4. Release input mutex
5. Store in variable
6. Acquire output mutex
7. Print using System Call 3
8. Release output mutex

**User Interaction**:
```
Please enter a value: 42
You entered: 
42
```

---

## Example 5: Memory Operations (Internal)

These are usually called by the interpreter, but shown for completeness:

```java
// Writing to memory
int ret = SystemCall.writeMemory(address, "stored_value", memory);
if (ret == SystemCall.SUCCESS) {
    System.out.println("Value stored");
}

// Reading from memory
Object value = SystemCall.readMemory(address, memory);
if (value != null) {
    System.out.println("Retrieved: " + value);
}
```

**Note**: Memory operations don't need mutexes because each process has its own memory space.

---

## Example 6: Multiple Processes with File Access

**Process 1:**
```
assign id "P1"
semWait file
writeFile results.txt Process1Result
semSignal file

semWait userOutput
print Process 1 complete
semSignal userOutput
```

**Process 2:**
```
assign id "P2"
semWait file
readFile results.txt
semSignal file

assign data results.txt
semWait userOutput
print data
semSignal userOutput
```

**Execution (May vary)**:
```
[Time 0] Process 1 acquires file mutex
[Time 1] Process 1 writes to results.txt
[Time 2] Process 1 releases file mutex
[Time 2] Process 2 acquires file mutex (was waiting)
[Time 3] Process 2 reads results.txt
[Time 4] Process 2 releases file mutex
[Time 5] Process 1 prints "Process 1 complete"
[Time 6] Process 2 prints file contents
```

**Key Point**: Process 2 had to wait for Process 1 to finish writing before reading.

---

## Example 7: Error Handling - File Not Found

**Program: SafeRead.txt**
```
semWait file
readFile missing.txt
semSignal file
print Result stored
```

**What happens**:
1. Process tries to read non-existent file
2. System Call returns NULL
3. Store NULL in variable
4. Print NULL (which may appear as empty or "null")

**Better approach with variable check** (if supported):
```
semWait file
readFile missing.txt
semSignal file
assign result missing.txt
semWait userOutput
print result
semSignal userOutput
```

**Output**:
```
[SYSCALL ERROR] File not found: missing.txt
```

---

## Example 8: Coordinated Multi-Process Workflow

**Process 1 - Data Generator:**
```
assign data "Important Data"
semWait file
writeFile data.txt Important Data
semSignal file
assign status "Data written"
semWait userOutput
print status
semSignal userOutput
```

**Process 2 - Data Reader:**
```
semWait file
readFile data.txt
semSignal file
assign content data.txt
semWait userOutput
print Processing:
print content
semSignal userOutput
```

**Process 3 - Logger:**
```
semWait userOutput
print All processes completed
semSignal userOutput
```

**Expected Output**:
```
[P1] Data written
Processing:
Important Data
All processes completed
```

---

## Example 9: Avoiding Deadlock

✅ **CORRECT** - No deadlock:
```
semWait file
writefile temp.txt data
semSignal file

semWait userOutput
print done
semSignal userOutput
```

Reason: Only one mutex is held at a time.

❌ **WRONG** - Could cause deadlock (but won't in our system):
```
semWait file
semWait userOutput  // Could deadlock if another process holds userOutput
writefile temp.txt data
print done
semSignal userOutput
semSignal file
```

Our system prevents this because:
- Single-threaded per process
- FIFO mutex ordering prevents circular waits

---

## Example 10: Process using All System Calls

**Comprehensive.txt**
```
# SC4: Get input
semWait userInput
assign name input
semSignal userInput

# SC3: Print message
semWait userOutput
print You entered:
print name
semSignal userOutput

# SC2: Write to file
assign message entered:  
semWait file
writeFile log.txt entered:
semSignal file

# SC1: Read from file
semWait file
readFile log.txt
semSignal file
assign content log.txt

# SC3: Print what was read back
semWait userOutput
print File contains:
print content
semSignal userOutput
```

**Execution Order**:
1. Get user input (protected)
2. Print confirmation (protected)
3. Write to file (protected)
4. Read from file (protected)
5. Print read content (protected)

---

## Common Patterns

### Pattern 1: Safe Output
```
semWait userOutput
print message1
print message2
print message3
semSignal userOutput
```
**Benefit**: Multiple prints are atomic (not interrupted)

### Pattern 2: File Read-Modify-Write
```
semWait file
readFile data.txt
semSignal file

# Modify locally (no lock needed)
assign modified data

semWait file
writeFile data.txt modified
semSignal file
```
**Benefit**: Only hold lock during actual I/O

### Pattern 3: Input with Confirmation
```
semWait userInput
assign response input
semSignal userInput

semWait userOutput
print You said:
print response
semSignal userOutput
```
**Benefit**: User input is always followed by confirmation

### Pattern 4: Guarded Variable Access
```
semWait userOutput
print Processing file:
print filename
print ...
semSignal userOutput
```
**Benefit**: Related output stays together

---

## Performance Tips

1. **Minimize Lock Holding Time**
   ```
   # GOOD - Brief lock
   semWait file
   readFile data.txt
   semSignal file
   ```

   ```
   # SLOW - Lock held too long
   semWait file
   readFile data.txt
   assign x data
   assign y x
   assign z y
   semSignal file
   ```

2. **Batch Related Operations**
   ```
   semWait userOutput
   print line1
   print line2
   print line3
   semSignal userOutput
   ```

3. **Don't Call in Tight Loops**
   ```
   # SLOW
   printFromTo 1 1000  # Each number may need protection separately
   
   # BETTER (if protected as atomic)
   semWait userOutput
   printFromTo 1 1000
   semSignal userOutput
   ```

---

## Troubleshooting System Calls

### Issue: System call returns NULL
**Possible Causes**:
- File doesn't exist (readFile)
- Memory address invalid
- Null parameter passed
- I/O error

**Debug**:
```
[SYSCALL ERROR] readFile: file not found - filename
```

### Issue: Process blocks forever
**Possible Causes**:
- Deadlock (very unlikely in single-threaded environment)
- Forgot to release mutex
- semSignal never called

**Check**:
1. Verify all semWait have matching semSignal
2. Check scheduler blocked queue
3. Look for processes stuck on semWait

### Issue: Output appears scrambled
**Cause**: Missing mutex protection on output

**Fix**:
```
# WRONG
print line1
print line2

# RIGHT
semWait userOutput
print line1
print line2
semSignal userOutput
```

---

## Summary

**Key Principles**:
1. Always protect I/O operations with appropriate mutex
2. Each process can have its own data safely
3. Hold locks for minimal time
4. Follow FIFO ordering (no deadlock)
5. Check return codes for errors

**System Call Protection**:
- readFile ← mutexFile
- writeFile ← mutexFile
- print ← mutexUserOutput
- input ← mutexUserInput
- readMemory ← none (per-process)
- writeMemory ← none (per-process)

---

**Document Version**: 1.0
**Last Updated**: April 13, 2026
