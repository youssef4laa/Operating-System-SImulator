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
            
            // CRITICAL FIX: Only increment program counter if instruction didn't block
            // If instruction caused a block (e.g., waiting for user input), 
            // don't increment so when process resumes, it retries this same instruction
            if (!pcb.status.equals("Blocked")) {
                pcb.programCounter++;
            } else {
                System.out.println("Process " + pcb.processID + " blocked on instruction: " + instruction);
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
     * Stores variable in process memory via symbol table
     */
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
                if (pcb.status.equals("Blocked")) return; // Blocked waiting for resource
            }
            value = SystemCall.input();
            if (mutexUserInput != null) {
                mutexUserInput.release(scheduler);
            }
        } else if (Parser.isNumber(valueStr)) {
            // Numeric value
            value = Integer.parseInt(valueStr);
        } else {
            // String value
            value = valueStr;
        }
        
        // Store variable in process memory
        storeVariable(varName, value, pcb, memory);
    }

    /**
     * Instruction: print variable value to console
     * Requires userOutput mutex
     */
    private static void handlePrint(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String varName = inst.args.get(0);
        
        // Retrieve variable value
        Object value = retrieveVariable(varName, pcb, memory);
        
        if (value == null) {
            throw new Exception("Variable not defined: " + varName);
        }
        
        // Acquire user output mutex
        if (mutexUserOutput != null) {
            mutexUserOutput.acquire(pcb, scheduler);
            if (pcb.status.equals("Blocked")) return; // Blocked waiting for resource
        }
        
        // Execute system call
        SystemCall.print(value.toString());
        
        // Release user output mutex
        if (mutexUserOutput != null) {
            mutexUserOutput.release(scheduler);
        }
    }

    /**
     * Instruction: print range of numbers from start to end
     * Requires userOutput mutex
     */
    private static void handlePrintFromTo(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String startStr = inst.args.get(0);
        String endStr = inst.args.get(1);
        
        // Parse start and end values (can be variable names or literals)
        int start = parseValue(startStr, pcb, memory);
        int end = parseValue(endStr, pcb, memory);
        
        // Acquire user output mutex
        if (mutexUserOutput != null) {
            mutexUserOutput.acquire(pcb, scheduler);
            if (pcb.status.equals("Blocked")) return; // Blocked waiting for resource
        }
        
        // Print range
        StringBuilder output = new StringBuilder();
        for (int i = start; i <= end; i++) {
            output.append(i);
            if (i < end) output.append(" ");
        }
        SystemCall.print(output.toString());
        
        // Release user output mutex
        if (mutexUserOutput != null) {
            mutexUserOutput.release(scheduler);
        }
    }

    /**
     * Instruction: read file contents into variable
     * Requires file mutex
     */
    private static void handleReadFile(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String filename = inst.args.get(0);
        String storeVar = inst.args.size() > 1 ? inst.args.get(1) : "fileData";
        
        // Acquire file mutex
        if (mutexFile != null) {
            mutexFile.acquire(pcb, scheduler);
            if (pcb.status.equals("Blocked")) return; // Blocked waiting for resource
        }
        
        // Execute system call
        String fileContents = SystemCall.readFile(filename);
        
        // Release file mutex
        if (mutexFile != null) {
            mutexFile.release(scheduler);
        }
        
        if (fileContents == null) {
            throw new Exception("Failed to read file: " + filename);
        }
        
        // Store contents in variable
        storeVariable(storeVar, fileContents, pcb, memory);
    }

    /**
     * Instruction: write data to file
     * Requires file mutex
     */
    private static void handleWriteFile(Parser.Instruction inst, PCB pcb, Memory memory) throws Exception {
        String filename = inst.args.get(0);
        String dataStr = String.join(" ", inst.args.subList(1, inst.args.size()));
        
        // Get data value (could be variable or literal)
        Object dataValue = retrieveVariable(dataStr, pcb, memory);
        if (dataValue == null) {
            dataValue = dataStr; // Use literal string if not a variable
        }
        
        // Acquire file mutex
        if (mutexFile != null) {
            mutexFile.acquire(pcb, scheduler);
            if (pcb.status.equals("Blocked")) return; // Blocked waiting for resource
        }
        
        // Execute system call
        int result = SystemCall.writeFile(filename, dataValue.toString());
        
        // Release file mutex
        if (mutexFile != null) {
            mutexFile.release(scheduler);
        }
        
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
            // Find first empty slot after instructions
            int allocAddr = -1;
            for (int i = pcb.minBound; i <= pcb.maxBound; i++) {
                Object current = memory.read(i);
                if (current == null || (current instanceof String && ((String)current).isEmpty())) {
                    allocAddr = i;
                    break;
                }
            }
            
            if (allocAddr == -1) {
                throw new Exception("Process " + pcb.processID + " ran out of variable space");
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
