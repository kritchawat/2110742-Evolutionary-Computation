package net.kcww.ec.nqueens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Handles logic for Backtracking, Las Vegas, and Mixed algorithms.
 */
public class NQueensSolver implements Runnable {

    public enum Mode {
        BACKTRACK, LAS_VEGAS, MIXED
    }

    public interface SolverListener {
        void onStep(int row, int col, boolean placing, String nodeId, String parentId);
        void onSolutionFound(int[] solution);
        void onLog(String message);
        void onFinished();
    }

    private final int n;
    private final Mode mode;
    private final int k; // For Mixed mode
    private final SolverListener listener;
    
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private volatile int delayMs = 50;
    
    private final Object pauseLock = new Object();
    private final Random random = new Random();

    public NQueensSolver(int n, Mode mode, int k, SolverListener listener) {
        this.n = n;
        this.mode = mode;
        this.k = k;
        this.listener = listener;
    }

    public void setDelay(int delayMs) {
        this.delayMs = delayMs;
    }

    public void stop() {
        running = false;
        resume(); // Unpause to allow exit
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }
    
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void run() {
        listener.onLog("Starting " + mode + " algorithm for N=" + n + "...");
        int[] board = new int[n];
        Arrays.fill(board, -1);

        long startTime = System.currentTimeMillis();
        boolean found = false;

        if (mode == Mode.LAS_VEGAS) {
            found = solveLasVegas(board);
        } else if (mode == Mode.MIXED) {
            found = solveMixed(board);
        } else {
            found = solveBacktrack(board, 0, "root");
        }

        long duration = System.currentTimeMillis() - startTime;
        if (!found && running) {
            listener.onLog("Algorithm finished. No solution found (or limit reached).");
        } else if (found) {
            listener.onLog("Finished in " + duration + "ms.");
        }
        listener.onFinished();
    }

    // --- Backtracking ---
    private boolean solveBacktrack(int[] board, int row, String parentId) {
        checkPause();
        if (!running) return false;

        if (row == n) {
            listener.onSolutionFound(Arrays.copyOf(board, n));
            listener.onLog("Solution Found: " + Arrays.toString(board));
            return true; // Stop after first solution for this app
        }

        for (int col = 0; col < n; col++) {
            if (!running) return false;

            String nodeId = parentId + "_" + col;
            listener.onStep(row, col, true, nodeId, parentId);
            sleep();

            if (isSafe(board, row, col)) {
                board[row] = col;
                if (solveBacktrack(board, row + 1, nodeId)) {
                    return true;
                }
                board[row] = -1;
            }

            // Backtrack visual
            if (running) {
                listener.onStep(row, col, false, nodeId, parentId);
            }
        }
        return false;
    }

    // --- Las Vegas (Pure Random) ---
    private boolean solveLasVegas(int[] board) {
        int maxRestarts = 1000;
        int restartCount = 0;

        while (running && restartCount < maxRestarts) {
            Arrays.fill(board, -1);
            boolean success = true;
            
            listener.onLog("Las Vegas Attempt #" + (restartCount + 1));

            for (int row = 0; row < n; row++) {
                checkPause();
                if (!running) return false;

                List<Integer> validCols = getValidColumns(board, row);
                if (validCols.isEmpty()) {
                    success = false;
                    break; // Dead end, restart
                }

                int col = validCols.get(random.nextInt(validCols.size()));
                board[row] = col;
                
                String nodeId = "R" + restartCount + "_" + row + "_" + col;
                listener.onStep(row, col, true, nodeId, "root"); // Simplified tree for LV
                sleep();
            }

            if (success) {
                listener.onSolutionFound(Arrays.copyOf(board, n));
                listener.onLog("Solution Found!");
                return true;
            }
            
            // Clear board visually before restart
            listener.onLog("Attempt failed. Restarting...");
            for(int r=0; r<n; r++) if(board[r] != -1) listener.onStep(r, board[r], false, "", "");
            
            restartCount++;
        }
        return false;
    }

    // --- Mixed (Random k, then Backtrack) ---
    private boolean solveMixed(int[] board) {
        // 1. Try to place k queens randomly
        int attempts = 0;
        while(running && attempts < 100) {
            Arrays.fill(board, -1);
            boolean kPlaced = true;
            
            listener.onLog("Mixed Mode: Trying to place first " + k + " queens randomly...");

            for (int row = 0; row < k; row++) {
                List<Integer> validCols = getValidColumns(board, row);
                if (validCols.isEmpty()) {
                    kPlaced = false;
                    break;
                }
                int col = validCols.get(random.nextInt(validCols.size()));
                board[row] = col;
                listener.onStep(row, col, true, "pre_" + row, "root");
                sleep();
            }

            if (kPlaced) {
                listener.onLog("Successfully placed " + k + " queens. Switching to Backtracking.");
                // 2. Solve the rest using backtracking
                if (solveBacktrack(board, k, "pre_" + (k-1))) {
                    return true;
                }
                listener.onLog("Backtracking failed for this random seed. Retrying random part...");
            }
            
            // Cleanup visual
            for(int r=0; r<n; r++) if(board[r] != -1) listener.onStep(r, board[r], false, "", "");
            attempts++;
        }
        return false;
    }

    private List<Integer> getValidColumns(int[] board, int row) {
        List<Integer> cols = new ArrayList<>();
        for (int c = 0; c < n; c++) {
            if (isSafe(board, row, c)) cols.add(c);
        }
        return cols;
    }

    private boolean isSafe(int[] board, int row, int col) {
        for (int i = 0; i < row; i++) {
            int placedCol = board[i];
            if (placedCol == -1) continue;
            if (placedCol == col || Math.abs(i - row) == Math.abs(placedCol - col)) {
                return false;
            }
        }
        return true;
    }

    private void checkPause() {
        synchronized (pauseLock) {
            while (paused && running) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void sleep() {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}