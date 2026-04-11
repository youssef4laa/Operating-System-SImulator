package os;
import java.util.Scanner;
import java.io.*;


public class SystemCall {
	
	    static Scanner scanner = new Scanner(System.in);

	    public static void print(String x) {
	        System.out.println(x);
	    }

	    public static String input() {
	        return scanner.nextLine();
	    }

	    public static void writeFile(String file, String data) throws Exception {
	        FileWriter fw = new FileWriter(file);
	        fw.write(data);
	        fw.close();
	    }

	    public static String readFile(String file) throws Exception {
	        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
				return br.readLine();
			}
	    }
	
}
