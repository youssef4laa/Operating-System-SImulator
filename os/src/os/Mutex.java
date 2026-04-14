package os;

import java.util.LinkedList;

/**
 * Mutex - Binary semaphore for mutual exclusion of critical resources
 * Ensures only ONE process can access a resource at a time
 * Supports FIFO ordering of blocked processes
 */
public class Mutex {
	
	private boolean isLocked = false;
	private PCB owner = null;  // Process currently holding the mutex
	private LinkedList<PCB> waitQueue = new LinkedList<>();  // Processes waiting for this mutex
	private String resourceName = "Unknown";  // For logging/debugging
	private int acquireCount = 0;  // Number of times this mutex has been acquired
	private boolean suppressLogging = false;  // Suppress logs during retries
	
	public Mutex() {
		this.resourceName = "Unknown";
	}
	
	public Mutex(String name) {
		this.resourceName = name;
	}
	
	/**
	 * Set whether to suppress logging output (for instruction retries)
	 */
	public void setSuppressLogging(boolean suppress) {
		this.suppressLogging = suppress;
	}

	/**
	 * Attempt to acquire the mutex (semWait operation)
	 * If free, immediately acquires it (returns true, process continues)
	 * If owned, blocks the process and adds to wait queue (returns false, process blocks)
	 * 
	 * @param p Process requesting the mutex
	 * @param scheduler Scheduler reference for queue management
	 * @return true if acquired immediately, false if blocked
	 */
	public boolean acquire(PCB p, Scheduler scheduler) {
		if (!isLocked) {
			// Mutex is free - acquire it immediately
			isLocked = true;
			owner = p;
			acquireCount++;
			if (!suppressLogging) {
				System.out.println("  [MUTEX] Process " + p.processID + " acquired mutex '" + 
					resourceName + "' (count: " + acquireCount + ")");
			}
			return true;  // Process continues without blocking
			
		} else if (owner == p) {
			// Same process trying to re-acquire - for now, we don't allow recursive locks
			// (non-reentrant mutex). If we need reentrant, count acquisitions
			if (!suppressLogging) {
				System.out.println("  [MUTEX] WARNING: Process " + p.processID + 
					" attempted to re-acquire mutex '" + resourceName + "' (already owns it)");
			}
			return true;  // Allow immediate return without blocking
			
		} else {
			// Different process owns the mutex - block this process
			if (!waitQueue.contains(p)) {  // Avoid adding duplicate
				waitQueue.add(p);
				p.status = "Blocked";
				scheduler.blockedQueue.add(p);
				if (!suppressLogging) {
					System.out.println("  [MUTEX] Process " + p.processID + " blocked on mutex '" + 
						resourceName + "' (waiting for process " + owner.processID + ")");
					System.out.println("  [MUTEX] Wait queue size: " + waitQueue.size());
				}
			}
			return false;  // Process is blocked
		}
	}

	/**
	 * Release the mutex (semSignal operation)
	 * If processes are waiting, transfers ownership to first waiting process
	 * If no waiting processes, unlocks the mutex
	 * 
	 * @param scheduler Scheduler reference for queue management
	 * @param currentProcess The process releasing the mutex (optional validation)
	 */
	public void release(Scheduler scheduler, PCB currentProcess) {
		if (currentProcess != null && owner != currentProcess) {
			System.err.println("  [MUTEX ERROR] Process " + currentProcess.processID + 
				" attempted to release mutex '" + resourceName + 
				"' owned by process " + (owner != null ? owner.processID : "NONE"));
			return;
		}
		
		if (!waitQueue.isEmpty()) {
			// Transfer ownership to next waiting process
			PCB nextProcess = waitQueue.poll();
			owner = nextProcess;
			nextProcess.status = "Ready";
			
			// Move from blocked queue to ready queue
			if (scheduler.blockedQueue.remove(nextProcess)) {
				scheduler.readyQueue.add(nextProcess);
				if (!suppressLogging) {
					System.out.println("  [MUTEX] Released to Process " + nextProcess.processID + 
						" from mutex '" + resourceName + "' (next in queue)");
					System.out.println("  [MUTEX] Wait queue size: " + waitQueue.size());
				}
			} else {
				System.err.println("  [MUTEX ERROR] Could not find released process in blocked queue");
			}
		} else {
			// No waiting processes - unlock the mutex
			isLocked = false;
			owner = null;
			if (!suppressLogging) {
				System.out.println("  [MUTEX] Mutex '" + resourceName + "' is now unlocked");
			}
		}
	}
	
	/**
	 * Overloaded release for compatibility with existing code
	 */
	public void release(Scheduler scheduler) {
		release(scheduler, owner);
	}
	
	/**
	 * Check if the mutex is currently locked
	 */
	public boolean isLocked() {
		return isLocked;
	}
	
	/**
	 * Get the current owner process
	 */
	public PCB getOwner() {
		return owner;
	}
	
	/**
	 * Get the name of the resource protected by this mutex
	 */
	public String getResourceName() {
		return resourceName;
	}
	
	/**
	 * Get the number of processes waiting for this mutex
	 */
	public int getWaitQueueSize() {
		return waitQueue.size();
	}
	
	/**
	 * Get a string representation of the mutex status
	 */
	@Override
	public String toString() {
		return "Mutex{" +
			"resource='" + resourceName + '\'' +
			", isLocked=" + isLocked +
			", owner=" + (owner != null ? "P" + owner.processID : "NONE") +
			", waitQueueSize=" + waitQueue.size() +
			'}';
	}
}
