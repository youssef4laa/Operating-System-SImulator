package os;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;



public class MemoryManager {
	
	    public static void swapOut(PCB pcb, Memory memory) throws Exception {
	        FileWriter fw = new FileWriter("Disk_" + pcb.processID + ".txt");

	        for (int i = pcb.minBound; i <= pcb.maxBound; i++) {
	            fw.write(memory.read(i) + "\n");
	            memory.write(i, null);
	        }

	        fw.close();
	    }


	    public static void swapIn(PCB pcb, Memory memory, int start) throws Exception {
	        BufferedReader br = new BufferedReader(new FileReader("Disk_" + pcb.processID + ".txt"));

	        String line;
	        int i = start;

	        while ((line = br.readLine()) != null) {
	            memory.write(i++, line);
	        }

	        pcb.minBound = start;
	        pcb.maxBound = i - 1;

	        br.close();
	    }
}
