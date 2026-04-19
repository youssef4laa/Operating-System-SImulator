package os;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * ProgramDropZone - Drag-and-drop UI component for loading program files
 * Accepts text (.txt) program files and notifies listener of dropped files
 * Displays visual feedback during drag operations and shows queued programs
 */
public class ProgramDropZone extends JPanel implements DropTargetListener {
    
    private List<File> droppedFiles = new ArrayList<>();
    private List<Integer> arrivalTimes = new ArrayList<>();
    private List<JSpinner> arrivalTimeSpinners = new ArrayList<>();
    private OnProgramsDropped onDrop;
    private boolean isDraggingOver = false;
    private static final Color DROP_ZONE_COLOR = new Color(240, 248, 255);
    private static final Color DRAG_OVER_COLOR = new Color(200, 230, 255);
    private static final Color TEXT_COLOR = new Color(100, 100, 100);
    
    public interface OnProgramsDropped {
        void onProgramsDropped(List<File> files, List<Integer> arrivalTimes);
    }
    
    public ProgramDropZone() {
        setLayout(new BorderLayout());
        setBackground(DROP_ZONE_COLOR);
        setBorder(BorderFactory.createDashedBorder(TEXT_COLOR, 2, 5, 3, true));
        setPreferredSize(new Dimension(400, 120));
        setMinimumSize(new Dimension(300, 80));
        
        // Enable drag and drop
        new DropTarget(this, DnDConstants.ACTION_COPY, this);
        
        updateDisplay();
    }
    
    /**
     * Set the callback listener for dropped programs
     */
    public void setOnProgramsDropped(OnProgramsDropped callback) {
        this.onDrop = callback;
    }
    
    /**
     * Get the list of dropped files
     */
    public List<File> getDroppedFiles() {
        return new ArrayList<>(droppedFiles);
    }
    
    /**
     * Get the arrival times for dropped programs
     */
    public List<Integer> getArrivalTimes() {
        return new ArrayList<>(arrivalTimes);
    }
    
    /**
     * Clear all dropped programs
     */
    public void clearPrograms() {
        droppedFiles.clear();
        arrivalTimes.clear();
        updateDisplay();
    }
    
    /**
     * Refresh the display
     */
    private void updateDisplay() {
        removeAll();
        
        if (droppedFiles.isEmpty()) {
            displayEmptyState();
        } else {
            displayProgramList();
        }
        
        revalidate();
        repaint();
    }
    
    /**
     * Display empty state prompt
     */
    private void displayEmptyState() {
        JPanel emptyPanel = new JPanel();
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setOpaque(false);
        emptyPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel dragLabel = new JLabel("📁 Drag & Drop Program Files Here");
        dragLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dragLabel.setForeground(TEXT_COLOR);
        dragLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel orLabel = new JLabel("— or —");
        orLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        orLabel.setForeground(new Color(150, 150, 150));
        orLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton browseButton = new JButton("Browse Files (Single or Multiple)");
        browseButton.setFont(new Font("Arial", Font.PLAIN, 11));
        browseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        browseButton.addActionListener(e -> browseAndAddFiles());
        
        JLabel hintLabel = new JLabel("Accepts .txt program files");
        hintLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        hintLabel.setForeground(new Color(150, 150, 150));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        emptyPanel.add(Box.createVerticalGlue());
        emptyPanel.add(dragLabel);
        emptyPanel.add(Box.createVerticalStrut(8));
        emptyPanel.add(orLabel);
        emptyPanel.add(Box.createVerticalStrut(8));
        emptyPanel.add(browseButton);
        emptyPanel.add(Box.createVerticalStrut(5));
        emptyPanel.add(hintLabel);
        emptyPanel.add(Box.createVerticalGlue());
        
        add(emptyPanel, BorderLayout.CENTER);
    }
    
    /**
     * Display list of queued programs with editable arrival times
     */
    private void displayProgramList() {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("Queued Programs (" + droppedFiles.size() + ") - Set Arrival Times:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setForeground(TEXT_COLOR);
        listPanel.add(titleLabel);
        listPanel.add(Box.createVerticalStrut(5));
        
        // Clear previous spinners
        arrivalTimeSpinners.clear();
        
        // Display each program with editable arrival time
        for (int i = 0; i < droppedFiles.size(); i++) {
            File file = droppedFiles.get(i);
            int arrivalTime = arrivalTimes.get(i);
            
            JPanel programBox = new JPanel();
            programBox.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 2));
            programBox.setOpaque(false);
            programBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            
            JLabel numLabel = new JLabel((i + 1) + ".");
            numLabel.setFont(new Font("Arial", Font.BOLD, 11));
            numLabel.setForeground(TEXT_COLOR);
            
            JLabel nameLabel = new JLabel(file.getName());
            nameLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            nameLabel.setForeground(TEXT_COLOR);
            
            JLabel timeLabel = new JLabel("Arrival Time: t =");
            timeLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            timeLabel.setForeground(new Color(120, 120, 120));
            
            // Create spinner for arrival time (0 to 1000)
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(arrivalTime, 0, 1000, 1));
            spinner.setPreferredSize(new Dimension(50, 25));
            spinner.setFont(new Font("Arial", Font.PLAIN, 10));
            int index = i;
            
