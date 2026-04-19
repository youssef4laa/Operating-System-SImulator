package os;

import java.io.File;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

/**
 * SimulationEngine - Manages the OS simulation execution
 * Handles state management, step-by-step execution, and process lifecycle
 * Bridges the GUI with the Scheduler
 */
public class SimulationEngine {
    
    private Memory memory;
    private Scheduler scheduler;
    private DebugConsole debugConsole;
    
    // Execution state
    private boolean initialized = false;
    private boolean running = false;
    private int clockCycle = 0;
    private int instructionsExecuted = 0;
    
    // Process tracking
    private Process currentProcess = null;
    private String currentInstruction = "";
    private SimulationListener listener;
    
    // Dynamic program loading
    private List<FileProgram> fileProgramQueue = new ArrayList<>();
    private int nextArrivalIndex = 0;
    
    /**
     * Inner class to hold file + arrival time pair
     */
    private static class FileProgram {
        File file;
        int arrivalTime;
        
        FileProgram(File file, int arrivalTime) {
            this.file = file;
            this.arrivalTime = arrivalTime;
        }
    }
    
    public SimulationEngine(DebugConsole debugConsole) {
        this.memory = new Memory();
        this.scheduler = new Scheduler();
        this.debugConsole = debugConsole;
        this.fileProgramQueue = new ArrayList<>();
        
        // Initialize interpreter with scheduler mutexes (required for semWait/semSignal)
        try {
            this.scheduler.initializeInterpreter();
        } catch (Exception e) {
            debugConsole.log("Failed to initialize interpreter: " + e.getMessage(), true);
        }
    }
    
    /**
     * Load programs from files for execution
     * Assigns sequential arrival times and validates file format
     */
    public void loadProgramsFromFiles(List<File> files, List<Integer> arrivalTimes) throws Exception {
        if (files == null || files.isEmpty()) {
            throw new Exception("No program files provided");
        }
        
        if (files.size() != arrivalTimes.size()) {
            throw new Exception("Mismatch between file count and arrival times");
        }
        
        fileProgramQueue.clear();
        nextArrivalIndex = 0;
        
        // Validate all files before adding to queue
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            int arrivalTime = arrivalTimes.get(i);
            
            if (!file.exists()) {
                throw new Exception("File not found: " + file.getAbsolutePath());
            }
            
            if (!file.isFile()) {
                throw new Exception("Not a file: " + file.getAbsolutePath());
            }
            
            if (!file.canRead()) {
                throw new Exception("Cannot read file: " + file.getAbsolutePath());
            }
            
            fileProgramQueue.add(new FileProgram(file, arrivalTime));
        }
        
