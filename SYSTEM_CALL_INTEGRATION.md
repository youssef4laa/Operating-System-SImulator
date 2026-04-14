# System Call Integration with Interpreter

This document explains how system calls are integrated with the instruction interpreter for seamless process execution.

## Architecture Overview

```
Program File
    ↓
Parser (reads instructions)
    ↓
Interpreter (executes instructions)
    ├→ semWait/semSignal (mutex operations)
    ├→ readFile/writeFile (file operations)
    ├→ print/printFromTo (output operations)
    ├→ input (input operations)
    └→ assign/variables (memory operations indirectly)
    ↓
SystemCall (actual OS call)
    ├→ readFile() - SC1
    ├→ writeFile() - SC2
    ├→ print() - SC3
    ├→ input() - SC4
    ├→ readMemory() - SC5
    └→ writeMemory() - SC6
    ↓
OS Resources (files, console, keyboard, memory)
```

## Interpreter System Call Handlers

The Interpreter class handles each instruction type and calls appropriate system calls:

### Handler 1: handleReadFile()
```java
private static void handleReadFile(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
    String filename = inst.args.get(0);
    String storeVar = inst.args.size() > 1 ? inst.args.get(1) : "fileData";
    
    // Acquire file mutex
    if (mutexFile != null) {
        mutexFile.acquire(pcb, scheduler);
        if (pcb.status.equals("Blocked")) return;  // Wait for resource
    }
    
    // Execute system call
    String fileContents = SystemCall.readFile(filename);  // SC1
    
    // Release file mutex
    if (mutexFile != null) {
        mutexFile.release(scheduler);
    }
    
    if (fileContents == null) {
        throw new Exception("Failed to read file: " + filename);
    }
    
    // Store result
    storeVariable(storeVar, fileContents, pcb, memory);
}
```

**Key Points**:
- Mutex acquired before system call
- System Call is invoked within mutex protection
- Mutex released after system call
- Return value stored in process variable
- Process blocks if mutex not available

### Handler 2: handleWriteFile()
```java
private static void handleWriteFile(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
    String filename = inst.args.get(0);
    String dataStr = String.join(" ", inst.args.subList(1, inst.args.size()));
    Object dataValue = retrieveVariable(dataStr, pcb, memory);
    
    // Acquire file mutex
    if (mutexFile != null) {
        mutexFile.acquire(pcb, scheduler);
        if (pcb.status.equals("Blocked")) return;
    }
    
    // Execute system call
    int ret = SystemCall.writeFile(filename, dataValue.toString());  // SC2
    
    // Release file mutex
    if (mutexFile != null) {
        mutexFile.release(scheduler);
    }
    
    if (ret != SystemCall.SUCCESS) {
        throw new Exception("Write failed: " + filename);
    }
}
```

**Key Points**:
- Retrieves variable value first
- Protects write operation with mutex
- Checks return code for errors
- Throws exception on failure

### Handler 3: handlePrint()
```java
private static void handlePrint(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
    String varName = inst.args.get(0);
    Object value = retrieveVariable(varName, pcb, memory);
    
    if (value == null) {
        throw new Exception("Variable not defined: " + varName);
    }
    
    // Acquire user output mutex
    if (mutexUserOutput != null) {
        mutexUserOutput.acquire(pcb, scheduler);
        if (pcb.status.equals("Blocked")) return;
    }
    
    // Execute system call
    SystemCall.print(value.toString());  // SC3
    
    // Release user output mutex
    if (mutexUserOutput != null) {
        mutexUserOutput.release(scheduler);
    }
}
```

**Key Points**:
- Gets variable value (no lock needed for memory read)
- Acquires output mutex
- Calls print system call
- Releases mutex

### Handler 4: handlePrintFromTo()
```java
private static void handlePrintFromTo(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
    String startStr = inst.args.get(0);
    String endStr = inst.args.get(1);
    
    // Parse values (can be variables or literals)
    int start = parseValue(startStr, pcb, memory);
    int end = parseValue(endStr, pcb, memory);
    
    // Acquire user output mutex
    if (mutexUserOutput != null) {
        mutexUserOutput.acquire(pcb, scheduler);
        if (pcb.status.equals("Blocked")) return;
    }
    
    // Print range atomically
    StringBuilder output = new StringBuilder();
    for (int i = start; i <= end; i++) {
        output.append(i);
        if (i < end) output.append(" ");
    }
    SystemCall.print(output.toString());  // SC3
    
    // Release user output mutex
    if (mutexUserOutput != null) {
        mutexUserOutput.release(scheduler);
    }
}
```

**Key Points**:
- Range printing is atomic (not interrupted)
- All numbers printed together
- Protected by single mutex hold

### Handler 5: handleAssign() with input
```java
private static void handleAssign(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
    String varName = inst.args.get(0);
    List<String> valueArgs = inst.args.subList(1, inst.args.size());
    String valueStr = String.join(" ", valueArgs);
    
    Object value;
    
    // Check if value is special keyword "input"
    if (valueStr.equalsIgnoreCase("input")) {
        // Acquire user input mutex
        if (mutexUserInput != null) {
            mutexUserInput.acquire(pcb, scheduler);
            if (pcb.status.equals("Blocked")) return;
        }
        
        // Execute system call
        value = SystemCall.input();  // SC4
        
        // Release user input mutex
        if (mutexUserInput != null) {
            mutexUserInput.release(scheduler);
        }
    } else if (Parser.isNumber(valueStr)) {
        value = Integer.parseInt(valueStr);
    } else {
        value = valueStr;
    }
    
    // Store variable in memory
    storeVariable(varName, value, pcb, memory);
}
```

