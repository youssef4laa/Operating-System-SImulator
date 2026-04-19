package os;

import java.util.*;

public class PCB {
	
	    public int processID;
	    public String status; 
	    public int programCounter;
	    public int minBound;
	    public int maxBound;
	    public int arrivalTime;
	    public int lastReadyEnqueueTime;  // Track when process was last added to ready queue
	    public int remainingTime;
	    public int priority;
	    
	    // Execution context
	    public List<String> instructionList;
	    public Map<String, Integer> symbolTable; // variable name -> memory address
	    public int instructionPointer; // index into instructionList
	    public int totalInstructions;
	    
	    // Memory management
	    public static final int MAX_VARIABLES_PER_PROCESS = 3;  // Exactly 3 variable slots per process
	    public int variableCount; // Track number of variables (max 3)
	    public int allocationSize; // Total memory words allocated to this process
	    
	    // For MLFQ scheduling
	    public int quantumUsed;
	    public int currentQueueLevel;
	    
	    // Retry tracking to prevent duplicate logs
	    public String lastExecutedInstructionLine = null;
	    public boolean isRetryingInstruction = false;

	    // Mutex ownership tracking for cleanup on termination
	    public List<Mutex> ownedMutexes;

	    public PCB(int id, int min, int max) {
	        this.processID = id;
	        this.status = "Ready";
	        this.programCounter = min;
	        this.minBound = min;
	        this.maxBound = max;
	        this.arrivalTime = 0;
	        this.lastReadyEnqueueTime = 0;  // Initialize to 0, will be set when enqueued
	        this.allocationSize = (max - min + 1);
	        this.instructionList = new ArrayList<>();
	        this.symbolTable = new HashMap<>();
	        this.variableCount = 0;
	        this.instructionPointer = 0;
	        this.quantumUsed = 0;
	        this.currentQueueLevel = 0;
	        this.ownedMutexes = new ArrayList<>();
	    }
	    
	    public String getNextInstruction() {
	        if (instructionPointer < instructionList.size()) {
	            return instructionList.get(instructionPointer++);
	        }
        return null;
    }
    
    /**
     * Go back to the previous instruction (for retry after blocking)
     * Called when a process is blocked and needs to retry the same instruction
     */
    public void retryInstruction() {
        if (instructionPointer > 0) {
            instructionPointer--;
        }
    }
    
    public void resetInstructionPointer() {
        this.instructionPointer = 0;
    }
    
    /**
     * Safely adds a variable to the symbol table with validation
     * @return true if variable was added, false if limit exceeded
     */
    public boolean addVariable(String varName, int memoryAddress) throws Exception {
	        if (variableCount >= MAX_VARIABLES_PER_PROCESS) {
	            throw new Exception("Variable limit exceeded for Process " + processID + 
	                               ": max " + MAX_VARIABLES_PER_PROCESS + " variables allowed");
	        }
	        symbolTable.put(varName, memoryAddress);
	        variableCount++;
	        return true;
	    }
	    
	    /**
	     * Get variable memory address with bounds checking
	     */
	    public int getVariableAddress(String varName) throws Exception {
	        if (!symbolTable.containsKey(varName)) {
	            throw new Exception("Variable '" + varName + "' not found in Process " + processID);
	        }
	        return symbolTable.get(varName);
	    }

}
