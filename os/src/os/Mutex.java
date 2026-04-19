package os;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

/**
 * Mutex - Binary semaphore for mutual exclusion of critical resources
 * Ensures only ONE process can access a resource at a time
 * Supports FIFO ordering of blocked processes
 * MLFQ-aware: Tracks queue levels and supports priority inheritance/boost
 */
public class Mutex {
	
	private boolean isLocked = false;
	private PCB owner = null;  // Process currently holding the mutex
	private LinkedList<PCB> waitQueue = new LinkedList<>();  // Processes waiting for this mutex
	private String resourceName = "Unknown";  // For logging/debugging
	private int acquireCount = 0;  // Number of times this mutex has been acquired
	private boolean suppressLogging = false;  // Suppress logs during retries
	
	// MLFQ support
	private Map<PCB, Integer> queueLevelMap = new HashMap<>();  // Track MLFQ queue level for each blocked process
	private boolean enablePriorityInheritance = true;  // Enable priority inheritance for mutex owners
	private boolean enablePriorityBoost = true;  // Boost unblocked processes to higher priority queues
	private int boostLevel = 0;  // Queue level to boost unblocked processes to (0 = highest priority)
	
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
	 * MLFQ-aware: Tracks queue level of blocked processes
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
				queueLevelMap.put(p, p.currentQueueLevel);  // Remember process's MLFQ queue level
				scheduler.enqueueBlocked(p);
				
				// Apply priority inheritance if enabled
				if (enablePriorityInheritance && owner != null && p.currentQueueLevel < owner.currentQueueLevel) {
					// Higher priority process (lower queue number) blocked waiting for lower priority process
					// Temporarily boost owner to same level as waiting process
					owner.inheritedPriority = p.currentQueueLevel;
					if (!suppressLogging) {
						System.out.println("  [MLFQ MUTEX] Priority inheritance: P" + owner.processID + 
							" boosted to queue Q" + p.currentQueueLevel + " (waiting for P" + p.processID + ")");
					}
				}
				
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
	 * MLFQ-aware: Boosts unblocked processes to higher priority queues if enabled
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
		
		// Clear priority inheritance if it was set
		if (releasingOwner.inheritedPriority != -1) {
			if (!suppressLogging) {
				System.out.println("  [MLFQ MUTEX] Priority inheritance released: P" + releasingOwner.processID + 
					" returns to queue Q" + releasingOwner.currentQueueLevel);
			}
			releasingOwner.inheritedPriority = -1;
		}
		
		if (!waitQueue.isEmpty()) {
			// Transfer ownership to next waiting process
			PCB nextProcess = waitQueue.poll();
			Integer originalLevel = queueLevelMap.remove(nextProcess);
			
			releasingOwner.ownedMutexes.remove(this);
			owner = nextProcess;
			if (!nextProcess.ownedMutexes.contains(this)) {
				nextProcess.ownedMutexes.add(this);
			}
			nextProcess.status = "Ready";
			nextProcess.lastReadyEnqueueTime = scheduler.time;  // Update when process re-enters ready queue
			
			// Apply priority boost if enabled
			if (enablePriorityBoost && originalLevel != null) {
				nextProcess.currentQueueLevel = boostLevel;  // Boost to highest priority queue
				if (!suppressLogging) {
					System.out.println("  [MLFQ MUTEX] Priority boost: P" + nextProcess.processID + 
						" boosted from Q" + originalLevel + " to Q" + boostLevel + " (mutex released)");
				}
			}

			if ("MLFQ".equalsIgnoreCase(scheduler.algorithm)) {
				scheduler.enqueueMLFQ(nextProcess);
			} else {
				scheduler.enqueueReady(nextProcess);
			}

			SchedulingEvent event = new SchedulingEvent(
				SchedulingEvent.EventType.PROCESS_UNBLOCKED,
				scheduler.time,
				nextProcess
			);
			event.eventDetails = "Unblocked from mutex '" + resourceName + "' and moved to ready state";
			scheduler.emitSchedulingEvent(event);
		// Note: Release is logged at higher level
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
	
	/**
	 * MLFQ SUPPORT: Enable/disable priority inheritance for mutex owners
	 * When enabled, processes with higher priority (lower queue level) blocked on a mutex
	 * will temporarily boost the owner process to the same priority level
	 */
	public void setPriorityInheritance(boolean enable) {
		this.enablePriorityInheritance = enable;
	}
	
	/**
	 * MLFQ SUPPORT: Enable/disable priority boost for unblocked processes
	 * When enabled, processes unblocked from a mutex are temporarily boosted to a higher priority queue
	 */
	public void setPriorityBoost(boolean enable) {
		this.enablePriorityBoost = enable;
	}
	
	/**
	 * MLFQ SUPPORT: Set the queue level to boost unblocked processes to
	 * @param level The MLFQ queue level (0-3, where 0 is highest priority)
	 */
	public void setBoostLevel(int level) {
		if (level >= 0 && level <= 3) {
			this.boostLevel = level;
		}
	}
	
	/**
	 * MLFQ SUPPORT: Check if priority inheritance is enabled
	 */
	public boolean isPriorityInheritanceEnabled() {
		return enablePriorityInheritance;
	}
	
	/**
	 * MLFQ SUPPORT: Check if priority boost is enabled
	 */
	public boolean isPriorityBoostEnabled() {
		return enablePriorityBoost;
	}
	
	/**
	 * MLFQ SUPPORT: Get the boost level
	 */
	public int getBoostLevel() {
		return boostLevel;
	}
	
	/**
	 * MLFQ SUPPORT: Get the highest priority (lowest queue number) process waiting on this mutex
	 */
	public PCB getHighestPriorityWaiter() {
		if (waitQueue.isEmpty()) return null;
		
		PCB highest = waitQueue.peek();
		int highestLevel = queueLevelMap.getOrDefault(highest, 3);
		
		for (PCB p : waitQueue) {
			int level = queueLevelMap.getOrDefault(p, 3);
			if (level < highestLevel) {
				highest = p;
				highestLevel = level;
			}
		}
		return highest;
	}
	
	/**
	 * MLFQ SUPPORT: Get detailed MLFQ status
	 */
	public String getMLFQStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append("Mutex{").append(resourceName).append("}\n");
		sb.append("  Locked: ").append(isLocked).append("\n");
		sb.append("  Owner: ").append(owner != null ? "P" + owner.processID + " (Q" + owner.currentQueueLevel + ")" : "NONE").append("\n");
		sb.append("  Wait Queue: ").append(waitQueue.size()).append(" processes\n");
		for (PCB p : waitQueue) {
			int level = queueLevelMap.getOrDefault(p, -1);
			sb.append("    - P").append(p.processID).append(" (Q").append(level).append(")\n");
		}
		sb.append("  Priority Inheritance: ").append(enablePriorityInheritance).append("\n");
		sb.append("  Priority Boost: ").append(enablePriorityBoost).append(" (to Q").append(boostLevel).append(")\n");
		return sb.toString();
	}
}
