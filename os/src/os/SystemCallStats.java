package os;

import java.util.*;

/**
 * SystemCallStats - Tracks statistics for all system calls
 * Records success/failure rates and call counts for performance analysis
 */
public class SystemCallStats {

    private Map<String, Integer> callCount = new HashMap<>();
    private Map<String, Integer> successCount = new HashMap<>();
    private Map<String, Integer> failureCount = new HashMap<>();
    private long totalCallTime = 0;
    private long startTime;
    
    public SystemCallStats() {
        reset();
        startTime = System.currentTimeMillis();
    }
    
    /**
     * Record a system call execution
     * @param callName Name of the system call
     * @param returnCode Return code (SUCCESS or error code)
     */
    public void recordCall(String callName, int returnCode) {
        // Initialize if first time seeing this call
        if (!callCount.containsKey(callName)) {
            callCount.put(callName, 0);
            successCount.put(callName, 0);
            failureCount.put(callName, 0);
        }
        
        // Increment counters
        callCount.put(callName, callCount.get(callName) + 1);
        
        if (returnCode == SystemCall.SUCCESS) {
            successCount.put(callName, successCount.get(callName) + 1);
        } else {
            failureCount.put(callName, failureCount.get(callName) + 1);
        }
    }
    
    /**
     * Get total number of calls for a system call
     */
    public int getCallCount(String callName) {
        return callCount.getOrDefault(callName, 0);
    }
    
    /**
     * Get success count for a system call
     */
    public int getSuccessCount(String callName) {
        return successCount.getOrDefault(callName, 0);
    }
    
    /**
     * Get failure count for a system call
     */
    public int getFailureCount(String callName) {
        return failureCount.getOrDefault(callName, 0);
    }
    
    /**
     * Get success rate as percentage
     */
    public double getSuccessRate(String callName) {
        int total = getCallCount(callName);
        if (total == 0) return 0.0;
        return (double) getSuccessCount(callName) / total * 100;
    }
    
    /**
     * Print all statistics
     */
    public void printStatistics() {
        System.out.println("\n========== SYSTEM CALL STATISTICS ==========");
        
        int totalCalls = callCount.values().stream().mapToInt(Integer::intValue).sum();
        int totalSuccess = successCount.values().stream().mapToInt(Integer::intValue).sum();
        int totalFailure = failureCount.values().stream().mapToInt(Integer::intValue).sum();
        
        System.out.println("Total System Calls: " + totalCalls);
        System.out.println("Total Successful: " + totalSuccess);
        System.out.println("Total Failed: " + totalFailure);
        if (totalCalls > 0) {
            System.out.println("Overall Success Rate: " + 
                String.format("%.2f%%", (double) totalSuccess / totalCalls * 100));
        }
        
        System.out.println("\nPer-Call Statistics:");
        System.out.println(String.format("%-15s %-8s %-8s %-8s %-12s", 
            "Call Name", "Total", "Success", "Failed", "Success %"));
        System.out.println("---------------------------------------------------");
        
        for (String callName : callCount.keySet()) {
            int count = callCount.get(callName);
            int success = successCount.get(callName);
            double rate = getSuccessRate(callName);
            System.out.println(String.format("%-15s %-8d %-8d %-8d %-12.2f%%", 
                callName, count, success, failureCount.get(callName), rate));
        }
        
        System.out.println("==========================================\n");
    }
    
    /**
     * Reset all statistics
     */
    public void reset() {
        callCount.clear();
        successCount.clear();
        failureCount.clear();
        totalCallTime = 0;
    }
    
    /**
     * Get total number of all sys calls
     */
    public int getTotalCalls() {
        return callCount.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Get total successful calls
     */
    public int getTotalSuccess() {
        return successCount.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Get total failed calls
     */
    public int getTotalFailure() {
        return failureCount.values().stream().mapToInt(Integer::intValue).sum();
    }
}
