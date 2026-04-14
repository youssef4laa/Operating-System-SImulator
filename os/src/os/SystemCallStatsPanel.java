package os;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * SystemCallStatsPanel - Displays system call statistics
 * Shows: call count, success rate, error codes
 */
public class SystemCallStatsPanel extends JPanel {
    
    private JTable statsTable;
    private DefaultTableModel tableModel;
    
    public SystemCallStatsPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        this.setBackground(new Color(250, 250, 250));
        
        createStatsTable();
        JScrollPane scrollPane = new JScrollPane(statsTable);
        this.add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Create table for system call statistics
     */
    private void createStatsTable() {
        // Create table model with columns
        String[] columnNames = {"Call", "Total", "Success", "Failure", "% Success"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        
        // Create JTable with the model
        statsTable = new JTable(tableModel);
        statsTable.setFont(new Font("Arial", Font.PLAIN, 11));
        statsTable.setRowHeight(25);
        statsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));
        
        // Set column widths
        statsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        statsTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        statsTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        statsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        statsTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        
        // Add initial rows for each system call
        String[] calls = {"print", "readFile", "writeFile", "input", "readMemory", "writeMemory"};
        for (String call : calls) {
            tableModel.addRow(new Object[]{call, 0, 0, 0, "0%"});
        }
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
                
                tableModel.setValueAt(total, i, 1);
                tableModel.setValueAt(success, i, 2);
                tableModel.setValueAt(failure, i, 3);
                tableModel.setValueAt(String.format("%.1f%%", rate), i, 4);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
