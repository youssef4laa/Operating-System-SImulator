package os;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;

/**
 * QueueVisualization - Displays process queues (Ready, Blocked)
 * Shows process IDs in queue order with color coding
 */
public class QueueVisualization extends JPanel {
    
    private String queueName;
    private LinkedList<PCB> queue;
    private JPanel processBox;
    private JLabel queueStatusLabel;
    
    public QueueVisualization(String name, LinkedList<PCB> queue) {
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        this.setBackground(new Color(250, 250, 250));
        this.queueName = name;
        this.queue = queue;
        
        // Status label
        queueStatusLabel = new JLabel(queueName + " Queue (0 processes)");
        queueStatusLabel.setFont(new Font("Arial", Font.BOLD, 11));
        this.add(queueStatusLabel, BorderLayout.NORTH);
        
        // Process display box
        processBox = new JPanel();
        processBox.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        processBox.setBorder(BorderFactory.createLineBorder(new Color(238, 238, 238), 1));
        processBox.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(processBox);
        scrollPane.setPreferredSize(new Dimension(0, 100)); // Increased from 70 to accommodate MLFQ queue levels
        this.add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Update queue visualization
     */
    public void update(LinkedList<PCB> queue, String algorithm) {
        processBox.removeAll();
        
        if (queue.isEmpty()) {
            JLabel emptyLabel = new JLabel("[Empty]");
            emptyLabel.setForeground(new Color(153, 153, 153));
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            processBox.add(emptyLabel);
            queueStatusLabel.setText(queueName + " Queue (0 processes)");
        } else {
            for (PCB pcb : queue) {
                // Create visual representation of process
                JPanel processCard = createProcessCard(pcb, algorithm);
                processBox.add(processCard);
            }
            queueStatusLabel.setText(queueName + " Queue (" + queue.size() + " processes)");
        }
        processBox.revalidate();
        processBox.repaint();
    }
    
    /**
     * Create visual card for a process
     */
    private JPanel createProcessCard(PCB pcb, String algorithm) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createLineBorder(new Color(0, 102, 204), 2));
        card.setBackground(new Color(230, 242, 255));
        
        // Adjust height if we need to show the MLFQ queue level
        boolean showLevel = "MLFQ".equalsIgnoreCase(algorithm) && "Ready".equals(this.queueName);
        int targetHeight = showLevel ? 85 : 70;
        
        card.setPreferredSize(new Dimension(80, targetHeight));
        card.setMaximumSize(new Dimension(80, targetHeight));
        
        // Process ID
        JLabel pidLabel = new JLabel("P" + pcb.processID);
        pidLabel.setFont(new Font("Arial", Font.BOLD, 12));
        pidLabel.setAlignmentX(CENTER_ALIGNMENT);
        
        // Status
        JLabel statusLabel = new JLabel(pcb.status);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        statusLabel.setForeground(new Color(0, 102, 204));
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        
        // PC
        JLabel pcLabel = new JLabel("PC:" + pcb.programCounter);
        pcLabel.setFont(new Font("Arial", Font.PLAIN, 8));
        pcLabel.setForeground(new Color(102, 102, 102));
        pcLabel.setAlignmentX(CENTER_ALIGNMENT);
        
        card.add(pidLabel);
        card.add(statusLabel);
        card.add(pcLabel);
        
        // Level
        if (showLevel) {
            JLabel levelLabel = new JLabel("Queue: Q" + pcb.currentQueueLevel);
            levelLabel.setFont(new Font("Arial", Font.BOLD, 9));
            levelLabel.setForeground(new Color(204, 102, 0));
            levelLabel.setAlignmentX(CENTER_ALIGNMENT);
            card.add(levelLabel);
        }
        
        return card;
    }
}
