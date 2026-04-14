package os;

import javax.swing.*;
import java.awt.*;

/**
 * MemoryVisualization - Displays 40-word memory as a visual grid
 * Color-coded: allocated (blue) vs. free (gray) words
 */
public class MemoryVisualization extends JPanel {
    
    private Memory memory;
    private static final int GRID_COLS = 8;
    private static final int GRID_ROWS = 5;
    private static final int CELL_WIDTH = 60;
    private static final int CELL_HEIGHT = 40;
    private static final int CELL_PADDING = 3;
    
    // Color constants
    private static final Color COLOR_EMPTY = new Color(232, 232, 232);
    private static final Color COLOR_PCB = new Color(230, 242, 255);
    private static final Color COLOR_INSTRUCTION = new Color(232, 245, 233);
    private static final Color COLOR_DATA = new Color(255, 243, 224);
    private static final Color BORDER_COLOR = new Color(204, 204, 204);
    private static final Color PCB_BORDER = new Color(0, 102, 204);
    private static final Color INSTRUCTION_BORDER = new Color(0, 153, 0);
    private static final Color DATA_BORDER = new Color(255, 152, 0);
    
    private MemoryCell[] cells;
    
    public MemoryVisualization(Memory memory) {
        this.memory = memory;
        this.setLayout(null);
        this.setBackground(new Color(249, 249, 249));
        this.setBorder(BorderFactory.createLineBorder(new Color(224, 224, 224), 1));
        
        // Calculate total size
        int totalWidth = GRID_COLS * (CELL_WIDTH + CELL_PADDING) + 20;
        int totalHeight = GRID_ROWS * (CELL_HEIGHT + CELL_PADDING) + 20;
        this.setPreferredSize(new Dimension(totalWidth, totalHeight));
        
        initializeCells();
    }
    
    private void initializeCells() {
        cells = new MemoryCell[40];
        for (int i = 0; i < 40; i++) {
            int row = i / GRID_COLS;
            int col = i % GRID_COLS;
            
            int x = 10 + col * (CELL_WIDTH + CELL_PADDING);
            int y = 10 + row * (CELL_HEIGHT + CELL_PADDING);
            
            cells[i] = new MemoryCell(i, x, y, CELL_WIDTH, CELL_HEIGHT);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw all cells
        for (MemoryCell cell : cells) {
            drawCell(g2d, cell);
        }
    }
    
    private void drawCell(Graphics2D g2d, MemoryCell cell) {
        // Draw background
        g2d.setColor(cell.bgColor);
        g2d.fillRect(cell.x, cell.y, cell.width, cell.height);
        
        // Draw border
        g2d.setColor(cell.borderColor);
        g2d.setStroke(new BasicStroke(cell.borderWidth));
        g2d.drawRect(cell.x, cell.y, cell.width, cell.height);
        
        // Draw text
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "[" + cell.address + "]: " + cell.content;
        int textX = cell.x + (cell.width - fm.stringWidth(text)) / 2;
        int textY = cell.y + ((cell.height - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(text, textX, textY);
    }
    
    /**
     * Update memory visualization
     */
    public void update(Memory memory) {
        try {
            for (int i = 0; i < 40; i++) {
                Object value = memory.read(i);
                MemoryCell cell = cells[i];
                
                if (value == null) {
                    cell.content = "Empty";
                    cell.bgColor = COLOR_EMPTY;
                    cell.borderColor = BORDER_COLOR;
                    cell.borderWidth = 1;
                } else if (value instanceof PCB) {
                    PCB pcb = (PCB) value;
                    cell.content = "PCB(P" + pcb.processID + ")";
                    cell.bgColor = COLOR_PCB;
                    cell.borderColor = PCB_BORDER;
                    cell.borderWidth = 2;
                } else if (value instanceof String) {
                    String str = (String) value;
                    if (str.length() > 12) {
                        str = str.substring(0, 10) + "...";
                    }
                    cell.content = "\"" + str + "\"";
                    cell.bgColor = COLOR_INSTRUCTION;
                    cell.borderColor = INSTRUCTION_BORDER;
                    cell.borderWidth = 2;
                } else {
                    cell.content = value.toString();
                    cell.bgColor = COLOR_DATA;
                    cell.borderColor = DATA_BORDER;
                    cell.borderWidth = 2;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        repaint();
    }
    
    /**
     * Inner class to represent a memory cell
     */
    private static class MemoryCell {
        int address;
        int x, y;
        int width, height;
        String content;
        Color bgColor;
        Color borderColor;
        int borderWidth;
        
        MemoryCell(int address, int x, int y, int width, int height) {
            this.address = address;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.content = "Empty";
            this.bgColor = COLOR_EMPTY;
            this.borderColor = BORDER_COLOR;
            this.borderWidth = 1;
        }
    }
}
