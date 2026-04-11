package os;

import java.util.Scanner;
import java.io.*;

/**
 * System Calls - Interface between processes and OS resources
 * All system calls should be protected by mutexes for safe concurrent access
 */
public class SystemCall {
	
    static Scanner scanner = new Scanner(System.in);
    
    // Return codes for system calls
    public static final int SUCCESS = 0;
    public static final int FILE_NOT_FOUND = -1;
    public static final int FILE_WRITE_ERROR = -2;
    public static final int MEMORY_ACCESS_ERROR = -3;
    public static final int INPUT_ERROR = -4;

    /**
     * System Call 1: Print text to console (protected by mutex)
     * Caller must acquire userOutput mutex before calling
     */
    public static int print(String x) {
        try {
            System.out.println(x);
            return SUCCESS;
        } catch (Exception e) {
            System.err.println("Print error: " + e.getMessage());
            return -1;
        }
    }

    /**
     * System Call 4: Read text input from user (protected by mutex)
     * Caller must acquire userInput mutex before calling
     */
    public static String input() {
        try {
            System.out.print("Please enter a value: ");
            return scanner.nextLine();
        } catch (Exception e) {
            System.err.println("Input error: " + e.getMessage());
            return null;
        }
    }

    /**
     * System Call 2: Write data to file (protected by mutex)
     * Caller must acquire file mutex before calling
     * @return SUCCESS on success, FILE_WRITE_ERROR on failure
     */
    public static int writeFile(String file, String data) {
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(data);
            fw.close();
            System.out.println("File written: " + file);
            return SUCCESS;
        } catch (IOException e) {
            System.err.println("File write error: " + e.getMessage());
            return FILE_WRITE_ERROR;
        }
    }

    /**
     * System Call 1: Read data from file (protected by mutex)
     * Caller must acquire file mutex before calling
     * @return File contents on success, null on failure
     */
    public static String readFile(String file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
            br.close();
            return content.toString();
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + file);
            return null;
        } catch (IOException e) {
            System.err.println("File read error: " + e.getMessage());
            return null;
        }
    }

    /**
     * System Call 5: Read data from process memory
     * Memory bounds are enforced by caller (via PCB)
     * @return Object at memory address, null if address is empty
     */
    public static Object readMemory(int address, Memory memory) {
        try {
            return memory.read(address);
        } catch (Exception e) {
            System.err.println("Memory read error at address " + address + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * System Call 6: Write data to process memory
     * Memory bounds are enforced by caller (via PCB)
     * @return SUCCESS on success, MEMORY_ACCESS_ERROR on failure
     */
    public static int writeMemory(int address, Object data, Memory memory) {
        try {
            memory.write(address, data);
            return SUCCESS;
        } catch (Exception e) {
            System.err.println("Memory write error at address " + address + ": " + e.getMessage());
            return MEMORY_ACCESS_ERROR;
        }
    }
}
