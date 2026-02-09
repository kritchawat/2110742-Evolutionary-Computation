package net.kcww.ec.eightqueen;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;

public class EightQueensFX extends Application implements Initializable {

    private static final int BOARD_SIZE = 8;
    private static final int TILE_SIZE = 50; 
    private static final String QUEEN_ICON = "♛";

    // --- FXML Components ---
    @FXML private BorderPane rootBorderPane;
    @FXML private RadioButton rbBacktrack;
    @FXML private RadioButton rbLasVegas;
    @FXML private RadioButton rbMixed;
    @FXML private Spinner<Integer> mixedSpinner;
    @FXML private Button runButton;
    @FXML private Button pauseButton; 
    @FXML private Button stopButton;
    @FXML private Button clearButton;
    @FXML private Label statusLabel;
    @FXML private GridPane boardGrid;
    
    @FXML private TextArea logArea;
    @FXML private AnchorPane logContainer;
    
    @FXML private SplitPane mainSplitPane;
    @FXML private ScrollPane treeScrollPane;
    @FXML private Pane treePane; 
    
    @FXML private CheckBox chkAnimate;
    @FXML private CheckBox chkShowTree;
    @FXML private CheckBox chkShowLog;
    @FXML private Slider speedSlider;

    // --- State ---
    private Square[][] boardSquares = new Square[BOARD_SIZE][BOARD_SIZE];
    private Visualizer visualizer;

    private volatile boolean isPaused = false;
    private volatile boolean isCancelled = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL fxmlUrl = getClass().getResource("/EightQueens.fxml");
        if (fxmlUrl == null) fxmlUrl = getClass().getResource("EightQueens.fxml");
        if (fxmlUrl == null) throw new IllegalStateException("FXMLLoader Error: 'EightQueens.fxml' not found.");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        primaryStage.setTitle("8-Queens Solver: Animated Tree & Board");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupBoard();
        setupControls();
        visualizer = new Visualizer(treePane);
        
