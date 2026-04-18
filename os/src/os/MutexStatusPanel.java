package os;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * MutexStatusPanel - Displays status of 3 system mutexes
 * Shows: locked/free, owner, wait queue size
 */
public class MutexStatusPanel extends JPanel {
    
    private JPanel mutexContainer;
    private MutexManager mutexManager;
    private final Map<String, JLabel> statusLabels = new HashMap<>();
    private final Map<String, JLabel> ownerLabels = new HashMap<>();
    private final Map<String, JLabel> waitLabels = new HashMap<>();
    
    public MutexStatusPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        this.setBackground(new Color(250, 250, 250));
        
        mutexContainer = new JPanel();
        mutexContainer.setLayout(new BoxLayout(mutexContainer, BoxLayout.Y_AXIS));
        mutexContainer.setOpaque(false);
        
        // Add three mutex status displays
        mutexContainer.add(createMutexDisplay("file", "File Access"));
        mutexContainer.add(Box.createVerticalStrut(5));
        mutexContainer.add(createMutexDisplay("userinput", "User Input"));
        mutexContainer.add(Box.createVerticalStrut(5));
        mutexContainer.add(createMutexDisplay("useroutput", "User Output"));
        mutexContainer.add(Box.createVerticalGlue());
        
        JScrollPane scrollPane = new JScrollPane(mutexContainer);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        this.add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Create visual display for a single mutex
     */
    private JPanel createMutexDisplay(String resourceKey, String resourceName) {
        JPanel mutexBox = new JPanel();
        mutexBox.setLayout(new BoxLayout(mutexBox, BoxLayout.Y_AXIS));
        mutexBox.setBackground(Color.WHITE);
        mutexBox.setBorder(BorderFactory.createLineBorder(new Color(153, 153, 153), 1));
        
        mutexBox.setBorder(BorderFactory.createTitledBorder(resourceName));
        
        // Status (locked/free)
        JLabel statusLabel = new JLabel("● Free");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(0, 153, 0));
        statusLabels.put(resourceKey, statusLabel);
        
        // Owner
        JLabel ownerLabel = new JLabel("Owner: None");
        ownerLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        ownerLabels.put(resourceKey, ownerLabel);
        
        // Wait queue
        JLabel waitLabel = new JLabel("Waiting: 0 processes");
        waitLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        waitLabels.put(resourceKey, waitLabel);
        
        mutexBox.add(statusLabel);
        mutexBox.add(ownerLabel);
        mutexBox.add(waitLabel);
        return mutexBox;
    }
    
    /**
     * Update mutex status display
     */
    public void update() {
        if (mutexManager == null) {
            return;
        }

        refreshResource("file");
        refreshResource("userinput");
        refreshResource("useroutput");
    }

    public void setMutexManager(MutexManager mutexManager) {
        this.mutexManager = mutexManager;
    }

    private void refreshResource(String resourceKey) {
        Mutex mutex = mutexManager.getMutex(resourceKey);
        if (mutex == null) {
            return;
        }

        JLabel statusLabel = statusLabels.get(resourceKey);
        JLabel ownerLabel = ownerLabels.get(resourceKey);
        JLabel waitLabel = waitLabels.get(resourceKey);

        if (mutex.isLocked()) {
            statusLabel.setText("● Locked");
            statusLabel.setForeground(new Color(204, 0, 0));
        } else {
            statusLabel.setText("● Free");
            statusLabel.setForeground(new Color(0, 153, 0));
        }

        PCB owner = mutex.getOwner();
        ownerLabel.setText(owner == null ? "Owner: None" : "Owner: P" + owner.processID);

        int waiting = mutex.getWaitQueueSize();
        waitLabel.setText("Waiting: " + waiting + (waiting == 1 ? " process" : " processes"));
    }
}
