package os;

import java.util.*;

/**
 * Scheduler - Manages process scheduling and resource allocation
 * Supports multiple scheduling algorithms: RR, HRRN, MLFQ
 */
public class Scheduler {
	
    LinkedList<PCB> readyQueue = new LinkedList<>();
    LinkedList<PCB> blockedQueue = new LinkedList<>();
    LinkedList<PCB> finishedQueue = new LinkedList<>();
    
    // MLFQ queues
    LinkedList<PCB> q0 = new LinkedList<>();
    LinkedList<PCB> q1 = new LinkedList<>();
    LinkedList<PCB> q2 = new LinkedList<>();
    LinkedList<PCB> q3 = new LinkedList<>();

    // Resource mutexes managed by MutexManager
    private MutexManager mutexManager;
    
    // Legacy references for backward compatibility
    Mutex mutexUserOutput;
    Mutex mutexUserInput;
    Mutex mutexFile;
    
    int time = 0;
    String algorithm = "RR"; // Default: Round Robin
    Memory memory;
    
    // Process scheduling state
    private boolean processesArrived = false;

    /**
     * Initializes scheduler and starts the main scheduling loop
     */
    public void start(Memory mem) throws Exception {
        this.memory = mem;
        
        // Initialize mutex manager
        this.mutexManager = new MutexManager();
        this.mutexUserOutput = mutexManager.getUserOutputMutex();
        this.mutexUserInput = mutexManager.getUserInputMutex();
        this.mutexFile = mutexManager.getFileMutex();
        
        // Initialize interpreter with scheduler and mutexes
        Interpreter.initialize(this, mutexUserOutput, mutexUserInput, mutexFile);
        
        System.out.println("========================================");
        System.out.println("Scheduler started with " + algorithm + " algorithm");
        System.out.println("Mutex Manager initialized");
        System.out.println("========================================\n");

        int maxIterations = 1000; // Prevent infinite loops for debugging
        int iteration = 0;
        
        while (iteration < maxIterations) {
            // Check for new process arrivals
            checkArrivals(time, memory);
            
            // Check termination: all queues empty
            boolean readyEmpty = readyQueue.isEmpty();
            boolean blockedEmpty = blockedQueue.isEmpty();
            boolean allMLFQEmpty = q0.isEmpty() && q1.isEmpty() && q2.isEmpty() && q3.isEmpty();
            
            if (readyEmpty && blockedEmpty && allMLFQEmpty) {
                // No more processes to run
                System.out.println("\n========================================");
                System.out.println("All processes completed");
                mutexManager.printStatistics();
                System.out.println("========================================\n");
                break;
            }

            // Print current state
            printSchedulerState();

            PCB current = null;

            // Schedule based on algorithm
            switch (algorithm.toUpperCase()) {
                case "RR":
                    current = readyQueue.poll();
                    if (current != null) {
                        RR(current, memory);
                    }
                    break;

                case "HRRN":
                    current = HRRN();
                    if (current != null) {
                        FCFS(current, memory);
                    }
                    break;

                case "MLFQ":
                    current = MLFQ();
                    if (current != null) {
                        runMLFQ(current, memory);
                    }
                    break;
            }

            // Handle finished processes
            Iterator<PCB> iter = readyQueue.iterator();
            while (iter.hasNext()) {
                PCB p = iter.next();
                if (p.status.equals("Finished")) {
                    iter.remove();
                    finishedQueue.add(p);
                    Process.terminateProcess(p, memory);
                }
            }

            memory.displayMemory();
            time++;
            iteration++;
        }
    }

    /**
     * Round Robin scheduling - 2 instructions per time slice
     */
    public void RR(PCB pcb, Memory memory) throws Exception {
        int quantum = 2; // 2 instructions per slice
        pcb.status = "Running";
        
        int counter = 0;
        
        while (counter < quantum && pcb.status.equals("Running")) {
            Interpreter.execute(pcb, memory);
            counter++;

            if (pcb.programCounter > pcb.maxBound || pcb.status.equals("Finished")) {
                break;
            }

            if (pcb.status.equals("Blocked")) {
                break;
            }
        }

        moveProcess(pcb);
    }

    /**
     * HRRN - Highest Response Ratio Next (non-preemptive)
     */
    public PCB HRRN() {
        PCB best = null;
        double maxRatio = -1;

        for (PCB p : readyQueue) {
            int waiting = time - p.arrivalTime;
            int burst = p.remainingTime;

            double ratio = (waiting + burst) / (double) burst;

            if (ratio > maxRatio) {
                maxRatio = ratio;
                best = p;
            }
        }
        
        if (best != null) {
            readyQueue.remove(best);
        }
        return best;
    }

    /**
     * FCFS - First Come First Served (for HRRN execution)
     */
    public void FCFS(PCB pcb, Memory memory) throws Exception {
        pcb.status = "Running";

        while (pcb.programCounter <= pcb.maxBound && pcb.status.equals("Running")) {
            Interpreter.execute(pcb, memory);

            if (pcb.status.equals("Blocked")) {
                break;
            }
            
            if (pcb.status.equals("Finished")) {
                break;
            }
        }

        moveProcess(pcb);
    }

    /**
     * MLFQ - Get next process from highest priority non-empty queue
     */
    public PCB MLFQ() {
        if (!q0.isEmpty()) return q0.poll();
        if (!q1.isEmpty()) return q1.poll();
        if (!q2.isEmpty()) return q2.poll();
        if (!q3.isEmpty()) return q3.poll();
        return null;
    }

