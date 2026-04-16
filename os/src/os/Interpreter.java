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

    public static void initialize(Scheduler sched, Mutex out, Mutex in, Mutex file) {
        scheduler = sched;
        mutexUserOutput = out;
        mutexUserInput = in;
        mutexFile = file;
    }

    /**
     * Execute one instruction from the process
     * Fetch -> Decode -> Execute cycle
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
            
            // Log execution
            System.out.println("Process " + pcb.processID + " executing: " + instruction);
            
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
                System.out.println("Process " + pcb.processID + " blocked on instruction: " + instruction);
            } else {
                // Instruction completed successfully, advance program counter
                pcb.programCounter++;
            }
            
        } catch (Exception e) {
            System.err.println("Execution error in Process " + pcb.processID + ": " + e.getMessage());
            e.printStackTrace();
            pcb.status = "Error";
            // Process terminates on unhandled error
        }
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
            // Set process ID context for input dialog
            SystemCall.setCurrentProcessId("P_" + pcb.processID);
            // Suppress logging on retry of this instruction
            value = SystemCall.input(pcb.isRetryingInstruction);
            SystemCall.clearCurrentProcessId();
            
            if (value == null) {
                throw new Exception("Failed to read input");
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
                throw new Exception("Failed to read file: " + filename);
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
    }

    /**
     * Instruction: print variable value to console
     * No longer protected by mutex - use semWait/semSignal for serialization
     */
    private static void handlePrint(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String varName = inst.args.get(0);
        
        // Retrieve variable value
        Object value = retrieveVariable(varName, pcb, memory);
        
        if (value == null) {
            throw new Exception("Variable not defined: " + varName);
        }
        
        // Execute system call directly (no mutex protection)
        SystemCall.print(value.toString());
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
        SystemCall.print(output.toString());
    }

    /**
     * Instruction: read file contents into variable
     * No longer protected by mutex - use semWait/semSignal for serialization
     */
    private static void handleReadFile(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String filename = inst.args.get(0);
        String storeVar = inst.args.size() > 1 ? inst.args.get(1) : "fileData";
        
        // Execute system call directly (no mutex protection)
        String fileContents = SystemCall.readFile(filename);
        
        if (fileContents == null) {
            throw new Exception("Failed to read file: " + filename);
        }
        
        // Store contents in variable
        storeVariable(storeVar, fileContents, pcb, memory);
    }

    /**
     * Instruction: write data to file
     * No longer protected by mutex - use semWait/semSignal for serialization
     */
    private static void handleWriteFile(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String filename = inst.args.get(0);
        String dataStr = String.join(" ", inst.args.subList(1, inst.args.size()));
        
        // Get data value (could be variable or literal)
        Object dataValue = retrieveVariable(dataStr, pcb, memory);
        if (dataValue == null) {
            dataValue = dataStr; // Use literal string if not a variable
        }
        
        // Execute system call directly (no mutex protection)
        int result = SystemCall.writeFile(filename, dataValue.toString());
        
        if (result != SystemCall.SUCCESS) {
            throw new Exception("Failed to write to file: " + filename);
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
     * Variable Management: Store a variable in process memory
     * Creates symbol table entry pointing to memory location
     */
    private static void storeVariable(String varName, Object value, PCB pcb, Memory memory) throws Exception {
        // Check if variable already exists
        if (pcb.symbolTable.containsKey(varName)) {
            int address = pcb.symbolTable.get(varName);
            if (MemoryManager.isAccessAllowed(pcb, address)) {
                memory.write(address, value);
            }
        } else {
            // Allocate new variable in process memory
            // Variables must go in the reserved variable region: last 3 words
            // Variable region: [maxBound-2, maxBound-1, maxBound]
            int varRegionStart = pcb.maxBound - 2;
            int allocAddr = -1;
            
            for (int i = varRegionStart; i <= pcb.maxBound; i++) {
                Object current = memory.read(i);
                if (current == null || (current instanceof String && ((String)current).isEmpty())) {
                    allocAddr = i;
                    break;
                }
            }
            
            if (allocAddr == -1) {
                throw new Exception("Process " + pcb.processID + " has exhausted variable space (3 words max)");
            }
            
            // Store variable
            memory.write(allocAddr, value);
            pcb.symbolTable.put(varName, allocAddr);
        }
    }

    /**
     * Variable Management: Retrieve a variable value from process memory
     */
    private static Object retrieveVariable(String varName, PCB pcb, Memory memory) throws Exception {
        if (!pcb.symbolTable.containsKey(varName)) {
            return null; // Variable not defined
        }
        
        int address = pcb.symbolTable.get(varName);
        if (MemoryManager.isAccessAllowed(pcb, address)) {
            return memory.read(address);
        }
        
        return null;
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
