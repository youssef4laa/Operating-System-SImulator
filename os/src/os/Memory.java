package os;

public class Memory {
    private final Object[] mem = new Object[40];

    public void write(int index, Object data) throws Exception {
        if (index < 0 || index >= mem.length) {
            throw new Exception("Memory Access Violation: Index " + index + " is out of physical bounds.");
        }
        mem[index] = data;
    }

    public Object read(int index) throws Exception {
        if (index < 0 || index >= mem.length) {
            throw new Exception("Memory Access Violation: Index " + index + " is out of physical bounds.");
        }
        return mem[index];
    }

    public void displayMemory() {
        System.out.println("--- Current Memory State ---");
        for (int i = 0; i < mem.length; i++) {
            System.out.printf("Word [%d]: %s%n", i, (mem[i] == null ? "Empty" : mem[i].toString()));
        }
        System.out.println("----------------------------");
    }

    public int getSize() {
        return mem.length; 
    }
    
    /**
     * Get the number of memory words currently in use
     */
    public int getUsedWords() {
        int used = 0;
        for (Object o : mem) {
            if (o != null) {
                used++;
            }
        }
        return used;
    }
    
    /**
     * Allocate memory for a process
     * Finds a contiguous block and updates the PCB with boundaries
     */
    public void allocate(PCB pcb) throws Exception {
        int requiredSize = pcb.allocationSize;
        int startIndex = -1;
        
        // Find first fit contiguous block
        int freeCount = 0;
        for (int i = 0; i < mem.length; i++) {
            if (mem[i] == null) {
                if (freeCount == 0) {
                    startIndex = i;
                }
                freeCount++;
                if (freeCount >= requiredSize) {
                    // Found enough space
                    pcb.minBound = startIndex;
                    pcb.maxBound = startIndex + requiredSize - 1;
                    
                    // Write PCB to memory
                    mem[startIndex] = pcb;
                    
                    return;
                }
            } else {
                freeCount = 0;
            }
        }
        
        throw new Exception("Not enough contiguous memory to allocate process");
    }
}
