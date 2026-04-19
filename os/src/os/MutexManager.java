package os;

import java.util.*;

/**
 * MutexManager - Centralized management of all system mutexes
 * Handles 3 critical resources: file access, user input, user output
 * Provides logging and statistics for mutex operations
 */
public class MutexManager {
	
	private final Mutex mutexUserOutput;
	private final Mutex mutexUserInput;
	private final Mutex mutexFile;
	
	private Map<String, Mutex> resourceMap;
	private Map<String, Long> statisticsAcquireCount;
	private boolean verboseLogging = true;
	
	/**
	 * Initialize the mutex manager with three resource mutexes
	 */
	public MutexManager() {
		// Create the three required mutexes
		this.mutexUserOutput = new Mutex("userOutput");
		this.mutexUserInput = new Mutex("userInput");
		this.mutexFile = new Mutex("file");
		
		// Create resource map for easy access
		this.resourceMap = new HashMap<>();
		resourceMap.put("useroutput", mutexUserOutput);
		resourceMap.put("userinput", mutexUserInput);
		resourceMap.put("file", mutexFile);
		
		// Initialize statistics
		this.statisticsAcquireCount = new HashMap<>();
		statisticsAcquireCount.put("userOutput", 0L);
		statisticsAcquireCount.put("userInput", 0L);
		statisticsAcquireCount.put("file", 0L);
		
		logInfo("MutexManager initialized with 3 mutexes");
	}
	
	/**
	 * Get the mutex for user output resource
	 */
	public Mutex getUserOutputMutex() {
		return mutexUserOutput;
	}
	
	/**
	 * Get the mutex for user input resource
	 */
	public Mutex getUserInputMutex() {
		return mutexUserInput;
	}
	
	/**
	 * Get the mutex for file access resource
	 */
	public Mutex getFileMutex() {
		return mutexFile;
	}
	
	/**
	 * Get a mutex by resource name
	 * @param resourceName Should be: "useroutput", "userinput", or "file"
	 * @return The appropriate Mutex, or null if resource not found
	 */
	public Mutex getMutex(String resourceName) {
		String key = resourceName.toLowerCase();
		return resourceMap.get(key);
	}
	
	/**
	 * Acquire a resource mutex (semWait operation)
	 * @param resourceName The name of the resource
	 * @param process The process attempting to acquire
	 * @param scheduler The scheduler for queue management
	 * @return true if acquired, false if blocked
	 */
	public boolean acquire(String resourceName, PCB process, Scheduler scheduler) {
		Mutex mutex = getMutex(resourceName);
		if (mutex == null) {
			logError("Unknown resource: " + resourceName);
			return false;
		}
		
		boolean acquired = mutex.acquire(process, scheduler);
		if (acquired) {
			incrementStatistic(resourceName);
		}
		return acquired;
	}
	
	/**
	 * Release a resource mutex (semSignal operation)
	 * @param resourceName The name of the resource
	 * @param currentProcess The process releasing the resource
	 * @param scheduler The scheduler for queue management
	 */
	public void release(String resourceName, PCB currentProcess, Scheduler scheduler) {
		Mutex mutex = getMutex(resourceName);
		if (mutex == null) {
			logError("Unknown resource: " + resourceName);
			return;
		}
		
		mutex.release(scheduler, currentProcess);
	}
	
	/**
	 * Release a resource mutex without ownership validation
	 */
	public void release(String resourceName, Scheduler scheduler) {
		Mutex mutex = getMutex(resourceName);
		if (mutex == null) {
			logError("Unknown resource: " + resourceName);
			return;
		}
		
		mutex.release(scheduler);
	}
	
