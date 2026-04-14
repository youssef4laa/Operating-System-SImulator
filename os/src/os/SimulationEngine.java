package os;

import java.util.LinkedList;

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
    
    // Program files
    private static final String[] PROGRAM_FILES = {
        "Program1.txt",
        "Program2.txt", 
        "Program3.txt"
    };
    
    // Process arrival times
    private static final int[] ARRIVAL_TIMES = { 0, 1, 4 };
    private LinkedList<Integer> processesToCreate;
    private int nextArrivalIndex = 0;
    
    public SimulationEngine(DebugConsole debugConsole) {
        this.memory = new Memory();
        this.scheduler = new Scheduler();
        this.debugConsole = debugConsole;
        this.processesToCreate = new LinkedList<>();
        
        for (int i = 0; i < ARRIVAL_TIMES.length; i++) {
            processesToCreate.add(i);
        }
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
            
            debugConsole.log("======================================");
            debugConsole.log("Simulation Initialized");
            debugConsole.log("Algorithm: " + algorithm);
            debugConsole.log("Memory Size: 40 words");
            debugConsole.log("Process Count: 3");
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
            initialize("RR");
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
            initialize("RR");
        }
        
        if (!running) {
            start();
        }
        
        try {
            // Check for new process arrivals
            if (nextArrivalIndex < ARRIVAL_TIMES.length) {
                if (ARRIVAL_TIMES[nextArrivalIndex] == clockCycle) {
                    int processNum = PROGRAM_FILES.length - ARRIVAL_TIMES.length + nextArrivalIndex;
                    createProcess(nextArrivalIndex, PROGRAM_FILES[nextArrivalIndex]);
                    nextArrivalIndex++;
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
        if (scheduler.readyQueue.isEmpty() && scheduler.blockedQueue.isEmpty()) {
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
        
        // Schedule and execute one instruction from current/next process
        try {
            // Get next process to execute
            PCB nextPCB = scheduler.selectNextProcess();
            
            if (nextPCB != null) {
                currentProcess = null; // Get from process list if needed
                
                debugConsole.log("[Clock " + clockCycle + "] Process P" + nextPCB.processID + 
                                " executing, PC: " + nextPCB.programCounter);
                
                // Execute one instruction
                if (nextPCB.instructionPointer < nextPCB.instructionList.size()) {
                    String instructionString = nextPCB.instructionList.get(nextPCB.instructionPointer);
                    debugConsole.log("  Instruction: " + instructionString);
                    
                    // Use the interpreter to execute the instruction
                    Interpreter.execute(nextPCB, memory);
                    instructionsExecuted++;
                    
                    // Check if process finished
                    if (nextPCB.instructionPointer >= nextPCB.instructionList.size()) {
                        debugConsole.log("  -> Process P" + nextPCB.processID + " completed");
                        scheduler.readyQueue.remove(nextPCB);
                        scheduler.finishedQueue.add(nextPCB);
                    }
                }
            }
            
        } catch (Exception e) {
            debugConsole.log("Scheduler cycle error: " + e.getMessage(), true);
        }
    }
    
    /**
     * Create a new process
     */
    private void createProcess(int processIdx, String filename) {
        try {
            debugConsole.log("[Clock " + clockCycle + "] Creating Process " + (processIdx + 1) + " from " + filename);
            
            // TODO: Implement file parsing - for now, use manual process creation
            // Parser.parseFile is not available, so we'll need to load programs differently
            // in the actual implementation
            
            debugConsole.log("  -> Process creation requires program loading implementation");
            
        } catch (Exception e) {
            debugConsole.log("Failed to create process: " + e.getMessage(), true);
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
        clockCycle = 0;
        instructionsExecuted = 0;
        nextArrivalIndex = 0;
        running = false;
        initialized = false;
        currentProcess = null;
        currentInstruction = "";
        
        debugConsole.clear();
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
    
    public int getClockCycle() {
        return clockCycle;
    }
    
    public int getInstructionsExecuted() {
        return instructionsExecuted;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public String getCurrentInstruction() {
        return currentInstruction;
    }
    
    public void setListener(SimulationListener listener) {
        this.listener = listener;
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
