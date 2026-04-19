package os;

import java.util.*;

/**
 * Interpreter - Executes process instructions one at a time
 * Responsible for: fetch, decode, execute, and state updates
 */
public class Interpreter {
    
    // Static reference to mutexes (initialized by scheduler)
    private static Mutex mutexUserOutput;
    private static Mutex mutexUserInput;
    private static Mutex mutexFile;
    private static Scheduler scheduler;

    /**
     * Exception type for handled runtime errors that should stop a process cleanly
     * without printing a full Java stack trace.
     */
    private static class HandledRuntimeException extends Exception {
        HandledRuntimeException(String message) {
            super(message);
        }
    }

    public static void initialize(Scheduler sched, Mutex out, Mutex in, Mutex file) {
        scheduler = sched;
        mutexUserOutput = out;
        mutexUserInput = in;
        mutexFile = file;
    }

    /**
     * Execute one instruction from the process
     * Fetch -> Decode -> Execute cycle
     * On fatal error, terminates the process immediately
     * @param pcb Process Control Block with instruction list
     * @param memory Main memory for data storage
     */
    public static void execute(PCB pcb, Memory memory) {
        if (pcb == null || memory == null) {
            System.err.println("Interpreter error: null PCB or memory");
            return;
        }
        
        try {
            // Fetch next instruction from instruction list
            String instructionLine = pcb.getNextInstruction();
            
            if (instructionLine == null) {
                // No more instructions - process complete
                pcb.status = "Finished";
                pcb.programCounter = pcb.maxBound + 1;
                return;
            }
            
            // Detect if this is a retry of the same instruction
            boolean isRetry = instructionLine.equals(pcb.lastExecutedInstructionLine);
            pcb.isRetryingInstruction = isRetry;
            
            // Decode instruction
            Parser.Instruction instruction = Parser.parseInstruction(instructionLine);
            
            if (instruction == null) {
                System.out.println("Process " + pcb.processID + ": skipping empty instruction");
                return;
            }
            
            // Note: Instruction execution is already logged in SimulationEngine via debugConsole
            
            // Track status before execution
            String statusBefore = pcb.status;
            
            // Execute instruction
            executeInstruction(instruction, pcb, memory);
            
            // Update last executed instruction (after successful execution or blocking)
            pcb.lastExecutedInstructionLine = instructionLine;
            
            // CRITICAL: Handle blocking scenarios
            if (pcb.status.equals("Blocked")) {
                // Process was blocked (e.g., waiting for mutex)
                // Decrement instruction pointer to retry this instruction
                pcb.retryInstruction();
                // Note: Blocking is already logged in SimulationEngine and Mutex
            } else if (!pcb.status.equals("Terminated")) {
                // Instruction completed successfully AND process not terminated, advance program counter
                pcb.programCounter++;
            }
            // If status is "Terminated", do NOT increment PC; scheduler will handle cleanup
            
        } catch (Exception e) {
            if (e instanceof HandledRuntimeException) {
                String handledMsg = e.getMessage() != null ? e.getMessage() : "Runtime error";
                System.err.println("[ERROR] Execution error in Process " + pcb.processID + ": " + handledMsg);
                pcb.status = "Error";
                return;
            }

            // Classify the exception as fatal or recoverable
            String errorMsg = e.getMessage();
            boolean isFatalError = isFatalException(e, errorMsg);
            
            if (isFatalError) {
                // Fatal error: terminate the process immediately
                System.err.println("[FATAL] Process " + pcb.processID + " terminating: " + errorMsg);
                e.printStackTrace();
                pcb.status = "Terminated";  // NEW STATUS: Terminated
                // DO NOT modify PC so scheduler can identify it as terminated
            } else {
                // Recoverable error: mark as Error but let scheduler decide
                System.err.println("[ERROR] Execution error in Process " + pcb.processID + ": " + errorMsg);
                e.printStackTrace();
                pcb.status = "Error";
            }
        }
    }
    