        pauseButton.setDisable(true);
        stopButton.setDisable(true);
    }

    private void setupBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                boolean isDark = (row + col) % 2 == 1;
                Square square = new Square(isDark);
                boardSquares[row][col] = square;
                boardGrid.add(square, col, row);
            }
        }
    }

    private void setupControls() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 4);
        mixedSpinner.setValueFactory(valueFactory);
        mixedSpinner.disableProperty().bind(rbMixed.selectedProperty().not());

        // Tree Visibility
        chkShowTree.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                if (!mainSplitPane.getItems().contains(treeScrollPane)) {
                    mainSplitPane.getItems().add(treeScrollPane);
                    mainSplitPane.setDividerPositions(0.5);
                }
            } else {
                mainSplitPane.getItems().remove(treeScrollPane);
            }
        });

        // Log Visibility
        chkShowLog.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            rootBorderPane.setBottom(isSelected ? logContainer : null);
        });

        // Zoom Logic
        treeScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                event.consume();
                double zoomFactor = (event.getDeltaY() > 0) ? 1.1 : 0.9;
                treePane.setScaleX(treePane.getScaleX() * zoomFactor);
                treePane.setScaleY(treePane.getScaleY() * zoomFactor);
            }
        });
    }

    // --- Actions ---
    @FXML
    private void runSolver() {
        clearBoard();
        visualizer.clear();
        
        runButton.setDisable(true);
        clearButton.setDisable(true);
        pauseButton.setDisable(false);
        stopButton.setDisable(false);
        pauseButton.setText("Pause");
        statusLabel.setText("Running...");

        isPaused = false;
        isCancelled = false;

        int mode = rbBacktrack.isSelected() ? 0 : (rbLasVegas.isSelected() ? 1 : 2);
        int k = mixedSpinner.getValue();

        Task<QueensSolver.SolverResult> task = new Task<>() {
            @Override
            protected QueensSolver.SolverResult call() {
                // Create Solver with implementation of Callback
                QueensSolver solver = new QueensSolver(BOARD_SIZE, new SolverUIBridge());
                
                QueensSolver.SolverResult result = null;

                if (mode == 0) {
                    result = solver.solveDeterministic();
                } else if (mode == 1) {
                    result = solver.solveMixed(BOARD_SIZE); 
                } else {
                    result = solver.solveMixed(k);
                }
                
                return result;
            }
        };
        
        long uiStartTime = System.nanoTime();

        task.setOnSucceeded(e -> {
            QueensSolver.SolverResult result = task.getValue();
            if (result != null) {
                long duration = System.nanoTime() - uiStartTime;
                updateUI(result, duration);
            } else {
                updateUI(null, 0);
            }
            resetButtons();
        });

        task.setOnFailed(e -> {
            logArea.appendText("Error: " + task.getException().getMessage() + "\n");
            task.getException().printStackTrace();
            resetButtons();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML private void togglePause() {
        isPaused = !isPaused;
        pauseButton.setText(isPaused ? "Resume" : "Pause");
        statusLabel.setText(isPaused ? "Paused" : "Running...");
    }

    @FXML private void stopSolver() {
        isCancelled = true;
        isPaused = false;
        statusLabel.setText("Stopping...");
    }

    @FXML private void clearLog() {
        logArea.clear();
    }

    @FXML private void clearBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                boardSquares[row][col].reset();
            }
        }
        treePane.getChildren().clear();
        treePane.setScaleX(1.0);
        treePane.setScaleY(1.0);
    }

    private void resetButtons() {
        runButton.setDisable(false);
        clearButton.setDisable(false);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);
        pauseButton.setText("Pause");
    }

    private void updateUI(QueensSolver.SolverResult result, long durationNano) {
        if (result == null || isCancelled) {
             statusLabel.setText("Cancelled.");
             logArea.appendText("Execution stopped by user.\n");
             return;
        }

        if (result.success) {
            String timeStr = String.format("%.3f ms", durationNano / 1_000_000.0);
            statusLabel.setText("Success! Time: " + timeStr + ", Restarts: " + result.attempts);
            logArea.appendText("Found solution in " + timeStr + "\n");
        } else {
            statusLabel.setText("Failed.");
            logArea.appendText("No solution found.\n");
        }
    }

    // --- Bridge between Solver and UI ---
    private class SolverUIBridge implements QueensSolver.SolverCallback {
        
        @Override
        public Object onNodeVisiting(int col, int row, Object parentNode) {
            sleepSync();
            TreeVisualNode parent = (TreeVisualNode) parentNode;
            TreeVisualNode node = new TreeVisualNode(col, row);
            
            Platform.runLater(() -> {
                visualizer.addNode(node, parent);
                boardSquares[row][col].setCandidate(true);
            });
            return node;
        }

        @Override
        public void onNodeSafe(Object nodeObj, int row, int col) {
            sleepSync();
            TreeVisualNode node = (TreeVisualNode) nodeObj;
            
            Platform.runLater(() -> {
                visualizer.setNodeStatus(node, Color.GREEN);
                boardSquares[row][col].setQueen(true);
            });
        }

        @Override
        public void onNodeBacktrack(Object nodeObj, int row, int col) {
            sleepSync();
            TreeVisualNode node = (TreeVisualNode) nodeObj;
            
            Platform.runLater(() -> {
                visualizer.setNodeStatus(node, Color.RED);
                boardSquares[row][col].setQueen(false);
            });
        }

        @Override
        public void onNodeDeadEnd(Object nodeObj, int row, int col) {
            TreeVisualNode node = (TreeVisualNode) nodeObj;
            Platform.runLater(() -> {
                visualizer.setNodeStatus(node, Color.GRAY);
                boardSquares[row][col].reset();
            });
        }

        @Override
        public void onRestart(int attempt) {
            sleepSync();
            Platform.runLater(() -> {
                visualizer.addRoot("Restart #" + attempt);
            });
        }

        @Override
        public void clearBoard() {
             Platform.runLater(() -> EightQueensFX.this.clearBoard());
        }

        @Override
        public void onLog(String message) {
            Platform.runLater(() -> logArea.appendText(message));
        }

        @Override
        public boolean checkControlFlags() {
            while (isPaused && !isCancelled) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    isCancelled = true;
                }
            }
            return !isCancelled;
        }

        private void sleepSync() {
            if (chkAnimate.isSelected()) {
                try {
                    long delay = (long) speedSlider.getValue();
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    // --- Visual Components ---
    private static class TreeVisualNode {
        final int col;
        final int row;
        double x, y;
        Circle shape;

        public TreeVisualNode(int col, int row) {
            this.col = col;
            this.row = row;
        }
    }

    private class Visualizer {
        private final Pane pane;
        private final double startX = 200;
        private final double startY = 30;
        private final double levelHeight = 60;
        private final Map<Integer, Double> levelXMap = new HashMap<>();

        public Visualizer(Pane pane) {
            this.pane = pane;
        }

        public void clear() {
            pane.getChildren().clear();
            levelXMap.clear();
        }

        public void addRoot(String label) {
            Text text = new Text(10, 20, label);
            text.setFont(Font.font("Arial", 14));
            pane.getChildren().add(text);
            levelXMap.clear();
        }

        public void addNode(TreeVisualNode node, TreeVisualNode parent) {
            double y = startY + (node.col * levelHeight);
            double x;
            if (parent != null) {
                double minX = levelXMap.getOrDefault(node.col, 0.0);
                x = Math.max(parent.x, minX + 30); 
            } else {
                x = levelXMap.getOrDefault(node.col, 20.0) + 30;
            }
            
            levelXMap.put(node.col, x);
            node.x = x;
            node.y = y;

            if (parent != null) {
                Line line = new Line(parent.x, parent.y, node.x, node.y);
                line.setStroke(Color.LIGHTGRAY);
                pane.getChildren().add(0, line);
            }

            Circle c = new Circle(x, y, 10, Color.WHITE);
            c.setStroke(Color.BLACK);
            node.shape = c;
            
            Text t = new Text(x - 3, y + 4, String.valueOf(node.row));
            t.setFont(Font.font(10));
            t.setMouseTransparent(true);

            pane.getChildren().addAll(c, t);
            pane.setPrefWidth(Math.max(pane.getWidth(), x + 50));
            pane.setPrefHeight(Math.max(pane.getHeight(), y + 50));
        }

        public void setNodeStatus(TreeVisualNode node, Color color) {
            if (node != null && node.shape != null) {
                node.shape.setFill(color);
                if (color == Color.GRAY) node.shape.setStroke(Color.LIGHTGRAY);
            }
        }
    }

    private static class Square extends StackPane {
        private final Rectangle bg;
        private final Text icon;
        private final boolean isDark; 
        private final Color darkColor = Color.web("#769656");
        private final Color lightColor = Color.web("#eeeed2");

        public Square(boolean isDark) {
            this.isDark = isDark;
            bg = new Rectangle(TILE_SIZE, TILE_SIZE);
            bg.setFill(isDark ? darkColor : lightColor);
            
            icon = new Text("");
            icon.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 30));
            icon.setFill(Color.BLACK);
            
            getChildren().addAll(bg, icon);
        }

        public void setQueen(boolean hasQueen) {
            icon.setText(hasQueen ? QUEEN_ICON : "");
            icon.setFill(Color.BLACK);
            bg.setFill(isDark ? darkColor : lightColor);
        }
        
        public void setCandidate(boolean isCandidate) {
            if (isCandidate) {
                bg.setFill(Color.YELLOW);
                icon.setText("?");
                icon.setFill(Color.DARKGOLDENROD);
            } else {
                reset();
            }
        }

        public void reset() {
            icon.setText("");
            bg.setFill(isDark ? darkColor : lightColor);
        }
    }
}