            // Update arrival time when spinner changes
            spinner.addChangeListener(e -> {
                int newTime = (Integer) spinner.getValue();
                arrivalTimes.set(index, newTime);
                // Notify engine of updated arrival times
                notifyArrivalTimesChanged();
            });
            
            arrivalTimeSpinners.add(spinner);
            
            JButton removeBtn = new JButton("✕");
            removeBtn.setFont(new Font("Arial", Font.PLAIN, 9));
            removeBtn.setMargin(new Insets(0, 3, 0, 3));
            removeBtn.setFocusPainted(false);
            removeBtn.addActionListener(e -> removeProgram(index));
            
            programBox.add(numLabel);
            programBox.add(nameLabel);
            programBox.add(Box.createHorizontalStrut(10));
            programBox.add(timeLabel);
            programBox.add(spinner);
            programBox.add(Box.createHorizontalGlue());
            programBox.add(removeBtn);
            
            listPanel.add(programBox);
        }
        
        // Add button to add more files
        JPanel addFilePanel = new JPanel();
        addFilePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));
        addFilePanel.setOpaque(false);
        addFilePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JButton addMoreBtn = new JButton("+ Add More Files");
        addMoreBtn.setFont(new Font("Arial", Font.PLAIN, 10));
        addMoreBtn.setMargin(new Insets(3, 8, 3, 8));
        addMoreBtn.setFocusPainted(false);
        addMoreBtn.addActionListener(e -> browseAndAddFiles());
        
        addFilePanel.add(addMoreBtn);
        
        listPanel.add(Box.createVerticalStrut(8));
        listPanel.add(addFilePanel);
        listPanel.add(Box.createVerticalGlue());
        
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }
    
    /**
     * Remove a program from the list
     */
    private void removeProgram(int index) {
        if (index >= 0 && index < droppedFiles.size()) {
            droppedFiles.remove(index);
            arrivalTimes.remove(index);
            updateDisplay();
            notifyArrivalTimesChanged();
        }
    }
    
    /**
     * Notify listener that arrival times have changed
     */
    private void notifyArrivalTimesChanged() {
        if (!droppedFiles.isEmpty() && onDrop != null) {
            onDrop.onProgramsDropped(new ArrayList<>(droppedFiles), new ArrayList<>(arrivalTimes));
        }
    }
    
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        if (isValidDrag(dtde)) {
            isDraggingOver = true;
            setBackground(DRAG_OVER_COLOR);
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        } else {
            dtde.rejectDrag();
        }
    }
    
    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if (isValidDrag(dtde)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        } else {
            dtde.rejectDrag();
        }
    }
    
    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }
    
    @Override
    public void dragExit(DropTargetEvent dte) {
        isDraggingOver = false;
        setBackground(DROP_ZONE_COLOR);
    }
    
    @Override
    public void drop(DropTargetDropEvent dtde) {
        isDraggingOver = false;
        setBackground(DROP_ZONE_COLOR);
        
        try {
            if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.rejectDrop();
                return;
            }
            
            dtde.acceptDrop(DnDConstants.ACTION_COPY);
            
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) dtde.getTransferable()
                .getTransferData(DataFlavor.javaFileListFlavor);
            
            // Filter and add valid files
            List<File> validFiles = new ArrayList<>();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    validFiles.add(file);
                    droppedFiles.add(file);
                    // Initialize arrival times starting from 0, incrementing by 1
                    arrivalTimes.add(droppedFiles.size() - 1);
                }
            }
            
            updateDisplay();
            
            // Notify listener with ALL current files
            if (!droppedFiles.isEmpty() && onDrop != null) {
                onDrop.onProgramsDropped(new ArrayList<>(droppedFiles), new ArrayList<>(arrivalTimes));
            }
            
            dtde.dropComplete(true);
            
        } catch (Exception e) {
            dtde.dropComplete(false);
        }
    }
    
    /**
     * Check if drag data is valid (contains files)
     */
    private boolean isValidDrag(DropTargetDragEvent dtde) {
        return dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }
    
    /**
     * Open file chooser to browse and add program files (single or multiple)
     */
    private void browseAndAddFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".txt");
            }
            
            @Override
            public String getDescription() {
                return "Text Files (*.txt)";
            }
        });
        
        int result = fileChooser.showOpenDialog(null);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            List<File> validFiles = new ArrayList<>();
            
            for (File file : selectedFiles) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    validFiles.add(file);
                    droppedFiles.add(file);
                    // Initialize arrival times starting from 0, incrementing by 1
                    arrivalTimes.add(droppedFiles.size() - 1);
                }
            }
            
            if (!validFiles.isEmpty()) {
                updateDisplay();
                
                // Notify listener with new arrival times
                if (onDrop != null) {
                    onDrop.onProgramsDropped(new ArrayList<>(droppedFiles), new ArrayList<>(arrivalTimes));
                }
            }
        }
    }
    
    /**
     * Override paint to show drag-over state
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (isDraggingOver) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(2));
            g2d.setColor(new Color(100, 150, 255));
            g2d.drawRect(2, 2, getWidth() - 4, getHeight() - 4);
        }
    }
}