    /**
     * Helper: Classify if an exception is fatal
     * Fatal errors cause immediate process termination
     * Examples: exhausted variable space, memory violation, undefined variable in critical operation
     */
    private static boolean isFatalException(Exception e, String errorMsg) {
        if (errorMsg == null) return true;  // Default to fatal for unknown errors
        
        boolean isFatal = errorMsg.contains("exhausted variable space") ||
                         errorMsg.contains("Memory access") ||
                         errorMsg.contains("outside process") ||
                         errorMsg.contains("Memory Access Violation") ||
                         errorMsg.contains("invalid address") ||
                         errorMsg.contains("Memory access denied") ||
                         errorMsg.contains("Undefined value") ||
                         errorMsg.contains("Variable not defined" );
        
        return isFatal;
    }

    /**
     * Dispatch instruction to appropriate handler method
     */
    private static void executeInstruction(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String command = inst.type.toLowerCase();
        
        switch (command) {
            case "assign":
                handleAssign(inst, pcb, memory);
                break;
                
            case "print":
                handlePrint(inst, pcb, memory);
                break;
                
            case "printfromto":
                handlePrintFromTo(inst, pcb, memory);
                break;
                
            case "readfile":
                handleReadFile(inst, pcb, memory);
                break;
                
            case "writefile":
                handleWriteFile(inst, pcb, memory);
                break;
                
            case "semwait":
                handleSemWait(inst, pcb);
                break;
                
            case "semsignal":
                handleSemSignal(inst, pcb);
                break;
                
            default:
                throw new Exception("Unknown instruction: " + command);
        }
    }

    /**
     * Instruction: assign variable value
     * Supports: literals, variables, input system call, and readFile system call
     * Stores variable in process memory via symbol table
     */
    private static void handleAssign(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String varName = inst.args.get(0);
        List<String> valueArgs = inst.args.subList(1, inst.args.size());
        String valueStr = String.join(" ", valueArgs);
        
        Object value;
        
        // Check if value is a system call that returns a value
        // Pattern 1: "input" - reads user input
        if (valueStr.equalsIgnoreCase("input")) {
            // Check if we already have a cached input value from retrying this same instruction
            if (pcb.isRetryingInstruction && 
                pcb.lastInputInstructionLine != null && 
                pcb.lastInputInstructionLine.equals(pcb.lastExecutedInstructionLine)) {
                // Use cached input value instead of showing popup again
                value = pcb.lastInputValue;
            } else {
                // First time executing or different instruction - show popup and cache the value
                SystemCall.setCurrentProcessId("P_" + pcb.processID);
                value = SystemCall.input(pcb.isRetryingInstruction);
                SystemCall.clearCurrentProcessId();
                
                if (value == null) {
                    if (SystemCall.consumeInputCancelledFlag()) {
                        // User cancelled GUI input and triggered simulation reset.
                        pcb.status = "Finished";
                        return;
                    }
                    throw new Exception("Failed to read input");
                }
                
                // Cache the input value and the instruction line it came from
                pcb.lastInputValue = (String) value;
                pcb.lastInputInstructionLine = pcb.lastExecutedInstructionLine;
            }
        }
        // Pattern 2: "readFile filename" - reads file contents
        else if (valueStr.toLowerCase().startsWith("readfile")) {
            String[] parts = valueStr.split("\\s+", 2);
            if (parts.length < 2) {
                throw new Exception("readFile requires a filename argument");
            }
            
            String filenameArg = parts[1];
            // Resolve filename: could be a variable or literal
            String filename = resolveStringValue(filenameArg, pcb, memory);
            
            // Call readFile system call
            value = SystemCall.readFile(filename);
            
            if (value == null) {
                showReadFileNotFoundPopup(pcb, filename);
                throw new HandledRuntimeException("Failed to read file: " + filename);
            }
        }
        // Pattern 3: numeric literal
        else if (Parser.isNumber(valueStr)) {
            value = Integer.parseInt(valueStr);
        }
        // Pattern 4: variable reference or string literal
        else {
            // Try to retrieve as variable first
            Object varValue = retrieveVariable(valueStr, pcb, memory);
            if (varValue != null) {
                value = varValue;  // Use variable value
            } else {
                value = valueStr;  // Use as string literal
            }
        }
        
        // Store variable in process memory
        storeVariable(varName, value, pcb, memory);
        
        // Clear input cache after successful assignment
        // (Cache is only used on retry to prevent duplicate popups)
        pcb.lastInputValue = null;
        pcb.lastInputInstructionLine = null;
    }

