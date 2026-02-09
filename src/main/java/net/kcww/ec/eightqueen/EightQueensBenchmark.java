package net.kcww.ec.eightqueen;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class EightQueensBenchmark {

    private static final int BOARD_SIZE = 8;
    private static final int TRIALS = 10000;
    private static final int WARMUP = 1000;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   8-Queens Las Vegas Algorithm Benchmark");
        System.out.println("==================================================\n");

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Benchmark Results");
        fileChooser.setSelectedFile(new File("analysis/benchmark_results.csv"));

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection != JFileChooser.APPROVE_OPTION) {
            System.out.println("Save command cancelled by user. Exiting benchmark.");
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileToSave))) {
            // Write CSV Header
            writer.println("k,time_ns,restarts");
            System.out.println("Running experiments and writing to " + fileToSave.getAbsolutePath() + "...");

            // 1. Benchmark Deterministic Backtracking (k=0)
            System.out.print("Running k=0 (Deterministic)... ");
            runBenchmark(0, true, writer);
            System.out.println("Done.");

            // 2. Benchmark Mixed & Pure Las Vegas (k = 1 to 8)
            for (int k = 1; k <= BOARD_SIZE; k++) {
                System.out.print("Running k=" + k + "... ");
                runBenchmark(k, false, writer);
                System.out.println("Done.");
            }

            System.out.println("\nBenchmark complete. Data saved to " + fileToSave.getName());
            System.out.println("You can now open 'Analysis.ipynb' to visualize the results.");

        } catch (IOException e) {
            System.err.println("Error writing to CSV file: " + e.getMessage());
        }
    }

    private static void runBenchmark(int k, boolean isDeterministic, PrintWriter writer) {
        // Silent Callback
        QueensSolver.SolverCallback silentCallback = new QueensSolver.SolverCallback() {
            @Override
            public Object onNodeVisiting(int col, int row, Object parentNode) {
                return null;
            }

            @Override
            public void onNodeSafe(Object node, int row, int col) {
            }

            @Override
            public void onNodeBacktrack(Object node, int row, int col) {
            }

            @Override
            public void onNodeDeadEnd(Object node, int row, int col) {
            }

            @Override
            public void onRestart(int attempt) {
            }

            @Override
            public void onLog(String message) {
            }

            @Override
            public boolean checkControlFlags() {
                return true;
            }

            @Override
            public void clearBoard() {
            }
        };

        QueensSolver solver = new QueensSolver(BOARD_SIZE, silentCallback);

        // JIT Warmup (Run but don't record)
        for (int i = 0; i < WARMUP; i++) {
            if (isDeterministic) solver.solveDeterministic();
            else solver.solveMixed(k);
        }

        // Actual Benchmark
        for (int i = 0; i < TRIALS; i++) {
            long start = System.nanoTime();
            QueensSolver.SolverResult result;

            if (isDeterministic) {
                result = solver.solveDeterministic();
            } else {
                result = solver.solveMixed(k);
            }
            long end = System.nanoTime();
            long duration = end - start;
            long restarts = result.attempts - 1;

            // Write raw data to CSV: k, time, restarts
            writer.println(k + "," + duration + "," + restarts);
        }
    }
}