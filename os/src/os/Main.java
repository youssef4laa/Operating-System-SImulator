package os;

/**
 * Main entry point for the Operating System simulator
 * Initializes memory, scheduler, and mutexes, then starts the simulation
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║   Operating System Simulator - Version 1.0               ║");
        System.out.println("║   CSEN 602 - Spring 2026                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        try {
            // Initialize main memory (40 words)
            Memory memory = new Memory();
            System.out.println("[Main] Initializing memory: 40 words");
            
            // Initialize scheduler
            Scheduler scheduler = new Scheduler();
            System.out.println("[Main] Initializing scheduler");
            
            // Set scheduling algorithm
            // Options: "RR" (Round Robin), "HRRN" (Highest Response Ratio Next), "MLFQ" (Multi-Level Feedback Queue)
            scheduler.algorithm = "RR";
            System.out.println("[Main] Scheduling algorithm: " + scheduler.algorithm);
            
            System.out.println("\n[Main] Starting simulation...\n");
            
            // Start the scheduler (this will load and execute all processes)
            scheduler.start(memory);
            
            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║   Simulation Completed                                   ║");
            System.out.println("╚════════════════════════════════════════════════════════╝\n");
            
        } catch (Exception e) {
            System.err.println("\n[Error] Simulation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
