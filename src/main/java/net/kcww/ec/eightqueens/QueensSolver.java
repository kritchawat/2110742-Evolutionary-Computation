package net.kcww.ec.eightqueens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class QueensSolver {

    public interface SolverCallback {
        Object onNodeVisiting(int col, int row, Object parentNode);
        void onNodeSafe(Object node, int row, int col);
        void onNodeBacktrack(Object node, int row, int col);
        void onNodeDeadEnd(Object node, int row, int col);
        void onRestart(int attempt);
        void onLog(String message);
        boolean checkControlFlags();
        void clearBoard();
    }

    public static class SolverResult {
        public boolean success;
        public int[] board;
        public long attempts;
    }

    private final int boardSize;
    private final SolverCallback callback;
    private final Random random = new Random();

    public QueensSolver(int boardSize, SolverCallback callback) {
        this.boardSize = boardSize;
        this.callback = callback;
    }

    // --- Deterministic Backtrack ---
    public SolverResult solveDeterministic() {
        int[] board = new int[boardSize];
        Arrays.fill(board, -1);
        SolverResult res = new SolverResult();
        res.attempts = 1;

        if (placeQueensBacktrack(board, 0, null)) {
            res.success = true;
            res.board = board;
        } else {
            res.success = false;
        }
        return res;
    }

    private boolean placeQueensBacktrack(int[] board, int col, Object parentNode) {
        if (!callback.checkControlFlags()) return false;
        if (col == boardSize) return true;

        for (int row = 0; row < boardSize; row++) {
            if (!callback.checkControlFlags()) return false;

            Object currentNode = callback.onNodeVisiting(col, row, parentNode);

            if (isSafe(board, row, col)) {
                board[col] = row;
                callback.onNodeSafe(currentNode, row, col);

                if (placeQueensBacktrack(board, col + 1, currentNode)) {
                    return true;
                }

                board[col] = -1;
                callback.onNodeBacktrack(currentNode, row, col);
            } else {
                callback.onNodeDeadEnd(currentNode, row, col);
            }
        }
        return false;
    }

    // --- Mixed / Las Vegas ---
    public SolverResult solveMixed(int k) {
        SolverResult res = new SolverResult();
        res.attempts = 0;
        int maxRestarts = 1000;

        while (res.attempts < maxRestarts) {
            if (!callback.checkControlFlags()) return null; 

            res.attempts++;
            callback.onRestart((int) res.attempts);
            callback.clearBoard();

            int[] board = new int[boardSize];
            Arrays.fill(board, -1);

            boolean randomPartSuccess = true;
            Object lastNode = null; 

            // Step 1: Random Placement
            for (int col = 0; col < k; col++) {
                if (!callback.checkControlFlags()) return null;

                List<Integer> safeRows = getSafeRows(board, col);

                if (safeRows.isEmpty()) {
                    randomPartSuccess = false;
                    callback.onLog("Dead end at col " + col + ". Restarting...\n");
                    break;
                }

                int randomRow = safeRows.get(random.nextInt(safeRows.size()));
                board[col] = randomRow;

                Object currentNode = callback.onNodeVisiting(col, randomRow, lastNode);
                callback.onNodeSafe(currentNode, randomRow, col); 
                
                lastNode = currentNode;
            }

            // Step 2: Backtrack rest
            if (randomPartSuccess) {
                if (placeQueensBacktrack(board, k, lastNode)) {
                    res.success = true;
                    res.board = board;
                    return res;
                }
                callback.onLog("Backtrack failed. Restarting...\n");
            }
        }

        res.success = false;
        return res;
    }

    // --- Helpers ---
    private boolean isSafe(int[] board, int row, int col) {
        for (int c = 0; c < col; c++) {
            int r = board[c];
            if (r == -1) continue;
            if (r == row) return false;
            if (Math.abs(r - row) == Math.abs(c - col)) return false;
        }
        return true;
    }

    private List<Integer> getSafeRows(int[] board, int col) {
        List<Integer> safe = new ArrayList<>();
        for (int row = 0; row < boardSize; row++) {
            if (isSafe(board, row, col)) safe.add(row);
        }
        return safe;
    }
}