**Key Points**:
- Special handling for "input" keyword
- Input protected and result stored
- Other values processed normally

## Instruction Execution Flow

```
Instruction: readFile data.txt
        ↓
Parser.parseInstruction()
        ↓
Interpreter.execute()
        ├→ Fetch: "readFile data.txt"
        ├→ Decode: instruction type = "readFile"
        ├→ Execute: executeInstruction()
        │   └→ handleReadFile()
        │       ├→ Check for blocking
        │       ├→ Acquire mutexFile
        │       ├→ Call SystemCall.readFile("data.txt")
        │       │   ├→ Validate filename
        │       │   ├→ Open file
        │       │   ├→ Read lines
        │       │   ├→ Record statistics
        │       │   └→ Return content
        │       ├→ Release mutexFile
        │       └→ Store in variable
        └→ Update program counter
```

## Blocking Behavior

When a process can't acquire a mutex, the Interpreter handles it properly:

```java
// In any handler
if (mutexFile != null) {
    mutexFile.acquire(pcb, scheduler);  // Returns false if blocked
    if (pcb.status.equals("Blocked")) {
        return;  // Don't execute system call, wait for mutex
    }
}

// System call only happens if mutex acquired
SystemCall.readFile(filename);
```

**Process State Transitions**:
```
Ready → Running
    ↓
Execute semWait userOutput
    ↓
Can acquire? YES → Continue with print
        ↓
        System Call executes
        ↓
        Execute semSignal
        ↓
Ready → Running (next instruction)

vs.

Execute semWait userOutput
    ↓
Can acquire? NO → Block
        ↓
Process.status = "Blocked"
Add to scheduler.blockedQueue
        ↓
Scheduler picks another process
        ↓
Later: Owner releases mutex
        ↓
Process unblocked, moved to readyQueue
        ↓
Process gets turn, retries semWait
        ↓
Now succeeds, continues
```

## Call Sequence Example

### Program Code
```
semWait file
readFile data.txt
semSignal file
print data
```

### Execution Sequence
```
1. Parser: instruction = "semWait file"
    → handleSemWait("file")
    → mutexFile.acquire(process, scheduler)
    → If free: acquired = true, continue
    → If busy: process blocked, return

2. Parser: instruction = "readFile data.txt"
    → handleReadFile()
    → mutexFile already acquired
    → SystemCall.readFile("data.txt")
        → Check filename validity
        → Open file
        → Read content
        → Record statistics
        → Return content string
    → Store in variable "data"

3. Parser: instruction = "semSignal file"
    → handleSemSignal("file")
    → mutexFile.release()
    → If processes waiting: unblock first one
    → Otherwise: unlock completely

4. Parser: instruction = "print data"
    → handlePrint("data")
    → mutexUserOutput.acquire()
    → SystemCall.print(dataContent)
    → mutexUserOutput.release()
```

## Error Handling in Interpreter

When system calls fail, Interpreter may:
1. Throw exception (terminates process)
2. Continue with NULL/error value
3. Log error and proceed

**Example**:
```java
String fileContents = SystemCall.readFile(filename);
if (fileContents == null) {
    throw new Exception("Failed to read: " + filename);
    // OR
    // Process variable set to NULL
}
```

## Performance Characteristics

| Operation | Time | Blocking |
|-----------|------|----------|
| Check variable | Fast (< 1ms) | No |
| Read file | Medium (depends on file size) | Mutex only |
| Write file | Medium (depends on size) | Mutex only |
| Print | Fast (< 1ms) | Mutex only |
| Input | Slow (waiting for user) | Mutex + user |
| Memory ops | Very fast (< 0.1ms) | No |

## Debugging System Call Issues

### Check: Is mutex acquired?
```java
if (pcb.status.equals("Blocked")) {
    System.out.println("Process blocked, waiting for mutex");
    return;
}
```

### Check: Did system call succeed?
```java
String result = SystemCall.readFile(filename);
if (result == null) {
    System.out.println("System call failed");
}
```

### Check: Statistics
```java
SystemCall.printStatistics();
// Shows success/failure rates
```

## Integration Testing

### Test Case 1: File Operations
```
Program: writeFile test.txt "data"
         readFile test.txt
         print test.txt
Verify: File created, read, and printed correctly
```

### Test Case 2: Mutex Protection
```
Process 1: readFile test.txt
Process 2: writeFile test.txt "new"
Verify: Operations don't interleave
```

### Test Case 3: User Input
```
Program: assign x input
         print x
Verify: Input taken and printed correctly
```

### Test Case 4: Error Handling
```
Program: readFile nonexistent.txt
         print test
Verify: Error logged, process continues
```

## Summary

The system call integration works through:
1. **Interpreter** - Recognizes instructions and handles them
2. **Handlers** - Acquire mutexes and manage control flow
3. **System Calls** - Perform actual OS operations
4. **Mutexes** - Protect concurrent access
5. **Statistics** - Track performance

All components work together to provide:
- ✅ Safe concurrent access to resources
- ✅ Proper error handling
- ✅ Performance tracking
- ✅ Easy debugging
- ✅ Fair resource allocation

---

**Document Version**: 1.0
**Last Updated**: April 13, 2026
