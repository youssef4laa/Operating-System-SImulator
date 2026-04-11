package os;

public class PCB {
	
	    int processID;
	    String status; 
	    int programCounter;
	    int minBound;
	    int maxBound;
		public int arrivalTime;
		public int remainingTime;
		public int priority;

	    public PCB(int id, int min, int max) {
	        this.processID = id;
	        this.status = "Ready";
	        this.programCounter = min;
	        this.minBound = min;
	        this.maxBound = max;
	    }
	
}