    /**
     * MLFQ - Run process with appropriate quantum for its queue level
     */
    public void runMLFQ(PCB pcb, Memory memory) throws Exception {
        pcb.status = "Running";
        
        int level = pcb.currentQueueLevel; 
        int quantum = (int) Math.pow(2, level);

        int counter = 0;

        while (counter < quantum && pcb.status.equals("Running")) {
            Interpreter.execute(pcb, memory);
            counter++;

            if (pcb.programCounter > pcb.maxBound || pcb.status.equals("Finished")) {
                break;
            }

            if (pcb.status.equals("Blocked")) {
                break;
            }
        }

        // Move to lower priority queue if used full quantum
        if (counter == quantum && pcb.currentQueueLevel < 3) {
            pcb.currentQueueLevel++;
        }

        moveToMLFQ(pcb);
    }

    /**
     * Moves process to appropriate queue after execution
     */
    public void moveProcess(PCB pcb) {
        if (pcb.status.equals("Finished")) {
            finishedQueue.add(pcb);
        } else if (pcb.status.equals("Blocked")) {
            blockedQueue.add(pcb);
        } else if (pcb.status.equals("Running")) {
            pcb.status = "Ready";
            readyQueue.add(pcb);
        }
    }

    /**
     * Moves process to appropriate MLFQ queue
     */
    public void moveToMLFQ(PCB pcb) {
        if (pcb.status.equals("Finished")) {
            finishedQueue.add(pcb);
            return;
        }

        if (pcb.status.equals("Blocked")) {
            blockedQueue.add(pcb);
            return;
        }

        pcb.status = "Ready";

        switch (pcb.currentQueueLevel) {
            case 0: q0.add(pcb); break;
            case 1: q1.add(pcb); break;
            case 2: q2.add(pcb); break;
            case 3: q3.add(pcb); break;
        }
    }

    /**
     * Checks for process arrivals and creates processes as they arrive
     * Implements swap logic if memory is insufficient
     * Process arrival times: P1 at t=0, P2 at t=1, P3 at t=4
     */
    public void checkArrivals(int time, Memory memory) throws Exception {
        if (time == 0) {
            PCB p1 = createProcessWithSwap("Program1.txt", memory);
            if (p1 != null) {
                p1.arrivalTime = time;
                readyQueue.add(p1);
                q0.add(p1);
                System.out.println(">>> Process 1 arrived at time " + time);
            }
        }

        if (time == 1) {
            PCB p2 = createProcessWithSwap("Program2.txt", memory);
            if (p2 != null) {
                p2.arrivalTime = time;
                readyQueue.add(p2);
                q0.add(p2);
                System.out.println(">>> Process 2 arrived at time " + time);
            }
        }

        if (time == 4) {
            PCB p3 = createProcessWithSwap("Program3.txt", memory);
            if (p3 != null) {
                p3.arrivalTime = time;
                readyQueue.add(p3);
                q0.add(p3);
                System.out.println(">>> Process 3 arrived at time " + time);
            }
        }
    }
    /**
     * Get the Mutex Manager instance
     */
    public MutexManager getMutexManager() {
        return mutexManager;
    }
    
    /**
     * Helper method to create a process with automatic swap if memory insufficient
     * @return PCB of created process, or null if creation fails
     */
    private PCB createProcessWithSwap(String fileName, Memory memory) throws Exception {
        try {
            return Process.createProcess(fileName, memory);
        } catch (Exception e) {
            // Check if it's a memory-full error
            if (e.getMessage().contains("No contiguous space")) {
                System.out.println("[SWAP] Memory full, attempting to swap out a process...");
                
                // Find a ready process to swap out (prefer those not currently running)
                PCB toSwap = findProcessToSwap();
                if (toSwap != null) {
                    System.out.println("[SWAP] Swapping out " + toSwap.processID);
                    MemoryManager.swapOut(toSwap, memory);
                    
                    // Try creating the process again
                    try {
                        return Process.createProcess(fileName, memory);
                    } catch (Exception e2) {
                        System.err.println("[SWAP] Failed to create process even after swap: " + e2.getMessage());
                        return null;
                    }
                } else {
                    System.err.println("[SWAP] No process available to swap out");
                    return null;
                }
            } else {
                // Different error, rethrow
                throw e;
            }
        }
    }
    
    /**
     * Finds a suitable process to swap out
     * Prefers ready processes over others, prioritizes least recently used
     * @return PCB to swap out, or null if none available
     */
    private PCB findProcessToSwap() {
        // Try to swap a ready process (not currently running)
        if (!readyQueue.isEmpty()) {
            // Prefer the process that has been waiting longest
            return readyQueue.getFirst();
        }
        
        // If no ready process, don't swap blocked processes (they're waiting for resources)
        // In a real OS, we might swap other processes here
        return null;
    }
    
    /**
     * Prints current scheduler state (queues and running process)
     */
    private void printSchedulerState() {
        System.out.println("\n[Time: " + time + "]");
        System.out.println("Ready Queue: " + queueToString(readyQueue));
        System.out.println("Blocked Queue: " + queueToString(blockedQueue));
        System.out.println("Finished Queue: " + queueToString(finishedQueue));
    }
    
    /**
     * Converts queue of PCBs to string representation
     */
    private String queueToString(LinkedList<PCB> queue) {
        if (queue.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < queue.size(); i++) {
            sb.append("P").append(queue.get(i).processID);
            if (i < queue.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}

