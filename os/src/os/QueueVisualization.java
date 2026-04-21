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
    private boolean mlfqReadyMode;
    private LinkedList<PCB> q0;
    private LinkedList<PCB> q1;
    private LinkedList<PCB> q2;
    private LinkedList<PCB> q3;
    
    public QueueVisualization(String name, LinkedList<PCB> queue) {
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        this.setBackground(new Color(250, 250, 250));
        this.queueName = name;
        this.queue = queue;
        this.mlfqReadyMode = false;
        this.q0 = new LinkedList<>();
        this.q1 = new LinkedList<>();
        this.q2 = new LinkedList<>();
        this.q3 = new LinkedList<>();
        
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
        scrollPane.setPreferredSize(new Dimension(0, 210));
        this.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Configure MLFQ lane mode for the ready queue visualization.
     */
    public void setMLFQMode(boolean enabled, LinkedList<PCB> q0, LinkedList<PCB> q1, LinkedList<PCB> q2, LinkedList<PCB> q3) {
        this.mlfqReadyMode = enabled && "Ready".equalsIgnoreCase(queueName);
        this.q0 = q0 != null ? q0 : new LinkedList<>();
        this.q1 = q1 != null ? q1 : new LinkedList<>();
        this.q2 = q2 != null ? q2 : new LinkedList<>();
        this.q3 = q3 != null ? q3 : new LinkedList<>();
    }
    
    /**
     * Update queue visualization
     */
    public void update(LinkedList<PCB> queue, String algorithm) {
        processBox.removeAll();

        if (mlfqReadyMode && "MLFQ".equalsIgnoreCase(algorithm)) {
            renderMLFQLanes();
            processBox.revalidate();
            processBox.repaint();
            return;
        }

        processBox.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        
        if (queue.isEmpty()) {
            JLabel emptyLabel = new JLabel("[Empty]");
            emptyLabel.setForeground(new Color(153, 153, 153));
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            processBox.add(emptyLabel);
            queueStatusLabel.setText(queueName + " Queue (0 processes)");
        } else {
            for (PCB pcb : queue) {
                // Create visual representation of process
                JPanel processCard = createProcessCard(pcb, algorithm, true);
                processBox.add(processCard);
            }
            queueStatusLabel.setText(queueName + " Queue (" + queue.size() + " processes)");
        }
        processBox.revalidate();
        processBox.repaint();
    }

    private void renderMLFQLanes() {
        processBox.setLayout(new BoxLayout(processBox, BoxLayout.Y_AXIS));
        processBox.add(createQueueLane("Q0 - Highest", q0, new Color(225, 240, 255), new Color(0, 102, 204)));
        processBox.add(Box.createVerticalStrut(6));
        processBox.add(createQueueLane("Q1", q1, new Color(255, 248, 220), new Color(255, 153, 0)));
        processBox.add(Box.createVerticalStrut(6));
        processBox.add(createQueueLane("Q2", q2, new Color(255, 239, 229), new Color(230, 95, 32)));
        processBox.add(Box.createVerticalStrut(6));
        processBox.add(createQueueLane("Q3", q3, new Color(240, 240, 240), new Color(120, 120, 120)));

        int total = q0.size() + q1.size() + q2.size() + q3.size();
        queueStatusLabel.setText(queueName + " Queue (" + total + " processes across Q0-Q3)");
    }

    private JPanel createQueueLane(String laneName, LinkedList<PCB> queueData, Color laneBg, Color laneBorder) {
        JPanel lanePanel = new JPanel(new BorderLayout(4, 4));
        lanePanel.setBackground(laneBg);
        lanePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(laneBorder, 2),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        JLabel laneHeader = new JLabel(laneName + " (" + queueData.size() + ")");
        laneHeader.setFont(new Font("Arial", Font.BOLD, 11));
        laneHeader.setForeground(darken(laneBorder));
        lanePanel.add(laneHeader, BorderLayout.NORTH);

        JPanel laneCards = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        laneCards.setOpaque(false);
        if (queueData.isEmpty()) {
            JLabel emptyLabel = new JLabel("[Empty]");
            emptyLabel.setForeground(new Color(120, 120, 120));
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 10));
            laneCards.add(emptyLabel);
        } else {
            for (PCB pcb : queueData) {
                laneCards.add(createProcessCard(pcb, "MLFQ", false));
            }
        }

        lanePanel.add(laneCards, BorderLayout.CENTER);
        return lanePanel;
    }

    private Color darken(Color color) {
        return new Color(
            Math.max(0, color.getRed() - 50),
            Math.max(0, color.getGreen() - 50),
            Math.max(0, color.getBlue() - 50)
        );
    }
    
    /**
     * Create visual card for a process
     */
    private JPanel createProcessCard(PCB pcb, String algorithm, boolean showQueueLevel) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createLineBorder(new Color(0, 102, 204), 2));
        card.setBackground(new Color(230, 242, 255));
        
        // Adjust height if we need to show the MLFQ queue level
        boolean showLevel = showQueueLevel && "MLFQ".equalsIgnoreCase(algorithm) && "Ready".equals(this.queueName);
        int targetHeight = showLevel ? 85 : 66;
        
        card.setPreferredSize(new Dimension(78, targetHeight));
        card.setMaximumSize(new Dimension(78, targetHeight));
        
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
