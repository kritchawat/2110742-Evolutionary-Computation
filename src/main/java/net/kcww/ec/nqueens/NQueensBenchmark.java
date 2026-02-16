package net.kcww.ec.nqueens;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NQueensBenchmark {

    private static final String CSV_HEADER = "Algorithm,N,K,RunID,TimeMS,NodesVisited,Success";
    private static final int MIN_N = 8;
    private static final int MAX_N = 24;
    private static final int MAX_N_BACKTRACK = 24;
    private static final int TRIALS = 1000;
    private static final int TIMEOUT_MS = 2000;

    public static void main(String[] args) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Benchmark Results");
        chooser.setSelectedFile(new File("analysis/n_queens_benchmark.csv"));

        int userSelection = chooser.showSaveDialog(null);

        if (userSelection != JFileChooser.APPROVE_OPTION) {
            System.out.println("Save command cancelled by user. Exiting benchmark.");
            return;
        }

        File outputFile = chooser.getSelectedFile();

        new Thread(() -> runBenchmarks(outputFile)).start();
    }

    private static void runBenchmarks(File outputFile) {
        // JIT Warm-up
        warmUp();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println(CSV_HEADER);
            System.out.println("Starting Benchmarks...");
            System.out.println("Saving to: " + outputFile.getAbsolutePath());
            System.out.println("Configuration: N=[" + MIN_N + ", " + MAX_N + "], Trials=" + TRIALS);

            // Loop through Board Sizes
            for (int n = MIN_N; n <= MAX_N; n++) {

                // --- Pure Backtracking (K=0) ---
                if (n <= MAX_N_BACKTRACK) {
                    // Optimization: Backtracking is deterministic. 1000 trials is redundant.
                    // We run 5 trials to get a stable timing average.
                    int deterministicTrials = 5;
                    runAlgorithmSuite(writer, "Backtracking", n, NQueensSolver.Mode.BACKTRACK, 0, deterministicTrials);
                }

                // --- Pure Las Vegas (K=N) ---
                runAlgorithmSuite(writer, "LasVegas", n, NQueensSolver.Mode.LAS_VEGAS, 0, TRIALS);

                // --- Mixed (Random + Backtrack) ---
                // Vary k from 1 to N
                for (int k = 1; k <= n; k++) {
                    runAlgorithmSuite(writer, "Mixed", n, NQueensSolver.Mode.MIXED, k, TRIALS);
                }
            }

            System.out.println("Benchmarks Complete.");

            JOptionPane.showMessageDialog(null,
                    "Benchmark results saved to:\n" + outputFile.getAbsolutePath(),
                    "Benchmark Complete",
                    JOptionPane.INFORMATION_MESSAGE);

            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to run benchmarks: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void warmUp() {
        System.out.print("Warming up JIT...");
        for (int i = 0; i < 50; i++) {
            runSingleTest(8, NQueensSolver.Mode.BACKTRACK, 0);
        }
        System.out.println(" Done.");
    }

    private static void runAlgorithmSuite(PrintWriter writer, String algoName, int n, NQueensSolver.Mode mode, int k, int numTrials) {
        System.out.printf("Running %s (N=%d, K=%d) x %d trials...%n", algoName, n, k, numTrials);

        for (int i = 0; i < numTrials; i++) {
            BenchmarkResult result = runSingleTest(n, mode, k);

            // CSV: Algorithm,N,K,RunID,TimeMS,NodesVisited,Success
            writer.printf("%s,%d,%d,%d,%d,%d,%b%n",
                    algoName, n, k, i, result.timeMs, result.nodesVisited, result.success);
            writer.flush();
        }
    }

    private static BenchmarkResult runSingleTest(int n, NQueensSolver.Mode mode, int k) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        AtomicLong endTime = new AtomicLong();
        AtomicInteger nodeCount = new AtomicInteger(0);
        AtomicInteger successFlag = new AtomicInteger(0); // 0=false, 1=true

        NQueensSolver solver = new NQueensSolver(n, mode, k, new NQueensSolver.SolverListener() {
            @Override
            public void onStep(int row, int col, boolean placing, String nodeId, String parentId) {
                if (placing) {
                    nodeCount.incrementAndGet();
                }
            }

            @Override
            public void onSolutionFound(int[] solution) {
                successFlag.set(1);
            }

            @Override
            public void onLog(String message) {
                // Ignore logs
            }

            @Override
            public void onFinished() {
                endTime.set(System.currentTimeMillis());
                latch.countDown();
            }
        });

        // Speed optimization: No delay
        solver.setDelay(0);

        Thread t = new Thread(solver);
        t.start();

        boolean finished = false;
        try {
            // Enforce Timeout
            finished = latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!finished) {
            solver.stop(); // Force stop
            // Penalty for timeout
            return new BenchmarkResult(TIMEOUT_MS, nodeCount.get(), false);
        }

        long duration = endTime.get() - startTime.get();
        return new BenchmarkResult(duration, nodeCount.get(), successFlag.get() == 1);
    }

    private static class BenchmarkResult {
        long timeMs;
        long nodesVisited;
        boolean success;

        public BenchmarkResult(long timeMs, long nodesVisited, boolean success) {
            this.timeMs = timeMs;
            this.nodesVisited = nodesVisited;
            this.success = success;
        }
    }
}