    /**
     * Instruction: print variable value to console
     * Resolves variable value and prints it
     * No longer protected by mutex - use semWait/semSignal for serialization
     */
    private static void handlePrint(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String varName = inst.args.get(0);
        
        // RESOLVE argument: check if it's a variable, use its value; otherwise use as literal
        String resolvedMessage = resolveArgument(varName, pcb, memory);
        
        // Execute system call with resolved value
        SystemCall.print(resolvedMessage, "P_" + pcb.processID);
    }

    /**
     * Instruction: print range of numbers from start to end
     * No longer protected by mutex - use semWait/semSignal for serialization
     */
    private static void handlePrintFromTo(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String startStr = inst.args.get(0);
        String endStr = inst.args.get(1);
        
        // Parse start and end values (can be variable names or literals)
        int start = parseValue(startStr, pcb, memory);
        int end = parseValue(endStr, pcb, memory);
        
        // Print range directly (no mutex protection)
        StringBuilder output = new StringBuilder();
        for (int i = start; i <= end; i++) {
            output.append(i);
            if (i < end) output.append(" ");
        }
        SystemCall.print(output.toString(), "P_" + pcb.processID);
    }

    /**
     * Instruction: read file contents into variable
     * Resolves filename from variable if needed
     * No longer protected by mutex - use semWait/semSignal for serialization
     */
    private static void handleReadFile(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String filenameArg = inst.args.get(0);
        String storeVar = inst.args.size() > 1 ? inst.args.get(1) : "fileData";
        
        // RESOLVE filename: check if it's a variable, use its value; otherwise use literal
        String resolvedFilename = resolveArgument(filenameArg, pcb, memory);
        
        // Execute system call with resolved filename
        String fileContents = SystemCall.readFile(resolvedFilename);
        
        if (fileContents == null) {
            showReadFileNotFoundPopup(pcb, resolvedFilename);
            throw new HandledRuntimeException("Failed to read file: " + resolvedFilename);
        }
        
        // Store contents in variable
        storeVariable(storeVar, fileContents, pcb, memory);
    }

    /**
     * Instruction: write data to file
     * Resolves both filename and data from variables if needed
     * No longer protected by mutex - use semWait/semSignal for serialization
     */
    private static void handleWriteFile(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String filenameArg = inst.args.get(0);
        String dataArg = String.join(" ", inst.args.subList(1, inst.args.size()));
        
        // RESOLVE filename: check if it's a variable, use its value; otherwise use literal
        String resolvedFilename = resolveArgument(filenameArg, pcb, memory);
        
        // RESOLVE data: check if it's a variable, use its value; otherwise use literal
        String resolvedData = resolveArgument(dataArg, pcb, memory);
        
        // Execute system call with resolved values
        int result = SystemCall.writeFile(resolvedFilename, resolvedData);
        
        if (result != SystemCall.SUCCESS) {
            throw new Exception("Failed to write to file: " + resolvedFilename + " (error code: " + result + ")");
        }
    }

    /**
     * Instruction: semWait - acquire a resource (mutex)
     * Blocks process if resource is not available
     */
    private static void handleSemWait(Parser.Instruction inst, PCB pcb) throws Exception {
        String resourceName = inst.args.get(0).toLowerCase();
        
        Mutex mutex = getMutex(resourceName);
        if (mutex == null) {
            throw new Exception("Unknown resource: " + resourceName);
        }
        
        mutex.acquire(pcb, scheduler);
        // If blocked, executor will handle it
    }

