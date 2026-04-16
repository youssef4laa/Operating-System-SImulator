package os;

/**
 * SchedulingEvent - Represents a scheduling event in the OS simulator
 * Used by SchedulerObserver pattern to notify listeners of scheduler state changes
 */
public class SchedulingEvent {
    
    public enum EventType {
        PROCESS_SELECTED,      // Process selected from ready queue
        PROCESS_BLOCKED,       // Process blocked on mutex
        PROCESS_UNBLOCKED,     // Process unblocked from mutex
        PROCESS_FINISHED,      // Process completed execution
        TIME_QUANTUM_EXPIRED,  // Time slice ended
        CLOCK_CYCLE            // New clock cycle
    }
    
    public EventType type;
    public int clockCycle;
    public PCB affectedProcess;      // Process that triggered the event (null for CLOCK_CYCLE)
    public String eventDetails;      // Additional description
    public int readyQueueSize;
    public int blockedQueueSize;
    public int finishedQueueSize;
    
    public SchedulingEvent(EventType type, int clockCycle) {
        this.type = type;
        this.clockCycle = clockCycle;
        this.eventDetails = "";
    }
    
    public SchedulingEvent(EventType type, int clockCycle, PCB process) {
        this.type = type;
        this.clockCycle = clockCycle;
        this.affectedProcess = process;
        this.eventDetails = "";
    }
    
    @Override
    public String toString() {
        String processInfo = affectedProcess != null ? 
            "P" + affectedProcess.processID : "N/A";
        return String.format("[Clock %d] %s - Process: %s - %s", 
            clockCycle, type, processInfo, eventDetails);
    }
}
