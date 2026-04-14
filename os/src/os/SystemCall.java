package os;

import java.util.Scanner;
import java.io.*;
import java.util.*;

/**
 * SystemCall - Interface between processes and OS resources
 * Implements 6 system calls for process-OS communication
 * All I/O operations should be protected by mutexes (enforced by caller)
 * 
 * System Calls:
 * 1. readFile(filename) - Read file contents
 * 2. writeFile(filename, data) - Write data to file
 * 3. print(message) - Print to console
 * 4. input() - Read user input
 * 5. readMemory(address, memory) - Read process memory
 * 6. writeMemory(address, data, memory) - Write process memory
 */
public class SystemCall {
	
    // Global input scanner
    private static Scanner scanner = new Scanner(System.in);
    
    // Statistics tracking
    private static SystemCallStats stats = new SystemCallStats();
    
    // Verbose logging flag
    private static boolean verboseLogging = true;
    
    // Return codes
    public static final int SUCCESS = 0;
    public static final int FILE_NOT_FOUND = -1;
    public static final int FILE_WRITE_ERROR = -2;
    public static final int MEMORY_ACCESS_ERROR = -3;
    public static final int INPUT_ERROR = -4;
    public static final int INVALID_PARAMETER = -5;
    public static final int IO_ERROR = -6;

    /**
     * System Call 3: Print text to console
     * MUST be protected by mutexUserOutput before calling
     * 
     * @param message Text message to print
     * @return SUCCESS (0) on success, error code on failure
     */
    public static int print(String message) {
        if (message == null) {
            logError("print: null message");
            return INVALID_PARAMETER;
        }
        
        try {
            System.out.println(message);
            stats.recordCall("print", SUCCESS);
            logInfo("print: '" + message + "'");
            return SUCCESS;
        } catch (Exception e) {
            logError("print: " + e.getMessage());
            stats.recordCall("print", -1);
            return -1;
        }
    }

    /**
     * System Call 4: Read text input from user
     * MUST be protected by mutexUserInput before calling
     * 
     * @return User input string, or null on error
     */
    public static String input() {
        try {
            System.out.print("Please enter a value: ");
            System.out.flush();
            String inputValue = scanner.nextLine();
            stats.recordCall("input", SUCCESS);
            logInfo("input: received '" + inputValue + "'");
            return inputValue;
        } catch (NoSuchElementException e) {
            logError("input: no input available");
            stats.recordCall("input", INPUT_ERROR);
            return null;
        } catch (Exception e) {
            logError("input: " + e.getMessage());
            stats.recordCall("input", INPUT_ERROR);
            return null;
        }
    }

