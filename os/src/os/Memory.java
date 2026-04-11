package os;

public class Memory {
	
	    Object[] mem = new Object[40];

	    public void write(int index, Object data) {
	        mem[index] = data;
	    }

	    public Object read(int index) {
	        return mem[index];
	    }
	
}
