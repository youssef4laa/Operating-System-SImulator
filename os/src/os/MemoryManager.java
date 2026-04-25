package os;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

/**
 * MemoryManager handles memory allocation, deallocation, and swapping
 */
public class MemoryManager {
    
    // Track allocated memory blocks: processID -> (minBound, maxBound)
    private static Map<Integer, int[]> allocatedBlocks = new HashMap<>();
    
    // Disk folder for swapped processes
    private static final String DISK_FOLDER = "disk";
    
    static {
        // Initialize disk folder
        File diskDir = new File(DISK_FOLDER);
        if (!diskDir.exists()) {
            diskDir.mkdir();
            System.out.println("Created disk swap folder: " + DISK_FOLDER);
        }
    }

    /**
     * Reset static memory-management state between simulation runs.
     */
    public static void resetState() {
        allocatedBlocks.clear();
    }

    /**
     * Validates that the required size is within reasonable bounds
     * @return true if size is valid
     */
    public static boolean validateAllocationSize(int requiredSize, Memory memory) {
        if (requiredSize <= 0) {
            System.err.println("Invalid allocation size: " + requiredSize);
            return false;
        }
        if (requiredSize > memory.getSize()) {
            System.err.println("Allocation size " + requiredSize + " exceeds total memory " + memory.getSize());
            return false;
        }
        return true;
    }
    
    /**
     * Calculates the minimum memory needed for a process
     * @return minimum words needed (1 PCB + code + 3 variable words)
     */
    public static int calculateMinimumSize(int numInstructions) {
        // Memory layout: 1 (PCB) + numInstructions + 3 (variables)
        return 1 + Math.max(numInstructions, 1) + 3;
    }
    
    /**
     * Get total memory currently in use
     */
    public static int getUsedMemory(Memory memory) throws Exception {
        int used = 0;
        for (int i = 0; i < memory.getSize(); i++) {
            if (memory.read(i) != null) {
                used++;
            }
        }
        return used;
    }
    
    /**
     * Get total free memory
     */
    public static int getFreeMemory(Memory memory) throws Exception {
        return memory.getSize() - getUsedMemory(memory);
    }

    /**
     * Finds a contiguous free memory block of required size using first-fit algorithm
     * @return Starting address of free block, or -1 if not found
     */
    public static int findAvailableBlock(Memory memory, int requiredSize) throws Exception {
        boolean[] occupied = new boolean[memory.getSize()];

        // Treat every tracked process range as occupied, even if some words are null.
        for (int[] bounds : allocatedBlocks.values()) {
            int startBound = Math.max(0, bounds[0]);
            int endBound = Math.min(memory.getSize() - 1, bounds[1]);
            for (int i = startBound; i <= endBound; i++) {
                occupied[i] = true;
            }
        }

        // Defensive fallback: if any non-null word exists outside tracked ranges, treat it as occupied.
        for (int i = 0; i < memory.getSize(); i++) {
            if (memory.read(i) != null) {
                occupied[i] = true;
            }
        }

        int count = 0;
        int start = -1;

        for (int i = 0; i < memory.getSize(); i++) {
            if (!occupied[i]) {
                if (start == -1) start = i;
                count++;
                if (count >= requiredSize) return start;
            } else {
                start = -1;
                count = 0;
            }
        }
        return -1;
    }

    /**
     * Ensures a contiguous block of required size is available, swapping out processes as needed.
     * @return starting address of available block or -1 when unable to obtain contiguous allocation.
     */
    public static int ensureContiguousBlock(Memory memory, int requiredSize) throws Exception {
        int start = findAvailableBlock(memory, requiredSize);
        if (start != -1) {
            return start;
        }

        // Keep swapping until a contiguous block is available or no process can be swapped.
        while (true) {
            int freeMemory = getFreeMemory(memory);
            if (freeMemory >= requiredSize) {
                if (!swapOutProcessByLongestTime(memory)) {
                    return -1;
                }
                start = findAvailableBlock(memory, requiredSize);
                if (start != -1) {
                    return start;
                }
            } else {
                if (!swapOutProcessByLongestTime(memory)) {
                    return -1;
                }
            }
        }
    }
    
