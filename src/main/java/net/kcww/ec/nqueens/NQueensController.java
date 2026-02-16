package net.kcww.ec.nqueens;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class NQueensController {

    // --- UI Injections ---
    @FXML private SplitPane mainSplitPane;
    @FXML private GridPane boardGrid;
    @FXML private StackPane treeContainer;
    @FXML private VBox logContainer;
    @FXML private TextArea logArea;

    @FXML private Spinner<Integer> sizeSpinner;
    @FXML private Spinner<Integer> kSpinner;
    @FXML private RadioButton rbBacktrack;
    @FXML private RadioButton rbLasVegas;
    @FXML private RadioButton rbMixed;

    @FXML private CheckBox chkAnimate;
    @FXML private CheckBox chkShowTree;
    @FXML private CheckBox chkShowLog;
    @FXML private Slider speedSlider;

    @FXML private Button solveButton;
    @FXML private Button stopButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;
    @FXML private Label statusLabel;

    // --- State ---
    private static final int MIN_N = 4;
    private static final int MAX_N = 20;
    private static final int DEFAULT_N = 8;

    private int n = DEFAULT_N;
    private NQueensSolver solver;
    private TreeVisualizer treeVisualizer;
    private Thread solverThread;

    @FXML
    public void initialize() {
        // Initialize Spinners
        sizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_N, MAX_N, DEFAULT_N));
        sizeSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            n = newV;
            updateKSpinner();
            resetUI();
        });

        // K Spinner (for Mixed mode)
        kSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, DEFAULT_N - 1, 1));

        // Mode Toggles
        rbMixed.selectedProperty().addListener((obs, oldV, newV) -> kSpinner.setDisable(!newV));

        // Speed Slider
        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (solver != null) solver.setDelay(newV.intValue());
        });

        // Initialize Tree Visualizer
        treeVisualizer = new TreeVisualizer();
        treeContainer.getChildren().add(treeVisualizer);
        treeVisualizer.toBack(); // Ensure label stays on top

        // Initialize UI State
        updateKSpinner();
        resetUI();
        handleViewToggles();
    }

    private void updateKSpinner() {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
            (SpinnerValueFactory.IntegerSpinnerValueFactory) kSpinner.getValueFactory();
        factory.setMax(n - 1); // K must be less than N
    }

    @FXML
    private void handleSolve() {
        if (solverThread != null && solverThread.isAlive()) return;

        resetBoardOnly();
        treeVisualizer.reset();
        setControlsLocked(true);
        statusLabel.setText("Running...");

        // Configuration
        NQueensSolver.Mode mode = NQueensSolver.Mode.BACKTRACK;
        if (rbLasVegas.isSelected()) mode = NQueensSolver.Mode.LAS_VEGAS;
        if (rbMixed.isSelected()) mode = NQueensSolver.Mode.MIXED;

        int k = kSpinner.getValue();
        int delay = chkAnimate.isSelected() ? (int)speedSlider.getValue() : 0;

        solver = new NQueensSolver(n, mode, k, new NQueensSolver.SolverListener() {
            @Override
            public void onStep(int row, int col, boolean placing, String nodeId, String parentId) {
                if (!chkAnimate.isSelected()) return;
                Platform.runLater(() -> {
                    updateBoard(row, col, placing);
                    if (chkShowTree.isSelected()) {
                        treeVisualizer.updateTree(row, col, placing, nodeId, parentId);
                    }
                });
            }

            @Override
            public void onSolutionFound(int[] solution) {
                Platform.runLater(() -> {
                    // Force draw final state in case animation was off
                    resetBoardOnly();
                    for(int r=0; r<n; r++) updateBoard(r, solution[r], true);
                    statusLabel.setText("Solution Found!");
                    statusLabel.setStyle("-fx-text-fill: green;");
                });
            }

            @Override
            public void onLog(String message) {
                Platform.runLater(() -> log(message));
            }

            @Override
            public void onFinished() {
                Platform.runLater(() -> {
                    setControlsLocked(false);
                    pauseButton.setText("Pause");
                    if (!statusLabel.getText().contains("Found")) {
                        statusLabel.setText("Finished.");
                    }
                });
            }
        });

        solver.setDelay(delay);
        solverThread = new Thread(solver);
        solverThread.setDaemon(true);
        solverThread.start();
    }

    @FXML
    private void handleStop() {
        if (solver != null) solver.stop();
        statusLabel.setText("Stopped.");
        log("Algorithm stopped by user.");
    }

    @FXML
    private void handlePause() {
        if (solver == null) return;

        if (solver.isPaused()) {
            solver.resume();
            pauseButton.setText("Pause");
            statusLabel.setText("Running...");
        } else {
            solver.pause();
            pauseButton.setText("Resume");
            statusLabel.setText("Paused");
        }
    }

    @FXML
    private void handleReset() {
        handleStop();
        resetUI();
        log("Board cleared.");
    }

    @FXML
    private void handleClearLog() {
        logArea.clear();
    }

    @FXML
    private void handleViewToggles() {
        // Toggle Log
        logContainer.setVisible(chkShowLog.isSelected());
        logContainer.setManaged(chkShowLog.isSelected());

        // Toggle Tree
        // If hidden, we collapse the split pane divider to the right (1.0)
        if (chkShowTree.isSelected()) {
            if (mainSplitPane.getItems().contains(treeContainer)) return;
            mainSplitPane.getItems().add(treeContainer);
            mainSplitPane.setDividerPositions(0.5);
        } else {
            mainSplitPane.getItems().remove(treeContainer);
        }
    }

    // --- UI Helpers ---

    private void resetUI() {
        resetBoardOnly();
        treeVisualizer.reset();
        statusLabel.setText("Ready (N=" + n + ")");
        statusLabel.setStyle("-fx-text-fill: black;");
    }

    private void resetBoardOnly() {
        boardGrid.getChildren().clear();
        double tileSize = Math.min(600 / n, 60); // Dynamic tile sizing

        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                Rectangle tile = new Rectangle(tileSize, tileSize);
                tile.setFill((row + col) % 2 == 0 ? Color.web("#F0D9B5") : Color.web("#B58863"));
                StackPane pane = new StackPane(tile);
                boardGrid.add(pane, col, row);
            }
        }
    }

    private void updateBoard(int row, int col, boolean placing) {
        StackPane pane = getSquare(row, col);
        if (pane == null) return;

        if (placing) {
            Text queen = new Text("♛");
            double fontSize = ((Rectangle)pane.getChildren().get(0)).getWidth() * 0.7;
            queen.setFont(Font.font("Segoe UI Symbol", fontSize));
            pane.getChildren().add(queen);
        } else {
            pane.getChildren().removeIf(node -> node instanceof Text);
        }
    }

    private StackPane getSquare(int row, int col) {
        for (var node : boardGrid.getChildren()) {
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == col) {
                return (StackPane) node;
            }
        }
        return null;
    }

    private void setControlsLocked(boolean locked) {
        solveButton.setDisable(locked);
        sizeSpinner.setDisable(locked);
        kSpinner.setDisable(locked || !rbMixed.isSelected());
        stopButton.setDisable(!locked);
        resetButton.setDisable(locked);
    }

    private void log(String msg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText("[" + time + "] " + msg + "\n");
    }
}