package os;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

/**
 * MutexStatusPanel - Displays status of 3 system mutexes
 * Shows: locked/free, owner, wait queue size
 */
public class MutexStatusPanel extends VBox {
    
    private VBox mutexContainer;
    
    public MutexStatusPanel() {
        super(10);
        this.setPadding(new Insets(10));
        this.setStyle("-fx-border-color: #dddddd; -fx-border-radius: 3; -fx-background-color: #fafafa;");
        
        mutexContainer = new VBox(8);
        
        // Add three mutex status displays
        mutexContainer.getChildren().addAll(
            createMutexDisplay("File Access"),
            createMutexDisplay("User Input"),
            createMutexDisplay("User Output")
        );
        
        this.getChildren().add(mutexContainer);
    }
    
    /**
     * Create visual display for a single mutex
     */
    private VBox createMutexDisplay(String resourceName) {
        VBox mutexBox = new VBox(5);
        mutexBox.setPadding(new Insets(10));
        mutexBox.setStyle(
            "-fx-border-color: #999999; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 3; " +
            "-fx-background-color: #ffffff;"
        );
        
        // Resource name
        Label nameLabel = new Label(resourceName);
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        
        // Status (locked/free)
        Label statusLabel = new Label("🟢 Free");
        statusLabel.setStyle("-fx-font-size: 11px;");
        statusLabel.setId("status_" + resourceName);
        
        // Owner
        Label ownerLabel = new Label("Owner: None");
        ownerLabel.setStyle("-fx-font-size: 10px;");
        ownerLabel.setId("owner_" + resourceName);
        
        // Wait queue
        Label waitLabel = new Label("Waiting: 0 processes");
        waitLabel.setStyle("-fx-font-size: 10px;");
        waitLabel.setId("wait_" + resourceName);
        
        mutexBox.getChildren().addAll(nameLabel, statusLabel, ownerLabel, waitLabel);
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
