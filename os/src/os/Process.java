package os;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Process {
	
    static int nextID = 1; 

    /**
     * Creates a process by reading the program file and allocating memory.
     * Dynamically allocates space for: PCB + instructions + 10 variable slots
     * Implements swap logic if memory is insufficient
     */
    public static PCB createProcess(String fileName, Memory memory) throws Exception {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            
            // Count instructions to calculate required space dynamically
            List<String> instructions = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                instructions.add(line);
            }
            br.close();
            
            // Calculate required size: 1 (PCB) + instructionCount + 10 (variable slots)
            int instructionCount = instructions.size();
            int requiredSize = 1 + instructionCount + 10;
            
            // Validate size
            if (!MemoryManager.validateAllocationSize(requiredSize, memory)) {
                throw new Exception("Invalid allocation size: " + requiredSize + " words exceeds memory capacity");
            }
            
            int start = MemoryManager.findAvailableBlock(memory, requiredSize);
            
            // If no space available, trigger swap (in a real OS, this would be more sophisticated)
            if (start == -1) {
                System.out.println("[MEMORY WARNING] Insufficient space for new process. Free: " + 
                                 MemoryManager.getFreeMemory(memory) + " words, Required: " + requiredSize);
                // Note: Actual swap triggering would happen in the Scheduler
                // For now, we throw an exception
                throw new Exception("No contiguous space (" + requiredSize + " words) available in memory. Swap needed.");
            }
            
            int current = start;
            
            // Create PCB
            PCB pcb = new PCB(nextID++, start, start + requiredSize - 1);
            
            // Store PCB at first position
            memory.write(current++, pcb);
            
            // Store instructions (already read above)
            for (String instruction : instructions) {
                memory.write(current++, instruction);
            }
            
            // Store instruction list in PCB
            pcb.instructionList = instructions;
            pcb.totalInstructions = instructions.size();
            
            // Initialize variable slots in memory (empty/null)
            // Variables will be accessed via symbol table (name -> memory index)
            // Reserve 10 slots for variables after instructions
            for (int i = 0; i < 10; i++) {
                memory.write(current++, null); // Initially empty
            }
            
            // Update PCB bounds to reflect actual usage (already set in PCB constructor)
            // But we already set maxBound correctly: start + requiredSize - 1
            pcb.allocationSize = pcb.maxBound - pcb.minBound + 1;
            
            // Calculate remaining time (approximate burst time)
            pcb.remainingTime = instructions.size();
            
            // Track allocation
            MemoryManager.trackAllocation(pcb.processID, pcb.minBound, pcb.maxBound);
            
            System.out.println("Process " + pcb.processID + " created: instructions=" + 
                             instructions.size() + ", memory=" + start + "-" + pcb.maxBound + 
                             " (" + pcb.allocationSize + " words)");
            
            return pcb;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Terminates a process and deallocates its memory.
     */
    public static void terminateProcess(PCB pcb, Memory memory) throws Exception {
        if (pcb == null) return;
        
        try {
            MemoryManager.deallocateBlock(pcb.minBound, pcb.maxBound, memory);
            MemoryManager.untrackAllocation(pcb.processID);
            pcb.status = "Finished";
            System.out.println("Process " + pcb.processID + " terminated, memory deallocated");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
	
    /**
     * Finds contiguous free memory block of given size.
     * @deprecated Use MemoryManager.findAvailableBlock instead
     */
    @Deprecated
    public static int findFreeSpace(Memory memory, int size) throws Exception {
        return MemoryManager.findAvailableBlock(memory, size);
    }
}

