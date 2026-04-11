package os;

import java.util.*;

public class Parser {
    
    /**
     * Represents a parsed instruction with type and arguments
     */
    public static class Instruction {
        public String type;
        public List<String> args;
        public String rawLine;
        
        public Instruction(String type, List<String> args, String rawLine) {
            this.type = type;
            this.args = args;
            this.rawLine = rawLine;
        }
        
        @Override
        public String toString() {
            return type + " " + String.join(" ", args);
        }
    }
    
    /**
     * Tokenizes a line into tokens
     */
    public static String[] parse(String line) {
        return line.trim().split("\\s+");
    }
    
    /**
     * Parses a line into an Instruction object with type and arguments.
     * Supports: assign, print, printFromTo, readFile, writeFile, semWait, semSignal
     */
    public static Instruction parseInstruction(String line) throws Exception {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        
        String[] tokens = parse(trimmed);
        if (tokens.length == 0) {
            return null;
        }
        
        String command = tokens[0].toLowerCase();
        List<String> args = new ArrayList<>();
        for (int i = 1; i < tokens.length; i++) {
            args.add(tokens[i]);
        }
        
        // Validate instruction format
        switch (command) {
            case "assign":
                if (args.size() < 2) {
                    throw new Exception("assign requires 2 arguments: assign <variable> <value>");
                }
                break;
                
            case "print":
                if (args.size() < 1) {
                    throw new Exception("print requires 1 argument: print <variable>");
                }
                break;
                
            case "printfromto":
                if (args.size() < 2) {
                    throw new Exception("printFromTo requires 2 arguments: printFromTo <start> <end>");
                }
                break;
                
            case "readfile":
                if (args.size() < 1) {
                    throw new Exception("readFile requires 1 argument: readFile <filename>");
                }
                break;
                
            case "writefile":
                if (args.size() < 2) {
                    throw new Exception("writeFile requires 2 arguments: writeFile <filename> <data>");
                }
                break;
                
            case "semwait":
                if (args.size() < 1) {
                    throw new Exception("semWait requires 1 argument: semWait <resource>");
                }
                break;
                
            case "semsignal":
                if (args.size() < 1) {
                    throw new Exception("semSignal requires 1 argument: semSignal <resource>");
                }
                break;
                
            default:
                throw new Exception("Unknown instruction: " + command);
        }
        
        return new Instruction(command, args, trimmed);
    }
    
    /**
     * Checks if a value is a number
     */
    public static boolean isNumber(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Tries to parse an integer, returns the value or null if not numeric
     */
    public static Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
