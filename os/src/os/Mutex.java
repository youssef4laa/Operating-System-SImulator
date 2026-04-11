package os;
import java.util.LinkedList;
public class Mutex {
	
	    boolean isLocked = false;
	    LinkedList<PCB> queue = new LinkedList<>();

	    public void acquire(PCB p, Scheduler scheduler) {
	        if (!isLocked) {
	            isLocked = true;
	        } else {
	            queue.add(p);
	            p.status = "Blocked";
	            scheduler.blockedQueue.add(p);
	        }
	    }

	    public void release(Scheduler scheduler) {
	        if (!queue.isEmpty()) {
	            PCB next = queue.poll();
	            next.status = "Ready";
	            scheduler.readyQueue.add(next);
	        } else {
	            isLocked = false;
	        }
	    }
	
}