    /**
     * Deallocates a memory block and clears all data
     * @param minBound Starting address (inclusive)
     * @param maxBound Ending address (inclusive)
     */
    public static void deallocateBlock(int minBound, int maxBound, Memory memory) throws Exception {
        if (minBound < 0 || maxBound >= memory.getSize() || minBound > maxBound) {
            throw new Exception("Invalid memory bounds: [" + minBound + ", " + maxBound + "]");
        }
        
        for (int i = minBound; i <= maxBound; i++) {
            memory.write(i, null);
        }
        // Note: Memory deallocation is logged at higher level (SimulationEngine)
    }
    
    /**
     * Track memory allocation for a process
     */
    public static void trackAllocation(int processID, int minBound, int maxBound) {
        for (Map.Entry<Integer, int[]> entry : allocatedBlocks.entrySet()) {
            int otherProcessId = entry.getKey();
            int[] bounds = entry.getValue();
            if (otherProcessId != processID && rangesOverlap(minBound, maxBound, bounds[0], bounds[1])) {
                throw new IllegalStateException(
                    "Memory overlap detected: P" + processID + " [" + minBound + "-" + maxBound +
                    "] intersects P" + otherProcessId + " [" + bounds[0] + "-" + bounds[1] + "]"
                );
            }
        }
        allocatedBlocks.put(processID, new int[]{minBound, maxBound});
    }

    private static boolean rangesOverlap(int aStart, int aEnd, int bStart, int bEnd) {
        return aStart <= bEnd && bStart <= aEnd;
    }
    
    /**
     * Untrack memory allocation when process is deallocated
     */
    public static void untrackAllocation(int processID) {
        allocatedBlocks.remove(processID);
    }
    
    /**
     * Get allocation for a specific process
     */
    public static int[] getAllocationBounds(int processID) {
        return allocatedBlocks.get(processID);
    }
    
    /**
     * Display all current allocations
     */
    public static void displayAllocations() {
        System.out.println("\n=== Current Memory Allocations ===");
        if (allocatedBlocks.isEmpty()) {
            System.out.println("No processes allocated");
        } else {
            for (Map.Entry<Integer, int[]> entry : allocatedBlocks.entrySet()) {
                int[] bounds = entry.getValue();
                int size = bounds[1] - bounds[0] + 1;
                System.out.printf("Process %d: [%d-%d] (size: %d words)%n", 
                    entry.getKey(), bounds[0], bounds[1], size);
            }
        }
        System.out.println("===================================\n");
    }
    
    /**
     * Checks if a process is allowed to access a memory address
     * @return true if address is within process bounds, false otherwise
     */
    public static boolean isAccessAllowed(PCB process, int address) {
        if (process == null) {
            System.err.println("Memory access error: PCB is null");
            return false;
        }
        
        if (address < process.minBound || address > process.maxBound) {
            System.err.println("Memory access violation: Process " + process.processID + 
                             " tried to access address " + address + 
                             " (bounds: " + process.minBound + "-" + process.maxBound + ")");
            return false;
        }
        
        return true;
    }

    /**
     * Swaps a process out to disk
     * Saves PCB state, instruction list, and memory contents
     */
    private static String serializeMemoryObject(Object data) {
        if (data == null) {
            return "NULL";
        }
        if (data instanceof PCB) {
            return "PCB";
        }
        if (data instanceof String) {
            String encoded = Base64.getEncoder().encodeToString(((String) data).getBytes(StandardCharsets.UTF_8));
            return "STRING:" + encoded;
        }
        if (data instanceof Integer) {
            return "INTEGER:" + data;
        }
        if (data instanceof Long) {
            return "LONG:" + data;
        }
        if (data instanceof Double) {
            return "DOUBLE:" + data;
        }
        if (data instanceof Boolean) {
            return "BOOLEAN:" + data;
        }
        // Fallback: preserve string form with encoding
        String fallback = Base64.getEncoder().encodeToString(data.toString().getBytes(StandardCharsets.UTF_8));
        return "STRING:" + fallback;
    }