    /**
     * Instruction: semSignal - release a resource (mutex)
     * Unblocks waiting process if any
     */
    private static void handleSemSignal(Parser.Instruction inst, PCB pcb) throws Exception {
        String resourceName = inst.args.get(0).toLowerCase();
        
        Mutex mutex = getMutex(resourceName);
        if (mutex == null) {
            throw new Exception("Unknown resource: " + resourceName);
        }
        
        mutex.release(scheduler);
    }

    /**
     * Helper: Get the variable region bounds for a process
     * Variables are ALWAYS stored in the last 3 words of process memory
     * @return array [varStart, varEnd] representing the 3-word variable region
     */
    private static int[] getVariableRegionBounds(PCB pcb) {
        // Variable region is always: [maxBound-2, maxBound-1, maxBound]
        return new int[] { pcb.maxBound - 2, pcb.maxBound };
    }

    /**
     * Keep symbol table metadata aligned with the fixed 3-slot variable model.
     */
    private static void validateVariableMetadata(PCB pcb) throws Exception {
        if (pcb == null) {
            throw new Exception("PCB is null during variable metadata validation");
        }

        int symbolCount = pcb.symbolTable.size();
        if (symbolCount > PCB.MAX_VARIABLES_PER_PROCESS) {
            throw new Exception("Process " + pcb.processID + " exceeded variable limit: " +
                              symbolCount + "/" + PCB.MAX_VARIABLES_PER_PROCESS);
        }

        // Recover from stale counter state so symbol table is the source of truth.
        if (pcb.variableCount != symbolCount) {
            pcb.variableCount = symbolCount;
        }
    }
    
    /**
     * Variable Management: Store a variable in process memory
     * Creates symbol table entry pointing to memory location
     * Enforces strict bounds: variables go in reserved 3-word region only
     */
    private static void storeVariable(String varName, Object value, PCB pcb, Memory memory) throws Exception {
        if (pcb == null || memory == null) {
            throw new Exception("Invalid PCB or memory for variable storage");
        }

        validateVariableMetadata(pcb);
        
        // Check if variable already exists
        if (pcb.symbolTable.containsKey(varName)) {
            int address = pcb.symbolTable.get(varName);
            // Verify address is within variable region
            int[] varBounds = getVariableRegionBounds(pcb);
            if (address < varBounds[0] || address > varBounds[1]) {
                throw new Exception("Variable " + varName + " stored at invalid address " + address);
            }
            if (MemoryManager.isAccessAllowed(pcb, address)) {
                memory.write(address, value);
            } else {
                throw new Exception("Memory access denied for variable " + varName + " at address " + address);
            }
        } else {
            // Allocate new variable in process memory
            // Variables MUST go in the reserved variable region: last 3 words
            // Variable region: [maxBound-2, maxBound-1, maxBound]
            int[] varBounds = getVariableRegionBounds(pcb);
            int varRegionStart = varBounds[0];
            int varRegionEnd = varBounds[1];

            if (varRegionStart < pcb.minBound || varRegionEnd > pcb.maxBound) {
                throw new Exception("Invalid variable region for process " + pcb.processID + ": [" +
                                  varRegionStart + "," + varRegionEnd + "] out of bounds [" +
                                  pcb.minBound + "," + pcb.maxBound + "]");
            }

            if (pcb.variableCount >= PCB.MAX_VARIABLES_PER_PROCESS ||
                pcb.symbolTable.size() >= PCB.MAX_VARIABLES_PER_PROCESS) {
                throw new Exception("Process " + pcb.processID +
                                  " has exhausted variable space (3 words max)");
            }

            int allocAddr = -1;
            
            // Scan the variable region for empty slots
            for (int i = varRegionStart; i <= varRegionEnd; i++) {
                Object current = memory.read(i);
                // Check if slot is empty (null)
                if (current == null) {
                    allocAddr = i;  // Use first empty slot
                    break;
                }
            }
            
            if (allocAddr == -1) {
                // No empty slots found
                throw new Exception("Process " + pcb.processID + " has exhausted variable space (3 words max)");
            }
            
            // Store variable at the allocated address
            memory.write(allocAddr, value);
            pcb.symbolTable.put(varName, allocAddr);
            pcb.variableCount++;

            validateVariableMetadata(pcb);
        }
    }

