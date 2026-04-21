package os;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * OSSimulatorGUI - Swing main application for OS visualization
 * Displays: Memory, Queues, Processes, Mutexes, System Calls, Scheduler, Debug Console
 * Supports: Step-through and Automatic execution modes
 * Integrates with SimulationEngine for proper state management
 */
public class OSSimulatorGUI extends JFrame {
    
    private SimulationEngine engine;
    private Timer automationTimer;
    
    // UI Components
    private MemoryVisualization memoryPanel;
    private QueueVisualization readyQueuePanel;
    private QueueVisualization blockedQueuePanel;
    private CurrentProcessPanel currentProcessPanel;
    private MutexStatusPanel mutexPanel;
    private SystemCallStatsPanel statsPanel;
    private TimelinePanel timelinePanel;
    private DebugConsole debugConsole;
    private ProgramDropZone dropZone;
    
    // Control elements
    private JComboBox<String> algorithmSelector;
    private JLabel statusLabel;
    private ButtonGroup modeGroup;
    private JSlider speedSlider;
    private JLabel speedValue;
    
    // Execution state
    private boolean executionPaused = true;
    private boolean stepMode = true;
    private double executionSpeed = 1.0;
    
    public OSSimulatorGUI() {
        // Initialize debug console first to capture output
        debugConsole = new DebugConsole();
        
        // Initialize simulation engine
        engine = new SimulationEngine(debugConsole);
        
        // Enable GUI-based input popups and reset simulation if input is cancelled
        SystemCall.setInputProvider(new GUIInputProvider(this::handleInputCancellation));
        
        // Setup listener for GUI updates
        engine.setListener(new SimulationEngine.SimulationListener() {
            @Override
            public void onInitialized() {
                updateAll();
            }
            
            @Override
            public void onStarted() {
                updateStatusLabel("Running");
            }
            
            @Override
            public void onStepComplete() {
                updateAll();
            }
            
            @Override
            public void onPaused() {
                updateStatusLabel("Paused");
            }
            
            @Override
            public void onResumed() {
                updateStatusLabel("Running");
            }
            
            @Override
            public void onCompleted() {
                updateStatusLabel("Completed");
                pauseExecution();
            }
            
            @Override
            public void onReset() {
                updateAll();
                updateStatusLabel("Ready");
            }
            
            @Override
            public void onStateChanged(Scheduler scheduler, Memory memory, int clockCycle) {
                updateAll();
            }
        });
        
        // Setup window
        this.setTitle("Operating System Simulator - CSEN 602 (Spring 2026)");
        this.setSize(1600, 1000);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
        
        // Create main layout
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));
        ((JPanel) contentPane).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top: Header and controls
        JPanel top = createHeader();
        contentPane.add(top, BorderLayout.NORTH);
        
        // Center: Main content in JSplitPane
        JSplitPane centerSplit = createCenterContent();
        contentPane.add(centerSplit, BorderLayout.CENTER);
        
        // Bottom: Status bar
        JPanel bottom = createStatusBar();
        contentPane.add(bottom, BorderLayout.SOUTH);
        
        updateStatusLabel("Ready");
    }
    
    /**
     * Create header with title and algorithm selector
     */
    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(204, 204, 204)));
        header.add(Box.createVerticalStrut(10));
        
        JLabel titleLabel = new JLabel("Operating System Simulator - CSEN 602 (Spring 2026)");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        header.add(titleLabel);
        
        header.add(Box.createVerticalStrut(10));
        
        // Algorithm selector and controls
        JPanel algorithmBox = new JPanel();
        algorithmBox.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));
        
        JLabel algoLabel = new JLabel("Algorithm:");
        algoLabel.setFont(new Font("Arial", Font.BOLD, 11));
        
        algorithmSelector = new JComboBox<>(new String[]{"RR", "HRRN", "MLFQ"});
        algorithmSelector.setSelectedItem("RR");
        algorithmSelector.setPreferredSize(new Dimension(80, 25));
        algorithmSelector.addActionListener(e -> resetSimulation(false));
        
        JLabel modeLabel = new JLabel("Mode:");
        modeLabel.setFont(new Font("Arial", Font.BOLD, 11));
        
        modeGroup = new ButtonGroup();
        JRadioButton stepButton = new JRadioButton("Step", true);
        JRadioButton autoButton = new JRadioButton("Auto", false);
        modeGroup.add(stepButton);
        modeGroup.add(autoButton);
        stepButton.addActionListener(e -> {
            stepMode = true;
            pauseExecution();
        });
        autoButton.addActionListener(e -> {
            stepMode = false;
        });
        
        JLabel speedLabel = new JLabel("Speed:");
        speedLabel.setFont(new Font("Arial", Font.BOLD, 11));
        
        speedSlider = new JSlider(10, 300, 100);
        speedSlider.setPreferredSize(new Dimension(100, 25));
        speedSlider.addChangeListener(e -> {
            executionSpeed = speedSlider.getValue() / 100.0;
            speedValue.setText(String.format("%.1fx", executionSpeed));
        });
        
        speedValue = new JLabel("1.0x");
        speedValue.setFont(new Font("Arial", Font.PLAIN, 10));
        
        algorithmBox.add(algoLabel);
        algorithmBox.add(algorithmSelector);
        algorithmBox.add(new JSeparator(SwingConstants.VERTICAL));
        algorithmBox.add(modeLabel);
        algorithmBox.add(stepButton);
        algorithmBox.add(autoButton);
        algorithmBox.add(new JSeparator(SwingConstants.VERTICAL));
        algorithmBox.add(speedLabel);
        algorithmBox.add(speedSlider);
        algorithmBox.add(speedValue);
        
        header.add(algorithmBox);
        header.add(Box.createVerticalStrut(10));
        
        // Drop zone for program files
        dropZone = new ProgramDropZone();
        dropZone.setOnProgramsDropped((files, arrivalTimes) -> onProgramsDropped(files, arrivalTimes));
        header.add(dropZone);
        header.add(Box.createVerticalStrut(10));
        
        return header;
    }
    
    /**
     * Create center content with split panes
     */
    private JSplitPane createCenterContent() {
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.34);
        
        // Left: Memory and Queues
        JPanel leftPanel = createLeftPanel();
        mainSplit.setLeftComponent(leftPanel);
        
        // Right: Timeline/Mutex/Stats + Debug Console in another split
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightSplit.setResizeWeight(0.7);
        
        JPanel centerPanel = createCenterPanel();
        rightSplit.setLeftComponent(centerPanel);
        
        JPanel debugPanel = createDebugPanel();
        rightSplit.setRightComponent(debugPanel);
        
        mainSplit.setRightComponent(rightSplit);
        mainSplit.setDividerLocation(520);
        rightSplit.setDividerLocation(760);
        
        return mainSplit;
    }
    
    /**
     * Create left panel with memory and queues
     */
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        panel.setPreferredSize(new Dimension(520, 0));
        
        JLabel sectionLabel = new JLabel("System State");
        sectionLabel.setFont(new Font("Arial", Font.BOLD, 13));
        panel.add(sectionLabel);
        panel.add(Box.createVerticalStrut(5));
        
        // Memory visualization
        JLabel memoryLabel = new JLabel("Memory (40 words)");
        memoryLabel.setFont(new Font("Arial", Font.BOLD, 11));
        memoryPanel = new MemoryVisualization(engine.getMemory());
        
        JScrollPane memScroll = new JScrollPane(memoryPanel);
        memScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        memScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        memScroll.setPreferredSize(new Dimension(500, 270));
        
        panel.add(memoryLabel);
        panel.add(memScroll);
        panel.add(Box.createVerticalStrut(5));
        
        // Ready Queue
        JLabel readyLabel = new JLabel("Ready Queue");
        readyLabel.setFont(new Font("Arial", Font.BOLD, 11));
        readyQueuePanel = new QueueVisualization("Ready", engine.getScheduler().getReadyQueue());
        
        panel.add(readyLabel);
        panel.add(readyQueuePanel);
        panel.add(Box.createVerticalStrut(5));
        
        // Blocked Queue
        JLabel blockedLabel = new JLabel("Blocked Queue");
        blockedLabel.setFont(new Font("Arial", Font.BOLD, 11));
        blockedQueuePanel = new QueueVisualization("Blocked", engine.getScheduler().blockedQueue);
        
        panel.add(blockedLabel);
        panel.add(blockedQueuePanel);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    /**
     * Create center panel with process info, timeline, stats, and mutexes
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        
        // TabbedPane for different views
        JTabbedPane tabPane = new JTabbedPane();
        
        // Tab 1: Current Process
        currentProcessPanel = new CurrentProcessPanel();
        tabPane.addTab("Process", currentProcessPanel);
        
        // Tab 2: Timeline
        timelinePanel = new TimelinePanel();
        tabPane.addTab("Timeline", timelinePanel);
        
        // Tab 3: Mutexes
        mutexPanel = new MutexStatusPanel();
        mutexPanel.setMutexManager(engine.getMutexManager());
        tabPane.addTab("Mutexes", mutexPanel);
        
        // Tab 4: Statistics
        statsPanel = new SystemCallStatsPanel();
        tabPane.addTab("Statistics", statsPanel);
        
        panel.add(tabPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create debug panel with debug console
     */
    private JPanel createDebugPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createLineBorder(new Color(221, 221, 221), 1));
        
        // Add debug console
        debugConsole.setPreferredSize(new Dimension(0, 470));
        panel.add(debugConsole, BorderLayout.CENTER);
        
        // Control buttons
        JPanel controlButtonsBox = createControlButtons();
        panel.add(controlButtonsBox, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Create control buttons
     */
    private JPanel createControlButtons() {
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(224, 224, 224)));
        
        JButton initButton = new JButton("Initialize");
        initButton.setFont(new Font("Arial", Font.PLAIN, 11));
        initButton.addActionListener(e -> initializeSimulation());
        
        JButton startButton = new JButton("Start");
        startButton.setFont(new Font("Arial", Font.PLAIN, 11));
        startButton.addActionListener(e -> startExecution());
        
        JButton stepButton = new JButton("Step");
        stepButton.setFont(new Font("Arial", Font.PLAIN, 11));
        stepButton.addActionListener(e -> executeOneStep());
        
        JButton pauseButton = new JButton("Pause");
        pauseButton.setFont(new Font("Arial", Font.PLAIN, 11));
        pauseButton.addActionListener(e -> pauseExecution());
        
        JButton resumeButton = new JButton("Resume");
        resumeButton.setFont(new Font("Arial", Font.PLAIN, 11));
        resumeButton.addActionListener(e -> resumeExecution());
        
        JButton resetButton = new JButton("Reset");
        resetButton.setFont(new Font("Arial", Font.PLAIN, 11));
        resetButton.addActionListener(e -> resetSimulation());
        
        buttons.add(initButton);
        buttons.add(startButton);
        buttons.add(stepButton);
        buttons.add(pauseButton);
        buttons.add(resumeButton);
        buttons.add(resetButton);
        
        // Add spacer and status
        buttons.add(Box.createHorizontalGlue());
        JLabel statusLabelText = new JLabel("Status: ");
        statusLabelText.setFont(new Font("Arial", Font.PLAIN, 11));
        buttons.add(statusLabelText);
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        buttons.add(statusLabel);
        
        return buttons;
    }
    
    /**
     * Create status bar
     */
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel();
        statusBar.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 8));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)));
        statusBar.setBackground(new Color(245, 245, 245));
        
        JLabel readyCountLabel = new JLabel("Ready: 0");
        JLabel blockedCountLabel = new JLabel("Blocked: 0");
        JLabel finishedCountLabel = new JLabel("Finished: 0");
        JLabel clockLabel = new JLabel("Clock Cycle: 0");
        
        statusBar.add(clockLabel);
        statusBar.add(readyCountLabel);
        statusBar.add(blockedCountLabel);
        statusBar.add(finishedCountLabel);
        
        return statusBar;
    }
    
    /**
     * Handle dropped program files
     */
    private void onProgramsDropped(List<File> files, List<Integer> arrivalTimes) {
        try {
            // Reset simulation before loading new programs
            pauseExecution();
            engine.reset();
            timelinePanel.clearLog();
            
            // Load programs into the engine
            engine.loadProgramsFromFiles(files, arrivalTimes);
            
            debugConsole.log("✓ Programs loaded successfully (" + files.size() + " file(s))");
            updateStatusLabel("Programs Loaded");
            
        } catch (Exception e) {
            debugConsole.log("✗ Error loading programs: " + e.getMessage(), true);
            JOptionPane.showMessageDialog(
                this,
                "Error loading programs:\n\n" + e.getMessage(),
                "Load Error",
                JOptionPane.ERROR_MESSAGE
            );
            updateStatusLabel("Error");
        }
    }
    
    /**
     * Initialize simulation
     */
    private void initializeSimulation() {
        // Check if programs are loaded
        if (!engine.hasProgramsLoaded()) {
            JOptionPane.showMessageDialog(
                this,
                "No programs loaded.\n\nPlease drag and drop program files (.txt) into the drop zone at the top.",
                "No Programs Loaded",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        String algorithm = (String) algorithmSelector.getSelectedItem();
        engine.initialize(algorithm);
        debugConsole.log("Simulation initialized with " + algorithm + " algorithm");
        updateStatusLabel("Initialized");
    }
    
    /**
     * Start execution
     */
    private void startExecution() {
        if (!engine.isInitialized()) {
            initializeSimulation();
            if (!engine.isInitialized()) return; // User might have cancelled or no programs loaded
        }
        if (!engine.isRunning()) {
            engine.start();
        }
        executionPaused = false;
        if (!stepMode) {
            startAutomaticExecution();
        }
        updateStatusLabel("Running");
    }
    
    /**
     * Execute one step
     */
    private void executeOneStep() {
        if (!engine.isInitialized()) {
            initializeSimulation();
            if (!engine.isInitialized()) return;
        }
        engine.step();
    }

    /**
     * Handle user cancellation of input dialogs by resetting simulation state.
     */
    private void handleInputCancellation() {
        resetSimulation(false);
    }
    
    /**
     * Start automatic execution
     */
    private void startAutomaticExecution() {
        if (automationTimer != null) {
            automationTimer.cancel();
        }
        
        long delayMs = (long) (500 / executionSpeed);
        
        automationTimer = new Timer();
        automationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!executionPaused) {
                    executeOneStep();
                }
            }
        }, delayMs, delayMs);
    }
    
    /**
     * Pause execution
     */
    private void pauseExecution() {
        engine.pause();
        executionPaused = true;
        if (automationTimer != null) {
            automationTimer.cancel();
            automationTimer = null;
        }
    }
    
    /**
     * Resume execution
     */
    private void resumeExecution() {
        engine.resume();
        executionPaused = false;
        if (!stepMode) {
            startAutomaticExecution();
        }
    }
    
    /**
     * Reset simulation
     */
    private void resetSimulation() {
        resetSimulation(true);
    }

    /**
     * Reset simulation with optional success dialog
     */
    private void resetSimulation(boolean showDialog) {
        pauseExecution();
        engine.reset();
        mutexPanel.setMutexManager(engine.getMutexManager());

        // Keep UI state aligned with engine reset behavior
        dropZone.clearPrograms();
        timelinePanel.clearLog();
        updateStatusLabel("Ready");

        if (showDialog) {
            JOptionPane.showMessageDialog(this, "Simulation reset to initial state", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Update all UI panels
     */
    private void updateAll() {
        SwingUtilities.invokeLater(() -> {
            mutexPanel.setMutexManager(engine.getMutexManager());
            memoryPanel.update(engine.getMemory());
            Scheduler scheduler = engine.getScheduler();
            boolean isMLFQ = "MLFQ".equalsIgnoreCase(scheduler.algorithm);
            readyQueuePanel.setMLFQMode(isMLFQ, scheduler.q0, scheduler.q1, scheduler.q2, scheduler.q3);
            readyQueuePanel.update(engine.getScheduler().getReadyQueue(), engine.getScheduler().algorithm);
            blockedQueuePanel.update(engine.getScheduler().blockedQueue, engine.getScheduler().algorithm);
            currentProcessPanel.update(engine.getScheduler());
            mutexPanel.update();
            statsPanel.update();
            timelinePanel.update(
                engine.getScheduler(),
                engine.getMemory(),
                engine.getClockCycle(),
                engine.getInstructionsExecuted()
            );
        });
    }
    
    /**
     * Update status label
     */
    private void updateStatusLabel(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            
            // Color based on status
            switch (status) {
                case "Running":
                    statusLabel.setForeground(new Color(0, 153, 0));
                    break;
                case "Paused":
                    statusLabel.setForeground(new Color(255, 153, 0));
                    break;
                case "Ready":
                    statusLabel.setForeground(new Color(0, 102, 204));
                    break;
                case "Completed":
                    statusLabel.setForeground(new Color(0, 153, 0));
                    statusLabel.setFont(new Font("Arial", Font.BOLD, 11));
                    break;
                case "Error":
                    statusLabel.setForeground(new Color(255, 0, 0));
                    break;
            }
        });
    }
    
    /**
     * Shutdown application
     */
    private void shutdown() {
        pauseExecution();
        debugConsole.restore();
        System.exit(0);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OSSimulatorGUI gui = new OSSimulatorGUI();
            gui.setVisible(true);
        });
    }
}
