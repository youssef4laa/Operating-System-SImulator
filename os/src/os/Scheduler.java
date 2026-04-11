package os;
import java.util.LinkedList;


public class Scheduler {
	
	    LinkedList<PCB> readyQueue = new LinkedList<>();
	    LinkedList<PCB> blockedQueue = new LinkedList<>();

	    
	    LinkedList<PCB> q0 = new LinkedList<>();
	    LinkedList<PCB> q1 = new LinkedList<>();
	    LinkedList<PCB> q2 = new LinkedList<>();
	    LinkedList<PCB> q3 = new LinkedList<>();

	    int time = 0;

	    
	    String algorithm;

	    public void start(Memory memory) throws Exception {

	        while (true) {

	          
	            checkArrivals(time, memory);

	            PCB current = null;

	           
	            switch (algorithm) {

	                case "RR":
	                    current = readyQueue.poll();
	                    if (current != null)
	                        RR(current, memory);
	                    break;

	                case "HRRN":
	                    current = HRRN();
	                    if (current != null)
	                        FCFS(current, memory);
	                    break;

	                case "MLFQ":
	                    current = MLFQ();
	                    if (current != null)
	                        runMLFQ(current, memory);
	                    break;
	            }

	            time++;
	        }
	    }

	    
	    public void RR(PCB pcb, Memory memory) throws Exception {

	        int quantum = 2;
	        pcb.status = "Running";

	        int counter = 0;

	        while (counter < quantum) {

	            Interpreter.execute(pcb, memory);
	            counter++;

	            if (pcb.programCounter > pcb.maxBound)
	                break;

	            if (pcb.status.equals("Blocked"))
	                break;
	        }

	        moveProcess(pcb);
	    }

	    
	    public PCB HRRN() {

	        PCB best = null;
	        double maxRatio = -1;

	        for (int i = 0; i < readyQueue.size(); i++) {

	            PCB p = readyQueue.get(i);

	            int waiting = time - p.arrivalTime;
	            int burst = p.remainingTime;

	            double ratio = (waiting + burst) / (double) burst;

	            if (ratio > maxRatio) {
	                maxRatio = ratio;
	                best = p;
	            }
	        }
	        readyQueue.remove(best);
	        return best;
	    }

	    public void FCFS(PCB pcb, Memory memory) throws Exception {

	        pcb.status = "Running";

	        while (pcb.programCounter <= pcb.maxBound) {

	            Interpreter.execute(pcb, memory);

	            if (pcb.status.equals("Blocked"))
	                break;
	        }

	        moveProcess(pcb);
	    }

	    
	    public PCB MLFQ() {

	        if (!q0.isEmpty()) return q0.poll();
	        if (!q1.isEmpty()) return q1.poll();
	        if (!q2.isEmpty()) return q2.poll();
	        if (!q3.isEmpty()) return q3.poll();

	        return null;
	    }

	    public void runMLFQ(PCB pcb, Memory memory) throws Exception {

	        pcb.status = "Running";

	        int level = pcb.priority; 
	        int quantum = (int) Math.pow(2, level);

	        int counter = 0;

	        while (counter < quantum) {

	            Interpreter.execute(pcb, memory);
	            counter++;

	            if (pcb.programCounter > pcb.maxBound)
	                break;

	            if (pcb.status.equals("Blocked"))
	                break;
	        }

	       
	        if (counter == quantum && pcb.priority < 3)
	            pcb.priority++;

	        moveToMLFQ(pcb);
	    }

	 

	    public void moveProcess(PCB pcb) {

	        if (pcb.programCounter > pcb.maxBound) {
	            pcb.status = "Finished";
	        } else if (pcb.status.equals("Blocked")) {
	            blockedQueue.add(pcb);
	        } else {
	            pcb.status = "Ready";
	            readyQueue.add(pcb);
	        }
	    }

	    public void moveToMLFQ(PCB pcb) {

	        if (pcb.programCounter > pcb.maxBound) {
	            pcb.status = "Finished";
	            return;
	        }

	        if (pcb.status.equals("Blocked")) {
	            blockedQueue.add(pcb);
	            return;
	        }

	        pcb.status = "Ready";

	        switch (pcb.priority) {
	            case 0: q0.add(pcb); break;
	            case 1: q1.add(pcb); break;
	            case 2: q2.add(pcb); break;
	            case 3: q3.add(pcb); break;
	        }
	    }

	
	    public void checkArrivals(int time, Memory memory) {

	        if (time == 0) {
	            PCB p1 = Process.createProcess("Program1.txt", memory);
	            p1.arrivalTime = time;
	            readyQueue.add(p1);
	            q0.add(p1); 
	        }

	        if (time == 1) {
	            PCB p2 = Process.createProcess("Program2.txt", memory);
	            p2.arrivalTime = time;
	            readyQueue.add(p2);
	            q0.add(p2);
	        }

	        if (time == 4) {
	            PCB p3 = Process.createProcess("Program3.txt", memory);
	            p3.arrivalTime = time;
	            readyQueue.add(p3);
	            q0.add(p3);
	        }
	    }
	}