    /**
     * Variable Management: Retrieve a variable value from process memory
     * Validates that variable is stored within process bounds
     */
    private static Object retrieveVariable(String varName, PCB pcb, Memory memory) throws Exception {
        if (pcb == null || !pcb.symbolTable.containsKey(varName)) {
            return null; // Variable not defined
        }
        
        int address = pcb.symbolTable.get(varName);
        
        // Verify variable is within process bounds
        if (!MemoryManager.isAccessAllowed(pcb, address)) {
            throw new Exception("Variable " + varName + " at address " + address + 
                              " is outside process " + pcb.processID + " bounds");
        }
        
        return memory.read(address);
    }

    /**
     * Helper: Resolve an argument (could be variable name or literal)
     * Looks up variables in the symbol table, returns the stored value or literal
     * @param argument The argument string (variable name or literal)
     * @param pcb Process Control Block
     * @param memory Main memory
     * @return Resolved value (from symbol table) or the original argument if not a variable
     */
    private static String resolveArgument(String argument, PCB pcb, Memory memory) throws Exception {
        if (argument == null) {
            throw new Exception("Cannot resolve null argument");
        }
        
        // Check if argument is a variable in the symbol table
        if (pcb.symbolTable.containsKey(argument)) {
            Object value = retrieveVariable(argument, pcb, memory);
            if (value != null) {
                return value.toString();
            }
        }
        
        // Not a variable, return as literal
        return argument;
    }
    
    /**
     * Helper: Resolve a string value that could be a variable or literal
     */
    private static String resolveStringValue(String value, PCB pcb, Memory memory) throws Exception {
        // Try to retrieve as variable first
        Object varValue = retrieveVariable(value, pcb, memory);
        if (varValue != null) {
            return varValue.toString();  // Return variable value
        }
        return value;  // Return as literal string
    }

    /**
     * Helper: Parse a value that could be a variable name or literal
     */
    private static int parseValue(String value, PCB pcb, Memory memory) throws Exception {
        Integer parsed = Parser.tryParseInt(value);
        if (parsed != null) {
            return parsed; // Direct integer literal
        }
        
        // Try to retrieve as variable
        Object varValue = retrieveVariable(value, pcb, memory);
        if (varValue != null && varValue instanceof Integer) {
            return (Integer) varValue;
        } else if (varValue != null) {
            try {
                return Integer.parseInt(varValue.toString());
            } catch (NumberFormatException e) {
                throw new Exception("Value not numeric: " + value);
            }
        }
        
        throw new Exception("Undefined value: " + value);
    }

    /**
     * Show an error popup for readFile file-not-found scenarios in GUI mode.
     */
    private static void showReadFileNotFoundPopup(PCB pcb, String filename) {
        String processId = pcb != null ? "P_" + pcb.processID : "Unknown";
        String title = "Process " + processId + " - File Read Error";
        String message = "File not found:\n" + filename + "\n\nThis process will be stopped.";

        SystemCall.showProcessOutputMessage(processId, title, message, true);
    }

    /**
     * Helper: Get mutex by resource name
     */
    private static Mutex getMutex(String resourceName) {
        switch (resourceName) {
            case "useroutput": return mutexUserOutput;
            case "userinput": return mutexUserInput;
            case "file": return mutexFile;
            default: return null;
        }
    }
}
