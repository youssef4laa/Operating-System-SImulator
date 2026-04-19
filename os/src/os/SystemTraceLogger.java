package os;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
import java.util.ArrayDeque;

/**
 * SystemTraceLogger - Implements the SchedulerObserver interface
 * Provides detailed system trace logging with clean formatting
 * 
 * Logs:
 * - Ready Queue, Blocked Queue, Finished Queue after each scheduling event
 * - Current executing process and its state
 * - Memory state (40-word array) after each clock cycle
 * - Disk file listing
 */
public class SystemTraceLogger implements SchedulerObserver {
    
    private static final String SEPARATOR = "=".repeat(100);
    private static final String SUBSEP = "-".repeat(100);
    private boolean enabled = true;
    private long simulationStartTime;
    
    public SystemTraceLogger() {}
    
    public SystemTraceLogger(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public void onSimulationStart() {
        if (!enabled) return;
        simulationStartTime = System.currentTimeMillis();
        System.out.println(SEPARATOR);
        System.out.println("SYSTEM TRACE LOG - OS Simulator Started");
        System.out.println("Timestamp: " + LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println(SEPARATOR);
    }
    
    @Override
    public void onSimulationEnd() {
        if (!enabled) return;
        long runtime = System.currentTimeMillis() - simulationStartTime;
        System.out.println(SEPARATOR);
        System.out.println("SYSTEM TRACE LOG - OS Simulator Ended");
        System.out.println("Total Runtime: " + runtime + "ms");
        System.out.println(SEPARATOR);
    }
    
    @Override
    public void onSchedulingEvent(SchedulingEvent event) {
        if (!enabled) return;
        
        System.out.println(SUBSEP);
        System.out.println("[SCHEDULING EVENT] " + event.toString());
        
        if (event.affectedProcess != null) {
            System.out.println("  Process Details:");
            System.out.println("    ID: P" + event.affectedProcess.processID);
            System.out.println("    Status: " + event.affectedProcess.status);
            System.out.println("    PC: " + event.affectedProcess.programCounter);
            System.out.println("    Instructions: " + event.affectedProcess.instructionList.size());
        }
        
        System.out.println("  Queue Sizes:");
        System.out.println("    Ready: " + event.readyQueueSize);
        System.out.println("    Blocked: " + event.blockedQueueSize);
        System.out.println("    Finished: " + event.finishedQueueSize);
        
        if (event.eventDetails != null && !event.eventDetails.isEmpty()) {
            System.out.println("  Details: " + event.eventDetails);
        }
    }
    
    @Override
    public void onClockCycle(int clockCycle, Memory memory, Scheduler scheduler) {
        if (!enabled) return;
        
        System.out.println(SUBSEP);
        System.out.println("[CLOCK CYCLE] " + clockCycle);
        
        // Memory state
        System.out.println("  Memory State (40 words):");
        printMemoryState(memory);
        
        // Queue states
        System.out.println("  Queue States:");
        printQueueStates(scheduler);
        
        // Disk state
        System.out.println("  Disk State:");
        printDiskState();
        
        System.out.println();
    }
    
    /**
     * Print the 40-word memory array in a readable format
     */
    private void printMemoryState(Memory memory) {
        if (memory == null) {
            System.out.println("    [Memory unavailable]");
            return;
        }
        
        int memSize = memory.getSize();
        
        // Print memory in rows of 10 words for readability
        System.out.println("    Address | Value (10 words per row)");
        System.out.println("    " + "-".repeat(70));
        
        for (int i = 0; i < memSize; i += 10) {
            StringBuilder line = new StringBuilder();
            line.append(String.format("    %3d-%3d | ", i, Math.min(i + 9, memSize - 1)));
            
            for (int j = i; j < Math.min(i + 10, memSize); j++) {
                Object value = null;
                try {
                    value = memory.read(j);
                } catch (Exception e) {
                    value = "[Error reading memory]";
                }
                
                String displayValue;
                
                if (value == null) {
                    displayValue = "null";
                } else if (value instanceof String && ((String)value).length() > 10) {
                    displayValue = "\"" + ((String)value).substring(0, 7) + "...\"";
                } else if (value instanceof String) {
                    displayValue = "\"" + value + "\"";
                } else {
                    displayValue = value.toString();
                }
                
                line.append(String.format("%-12s ", displayValue));
            }
            
            System.out.println(line.toString());
        }
    }
    
    /**
     * Print the current queue states
     */
    private void printQueueStates(Scheduler scheduler) {
        if (scheduler == null) {
            System.out.println("    [Scheduler unavailable]");
            return;
        }
        
        // Ready Queue
        System.out.print("    Ready Queue: [");
        Queue<PCB> rq = new ArrayDeque<>(scheduler.getReadyQueue());
        if (!rq.isEmpty()) {
            Queue<PCB> tempQueue = new ArrayDeque<>(rq);
            while (!tempQueue.isEmpty()) {
                PCB p = tempQueue.poll();
                System.out.print("P" + p.processID);
                if (!tempQueue.isEmpty()) System.out.print(", ");
            }
        } else {
            System.out.print("empty");
        }
        System.out.println("]");
        
        // Blocked Queue
        System.out.print("    Blocked Queue: [");
        if (!scheduler.blockedQueue.isEmpty()) {
            Queue<PCB> tempQueue = new ArrayDeque<>(scheduler.blockedQueue);
            while (!tempQueue.isEmpty()) {
                PCB p = tempQueue.poll();
                System.out.print("P" + p.processID);
                if (!tempQueue.isEmpty()) System.out.print(", ");
            }
        } else {
            System.out.print("empty");
        }
        System.out.println("]");
        
        // Finished Queue
        System.out.print("    Finished Queue: [");
        if (!scheduler.finishedQueue.isEmpty()) {
            Queue<PCB> tempQueue = new ArrayDeque<>(scheduler.finishedQueue);
            while (!tempQueue.isEmpty()) {
                PCB p = tempQueue.poll();
                System.out.print("P" + p.processID);
                if (!tempQueue.isEmpty()) System.out.print(", ");
            }
        } else {
            System.out.print("empty");
        }
        System.out.println("]");
    }
    
    /**
     * Print disk state (files that have been written)
     */
    private void printDiskState() {
        // This is a placeholder - in a real system, you'd track disk files
        System.out.println("    [Disk state would show file listing here]");
    }
}
