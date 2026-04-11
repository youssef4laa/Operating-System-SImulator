package os;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Process {
	
    static int nextID = 1; 

    /**
     * Creates a process by reading the program file and allocating memory.
     * Allocates space for: PCB + instructions + 3 variables
     * Implements swap logic if memory is insufficient
     */
    public static PCB createProcess(String fileName, Memory memory) throws Exception {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            
            // Calculate required space: PCB + instructions + 3 variables (min 10 words)
            int requiredSize = 15;
            
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
            PCB pcb = new PCB(nextID++, start, start + 14);
            
            // Store PCB at first position
            memory.write(current++, pcb);
            
            // Read and store instructions (un-parsed lines)
            List<String> instructions = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                instructions.add(line);
                memory.write(current++, line);
            }
            br.close();
            
            // Store instruction list in PCB
            pcb.instructionList = instructions;
            pcb.totalInstructions = instructions.size();
            
            // Initialize 3 variable slots in memory
            // Variables will be accessed via symbol table (name -> memory index)
            int var1Address = current;
            int var2Address = current + 1;
            int var3Address = current + 2;
            
            memory.write(var1Address, null); // Initially empty
            memory.write(var2Address, null);
            memory.write(var3Address, null);
            
            // Update PCB bounds to reflect actual usage
            pcb.maxBound = current + 2;
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

