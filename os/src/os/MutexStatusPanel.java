package os;

import javax.swing.*;
import java.awt.*;

/**
 * MutexStatusPanel - Displays status of 3 system mutexes
 * Shows: locked/free, owner, wait queue size
 */
public class MutexStatusPanel extends JPanel {
    
    private JPanel mutexContainer;
    
    public MutexStatusPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        this.setBackground(new Color(250, 250, 250));
        
        mutexContainer = new JPanel();
        mutexContainer.setLayout(new BoxLayout(mutexContainer, BoxLayout.Y_AXIS));
        mutexContainer.setOpaque(false);
        
        // Add three mutex status displays
        mutexContainer.add(createMutexDisplay("File Access"));
        mutexContainer.add(Box.createVerticalStrut(5));
        mutexContainer.add(createMutexDisplay("User Input"));
        mutexContainer.add(Box.createVerticalStrut(5));
        mutexContainer.add(createMutexDisplay("User Output"));
        mutexContainer.add(Box.createVerticalGlue());
        
        JScrollPane scrollPane = new JScrollPane(mutexContainer);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        this.add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Create visual display for a single mutex
     */
    private JPanel createMutexDisplay(String resourceName) {
        JPanel mutexBox = new JPanel();
        mutexBox.setLayout(new BoxLayout(mutexBox, BoxLayout.Y_AXIS));
        mutexBox.setBackground(Color.WHITE);
        mutexBox.setBorder(BorderFactory.createLineBorder(new Color(153, 153, 153), 1));
        
        mutexBox.setBorder(BorderFactory.createTitledBorder(resourceName));
        
        // Status (locked/free)
        JLabel statusLabel = new JLabel("● Free");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(0, 153, 0));
        statusLabel.setName("status_" + resourceName);
        
        // Owner
        JLabel ownerLabel = new JLabel("Owner: None");
        ownerLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        ownerLabel.setName("owner_" + resourceName);
        
        // Wait queue
        JLabel waitLabel = new JLabel("Waiting: 0 processes");
        waitLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        waitLabel.setName("wait_" + resourceName);
        
        mutexBox.add(statusLabel);
        mutexBox.add(ownerLabel);
        mutexBox.add(waitLabel);
        return mutexBox;
    }
    
    /**
     * Update mutex status display
     */
    public void update() {
        // This would be called to update mutex states
        // For now, just placeholder implementation
        // In future, integrate with actual MutexManager
    }
}
