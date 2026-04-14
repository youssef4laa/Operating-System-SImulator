package os;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.control.*;

/**
 * SystemCallStatsPanel - Displays system call statistics
 * Shows: call count, success rate, error codes
 */
public class SystemCallStatsPanel extends VBox {
    
    private TableView<CallStatRow> statsTable;
    
    public SystemCallStatsPanel() {
        super(10);
        this.setPadding(new Insets(10));
        this.setStyle("-fx-border-color: #dddddd; -fx-border-radius: 3; -fx-background-color: #fafafa;");
        
        createStatsTable();
        this.getChildren().add(statsTable);
    }
    
    /**
     * Create table for system call statistics
     */
    private void createStatsTable() {
        statsTable = new TableView<>();
        statsTable.setPrefHeight(200);
        
        // Call name column
        TableColumn<CallStatRow, String> callColumn = new TableColumn<>("Call");
        callColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().callName));
        callColumn.setPrefWidth(80);
        
        // Total calls column
        TableColumn<CallStatRow, Integer> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().total));
        totalColumn.setPrefWidth(60);
        
        // Success column
        TableColumn<CallStatRow, Integer> successColumn = new TableColumn<>("Success");
        successColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().success));
        successColumn.setPrefWidth(70);
        
        // Failure column
        TableColumn<CallStatRow, Integer> failureColumn = new TableColumn<>("Failure");
        failureColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().failure));
        failureColumn.setPrefWidth(70);
        
        // Success rate column
        TableColumn<CallStatRow, String> rateColumn = new TableColumn<>("% Success");
        rateColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().successRate));
        rateColumn.setPrefWidth(80);
        
        statsTable.getColumns().addAll(callColumn, totalColumn, successColumn, failureColumn, rateColumn);
        
        // Add sample data
        statsTable.getItems().addAll(
            new CallStatRow("print", 0, 0, 0, "0%"),
            new CallStatRow("readFile", 0, 0, 0, "0%"),
            new CallStatRow("writeFile", 0, 0, 0, "0%"),
            new CallStatRow("input", 0, 0, 0, "0%"),
            new CallStatRow("readMemory", 0, 0, 0, "0%"),
            new CallStatRow("writeMemory", 0, 0, 0, "0%")
        );
    }
    
    /**
     * Update statistics display
     */
    public void update() {
        try {
            SystemCallStats stats = SystemCall.getStats();
            
            String[] calls = {"print", "readFile", "writeFile", "input", "readMemory", "writeMemory"};
            
            for (int i = 0; i < calls.length; i++) {
                int total = (int) stats.getCallCount(calls[i]);
                int success = (int) stats.getSuccessCount(calls[i]);
                int failure = total - success;
                double rate = stats.getSuccessRate(calls[i]);
                
                CallStatRow row = statsTable.getItems().get(i);
                row.total = total;
                row.success = success;
                row.failure = failure;
                row.successRate = String.format("%.1f%%", rate);
            }
            
            statsTable.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Data model for table rows
     */
    public static class CallStatRow {
        public String callName;
        public int total;
        public int success;
        public int failure;
        public String successRate;
        
        public CallStatRow(String callName, int total, int success, int failure, String successRate) {
            this.callName = callName;
            this.total = total;
            this.success = success;
            this.failure = failure;
            this.successRate = successRate;
        }
    }
}
