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
    LinkedList<PCB> swappedQueue = new LinkedList<>();  // Track processes swapped to disk
    
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
    
    // Observer pattern for logging
    private List<SchedulerObserver> observers = new ArrayList<>();

    /**
     * Initialize the Interpreter with mutex manager and scheduler
     * Must be called before any process execution (semWait/semSignal)
     * Separated from start() to support GUI initialization
     */
    public void initializeInterpreter() throws Exception {
        // Initialize mutex manager
        this.mutexManager = new MutexManager();
        this.mutexUserOutput = mutexManager.getUserOutputMutex();
        this.mutexUserInput = mutexManager.getUserInputMutex();
        this.mutexFile = mutexManager.getFileMutex();
        
        // Initialize interpreter with scheduler and mutexes
        Interpreter.initialize(this, mutexUserOutput, mutexUserInput, mutexFile);
    }

    /**
     * Observer Pattern: Register an observer
     */
    public void addObserver(SchedulerObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    /**
     * Observer Pattern: Unregister an observer
     */
    public void removeObserver(SchedulerObserver observer) {
        observers.remove(observer);
    }

    /**
     * Observer Pattern: Notify all observers of a scheduling event
     */
    private void notifySchedulingEvent(SchedulingEvent event) {
        event.readyQueueSize = readyQueue.size();
        event.blockedQueueSize = blockedQueue.size();
        event.finishedQueueSize = finishedQueue.size();
        
        for (SchedulerObserver observer : observers) {
            observer.onSchedulingEvent(event);
        }
    }

    /**
     * Public wrapper for GUI-side execution paths that need to emit scheduling events.
     */
    public void emitSchedulingEvent(SchedulingEvent event) {
        notifySchedulingEvent(event);
    }

    /**
     * Synchronize scheduler time with external drivers (e.g., SimulationEngine).
     */
    public void setTime(int time) {
        this.time = time;
    }

    /**
     * Observer Pattern: Notify all observers of a clock cycle (for memory/state snapshots)
     */
    private void notifyClockCycle(int clockCycle) {
        for (SchedulerObserver observer : observers) {
            observer.onClockCycle(clockCycle, memory, this);
        }
    }

    /**
     * Observer Pattern: Notify all observers of simulation start
     */
    private void notifySimulationStart() {
        for (SchedulerObserver observer : observers) {
            observer.onSimulationStart();
        }
    }

    /**
     * Observer Pattern: Notify all observers of simulation end
     */
    private void notifySimulationEnd() {
        for (SchedulerObserver observer : observers) {
            observer.onSimulationEnd();
        }
    }

    /**
     * Initializes scheduler and starts the main scheduling loop
     */
    public void start(Memory mem) throws Exception {
        this.memory = mem;
        
        // Initialize interpreter and mutexes
        initializeInterpreter();
        
        // Notify observers of simulation start
        notifySimulationStart();
        
        System.out.println("========================================");
        System.out.println("Scheduler started with " + algorithm + " algorithm");
        System.out.println("Mutex Manager initialized");
        System.out.println("========================================\n");

        int maxIterations = 1000; // Prevent infinite loops for debugging
        int iteration = 0;
        
        while (iteration < maxIterations) {
            // Check for new process arrivals at the START of this time unit
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
                
                // Notify observers of simulation end
                notifySimulationEnd();
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
                        // Notify of process selection
                        SchedulingEvent event = new SchedulingEvent(
                            SchedulingEvent.EventType.PROCESS_SELECTED, time, current);
                        event.eventDetails = "Selected from ready queue";
                        notifySchedulingEvent(event);
                        
                        RR(current, memory);
                    }
                    break;

                case "HRRN":
                    current = HRRN();
                    if (current != null) {
                        // Notify of process selection
                        SchedulingEvent event = new SchedulingEvent(
                            SchedulingEvent.EventType.PROCESS_SELECTED, time, current);
                        event.eventDetails = "Selected via HRRN";
                        notifySchedulingEvent(event);
                        
                        FCFS(current, memory);
                    }
                    break;

                case "MLFQ":
                    current = MLFQ();
                    if (current != null) {
                        // Notify of process selection
                        SchedulingEvent event = new SchedulingEvent(
                            SchedulingEvent.EventType.PROCESS_SELECTED, time, current);
                        event.eventDetails = "Selected via MLFQ";
                        notifySchedulingEvent(event);
                        
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
                    
                    // Notify of process finish
                    SchedulingEvent event = new SchedulingEvent(
                        SchedulingEvent.EventType.PROCESS_FINISHED, time, p);
                    event.eventDetails = "Process completed";
                    notifySchedulingEvent(event);
                    
                    Process.terminateProcess(p, memory, this);
                }
            }

            memory.displayMemory();
            
            // Notify of clock cycle completion
            notifyClockCycle(time);
            
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
        pcb.quantumUsed = 0;  // Track quantum usage
        
        while (pcb.quantumUsed < quantum && pcb.status.equals("Running")) {
            // Guard: Check if there are instructions left to execute
            if (pcb.instructionPointer >= pcb.instructionList.size()) {
                pcb.status = "Finished";
                break;
            }
            
            // Execute one instruction
            Interpreter.execute(pcb, memory);
            pcb.quantumUsed++;

            // Check if process terminated with fatal error
            if (pcb.status.equals("Terminated")) {
                break;
            }

            // Check if process blocked on resource
            if (pcb.status.equals("Blocked")) {
                break;
            }

            // Check if process finished (all instructions executed)
            if (pcb.status.equals("Finished")) {
                break;
            }
            
            // Check if we've gone beyond instruction bounds
            if (pcb.instructionPointer >= pcb.instructionList.size()) {
                pcb.status = "Finished";
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
        if (pcb.status.equals("Finished") || pcb.status.equals("Terminated")) {
            readyQueue.remove(pcb);
            blockedQueue.remove(pcb);
            try {
                Process.terminateProcess(pcb, memory, this);
            } catch (Exception e) {
                System.err.println("[SCHEDULER CLEANUP ERROR] " + e.getMessage());
            }
            if (!finishedQueue.contains(pcb)) {
                finishedQueue.add(pcb);
            }
        } else if (pcb.status.equals("Blocked")) {
            blockedQueue.add(pcb);
            
            // Notify observers of blocking event
            SchedulingEvent event = new SchedulingEvent(
                SchedulingEvent.EventType.PROCESS_BLOCKED, time, pcb);
            event.eventDetails = "Process blocked on resource";
            notifySchedulingEvent(event);
        } else if (pcb.status.equals("Running")) {
            pcb.status = "Ready";
            readyQueue.add(pcb);
        }
    }

    /**
     * Moves process to appropriate MLFQ queue
     */
    public void moveToMLFQ(PCB pcb) {
        if (pcb.status.equals("Finished") || pcb.status.equals("Terminated")) {
            readyQueue.remove(pcb);
            blockedQueue.remove(pcb);
            q0.remove(pcb);
            q1.remove(pcb);
            q2.remove(pcb);
            q3.remove(pcb);
            try {
                Process.terminateProcess(pcb, memory, this);
            } catch (Exception e) {
                System.err.println("[SCHEDULER CLEANUP ERROR] " + e.getMessage());
            }
            if (!finishedQueue.contains(pcb)) {
                finishedQueue.add(pcb);
            }
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
                readyQueue.add(p1);  // First process, add normally
                q0.add(p1);
                System.out.println(">>> Process 1 arrived at time " + time);
            }
        }

        if (time == 1) {
            PCB p2 = createProcessWithSwap("Program2.txt", memory);
            if (p2 != null) {
                p2.arrivalTime = time;
                // RR fairness: Add newly arrived processes to the FRONT so they get a turn
                // before any process gets a second turn
                readyQueue.addFirst(p2);
                q0.add(p2);
                System.out.println(">>> Process 2 arrived at time " + time);
            }
        }

        if (time == 4) {
            PCB p3 = createProcessWithSwap("Program3.txt", memory);
            if (p3 != null) {
                p3.arrivalTime = time;
                // RR fairness: Add newly arrived processes to the FRONT so they get a turn
                // before any process gets a second turn
                readyQueue.addFirst(p3);
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
    
    /**
     * Select next process based on current algorithm for GUI-based step execution
     * Used by SimulationEngine for single-step mode
     */
    public PCB selectNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }
        
        PCB selected = null;
        
        switch (algorithm.toUpperCase()) {
            case "RR":
                selected = readyQueue.peek(); // Peek without removing
                break;
                
            case "HRRN":
                selected = selectHRRN();
                break;
                
            case "MLFQ":
                selected = selectMLFQ();
                break;
                
            default:
                selected = readyQueue.peek();
        }
        
        return selected;
    }
    
    /**
     * Select best process using HRRN without removing
     */
    private PCB selectHRRN() {
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
        
        return best;
    }
    
    /**
     * Select best process using MLFQ without removing
     */
    private PCB selectMLFQ() {
        if (!q0.isEmpty()) return q0.peek();
        if (!q1.isEmpty()) return q1.peek();
        if (!q2.isEmpty()) return q2.peek();
        if (!q3.isEmpty()) return q3.peek();
        return null;
    }
}

