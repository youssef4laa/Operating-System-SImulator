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
			if (p != null && !p.ownedMutexes.contains(this)) {
				p.ownedMutexes.add(this);
			}
			acquireCount++;
		// Note: Mutex acquire is logged at higher level when needed
		return true;  // Process continues without blocking
		
		} else if (owner == p) {
			// Same process trying to re-acquire - for now, we don't allow recursive locks
			// (non-reentrant mutex). If we need reentrant, count acquisitions
		// Note: Re-acquire warning is logged at higher level when needed
			return true;  // Allow immediate return without blocking
			
		} else {
			// Different process owns the mutex - block this process
			if (!waitQueue.contains(p)) {  // Avoid adding duplicate
				waitQueue.add(p);
				p.status = "Blocked";
				scheduler.blockedQueue.add(p);
				SchedulingEvent event = new SchedulingEvent(
					SchedulingEvent.EventType.PROCESS_BLOCKED,
					scheduler.time,
					p
				);
				event.eventDetails = "Blocked on mutex '" + resourceName + "' owned by P" + owner.processID;
				scheduler.emitSchedulingEvent(event);
			// Note: Blocking is logged at higher level
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

		if (owner == null) {
			isLocked = false;
			return;
		}

		PCB releasingOwner = owner;
		
		if (!waitQueue.isEmpty()) {
			// Transfer ownership to next waiting process
			PCB nextProcess = waitQueue.poll();
			releasingOwner.ownedMutexes.remove(this);
			owner = nextProcess;
			if (!nextProcess.ownedMutexes.contains(this)) {
				nextProcess.ownedMutexes.add(this);
			}
			nextProcess.status = "Ready";
			nextProcess.lastReadyEnqueueTime = scheduler.time;  // Update when process re-enters ready queue
			
			// Move from blocked queue to ready queue
			if (scheduler.blockedQueue.remove(nextProcess)) {
				scheduler.readyQueue.add(nextProcess);
				SchedulingEvent event = new SchedulingEvent(
					SchedulingEvent.EventType.PROCESS_UNBLOCKED,
					scheduler.time,
					nextProcess
				);
				event.eventDetails = "Unblocked from mutex '" + resourceName + "' and moved to ready queue";
				scheduler.emitSchedulingEvent(event);
			// Note: Release is logged at higher level
			} else {
				System.err.println("  [MUTEX ERROR] Could not find released process in blocked queue");
			}
		} else {
			// No waiting processes - unlock the mutex
			releasingOwner.ownedMutexes.remove(this);
			isLocked = false;
			owner = null;
			// Note: Mutex unlock is logged at higher level when needed
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