        // Log program queue
        debugConsole.log("======================================");
        debugConsole.log("Programs loaded: " + fileProgramQueue.size());
        for (int i = 0; i < fileProgramQueue.size(); i++) {
            FileProgram fp = fileProgramQueue.get(i);
            debugConsole.log("  Program " + (i + 1) + ": " + fp.file.getName() + 
                           " (arrival time: " + fp.arrivalTime + ")");
        }
        debugConsole.log("======================================");
    }
    
    /**
     * Initialize the simulation
     */
    public void initialize(String algorithm) {
        try {
            scheduler.algorithm = algorithm;
            clockCycle = 0;
            instructionsExecuted = 0;
            nextArrivalIndex = 0;
            running = false;
            initialized = true;
            
            // Sort programs by arrival time to ensure correct execution order
            fileProgramQueue.sort((fp1, fp2) -> Integer.compare(fp1.arrivalTime, fp2.arrivalTime));
            
            debugConsole.log("======================================");
            debugConsole.log("Simulation Initialized");
            debugConsole.log("Algorithm: " + algorithm);
            debugConsole.log("Memory Size: 40 words");
            debugConsole.log("Process Count: " + fileProgramQueue.size());
            debugConsole.log("======================================");
            
            if (listener != null) listener.onInitialized();
            
        } catch (Exception e) {
            debugConsole.log("Initialization failed: " + e.getMessage(), true);
        }
    }
    
    /**
     * Start the simulation
     */
    public void start() {
        if (!initialized) {
            initialize(scheduler.algorithm != null ? scheduler.algorithm : "RR");
        }
        running = true;
        debugConsole.log("\nSimulation started");
        if (listener != null) listener.onStarted();
    }
    
    /**
     * Execute a single step (one clock cycle or one instruction)
     */
    public void step() {
        if (!initialized) {
            if (fileProgramQueue.isEmpty()) {
                debugConsole.log("No programs loaded. Please drag and drop program files first.", true);
                return;
            }
            initialize(scheduler.algorithm != null ? scheduler.algorithm : "RR");
        }
        
        if (!running) {
            start();
        }
        
        try {
            // Check for new process arrivals (load ALL processes arriving at this time)
            while (nextArrivalIndex < fileProgramQueue.size()) {
                FileProgram nextProgram = fileProgramQueue.get(nextArrivalIndex);
                if (nextProgram.arrivalTime == clockCycle) {
                    createProcess(nextArrivalIndex, nextProgram.file);
                    nextArrivalIndex++;
                } else {
                    break; // No more processes arriving at this time
                }
            }
            
            // Execute scheduler tick
            executeSchedulerCycle();
            
            clockCycle++;
            updateStatusDisplay();
            
            if (listener != null) {
                listener.onStepComplete();
            }
            
        } catch (Exception e) {
            debugConsole.log("Step execution error: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    /**
     * Execute one scheduler cycle
     */
    private void executeSchedulerCycle() {
        boolean allMLFQEmpty = scheduler.q0.isEmpty() && scheduler.q1.isEmpty() && 
                               scheduler.q2.isEmpty() && scheduler.q3.isEmpty();
        boolean hrnActive = currentRunningPCB != null && "Running".equals(currentRunningPCB.status);

        if (scheduler.readyQueue.isEmpty() && scheduler.blockedQueue.isEmpty() && allMLFQEmpty && !hrnActive) {
            if (running) {
                running = false;
                debugConsole.log("\n========================================");
                debugConsole.log("All processes completed");
                debugConsole.log("Total Clock Cycles: " + clockCycle);
                debugConsole.log("Instructions Executed: " + instructionsExecuted);
                debugConsole.log("========================================");
                if (listener != null) listener.onCompleted();
            }
            return;
        }

        scheduler.setTime(clockCycle);

        // Schedule and execute according to selected algorithm
        try {
            if ("RR".equalsIgnoreCase(scheduler.algorithm)) {
                executeRRQuantumStep();
            } else if ("HRRN".equalsIgnoreCase(scheduler.algorithm)) {
                executeHRRNStep();
            } else {
                executeSingleInstructionStep();
            }
        } catch (Exception e) {
            debugConsole.log("Scheduler cycle error: " + e.getMessage(), true);
        }
    }

    /**
     * GUI step mode for RR: execute one full quantum (up to 2 instructions).
     */
    private void executeRRQuantumStep() {
        if (scheduler.readyQueue.isEmpty()) {
            return;
        }

        PCB nextPCB = scheduler.readyQueue.poll();
        if (nextPCB == null) {
            return;
        }

        currentProcess = null;
        nextPCB.status = "Running";
        nextPCB.quantumUsed = 0;

        SchedulingEvent selectedEvent = new SchedulingEvent(
            SchedulingEvent.EventType.PROCESS_SELECTED,
            clockCycle,
            nextPCB
        );
        selectedEvent.eventDetails = "Selected from ready queue (RR)";
        scheduler.emitSchedulingEvent(selectedEvent);

        int quantum = 2;
        while (nextPCB.quantumUsed < quantum && "Running".equals(nextPCB.status)) {
            if (nextPCB.instructionPointer >= nextPCB.instructionList.size()) {
                nextPCB.status = "Finished";
                break;
            }

            String instructionString = nextPCB.instructionList.get(nextPCB.instructionPointer);
            currentInstruction = instructionString;
            debugConsole.log("[Clock " + clockCycle + "] Process P" + nextPCB.processID +
                           " executing, PC: " + nextPCB.programCounter);
            debugConsole.log("  Instruction: " + instructionString);

            Interpreter.execute(nextPCB, memory);
            instructionsExecuted++;
            nextPCB.quantumUsed++;

            if ("Terminated".equals(nextPCB.status)) {
                debugConsole.log("  -> Process P" + nextPCB.processID + " TERMINATED (fatal error)");
                scheduler.readyQueue.remove(nextPCB);
                scheduler.blockedQueue.remove(nextPCB);
                try {
                    Process.terminateProcess(nextPCB, memory, scheduler);
                } catch (Exception e) {
                    debugConsole.log("Cleanup failed for terminated process: " + e.getMessage(), true);
                }
                if (!scheduler.finishedQueue.contains(nextPCB)) {
                    scheduler.finishedQueue.add(nextPCB);
                }

                SchedulingEvent finishedEvent = new SchedulingEvent(
                    SchedulingEvent.EventType.PROCESS_FINISHED,
                    clockCycle,
                    nextPCB
                );
                finishedEvent.eventDetails = "Process terminated due to fatal error";
                scheduler.emitSchedulingEvent(finishedEvent);
                return;
            }

            if ("Blocked".equals(nextPCB.status)) {
                debugConsole.log("  -> Process P" + nextPCB.processID + " blocked on resource");
                return;
            }

            if (nextPCB.instructionPointer >= nextPCB.instructionList.size()) {
                nextPCB.status = "Finished";
                debugConsole.log("  -> Process P" + nextPCB.processID + " completed");
                scheduler.readyQueue.remove(nextPCB);
                scheduler.blockedQueue.remove(nextPCB);
                try {
                    Process.terminateProcess(nextPCB, memory, scheduler);
                } catch (Exception e) {
                    debugConsole.log("Cleanup failed for finished process: " + e.getMessage(), true);
                }
                if (!scheduler.finishedQueue.contains(nextPCB)) {
                    scheduler.finishedQueue.add(nextPCB);
                }

                SchedulingEvent finishedEvent = new SchedulingEvent(
                    SchedulingEvent.EventType.PROCESS_FINISHED,
                    clockCycle,
                    nextPCB
                );
                finishedEvent.eventDetails = "Process completed";
                scheduler.emitSchedulingEvent(finishedEvent);
                return;
            }
        }

        if ("Running".equals(nextPCB.status)) {
            nextPCB.status = "Ready";
            scheduler.readyQueue.add(nextPCB);

            SchedulingEvent quantumEvent = new SchedulingEvent(
                SchedulingEvent.EventType.TIME_QUANTUM_EXPIRED,
                clockCycle,
                nextPCB
            );
            quantumEvent.eventDetails = "RR quantum expired; moved to back of ready queue";
            scheduler.emitSchedulingEvent(quantumEvent);
            debugConsole.log("  -> RR quantum expired for P" + nextPCB.processID + " (2/2)");
        }
    }

    private PCB currentRunningPCB = null;

    /**
     * GUI step mode for HRRN: execution is non-preemptive.
     * Once a process is selected, it runs until finished or blocked.
     */
    private void executeHRRNStep() {
        PCB nextPCB = null;
        
        // If HRRN is already running a process, keep running it
        if (currentRunningPCB != null && "Running".equals(currentRunningPCB.status)) {
            nextPCB = currentRunningPCB;
        } else {
            // Select best process via HRRN
            nextPCB = scheduler.selectNextProcess(); // will call selectHRRN
            
            if (nextPCB != null) {
                // Actually remove it from the ready queue since HRRN is non-preemptive
                scheduler.readyQueue.remove(nextPCB);
                nextPCB.status = "Running";
                currentRunningPCB = nextPCB;
                
                SchedulingEvent selectedEvent = new SchedulingEvent(
                    SchedulingEvent.EventType.PROCESS_SELECTED,
                    clockCycle,
                    nextPCB
                );
                selectedEvent.eventDetails = "Selected via HRRN (Non-preemptive)";
                scheduler.emitSchedulingEvent(selectedEvent);
            }
        }

        if (nextPCB == null) {
            return;
        }

        if (nextPCB.instructionPointer >= nextPCB.instructionList.size()) {
            nextPCB.status = "Finished";
            currentRunningPCB = null;
            try {
                Process.terminateProcess(nextPCB, memory, scheduler);
            } catch (Exception e) {
                debugConsole.log("Cleanup failed for finished process: " + e.getMessage(), true);
            }
            if (!scheduler.finishedQueue.contains(nextPCB)) {
                scheduler.finishedQueue.add(nextPCB);
            }

            SchedulingEvent finishedEvent = new SchedulingEvent(
                SchedulingEvent.EventType.PROCESS_FINISHED,
                clockCycle,
                nextPCB
            );
            finishedEvent.eventDetails = "Process completed";
            scheduler.emitSchedulingEvent(finishedEvent);
            return;
        }

        String instructionString = nextPCB.instructionList.get(nextPCB.instructionPointer);
        currentInstruction = instructionString;
        debugConsole.log("[Clock " + clockCycle + "] Process P" + nextPCB.processID +
                       " executing, PC: " + nextPCB.programCounter);
        debugConsole.log("  Instruction: " + instructionString);

        Interpreter.execute(nextPCB, memory);
        instructionsExecuted++;

        if ("Terminated".equals(nextPCB.status)) {
            debugConsole.log("  -> Process P" + nextPCB.processID + " TERMINATED (fatal error)");
            currentRunningPCB = null;
            scheduler.readyQueue.remove(nextPCB);
            scheduler.blockedQueue.remove(nextPCB);
            try {
                Process.terminateProcess(nextPCB, memory, scheduler);
            } catch (Exception e) {
                debugConsole.log("Cleanup failed for terminated process: " + e.getMessage(), true);
            }
            if (!scheduler.finishedQueue.contains(nextPCB)) {
                scheduler.finishedQueue.add(nextPCB);
            }

            SchedulingEvent finishedEvent = new SchedulingEvent(
                SchedulingEvent.EventType.PROCESS_FINISHED,
                clockCycle,
                nextPCB
            );
            finishedEvent.eventDetails = "Process terminated due to fatal error";
            scheduler.emitSchedulingEvent(finishedEvent);
            return;
        }

        if ("Blocked".equals(nextPCB.status)) {
            debugConsole.log("  -> Process P" + nextPCB.processID + " blocked on resource");
            currentRunningPCB = null; // No longer running
            // the mutex already added it to blockedQueue
            return;
        }

        if (nextPCB.instructionPointer >= nextPCB.instructionList.size()) {
            nextPCB.status = "Finished";
            currentRunningPCB = null;
            debugConsole.log("  -> Process P" + nextPCB.processID + " completed");
            try {
                Process.terminateProcess(nextPCB, memory, scheduler);
            } catch (Exception e) {
                debugConsole.log("Cleanup failed for finished process: " + e.getMessage(), true);
            }
            if (!scheduler.finishedQueue.contains(nextPCB)) {
                scheduler.finishedQueue.add(nextPCB);
            }

            SchedulingEvent finishedEvent = new SchedulingEvent(
                SchedulingEvent.EventType.PROCESS_FINISHED,
                clockCycle,
                nextPCB
            );
            finishedEvent.eventDetails = "Process completed";
            scheduler.emitSchedulingEvent(finishedEvent);
        }
        // Note: We DO NOT set status back to "Ready". It stays "Running" since it's non-preemptive.
    }

    /**
     * Non-RR algorithms keep single-instruction GUI stepping behavior.
     */
    private void executeSingleInstructionStep() {
        PCB nextPCB = scheduler.selectNextProcess();

        if (nextPCB == null) {
            return;
        }

        currentProcess = null;
        if ("Ready".equals(nextPCB.status)) {
            nextPCB.status = "Running";
        }

        SchedulingEvent selectedEvent = new SchedulingEvent(
            SchedulingEvent.EventType.PROCESS_SELECTED,
            clockCycle,
            nextPCB
        );
        selectedEvent.eventDetails = "Selected for single-step execution";
        scheduler.emitSchedulingEvent(selectedEvent);

        if (nextPCB.instructionPointer >= nextPCB.instructionList.size()) {
            nextPCB.status = "Finished";
            scheduler.readyQueue.remove(nextPCB);
            try {
                Process.terminateProcess(nextPCB, memory, scheduler);
            } catch (Exception e) {
                debugConsole.log("Cleanup failed for finished process: " + e.getMessage(), true);
            }
            if (!scheduler.finishedQueue.contains(nextPCB)) {
                scheduler.finishedQueue.add(nextPCB);
            }

            SchedulingEvent finishedEvent = new SchedulingEvent(
                SchedulingEvent.EventType.PROCESS_FINISHED,
                clockCycle,
                nextPCB
            );
            finishedEvent.eventDetails = "Process completed";
            scheduler.emitSchedulingEvent(finishedEvent);
            return;
        }

        String instructionString = nextPCB.instructionList.get(nextPCB.instructionPointer);
        currentInstruction = instructionString;
        debugConsole.log("[Clock " + clockCycle + "] Process P" + nextPCB.processID +
                       " executing, PC: " + nextPCB.programCounter);
        debugConsole.log("  Instruction: " + instructionString);

        Interpreter.execute(nextPCB, memory);
        instructionsExecuted++;

        if ("Terminated".equals(nextPCB.status)) {
            debugConsole.log("  -> Process P" + nextPCB.processID + " TERMINATED (fatal error)");
            scheduler.readyQueue.remove(nextPCB);
            scheduler.blockedQueue.remove(nextPCB);
            try {
                Process.terminateProcess(nextPCB, memory, scheduler);
            } catch (Exception e) {
                debugConsole.log("Cleanup failed for terminated process: " + e.getMessage(), true);
            }
            if (!scheduler.finishedQueue.contains(nextPCB)) {
                scheduler.finishedQueue.add(nextPCB);
            }

            SchedulingEvent finishedEvent = new SchedulingEvent(
                SchedulingEvent.EventType.PROCESS_FINISHED,
                clockCycle,
                nextPCB
            );
            finishedEvent.eventDetails = "Process terminated due to fatal error";
            scheduler.emitSchedulingEvent(finishedEvent);
            return;
        }

        if ("Blocked".equals(nextPCB.status)) {
            scheduler.readyQueue.remove(nextPCB);
            return;
        }

        if (nextPCB.instructionPointer >= nextPCB.instructionList.size()) {
            nextPCB.status = "Finished";
            debugConsole.log("  -> Process P" + nextPCB.processID + " completed");
            scheduler.readyQueue.remove(nextPCB);
            try {
                Process.terminateProcess(nextPCB, memory, scheduler);
            } catch (Exception e) {
                debugConsole.log("Cleanup failed for finished process: " + e.getMessage(), true);
            }
            if (!scheduler.finishedQueue.contains(nextPCB)) {
                scheduler.finishedQueue.add(nextPCB);
            }

            SchedulingEvent finishedEvent = new SchedulingEvent(
                SchedulingEvent.EventType.PROCESS_FINISHED,
                clockCycle,
                nextPCB
            );
            finishedEvent.eventDetails = "Process completed";
            scheduler.emitSchedulingEvent(finishedEvent);
        } else if ("Running".equals(nextPCB.status)) {
            nextPCB.status = "Ready";
        }
    }
    
    /**
     * Create a new process from file
     */
    private void createProcess(int processIdx, File file) {
        try {
            debugConsole.log("[Clock " + clockCycle + "] Creating Process " + (processIdx + 1) + 
                           " from " + file.getName());
            
            // Use Process.createProcess to load and parse the program file
            PCB pcb = Process.createProcess(file.getAbsolutePath(), memory);
            
            // Transfer arrival time from FileProgram to PCB
            if (processIdx < fileProgramQueue.size()) {
                FileProgram fileProgram = fileProgramQueue.get(processIdx);
                pcb.arrivalTime = fileProgram.arrivalTime;
                pcb.lastReadyEnqueueTime = clockCycle;  // Set when process enters ready queue
            }
            
            scheduler.readyQueue.add(pcb);
            
            debugConsole.log("  -> Process P" + pcb.processID + " created successfully");
            
        } catch (Exception e) {
            debugConsole.log("Failed to create process: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    /**
     * Pause the simulation
     */
    public void pause() {
        running = false;
        debugConsole.log("Simulation paused at clock cycle " + clockCycle);
        if (listener != null) listener.onPaused();
    }
    
    /**
     * Resume the simulation
     */
    public void resume() {
        if (initialized) {
            running = true;
            debugConsole.log("Simulation resumed");
            if (listener != null) listener.onResumed();
        }
    }
    
    /**
     * Reset simulation to initial state
     */
    public void reset() {
        memory = new Memory();
        scheduler = new Scheduler();
        MemoryManager.resetState();
        Process.resetIdCounter();
        
        // Reinitialize interpreter with fresh scheduler mutexes
        try {
            scheduler.initializeInterpreter();
        } catch (Exception e) {
            debugConsole.log("Failed to reinitialize interpreter: " + e.getMessage(), true);
        }
        
        clockCycle = 0;
        instructionsExecuted = 0;
        nextArrivalIndex = 0;
        running = false;
        initialized = false;
        currentProcess = null;
        currentInstruction = "";
        fileProgramQueue.clear();

        int deletedFiles = SystemCall.cleanupGeneratedFiles();
        SystemCall.resetStatistics();
        SystemCall.clearCurrentProcessId();
        
        debugConsole.clear();
        if (deletedFiles > 0) {
            debugConsole.log("Reset cleanup: deleted " + deletedFiles + " generated file(s)");
        }
        debugConsole.log("Simulation reset");
        if (listener != null) listener.onReset();
    }
    
    /**
     * Update status display on GUI
     */
    private void updateStatusDisplay() {
        if (listener != null) {
            listener.onStateChanged(scheduler, memory, clockCycle);
        }
    }
    
    // ============= Getters =============
    
    public Memory getMemory() {
        return memory;
    }
    
    public Scheduler getScheduler() {
        return scheduler;
    }

    public MutexManager getMutexManager() {
        return scheduler.getMutexManager();
    }
    
    public int getClockCycle() {
        return clockCycle;
    }
    
    public int getInstructionsExecuted() {
        return instructionsExecuted;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public String getCurrentInstruction() {
        return currentInstruction;
    }
    
    public void setListener(SimulationListener listener) {
        this.listener = listener;
    }
    
    /**
     * Check if programs are loaded
     */
    public boolean hasProgramsLoaded() {
        return !fileProgramQueue.isEmpty();
    }
    
    /**
     * Get the number of loaded programs
     */
    public int getProgramCount() {
        return fileProgramQueue.size();
    }
    
    // ============= Listener Interface =============
    
    public interface SimulationListener {
        void onInitialized();
        void onStarted();
        void onStepComplete();
        void onPaused();
        void onResumed();
        void onCompleted();
        void onReset();
        void onStateChanged(Scheduler scheduler, Memory memory, int clockCycle);
    }
}
