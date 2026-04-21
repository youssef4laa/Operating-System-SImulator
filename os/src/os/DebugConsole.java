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
    private JLabel titleLabel;
    private SimpleAttributeSet normalStyle;
    private SimpleAttributeSet errorStyle;
    private SimpleAttributeSet clockStyle;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private CustomPrintStream customOut;
    private CustomPrintStream customErr;
    
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MAX_LINES = 1000;
    private boolean darkModeEnabled = false;
    
    public DebugConsole() {
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        this.setBackground(new Color(245, 245, 245));
        
        // Title
        titleLabel = new JLabel("Debug Console");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(new Color(33, 33, 33));
        titleLabel.setBackground(new Color(238, 238, 238));
        titleLabel.setOpaque(true);
        titleLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 210, 210)),
            BorderFactory.createEmptyBorder(7, 8, 7, 8)
        ));
        this.add(titleLabel, BorderLayout.NORTH);
        
        // Console output with JTextPane for styling
        consoleOutput = new JTextPane();
        consoleOutput.setEditable(false);
        consoleOutput.setFont(new Font("Consolas", Font.PLAIN, 12));
        consoleOutput.setBackground(new Color(18, 18, 18));
        consoleOutput.setForeground(new Color(144, 238, 144));
        consoleOutput.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        consoleOutput.setText("System ready. Waiting for execution...\n");
        
        // Setup text styles
        normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, new Color(157, 255, 157));
        StyleConstants.setFontFamily(normalStyle, "Consolas");
        StyleConstants.setFontSize(normalStyle, 12);
        
        errorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errorStyle, new Color(255, 138, 128));
        StyleConstants.setFontFamily(errorStyle, "Consolas");
        StyleConstants.setFontSize(errorStyle, 12);

        clockStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(clockStyle, new Color(102, 178, 255));
        StyleConstants.setFontFamily(clockStyle, "Consolas");
        StyleConstants.setFontSize(clockStyle, 12);
        
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
                SimpleAttributeSet styleToUse;
                if (isError) {
                    styleToUse = errorStyle;
                } else if (message.contains("[Clock ") || message.contains("[CLOCK CYCLE]")) {
                    styleToUse = clockStyle;
                } else {
                    styleToUse = normalStyle;
                }

                doc.insertString(doc.getLength(), fullMessage, styleToUse);
                
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
     * Toggle dark mode visuals for the debug console.
     */
    public void setDarkMode(boolean enabled) {
        this.darkModeEnabled = enabled;

        if (enabled) {
            this.setBackground(new Color(13, 27, 42));
            this.setBorder(BorderFactory.createLineBorder(new Color(39, 59, 99), 1));
            titleLabel.setForeground(new Color(224, 236, 255));
            titleLabel.setBackground(new Color(22, 41, 74));
            titleLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(39, 59, 99)),
                BorderFactory.createEmptyBorder(7, 8, 7, 8)
            ));
            consoleOutput.setBackground(new Color(8, 20, 38));
            consoleOutput.setForeground(new Color(197, 221, 255));
            StyleConstants.setForeground(normalStyle, new Color(197, 221, 255));
            StyleConstants.setForeground(errorStyle, new Color(255, 145, 145));
            StyleConstants.setForeground(clockStyle, new Color(91, 167, 255));
        } else {
            this.setBackground(new Color(245, 245, 245));
            this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
            titleLabel.setForeground(new Color(33, 33, 33));
            titleLabel.setBackground(new Color(238, 238, 238));
            titleLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(7, 8, 7, 8)
            ));
            consoleOutput.setBackground(new Color(18, 18, 18));
            consoleOutput.setForeground(new Color(144, 238, 144));
            StyleConstants.setForeground(normalStyle, new Color(157, 255, 157));
            StyleConstants.setForeground(errorStyle, new Color(255, 138, 128));
            StyleConstants.setForeground(clockStyle, new Color(102, 178, 255));
        }

        consoleOutput.repaint();
        repaint();
    }

    public boolean isDarkModeEnabled() {
        return darkModeEnabled;
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
