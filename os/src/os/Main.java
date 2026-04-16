package os;

/**
 * Main entry point for the Operating System simulator
 * Launches the Swing GUI for OS simulation
 * 
 * Also supports command-line mode with -cli flag
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║   Operating System Simulator - Version 2.0               ║");
        System.out.println("║   CSEN 602 - Spring 2026                                 ║");
        System.out.println("║   With GUI and Real-time Visualization                   ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        // Check for command-line mode flag
        if (args.length > 0 && args[0].equals("-cli")) {
            runCLIMode();
        } else {
            // Launch Swing GUI
            System.out.println("[Main] Launching GUI interface...\n");
            OSSimulatorGUI.main(args);
        }
    }
    
    /**
     * Run in command-line mode (no GUI)
     */
    private static void runCLIMode() {
        try {
            System.out.println("[Main] Running in CLI mode (no GUI)\n");
            
            // Initialize main memory (40 words)
            Memory memory = new Memory();
            System.out.println("[Main] Initializing memory: 40 words");
            
            // Initialize scheduler
            Scheduler scheduler = new Scheduler();
            System.out.println("[Main] Initializing scheduler");
            
            // Register system trace logger for detailed logging
            SystemTraceLogger traceLogger = new SystemTraceLogger(true);  // true = enabled
            scheduler.addObserver(traceLogger);
            System.out.println("[Main] System trace logger registered");
            
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
