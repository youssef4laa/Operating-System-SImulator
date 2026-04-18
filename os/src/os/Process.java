package os;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Process {
	
    static int nextID = 1; 

    /**
     * Reset process ID sequence for a fresh simulation run.
     */
    public static void resetIdCounter() {
        nextID = 1;
    }

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
            
            // Calculate required size: 1 (PCB) + instructionCount + 3 (variable slots)
            // Memory layout: [PCB | Instructions | Variables(3 words)]
            int instructionCount = instructions.size();
            int requiredSize = 1 + instructionCount + 3;
            
            // Validate size
            if (!MemoryManager.validateAllocationSize(requiredSize, memory)) {
                throw new Exception("Invalid allocation size: " + requiredSize + " words exceeds memory capacity");
            }
            
            // Check memory pressure and trigger swap if needed
            if (!MemoryManager.checkAndTriggerSwap(memory, requiredSize)) {
                throw new Exception("Memory swap failed: Cannot free space for process (" + 
                                   requiredSize + " words required)");
            }
            
            int start = MemoryManager.findAvailableBlock(memory, requiredSize);
            
            // If no space available even after swap, fail
            if (start == -1) {
                System.out.println("[MEMORY ERROR] Insufficient space for new process. Free: " + 
                                 MemoryManager.getFreeMemory(memory) + " words, Required: " + requiredSize);
                throw new Exception("No contiguous space (" + requiredSize + " words) available in memory. Swap failed.");
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
            // Reserve exactly 3 slots for variables after instructions
            for (int i = 0; i < 3; i++) {
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
        terminateProcess(pcb, memory, null);
    }

    /**
     * Terminates a process, releases owned mutexes, and deallocates memory.
     */
    public static void terminateProcess(PCB pcb, Memory memory, Scheduler scheduler) throws Exception {
        if (pcb == null) return;
        
        try {
            cleanupOwnedMutexes(pcb, scheduler);
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
	 * Releases all mutexes currently owned by a process.
	 */
	public static void cleanupOwnedMutexes(PCB pcb, Scheduler scheduler) {
		if (pcb == null || pcb.ownedMutexes == null || pcb.ownedMutexes.isEmpty()) {
			return;
		}

		List<Mutex> ownedCopy = new ArrayList<>(pcb.ownedMutexes);
		for (Mutex mutex : ownedCopy) {
			try {
				if (scheduler != null) {
					mutex.release(scheduler, pcb);
				}
			} catch (Exception e) {
				System.err.println("[CLEANUP ERROR] Failed to release mutex '" +
					mutex.getResourceName() + "' for process " + pcb.processID + ": " + e.getMessage());
			}
		}

		pcb.ownedMutexes.clear();
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

