package os;

public class Memory {
    private final Object[] mem = new Object[40];

    public void write(int index, Object data) throws Exception {
        if (index < 0 || index >= mem.length) {
            throw new Exception("Memory Access Violation: Index " + index + " is out of physical bounds.");
        }
        mem[index] = data;
    }

    public Object read(int index) throws Exception {
        if (index < 0 || index >= mem.length) {
            throw new Exception("Memory Access Violation: Index " + index + " is out of physical bounds.");
        }
        return mem[index];
    }

    public void displayMemory() {
        System.out.println("--- Current Memory State ---");
        for (int i = 0; i < mem.length; i++) {
            System.out.printf("Word [%d]: %s%n", i, (mem[i] == null ? "Empty" : mem[i].toString()));
        }
        System.out.println("----------------------------");
    }

    public int getSize() {
        return mem.length; 
    }
}
