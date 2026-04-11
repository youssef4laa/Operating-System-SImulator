package os;

	import java.io.BufferedReader;
	import java.io.FileReader;

public class Process {
	

	    static int nextID = 1; 

	    public static PCB createProcess(String fileName, Memory memory) {

	        try {
	            BufferedReader br = new BufferedReader(new FileReader(fileName));

	            
	            int start = findFreeSpace(memory, 15); 
	            int current = start;

	           
	            PCB pcb = new PCB(nextID++, start, start + 14);

	       
	            memory.write(current++, pcb);

	            String line;
	            while ((line = br.readLine()) != null) {
	                memory.write(current++, line);
	            }

	         
	            memory.write(current++, "var1");
	            memory.write(current++, "var2");
	            memory.write(current++, "var3");

	            br.close();

	            pcb.remainingTime = current - start;

	            return pcb;

	        } catch (Exception e) {
	            e.printStackTrace();
	            return null;
	        }
	    }

	    
	    public static int findFreeSpace(Memory memory, int size) {

	        int count = 0;

	        for (int i = 0; i < 40; i++) {

	            if (memory.read(i) == null) {
	                count++;
	            } else {
	                count = 0;
	            }

	            if (count == size) {
	                return i - size + 1;
	            }
	        }

	       
	        throw new RuntimeException("No space in memory (need swapping)");
	    }
	}