    private static Object deserializeMemoryObject(String serialized) throws Exception {
        if (serialized == null || serialized.equals("NULL")) {
            return null;
        }
        if (serialized.equals("PCB")) {
            return "PCB"; // handled by swapIn caller
        }
        if (serialized.startsWith("STRING:")) {
            String payload = serialized.substring("STRING:".length());
            return new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
        }
        if (serialized.startsWith("INTEGER:")) {
            return Integer.parseInt(serialized.substring("INTEGER:".length()));
        }
        if (serialized.startsWith("LONG:")) {
            return Long.parseLong(serialized.substring("LONG:".length()));
        }
        if (serialized.startsWith("DOUBLE:")) {
            return Double.parseDouble(serialized.substring("DOUBLE:".length()));
        }
        if (serialized.startsWith("BOOLEAN:")) {
            return Boolean.parseBoolean(serialized.substring("BOOLEAN:".length()));
        }
        throw new Exception("Unsupported serialized memory object: " + serialized);
    }

    public static void swapOut(PCB pcb, Memory memory) throws Exception {
        System.out.println("Swapping OUT Process ID: " + pcb.processID);
        
        String filename = DISK_FOLDER + File.separator + "Disk_Process_" + pcb.processID + ".txt";
        PrintWriter pw = new PrintWriter(new FileWriter(filename));
        
        // Save PCB state
        pw.println(pcb.processID);
        pw.println(pcb.status);
        pw.println(pcb.programCounter);
        pw.println(pcb.instructionPointer);
        pw.println(pcb.totalInstructions);
        pw.println(pcb.variableCount);
        pw.println(pcb.allocationSize);
        
        // Save instruction list
        pw.println(pcb.instructionList.size());
        for (String instruction : pcb.instructionList) {
            pw.println(serializeMemoryObject(instruction));
        }
        
        // Save symbol table (variable mappings)
        pw.println(pcb.symbolTable.size());
        for (Map.Entry<String, Integer> entry : pcb.symbolTable.entrySet()) {
            pw.println(serializeMemoryObject(entry.getKey()) + ":" + entry.getValue());
        }
        
        // Save memory contents and clear memory
        for (int i = pcb.minBound; i <= pcb.maxBound; i++) {
            Object data = memory.read(i);
            pw.println(serializeMemoryObject(data));
            memory.write(i, null);
        }
        pw.close();
        
        // Untrack allocation since it's no longer in memory
        untrackAllocation(pcb.processID);
        pcb.status = "Swapped";
        System.out.println("Process " + pcb.processID + " swapped to disk: " + filename);
    }

