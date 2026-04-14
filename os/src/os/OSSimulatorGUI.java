package os;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

/**
 * OSSimulatorGUI - JavaFX main application for OS visualization
 * Displays: Memory, Queues, Processes, Mutexes, System Calls, Scheduler
 * Supports: Step-through and Automatic execution modes
 */
public class OSSimulatorGUI extends Application {
    
    private Stage primaryStage;
    private Scheduler scheduler;
    private Memory memory;
    private Timeline animationTimeline;
    
    // UI Components
    private MemoryVisualization memoryPanel;
    private QueueVisualization readyQueuePanel;
    private QueueVisualization blockedQueuePanel;
    private CurrentProcessPanel currentProcessPanel;
    private ControlPanel controlPanel;
    private MutexStatusPanel mutexPanel;
    private SystemCallStatsPanel statsPanel;
    
    // Execution state
    private boolean executionPaused = true;
    private boolean stepMode = true;
    private double executionSpeed = 1.0; // 1.0 = normal, 0.5 = slow, 2.0 = fast
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
        // Initialize backend
        memory = new Memory();
        scheduler = new Scheduler();
        
        // Create main layout
        BorderPane root = createMainLayout();
        
        // Create scene
        Scene scene = new Scene(root, 1400, 900);
        
        // Setup stage
        primaryStage.setTitle("Operating System Simulator - CSEN 602");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> shutdown());
        primaryStage.show();
    }
    
    /**
     * Create the main layout with all panels
     */
    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Top: Header and controls
        VBox top = createHeader();
        root.setTop(top);
        
        // Center: Main content (left: memory/queues, middle: current process, right: stats)
        HBox center = createCenterContent();
        root.setCenter(center);
        
        // Bottom: Control buttons
        HBox bottom = createControlButtons();
        root.setBottom(bottom);
        
        return root;
    }
    
    /**
     * Create header with title and algorithm selector
     */
    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 2 0; -fx-padding: 10;");
        
        Label titleLabel = new Label("Operating System Simulator - CSEN 602 (Spring 2026)");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Algorithm selector
        HBox algorithmBox = new HBox(10);
        Label algoLabel = new Label("Algorithm:");
        ComboBox<String> algoSelector = new ComboBox<>();
        algoSelector.getItems().addAll("RR", "HRRN", "MLFQ");
        algoSelector.setValue("RR");
        algoSelector.setOnAction(e -> {
            scheduler.algorithm = algoSelector.getValue();
            updateAll();
        });
        
        algorithmBox.getChildren().addAll(algoLabel, algoSelector);
        
        header.getChildren().addAll(titleLabel, algorithmBox);
        return header;
    }
    
    /**
     * Create center content with three columns
     */
    private HBox createCenterContent() {
        HBox center = new HBox(10);
        
        // Left column: Memory and Queues
        VBox leftPanel = createLeftPanel();
        
        // Middle column: Current Process
        VBox middlePanel = createMiddlePanel();
        
        // Right column: Stats and Mutexes
        VBox rightPanel = createRightPanel();
        
        // Set grow priorities
        HBox.setHgrow(leftPanel, Priority.SOMETIMES);
        HBox.setHgrow(middlePanel, Priority.SOMETIMES);
        HBox.setHgrow(rightPanel, Priority.SOMETIMES);
        
        center.getChildren().addAll(leftPanel, middlePanel, rightPanel);
        return center;
    }
    
    /**
     * Create left panel with memory and queues
     */
    private VBox createLeftPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-padding: 10;");
        
        // Memory visualization
        Label memoryLabel = new Label("Memory (40 words)");
        memoryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        memoryPanel = new MemoryVisualization(memory);
        
        // Ready Queue
        Label readyLabel = new Label("Ready Queue");
        readyLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        readyQueuePanel = new QueueVisualization("Ready", scheduler.readyQueue);
        
        // Blocked Queue
        Label blockedLabel = new Label("Blocked Queue");
        blockedLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        blockedQueuePanel = new QueueVisualization("Blocked", scheduler.blockedQueue);
        
        panel.getChildren().addAll(
            memoryLabel, memoryPanel,
            new Separator(),
            readyLabel, readyQueuePanel,
            blockedLabel, blockedQueuePanel
        );
        
        VBox.setVgrow(memoryPanel, Priority.SOMETIMES);
        return panel;
    }
    
    /**
     * Create middle panel with current process info
     */
    private VBox createMiddlePanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-padding: 10;");
        
        Label currentLabel = new Label("Current Process");
        currentLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        currentProcessPanel = new CurrentProcessPanel();
        
        panel.getChildren().addAll(currentLabel, currentProcessPanel);
        VBox.setVgrow(currentProcessPanel, Priority.ALWAYS);
        
        return panel;
    }
    
    /**
     * Create right panel with stats and mutex status
     */
    private VBox createRightPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-padding: 10;");
        
        // Mutexes
        Label mutexLabel = new Label("Mutex Status");
        mutexLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        mutexPanel = new MutexStatusPanel();
        
        // Statistics
        Label statsLabel = new Label("System Call Statistics");
        statsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        statsPanel = new SystemCallStatsPanel();
        
        panel.getChildren().addAll(
            mutexLabel, mutexPanel,
            new Separator(),
            statsLabel, statsPanel
        );
        
        VBox.setVgrow(mutexPanel, Priority.SOMETIMES);
        VBox.setVgrow(statsPanel, Priority.SOMETIMES);
        
        return panel;
    }
    
    /**
     * Create bottom control buttons
     */
    private HBox createControlButtons() {
        HBox buttons = new HBox(10);
        buttons.setPadding(new Insets(10));
        buttons.setStyle("-fx-border-color: #cccccc; -fx-border-width: 2 0 0 0;");
        
        // Mode selector
        Label modeLabel = new Label("Mode:");
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton stepButton = new RadioButton("Step-Through");
        RadioButton autoButton = new RadioButton("Automatic");
        stepButton.setToggleGroup(modeGroup);
        autoButton.setToggleGroup(modeGroup);
        stepButton.setSelected(true);
        
        // Speed control
        Label speedLabel = new Label("Speed:");
        Slider speedSlider = new Slider(0.1, 3.0, 1.0);
        speedSlider.setStyle("-fx-control-inner-background: #e0e0e0;");
        Label speedValue = new Label("1.0x");
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            executionSpeed = newVal.doubleValue();
            speedValue.setText(String.format("%.1fx", executionSpeed));
        });
        
        // Execution buttons
        Button startButton = new Button("Start");
        startButton.setStyle("-fx-font-size: 12px; -fx-padding: 8px 20px;");
        startButton.setOnAction(e -> startExecution());
        
        Button stepButton2 = new Button("Step");
        stepButton2.setStyle("-fx-font-size: 12px; -fx-padding: 8px 20px;");
        stepButton2.setOnAction(e -> executeOneStep());
        
        Button pauseButton = new Button("Pause");
        pauseButton.setStyle("-fx-font-size: 12px; -fx-padding: 8px 20px;");
        pauseButton.setOnAction(e -> pauseExecution());
        
        Button resumeButton = new Button("Resume");
        resumeButton.setStyle("-fx-font-size: 12px; -fx-padding: 8px 20px;");
        resumeButton.setOnAction(e -> resumeExecution());
        
        Button resetButton = new Button("Reset");
        resetButton.setStyle("-fx-font-size: 12px; -fx-padding: 8px 20px;");
        resetButton.setOnAction(e -> resetSimulation());
        
        buttons.getChildren().addAll(
            modeLabel, stepButton, autoButton,
            speedLabel, speedSlider, speedValue,
            new Separator(),
            startButton, stepButton2, pauseButton, resumeButton, resetButton
        );
        
        return buttons;
    }
    
    /**
     * Start execution in selected mode
     */
    private void startExecution() {
        executionPaused = false;
        if (!stepMode) {
            startAutomaticExecution();
        }
    }
    
    /**
     * Execute one step in step-through mode
     */
    private void executeOneStep() {
        if (scheduler.readyQueue.isEmpty() && scheduler.blockedQueue.isEmpty()) {
            showAlert("Simulation Complete", "All processes have finished.");
            return;
        }
        try {
            // Execute one clock cycle
            scheduler.start(memory);
            updateAll();
        } catch (Exception e) {
            showAlert("Execution Error", e.getMessage());
        }
    }
    
    /**
     * Start automatic execution with timeline
     */
    private void startAutomaticExecution() {
        if (animationTimeline != null) {
            animationTimeline.stop();
        }
        
        // Calculate duration based on speed
        long durationMs = (long) (500 / executionSpeed); // 500ms base, adjusted by speed
        
        animationTimeline = new Timeline(
            new KeyFrame(Duration.millis(durationMs), e -> {
                try {
                    if (!executionPaused) {
                        executeOneStep();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            })
        );
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
        animationTimeline.play();
    }
    
    /**
     * Pause execution
     */
    private void pauseExecution() {
        executionPaused = true;
        if (animationTimeline != null) {
            animationTimeline.stop();
        }
    }
    
    /**
     * Resume execution
     */
    private void resumeExecution() {
        executionPaused = false;
        startAutomaticExecution();
    }
    
    /**
     * Reset simulation to initial state
     */
    private void resetSimulation() {
        pauseExecution();
        try {
            memory = new Memory();
            scheduler = new Scheduler();
            updateAll();
            showAlert("Reset", "Simulation reset to initial state.");
        } catch (Exception e) {
            showAlert("Reset Error", e.getMessage());
        }
    }
    
    /**
     * Update all UI panels
     */
    private void updateAll() {
        Platform.runLater(() -> {
            memoryPanel.update(memory);
            readyQueuePanel.update(scheduler.readyQueue);
            blockedQueuePanel.update(scheduler.blockedQueue);
            currentProcessPanel.update(scheduler);
            mutexPanel.update();
            statsPanel.update();
        });
    }
    
    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Shutdown application
     */
    private void shutdown() {
        pauseExecution();
        System.exit(0);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
