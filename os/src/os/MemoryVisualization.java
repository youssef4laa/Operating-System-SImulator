package os;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

/**
 * MemoryVisualization - Displays 40-word memory as a visual grid
 * Color-coded: allocated (blue) vs. free (gray) words
 */
public class MemoryVisualization extends VBox {
    
    private GridPane memoryGrid;
    private Label[] wordLabels;
    private StackPane[] wordCells;
    private Memory memory;
    
    public MemoryVisualization(Memory memory) {
        super(5);
        this.memory = memory;
        this.setPadding(new Insets(10));
        this.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 3; -fx-background-color: #f9f9f9;");
        
        initializeGrid();
        this.getChildren().add(memoryGrid);
    }
    
    /**
     * Initialize 40-word memory grid (8 columns, 5 rows)
     */
    private void initializeGrid() {
        memoryGrid = new GridPane();
        memoryGrid.setHgap(3);
        memoryGrid.setVgap(3);
        memoryGrid.setPadding(new Insets(5));
        
        wordLabels = new Label[40];
        wordCells = new StackPane[40];
        
        // Create 8x5 grid (8 words per row, 5 rows)
        for (int i = 0; i < 40; i++) {
            int row = i / 8;
            int col = i % 8;
            
            // Create word cell
            StackPane cell = new StackPane();
            cell.setPrefWidth(60);
            cell.setPrefHeight(40);
            cell.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-background-color: #e8e8e8;");
            
            // Create label for word address and content
            Label wordLabel = new Label("[" + i + "]: Empty");
            wordLabel.setStyle("-fx-font-size: 9px; -fx-text-alignment: center;");
            
            cell.getChildren().add(wordLabel);
            memoryGrid.add(cell, col, row);
            
            wordLabels[i] = wordLabel;
            wordCells[i] = cell;
        }
    }
    
    /**
     * Update memory visualization
     */
    public void update(Memory memory) {
        try {
            for (int i = 0; i < 40; i++) {
                Object value = memory.read(i);
                
                if (value == null) {
                    // Empty cell
                    wordLabels[i].setText("[" + i + "]: Empty");
                    wordCells[i].setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-background-color: #e8e8e8;");
                } else if (value instanceof PCB) {
                    // PCB - show process ID
                    PCB pcb = (PCB) value;
                    wordLabels[i].setText("[" + i + "]: PCB(P" + pcb.processID + ")");
                    wordCells[i].setStyle("-fx-border-color: #0066cc; -fx-border-width: 2; -fx-background-color: #e6f2ff;");
                } else if (value instanceof String) {
                    // Instruction or data
                    String str = (String) value;
                    if (str.length() > 12) {
                        str = str.substring(0, 10) + "...";
                    }
                    wordLabels[i].setText("[" + i + "]: \"" + str + "\"");
                    wordCells[i].setStyle("-fx-border-color: #009900; -fx-border-width: 2; -fx-background-color: #e8f5e9;");
                } else {
                    // Other data
                    wordLabels[i].setText("[" + i + "]: " + value.toString());
                    wordCells[i].setStyle("-fx-border-color: #ff9800; -fx-border-width: 2; -fx-background-color: #fff3e0;");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
