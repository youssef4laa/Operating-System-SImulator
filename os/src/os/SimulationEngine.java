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
            if (fileProgramQueue.isEmpty()) {
                debugConsole.log("No programs loaded. Please drag and drop program files first.", true);
                return;
            }
            initialize("RR");
        }
        
        if (!running) {
            start();
        }
        
        try {
            // Check for new process arrivals
            if (nextArrivalIndex < fileProgramQueue.size()) {
                FileProgram nextProgram = fileProgramQueue.get(nextArrivalIndex);
                if (nextProgram.arrivalTime == clockCycle) {
                    createProcess(nextArrivalIndex, nextProgram.file);
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
     * Create a new process from file
     */
    private void createProcess(int processIdx, File file) {
        try {
            debugConsole.log("[Clock " + clockCycle + "] Creating Process " + (processIdx + 1) + 
                           " from " + file.getName());
            
            // Use Process.createProcess to load and parse the program file
            PCB pcb = Process.createProcess(file.getAbsolutePath(), memory);
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
        clockCycle = 0;
        instructionsExecuted = 0;
        nextArrivalIndex = 0;
        running = false;
        initialized = false;
        currentProcess = null;
        currentInstruction = "";
        // Note: Do NOT clear fileProgramQueue - user can reload same programs
        
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