	/**
	 * Get current status of all mutexes
	 */
	public String getMutexStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n========== MUTEX STATUS ==========\n");
		sb.append(mutexUserOutput.toString()).append("\n");
		sb.append(mutexUserInput.toString()).append("\n");
		sb.append(mutexFile.toString()).append("\n");
		sb.append("==================================\n");
		return sb.toString();
	}
	
	/**
	 * Get detailed status of a specific mutex
	 */
	public String getMutexStatus(String resourceName) {
		Mutex mutex = getMutex(resourceName);
		if (mutex == null) {
			return "Unknown resource: " + resourceName;
		}
		return mutex.toString();
	}
	
	/**
	 * Print statistics about mutex usage
	 */
	public void printStatistics() {
		System.out.println("\n========== MUTEX STATISTICS ==========");
		for (String resource : statisticsAcquireCount.keySet()) {
			System.out.println(resource + " acquire count: " + 
				statisticsAcquireCount.get(resource));
		}
		System.out.println("======================================\n");
	}
	
	/**
	 * Increment acquire statistics
	 */
	private void incrementStatistic(String resourceName) {
		String key = resourceName.substring(0, 1).toUpperCase() + 
			resourceName.substring(1);
		statisticsAcquireCount.put(key, statisticsAcquireCount.get(key) + 1);
	}
	
	/**
	 * Check if any mutex has waiting processes
	 */
	public boolean hasBlockedProcesses() {
		return mutexUserOutput.getWaitQueueSize() > 0 ||
			   mutexUserInput.getWaitQueueSize() > 0 ||
			   mutexFile.getWaitQueueSize() > 0;
	}
	
	/**
	 * Get total number of blocked processes across all mutexes
	 */
	public int getTotalBlockedProcesses() {
		return mutexUserOutput.getWaitQueueSize() +
			   mutexUserInput.getWaitQueueSize() +
			   mutexFile.getWaitQueueSize();
	}
	
	/**
	 * Enable or disable verbose logging
	 */
	public void setVerboseLogging(boolean verbose) {
		this.verboseLogging = verbose;
	}
	
	/**
	 * MLFQ SUPPORT: Configure priority inheritance for all mutexes
	 * When enabled, processes with higher priority blocked on a mutex will boost the owner process
	 * @param enable true to enable priority inheritance
	 */
	public void configureMLFQPriorityInheritance(boolean enable) {
		mutexUserOutput.setPriorityInheritance(enable);
		mutexUserInput.setPriorityInheritance(enable);
		mutexFile.setPriorityInheritance(enable);
		logInfo("MLFQ Priority Inheritance: " + (enable ? "ENABLED" : "DISABLED"));
	}
	
	/**
	 * MLFQ SUPPORT: Configure priority boost for all mutexes
	 * When enabled, processes unblocked from a mutex are temporarily boosted to a higher priority queue
	 * @param enable true to enable priority boost
	 */
	public void configureMLFQPriorityBoost(boolean enable) {
		mutexUserOutput.setPriorityBoost(enable);
		mutexUserInput.setPriorityBoost(enable);
		mutexFile.setPriorityBoost(enable);
		logInfo("MLFQ Priority Boost: " + (enable ? "ENABLED" : "DISABLED"));
	}
	
	/**
	 * MLFQ SUPPORT: Set the queue level to boost unblocked processes to
	 * @param level The MLFQ queue level (0-3, where 0 is highest priority)
	 */
	public void configureMLFQBoostLevel(int level) {
		mutexUserOutput.setBoostLevel(level);
		mutexUserInput.setBoostLevel(level);
		mutexFile.setBoostLevel(level);
		logInfo("MLFQ Boost Level set to Q" + level);
	}
	
	/**
	 * MLFQ SUPPORT: Get detailed MLFQ status for all mutexes
	 */
	public String getMLFQStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n========== MLFQ MUTEX STATUS ==========\n");
		sb.append(mutexUserOutput.getMLFQStatus()).append("\n");
		sb.append(mutexUserInput.getMLFQStatus()).append("\n");
		sb.append(mutexFile.getMLFQStatus()).append("\n");
		sb.append("=======================================\n");
		return sb.toString();
	}
	
	/**
	 * Log information message
	 */
	private void logInfo(String message) {
		if (verboseLogging) {
			System.out.println("[MutexManager] " + message);
		}
	}
	
	/**
	 * Log error message
	 */
	private void logError(String message) {
		System.err.println("[MutexManager ERROR] " + message);
	}
}
