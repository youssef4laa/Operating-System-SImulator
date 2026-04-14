package os;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.collections.ObservableList;
import java.util.LinkedList;

/**
 * QueueVisualization - Displays process queues (Ready, Blocked)
 * Shows process IDs in queue order with color coding
 */
public class QueueVisualization extends VBox {
    
    private String queueName;
    private LinkedList<PCB> queue;
    private HBox processBox;
    private Label queueStatusLabel;
    
    public QueueVisualization(String name, LinkedList<PCB> queue) {
        super(5);
        this.queueName = name;
        this.queue = queue;
        this.setPadding(new Insets(10));
        this.setStyle("-fx-border-color: #dddddd; -fx-border-radius: 3; -fx-background-color: #fafafa;");
        
        // Status label
        queueStatusLabel = new Label(queueName + " Queue (0 processes)");
        queueStatusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        
        // Process display box
        processBox = new HBox(5);
        processBox.setPadding(new Insets(5));
        processBox.setStyle("-fx-border-color: #eeeeee; -fx-border-radius: 2; -fx-background-color: #ffffff;");
        
        ScrollPane scrollPane = new ScrollPane(processBox);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefHeight(60);
        
        this.getChildren().addAll(queueStatusLabel, scrollPane);
    }
    
    /**
     * Update queue visualization
     */
    public void update(LinkedList<PCB> queue) {
        processBox.getChildren().clear();
        
        if (queue.isEmpty()) {
            Label emptyLabel = new Label("[Empty]");
            emptyLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
            processBox.getChildren().add(emptyLabel);
            queueStatusLabel.setText(queueName + " Queue (0 processes)");
        } else {
            for (PCB pcb : queue) {
                // Create visual representation of process
                VBox processCard = createProcessCard(pcb);
                processBox.getChildren().add(processCard);
            }
            queueStatusLabel.setText(queueName + " Queue (" + queue.size() + " processes)");
        }
    }
    
    /**
     * Create visual card for a process
     */
    private VBox createProcessCard(PCB pcb) {
        VBox card = new VBox(3);
        card.setPadding(new Insets(8));
        card.setStyle(
            "-fx-border-color: #0066cc; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 3; " +
            "-fx-background-color: #e6f2ff; " +
            "-fx-min-width: 70; " +
            "-fx-alignment: center;"
        );
        
        // Process ID
        Label pidLabel = new Label("P" + pcb.processID);
        pidLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        
        // Status
        Label statusLabel = new Label(pcb.status);
        statusLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #0066cc;");
        
        // PC
        Label pcLabel = new Label("PC:" + pcb.programCounter);
        pcLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #666666;");
        
        card.getChildren().addAll(pidLabel, statusLabel, pcLabel);
        return card;
    }
}
