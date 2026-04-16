package os;

/**
 * SchedulerObserver - Observer interface for scheduler events
 * Implements the Observer design pattern to decouple logging from core scheduling logic
 * 
 * Observers are notified of:
 * 1. Scheduling events (process selection, blocking, finish)
 * 2. Clock cycles (for memory and system state snapshots)
 */
public interface SchedulerObserver {
    
    /**
     * Called when a scheduling event occurs (process selected, blocked, finished, etc.)
     * 
     * @param event SchedulingEvent containing event type, process, queue sizes, etc.
     */
    void onSchedulingEvent(SchedulingEvent event);
    
    /**
     * Called at the end of each clock cycle
     * Use this for periodic logging of system state (memory, queues, etc.)
     * 
     * @param clockCycle Current clock cycle number
     * @param memory Memory state at end of cycle
     * @param scheduler Scheduler with current queue states
     */
    void onClockCycle(int clockCycle, Memory memory, Scheduler scheduler);
    
    /**
     * Called when simulation starts
     */
    void onSimulationStart();
    
    /**
     * Called when simulation ends
     */
    void onSimulationEnd();
}