    /**
     * Swaps a process back in from disk
     * Restores PCB state, instruction list, and memory contents
     */
    public static void swapIn(PCB pcb, Memory memory, int newStart) throws Exception {
        System.out.println("Swapping IN Process ID: " + pcb.processID);
        
        String filename = DISK_FOLDER + File.separator + "Disk_Process_" + pcb.processID + ".txt";
        File file = new File(filename);
        if (!file.exists()) {
            throw new Exception("Swap file not found: " + filename);
        }

        BufferedReader br = new BufferedReader(new FileReader(file));
        
        // Restore PCB state
        int processID = Integer.parseInt(br.readLine());
        String savedStatus = br.readLine();
        int programCounter = Integer.parseInt(br.readLine());
        int instructionPointer = Integer.parseInt(br.readLine());
        int totalInstructions = Integer.parseInt(br.readLine());
        int variableCount = Integer.parseInt(br.readLine());
        int allocationSize = Integer.parseInt(br.readLine());
        
        if (processID != pcb.processID) {
            br.close();
            throw new Exception("Swap file process ID mismatch: expected " + pcb.processID + " but found " + processID);
        }
        
        // Restore instruction list
        int instructionCount = Integer.parseInt(br.readLine());
        List<String> instructions = new ArrayList<>();
        for (int i = 0; i < instructionCount; i++) {
            String instructionLine = br.readLine();
            if (instructionLine == null) {
                br.close();
                throw new Exception("Unexpected end of swap file while restoring instructions for process " + pcb.processID);
            }
            Object value = deserializeMemoryObject(instructionLine);
            if (value instanceof String) {
                instructions.add((String) value);
            } else {
                throw new Exception("Invalid instruction serialization for process " + pcb.processID);
            }
        }
        
        // Restore symbol table
        int symbolTableSize = Integer.parseInt(br.readLine());
        Map<String, Integer> symbolTable = new HashMap<>();
        for (int i = 0; i < symbolTableSize; i++) {
            String line = br.readLine();
            if (line == null) {
                br.close();
                throw new Exception("Unexpected end of swap file while restoring symbol table for process " + pcb.processID);
            }
            int separatorIndex = line.lastIndexOf(":");
            if (separatorIndex == -1) {
                br.close();
                throw new Exception("Invalid symbol table entry in swap file: " + line);
            }
            String varName = line.substring(0, separatorIndex);
            int oldAddress = Integer.parseInt(line.substring(separatorIndex + 1));
            int newAddress = newStart + (oldAddress - pcb.minBound);
            symbolTable.put(varName, newAddress);
        }
        
        // Restore memory contents
        int current = newStart;
        for (int i = 0; i < allocationSize; i++) {
            String line = br.readLine();
            if (line == null) {
                br.close();
                throw new Exception("Unexpected end of swap file while restoring memory contents for process " + pcb.processID);
            }
            if (i == 0) {
                if (!"PCB".equals(line)) {
                    br.close();
                    throw new Exception("Swap file corrupted: expected PCB marker at start of memory contents for process " + pcb.processID);
                }
                memory.write(current++, pcb);
                continue;
            }
            Object data = deserializeMemoryObject(line);
            memory.write(current++, data);
        }
        br.close();
        
        // Update PCB with new state
        pcb.status = "Ready";
        pcb.programCounter = programCounter;
        pcb.instructionPointer = instructionPointer;
        pcb.totalInstructions = totalInstructions;
        pcb.variableCount = variableCount;
        pcb.allocationSize = allocationSize;
        pcb.instructionList = instructions;
        pcb.symbolTable = symbolTable;
        pcb.minBound = newStart;
        pcb.maxBound = newStart + allocationSize - 1;
        
        // Track new allocation
        trackAllocation(pcb.processID, newStart, pcb.maxBound);
        
        // Delete swap file
        file.delete();
        System.out.println("Process " + pcb.processID + " swapped in at addresses " + 
                         newStart + "-" + pcb.maxBound);
    }
    
    /**
     * Checks if memory pressure requires swapping, triggers swap if needed
     * Swaps out process with longest remaining time until enough space is freed
     * @return true if space is now available, false if swap was not possible
     */
    public static boolean checkAndTriggerSwap(Memory memory, int requiredSize) throws Exception {
        int freeMemory = getFreeMemory(memory);
        
        if (freeMemory >= requiredSize) {
            return true; // Enough space already available
        }
        
        System.out.println("[SWAP TRIGGER] Memory pressure detected. Free: " + freeMemory + 
                         " words, Required: " + requiredSize + " words");
        
        // Keep swapping until we have enough space
        while (freeMemory < requiredSize) {
            if (!swapOutProcessByLongestTime(memory)) {
                System.out.println("[SWAP FAILED] No process available to swap out");
                return false; // No process to swap
            }
            freeMemory = getFreeMemory(memory);
            System.out.println("[SWAP COMPLETE] After swap: " + freeMemory + " words free");
        }
        
        return true; // Space is now available
    }
    
    /**
     * Finds the process in memory with longest remaining time and swaps it out
     * @return true if a process was swapped, false if no process to swap
     */
    public static boolean swapOutProcessByLongestTime(Memory memory) throws Exception {
        PCB victimProcess = null;
        int longestTime = -1;
        
        // Scan all memory to find all loaded PCBs
        for (int i = 0; i < memory.getSize(); i++) {
            Object obj = memory.read(i);
            if (obj instanceof PCB) {
                PCB pcb = (PCB) obj;
                // Find process with longest remaining time (that's not already finished)
                if (!pcb.status.equals("Finished") && pcb.remainingTime > longestTime) {
                    victimProcess = pcb;
                    longestTime = pcb.remainingTime;
                }
            }
        }
        
        if (victimProcess == null) {
            System.out.println("[SWAP] No eligible process to swap out");
            return false;
        }
        
        System.out.println("[SWAP] Selected Process " + victimProcess.processID + 
                         " (remaining time: " + longestTime + " instructions) for swapout");
        swapOut(victimProcess, memory);
        return true;
    }
}