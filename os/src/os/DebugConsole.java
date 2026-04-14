package os;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * DebugConsole - Captures and displays system output in real-time
 * Shows: Log messages, system calls, process state changes, errors
 * Integrates PrintStream redirection for live monitoring
 */
public class DebugConsole extends JPanel {
    
    private JTextPane consoleOutput;
    private SimpleAttributeSet normalStyle;
    private SimpleAttributeSet errorStyle;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private CustomPrintStream customOut;
    private CustomPrintStream customErr;
    
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MAX_LINES = 1000;
    
    public DebugConsole() {
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        
        // Title
        JLabel titleLabel = new JLabel("Debug Console");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setBackground(new Color(250, 250, 250));
        titleLabel.setOpaque(true);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.add(titleLabel, BorderLayout.NORTH);
        
        // Console output with JTextPane for styling
        consoleOutput = new JTextPane();
        consoleOutput.setEditable(false);
        consoleOutput.setFont(new Font("Courier New", Font.PLAIN, 9));
        consoleOutput.setBackground(new Color(30, 30, 30));
        consoleOutput.setText("System ready. Waiting for execution...\n");
        
        // Setup text styles
        normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, new Color(0, 255, 0));
        
        errorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errorStyle, new Color(255, 100, 100));
        
        // Style the initial text
        StyledDocument doc = consoleOutput.getStyledDocument();
        doc.setCharacterAttributes(0, doc.getLength(), normalStyle, false);
        
        JScrollPane scrollPane = new JScrollPane(consoleOutput);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        this.add(scrollPane, BorderLayout.CENTER);
        
        // Redirect stdout and stderr
        this.originalOut = System.out;
        this.originalErr = System.err;
        this.customOut = new CustomPrintStream(System.out, false);
        this.customErr = new CustomPrintStream(System.err, true);
        
        System.setOut(customOut);
        System.setErr(customErr);
    }
    
    /**
     * Append message to console
     */
    public void log(String message) {
        log(message, false);
    }
    
    /**
     * Append message to console with error flag
     */
    public void log(String message, boolean isError) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timestamp = LocalTime.now().format(timeFormatter);
                String prefix = isError ? "[ERROR] " : "[" + timestamp + "] ";
                String fullMessage = prefix + message + "\n";
                
                StyledDocument doc = consoleOutput.getStyledDocument();
                doc.insertString(doc.getLength(), fullMessage, isError ? errorStyle : normalStyle);
                
                // Limit console size
                int lineCount = consoleOutput.getText().split("\n").length;
                if (lineCount > MAX_LINES) {
                    String text = consoleOutput.getText();
                    int firstNewline = text.indexOf('\n');
                    if (firstNewline != -1) {
                        doc.remove(0, firstNewline + 1);
                    }
                }
                
                // Auto-scroll to bottom
                consoleOutput.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Clear console output
     */
    public void clear() {
        SwingUtilities.invokeLater(() -> consoleOutput.setText(""));
    }
    
    /**
     * Restore original print streams
     */
    public void restore() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Inner class to capture print stream output
     */
    private class CustomPrintStream extends PrintStream {
        private boolean isError;
        
        public CustomPrintStream(PrintStream original, boolean isError) {
            super(original, true);
            this.isError = isError;
        }
        
        @Override
        public void println(String x) {
            super.println(x);
            log(x, isError);
        }
        
        @Override
        public void print(String x) {
            super.print(x);
            if (x != null && !x.isEmpty()) {
                log(x, isError);
            }
        }
        
        @Override
        public void println() {
            super.println();
        }
    }
}
