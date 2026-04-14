package os;

import javax.swing.*;
import java.awt.*;

/**
 * CurrentProcessPanel - Displays information about currently executing process
 */
public class CurrentProcessPanel extends JPanel {
    
    private JLabel processIDLabel;
    private JLabel statusLabel;
    private JLabel instructionLabel;
    private JLabel pcLabel;
    private JLabel memoryBoundsLabel;
    private JLabel arrivalTimeLabel;
    private JLabel remainingTimeLabel;
    private JTextArea instructionDetailsArea;
    
    public CurrentProcessPanel() {
        this.setLayout(new BorderLayout(0, 8));
        this.setBackground(new Color(250, 250, 250));
        this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        
        initializeComponents();
    }
    
    private void initializeComponents() {
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setOpaque(false);
        
        // Process ID
        processIDLabel = new JLabel("Process ID: None");
        processIDLabel.setFont(new Font("Arial", Font.BOLD, 14));
        processIDLabel.setForeground(new Color(0, 102, 204));
        
        // Status
        statusLabel = new JLabel("Status: Idle");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Current Instruction
        instructionLabel = new JLabel("Current Instruction: --");
        instructionLabel.setFont(new Font("Arial", Font.BOLD, 11));
        
        // Program Counter
        pcLabel = new JLabel("Program Counter: --");
        pcLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        
        // Memory Bounds
        memoryBoundsLabel = new JLabel("Memory Bounds: --");
        memoryBoundsLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        
        // Arrival Time
        arrivalTimeLabel = new JLabel("Arrival Time: --");
        arrivalTimeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        
        // Remaining Time
        remainingTimeLabel = new JLabel("Remaining Time: --");
        remainingTimeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        
        // Instruction Details
        JLabel detailsTitle = new JLabel("Instruction Details:");
        detailsTitle.setFont(new Font("Arial", Font.BOLD, 11));
        
        instructionDetailsArea = new JTextArea();
        instructionDetailsArea.setEditable(false);
        instructionDetailsArea.setLineWrap(true);
        instructionDetailsArea.setWrapStyleWord(true);
        instructionDetailsArea.setFont(new Font("Courier New", Font.PLAIN, 10));
        instructionDetailsArea.setBackground(Color.WHITE);
        instructionDetailsArea.setText("No process executing...");
        JScrollPane scrollPane = new JScrollPane(instructionDetailsArea);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        
        innerPanel.add(processIDLabel);
        innerPanel.add(Box.createVerticalStrut(3));
        innerPanel.add(statusLabel);
        innerPanel.add(Box.createVerticalStrut(5));
        innerPanel.add(new JSeparator());
        innerPanel.add(Box.createVerticalStrut(3));
        innerPanel.add(instructionLabel);
        innerPanel.add(pcLabel);
        innerPanel.add(memoryBoundsLabel);
        innerPanel.add(arrivalTimeLabel);
        innerPanel.add(remainingTimeLabel);
        innerPanel.add(Box.createVerticalStrut(5));
        innerPanel.add(new JSeparator());
        innerPanel.add(Box.createVerticalStrut(3));
        innerPanel.add(detailsTitle);
        innerPanel.add(Box.createVerticalStrut(3));
        innerPanel.add(scrollPane);
        
        JScrollPane mainScroll = new JScrollPane(innerPanel);
        mainScroll.getVerticalScrollBar().setUnitIncrement(10);
        this.add(mainScroll, BorderLayout.CENTER);
    }
    
    /**
     * Update current process display
     */
    public void update(Scheduler scheduler) {
        if (scheduler == null || scheduler.readyQueue.isEmpty()) {
            processIDLabel.setText("Process ID: None");
            statusLabel.setText("Status: Idle");
            instructionLabel.setText("Current Instruction: --");
            pcLabel.setText("Program Counter: --");
            memoryBoundsLabel.setText("Memory Bounds: --");
            arrivalTimeLabel.setText("Arrival Time: --");
            remainingTimeLabel.setText("Remaining Time: --");
            instructionDetailsArea.setText("No process executing...");
            return;
        }
        
        // Try to get current process from ready queue (first one would be executing)
        if (!scheduler.readyQueue.isEmpty()) {
            PCB pcb = scheduler.readyQueue.peek();
            if (pcb != null) {
                processIDLabel.setText("Process ID: P" + pcb.processID);
                statusLabel.setText("Status: " + pcb.status);
                
                // Current instruction
                String currentInst = "None";
                if (pcb.instructionPointer < pcb.instructionList.size()) {
                    currentInst = pcb.instructionList.get(pcb.instructionPointer);
                }
                instructionLabel.setText("Current Instruction: " + currentInst);
                
                pcLabel.setText("Program Counter: " + pcb.programCounter + " / " + pcb.instructionList.size());
                memoryBoundsLabel.setText("Memory Bounds: [" + pcb.minBound + " - " + pcb.maxBound + "]");
                arrivalTimeLabel.setText("Arrival Time: " + pcb.arrivalTime);
                remainingTimeLabel.setText("Remaining Time: " + pcb.remainingTime);
                
                // Instruction details
                StringBuilder details = new StringBuilder();
                details.append("Total Instructions: ").append(pcb.instructionList.size()).append("\n");
                details.append("Allocation Size: ").append(pcb.allocationSize).append(" words\n");
                details.append("Variables: ").append(pcb.variableCount).append(" / ").append(PCB.MAX_VARIABLES_PER_PROCESS).append("\n\n");
                details.append("Symbol Table:\n");
                pcb.symbolTable.forEach((name, address) ->
                    details.append("  ").append(name).append(" @ [").append(address).append("]\n")
                );
                
                instructionDetailsArea.setText(details.toString());
            }
        }
    }
}