    /**
     * System Call 2: Write data to file
     * MUST be protected by mutexFile before calling
     * Creates file if it doesn't exist, overwrites if it does
     * 
     * @param filename Name of file to write
     * @param data Data to write to file
     * @return SUCCESS (0) on success, error code on failure
     */
    public static int writeFile(String filename, String data) {
        if (filename == null || filename.isEmpty()) {
            logError("writeFile: invalid filename");
            stats.recordCall("writeFile", INVALID_PARAMETER);
            return INVALID_PARAMETER;
        }
        
        if (data == null) {
            logError("writeFile: null data");
            stats.recordCall("writeFile", INVALID_PARAMETER);
            return INVALID_PARAMETER;
        }
        
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write(data);
            fw.close();
            
            stats.recordCall("writeFile", SUCCESS);
            logInfo("writeFile: wrote " + data.length() + " bytes to '" + filename + "'");
            System.out.println("[SYSCALL] File written: " + filename);
            return SUCCESS;
        } catch (IOException e) {
            logError("writeFile: " + e.getMessage());
            stats.recordCall("writeFile", FILE_WRITE_ERROR);
            System.err.println("[SYSCALL ERROR] File write error: " + e.getMessage());
            return FILE_WRITE_ERROR;
        }
    }

    /**
     * System Call 1: Read data from file
     * MUST be protected by mutexFile before calling
     * 
     * @param filename Name of file to read
     * @return File contents as String, or null on error
     */
    public static String readFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            logError("readFile: invalid filename");
            stats.recordCall("readFile", INVALID_PARAMETER);
            return null;
        }
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            StringBuilder content = new StringBuilder();
            String line;
            int lineCount = 0;
            
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
                lineCount++;
            }
            br.close();
            
            stats.recordCall("readFile", SUCCESS);
            logInfo("readFile: read " + lineCount + " lines from '" + filename + "'");
            return content.toString();
        } catch (FileNotFoundException e) {
            logError("readFile: file not found - " + filename);
            stats.recordCall("readFile", FILE_NOT_FOUND);
            System.err.println("[SYSCALL ERROR] File not found: " + filename);
            return null;
        } catch (IOException e) {
            logError("readFile: " + e.getMessage());
            stats.recordCall("readFile", IO_ERROR);
            System.err.println("[SYSCALL ERROR] File read error: " + e.getMessage());
            return null;
        }
    }

    /**
     * System Call 5: Read data from process memory
     * No mutex needed - memory is per-process
     * Memory bounds are enforced by Memory class
     * 
     * @param address Memory address to read from
     * @param memory Memory instance
     * @return Object at memory address, null if empty or error
     */
    public static Object readMemory(int address, Memory memory) {
        if (memory == null) {
            logError("readMemory: null memory object");
            stats.recordCall("readMemory", MEMORY_ACCESS_ERROR);
            return null;
        }
        
        try {
            Object value = memory.read(address);
            stats.recordCall("readMemory", SUCCESS);
            logInfo("readMemory: read from address " + address + " = " + value);
            return value;
        } catch (IndexOutOfBoundsException e) {
            logError("readMemory: address out of bounds - " + address);
            stats.recordCall("readMemory", MEMORY_ACCESS_ERROR);
            return null;
        } catch (Exception e) {
            logError("readMemory: " + e.getMessage());
            stats.recordCall("readMemory", MEMORY_ACCESS_ERROR);
            return null;
        }
    }

    /**
     * System Call 6: Write data to process memory
     * No mutex needed - memory is per-process
     * Memory bounds are enforced by Memory class
     * 
     * @param address Memory address to write to
     * @param data Data to write
     * @param memory Memory instance
     * @return SUCCESS (0) on success, error code on failure
     */
    public static int writeMemory(int address, Object data, Memory memory) {
        if (memory == null) {
            logError("writeMemory: null memory object");
            stats.recordCall("writeMemory", MEMORY_ACCESS_ERROR);
            return MEMORY_ACCESS_ERROR;
        }
        
        if (data == null) {
            logError("writeMemory: null data at address " + address);
            stats.recordCall("writeMemory", INVALID_PARAMETER);
            return INVALID_PARAMETER;
        }
        
        try {
            memory.write(address, data);
            stats.recordCall("writeMemory", SUCCESS);
            logInfo("writeMemory: wrote to address " + address + " = " + data);
            return SUCCESS;
        } catch (IndexOutOfBoundsException e) {
            logError("writeMemory: address out of bounds - " + address);
            stats.recordCall("writeMemory", MEMORY_ACCESS_ERROR);
            return MEMORY_ACCESS_ERROR;
        } catch (Exception e) {
            logError("writeMemory: " + e.getMessage());
            stats.recordCall("writeMemory", MEMORY_ACCESS_ERROR);
            return MEMORY_ACCESS_ERROR;
        }
    }

    // ============= UTILITY METHODS =============

    /**
     * Get system call statistics
     */
    public static SystemCallStats getStats() {
        return stats;
    }

    /**
     * Enable/disable verbose logging
     */
    public static void setVerboseLogging(boolean verbose) {
        verboseLogging = verbose;
        logInfo("Verbose logging: " + (verbose ? "ENABLED" : "DISABLED"));
    }

    /**
     * Log information message
     */
    private static void logInfo(String message) {
        if (verboseLogging) {
            System.out.println("[SYSCALL] " + message);
        }
    }

    /**
     * Log error message
     */
    private static void logError(String message) {
        System.err.println("[SYSCALL ERROR] " + message);
    }

    /**
     * Print all system call statistics
     */
    public static void printStatistics() {
        stats.printStatistics();
    }

    /**
     * Reset all statistics
     */
    public static void resetStatistics() {
        stats.reset();
    }

    /**
     * Get return code as human-readable string
     */
    public static String getReturnCodeName(int code) {
        switch (code) {
            case SUCCESS: return "SUCCESS";
            case FILE_NOT_FOUND: return "FILE_NOT_FOUND";
            case FILE_WRITE_ERROR: return "FILE_WRITE_ERROR";
            case MEMORY_ACCESS_ERROR: return "MEMORY_ACCESS_ERROR";
            case INPUT_ERROR: return "INPUT_ERROR";
            case INVALID_PARAMETER: return "INVALID_PARAMETER";
            case IO_ERROR: return "IO_ERROR";
            default: return "UNKNOWN_ERROR(" + code + ")";
        }
    }

    /**
     * Close the system call subsystem
     */
    public static void shutdown() {
        if (scanner != null) {
            scanner.close();
        }
        logInfo("System calls shutdown");
    }
}
