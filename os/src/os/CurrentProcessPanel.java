package os;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Separator;

/**
 * CurrentProcessPanel - Displays information about currently executing process
 */
public class CurrentProcessPanel extends VBox {
    
    private Label processIDLabel;
    private Label statusLabel;
    private Label instructionLabel;
    private Label pcLabel;
    private Label memoryBoundsLabel;
    private Label arrivalTimeLabel;
    private Label remainingTimeLabel;
    private TextArea instructionDetailsArea;
    
    public CurrentProcessPanel() {
        super(8);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-border-color: #dddddd; -fx-border-radius: 3; -fx-background-color: #fafafa;");
        
        initializeComponents();
    }
    
    private void initializeComponents() {
        // Process ID
        processIDLabel = new Label("Process ID: None");
        processIDLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0066cc;");
        
        // Status
        statusLabel = new Label("Status: Idle");
        statusLabel.setStyle("-fx-font-size: 12px;");
        
        // Current Instruction
        instructionLabel = new Label("Current Instruction: --");
        instructionLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        
        // Program Counter
        pcLabel = new Label("Program Counter: --");
        pcLabel.setStyle("-fx-font-size: 11px;");
        
        // Memory Bounds
        memoryBoundsLabel = new Label("Memory Bounds: --");
        memoryBoundsLabel.setStyle("-fx-font-size: 11px;");
        
        // Arrival Time
        arrivalTimeLabel = new Label("Arrival Time: --");
        arrivalTimeLabel.setStyle("-fx-font-size: 11px;");
        
        // Remaining Time
        remainingTimeLabel = new Label("Remaining Time: --");
        remainingTimeLabel.setStyle("-fx-font-size: 11px;");
        
        // Instruction Details
        Label detailsTitle = new Label("Instruction Details:");
        detailsTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        
        instructionDetailsArea = new TextArea();
        instructionDetailsArea.setPrefHeight(150);
        instructionDetailsArea.setEditable(false);
        instructionDetailsArea.setWrapText(true);
        instructionDetailsArea.setStyle("-fx-font-size: 10px; -fx-control-inner-background: #ffffff;");
        instructionDetailsArea.setText("No process executing...");
        
        this.getChildren().addAll(
            processIDLabel,
            statusLabel,
            new Separator(),
            instructionLabel,
            pcLabel,
            memoryBoundsLabel,
            arrivalTimeLabel,
            remainingTimeLabel,
            new Separator(),
            detailsTitle,
            instructionDetailsArea
        );
        
        VBox.setVgrow(instructionDetailsArea, Priority.ALWAYS);
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
