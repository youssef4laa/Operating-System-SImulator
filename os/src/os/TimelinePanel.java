package os;

import javax.swing.*;
import java.awt.*;

/**
 * TimelinePanel - Displays simulation timeline and statistics
 * Shows: Clock cycle, instructions executed, memory usage, process count
 */
public class TimelinePanel extends JPanel {
    
    private JLabel clockCycleLabel;
    private JLabel instructionsLabel;
    private JLabel memoryUsageLabel;
    private JLabel readyCountLabel;
    private JLabel blockedCountLabel;
    private JLabel finishedCountLabel;
    private JTextArea timelineLog;
    private JPanel clockHighlightPanel;
    private JLabel clockTitleLabel;
    private JPanel statsGrid;
    private JScrollPane logScrollPane;
    private JLabel titleLabel;
    private JLabel logLabel;
    
    public TimelinePanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        this.setBackground(new Color(250, 250, 250));
        
        initializeComponents();
    }
    
    private void initializeComponents() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        // Title
        titleLabel = new JLabel("Execution Timeline");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 13));
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(5));

        // Clock cycle highlight (kept separate so the most important metric is prominent)
        clockHighlightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        clockHighlightPanel.setBackground(new Color(236, 245, 255));
        clockHighlightPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204), 2),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));

        clockTitleLabel = new JLabel("Clock Cycle");
        clockTitleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        clockTitleLabel.setForeground(new Color(0, 71, 171));
        clockCycleLabel = new JLabel("0");
        clockCycleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        clockCycleLabel.setForeground(new Color(0, 102, 204));

        clockHighlightPanel.add(clockTitleLabel);
        clockHighlightPanel.add(clockCycleLabel);
        contentPanel.add(clockHighlightPanel);
        contentPanel.add(Box.createVerticalStrut(8));
        
        // Stats grid
        statsGrid = new JPanel();
        statsGrid.setLayout(new GridLayout(2, 5, 16, 10));
        statsGrid.setBackground(Color.WHITE);
        statsGrid.setBorder(BorderFactory.createLineBorder(new Color(238, 238, 238), 1));
        
        // Instructions executed
        JLabel instLabel = new JLabel("Instructions:");
        instLabel.setFont(new Font("Arial", Font.BOLD, 10));
        instructionsLabel = new JLabel("0");
        instructionsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        instructionsLabel.setForeground(new Color(0, 153, 0));
        statsGrid.add(instLabel);
        statsGrid.add(instructionsLabel);
        
        // Memory usage
        JLabel memLabel = new JLabel("Memory:");
        memLabel.setFont(new Font("Arial", Font.BOLD, 10));
        memoryUsageLabel = new JLabel("0/40 words");
        memoryUsageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        memoryUsageLabel.setForeground(new Color(255, 102, 0));
        statsGrid.add(memLabel);
        statsGrid.add(memoryUsageLabel);
        
        // Process counts - Row 2
        JLabel readyLabel = new JLabel("Ready:");
        readyLabel.setFont(new Font("Arial", Font.BOLD, 10));
        readyCountLabel = new JLabel("0");
        readyCountLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statsGrid.add(readyLabel);
        statsGrid.add(readyCountLabel);
        
        JLabel blockedLabel = new JLabel("Blocked:");
        blockedLabel.setFont(new Font("Arial", Font.BOLD, 10));
        blockedCountLabel = new JLabel("0");
        blockedCountLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statsGrid.add(blockedLabel);
        statsGrid.add(blockedCountLabel);
        
        JLabel finishedLabel = new JLabel("Finished:");
        finishedLabel.setFont(new Font("Arial", Font.BOLD, 10));
        finishedCountLabel = new JLabel("0");
        finishedCountLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statsGrid.add(finishedLabel);
        statsGrid.add(finishedCountLabel);
        
        contentPanel.add(statsGrid);
        contentPanel.add(Box.createVerticalStrut(8));
        
        // Timeline log
        logLabel = new JLabel("Timeline Log:");
        logLabel.setFont(new Font("Arial", Font.BOLD, 10));
        contentPanel.add(logLabel);
        contentPanel.add(Box.createVerticalStrut(3));
        
        timelineLog = new JTextArea();
        timelineLog.setEditable(false);
        timelineLog.setLineWrap(true);
        timelineLog.setWrapStyleWord(true);
        timelineLog.setFont(new Font("Courier New", Font.PLAIN, 9));
        timelineLog.setBackground(Color.WHITE);
        logScrollPane = new JScrollPane(timelineLog);
        logScrollPane.setPreferredSize(new Dimension(0, 120));
        contentPanel.add(logScrollPane);
        
        this.add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Update timeline display
     */
    public void update(Scheduler scheduler, Memory memory, int clockCycle, int instructionsExecuted) {
        clockCycleLabel.setText(String.valueOf(clockCycle));
        instructionsLabel.setText(String.valueOf(instructionsExecuted));
        
        int memoryUsed = memory.getUsedWords();
        memoryUsageLabel.setText(memoryUsed + "/40 words");
        
        readyCountLabel.setText(String.valueOf(scheduler.readyQueue.size()));
        blockedCountLabel.setText(String.valueOf(scheduler.blockedQueue.size()));
        finishedCountLabel.setText(String.valueOf(scheduler.finishedQueue.size()));
    }
    
    /**
     * Add event to timeline log
     */
    public void logEvent(String event) {
        timelineLog.append("[" + clockCycleLabel.getText() + "] " + event + "\n");
        
        // Limit log size
        String text = timelineLog.getText();
        int lineCount = text.split("\n").length;
        if (lineCount > 20) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                timelineLog.setText(text.substring(firstNewline + 1));
            }
        }
    }
    
    /**
     * Clear timeline log
     */
    public void clearLog() {
        timelineLog.setText("");
    }

    /**
     * Apply dark mode colors while preserving a blue clock emphasis.
     */
    public void setDarkMode(boolean enabled) {
        if (enabled) {
            this.setBackground(new Color(16, 34, 58));
            this.setBorder(BorderFactory.createLineBorder(new Color(39, 59, 99), 1));
            titleLabel.setForeground(new Color(224, 236, 255));
            logLabel.setForeground(new Color(191, 213, 244));
            clockHighlightPanel.setBackground(new Color(20, 52, 92));
            clockHighlightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(91, 167, 255), 2),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
            clockTitleLabel.setForeground(new Color(155, 205, 255));
            clockCycleLabel.setForeground(new Color(91, 167, 255));
            statsGrid.setBackground(new Color(21, 44, 78));
            timelineLog.setBackground(new Color(8, 20, 38));
            timelineLog.setForeground(new Color(205, 225, 255));
            logScrollPane.getViewport().setBackground(new Color(8, 20, 38));
        } else {
            this.setBackground(new Color(250, 250, 250));
            this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
            titleLabel.setForeground(Color.BLACK);
            logLabel.setForeground(Color.BLACK);
            clockHighlightPanel.setBackground(new Color(236, 245, 255));
            clockHighlightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 102, 204), 2),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
            clockTitleLabel.setForeground(new Color(0, 71, 171));
            clockCycleLabel.setForeground(new Color(0, 102, 204));
            statsGrid.setBackground(Color.WHITE);
            timelineLog.setBackground(Color.WHITE);
            timelineLog.setForeground(Color.BLACK);
            logScrollPane.getViewport().setBackground(Color.WHITE);
        }

        repaint();
    }
}
