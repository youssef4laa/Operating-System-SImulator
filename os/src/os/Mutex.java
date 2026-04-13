package os;
import java.util.LinkedList;
public class Mutex {
	
	    boolean isLocked = false;
	    PCB owner = null;  // Track which process owns this mutex
	    LinkedList<PCB> queue = new LinkedList<>();

	    /**
	     * Acquire mutex - allows same process to re-acquire (reentrant)
	     * @param p Process requesting the mutex
	     * @param scheduler Scheduler reference for queue management
	     */
	    public void acquire(PCB p, Scheduler scheduler) {
	        if (!isLocked) {
	            // Mutex is free, acquire it
	            isLocked = true;
	            owner = p;
	        } else if (owner == p) {
	            // Same process can re-acquire (reentrant - no block)
	            return;
	        } else {
	            // Different process owns it, block this process
	            queue.add(p);
	            p.status = "Blocked";
	            scheduler.blockedQueue.add(p);
	        }
	    }

	    /**
	     * Release mutex and unblock waiting process if any
	     * Transfers ownership to the next waiting process
	     * @param scheduler Scheduler reference for queue management
	     */
	    public void release(Scheduler scheduler) {
	        if (!queue.isEmpty()) {
	            // Transfer ownership to waiting process
	            PCB next = queue.poll();
	            owner = next;
	            next.status = "Ready";
	            // Remove from blocked queue and add to ready queue
	            scheduler.blockedQueue.remove(next);
	            scheduler.readyQueue.add(next);
	        } else {
	            // No waiting processes, unlock
	            isLocked = false;
	            owner = null;
	        }
	    }
	
}
