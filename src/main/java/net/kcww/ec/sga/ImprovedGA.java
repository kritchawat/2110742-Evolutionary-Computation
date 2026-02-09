package net.kcww.ec.sga;

import java.util.Random;

public class ImprovedGA {

    // --- Configuration ---
    // increased population to maintain diversity
    static final int POPULATION_SIZE = 50; 
    static final int GENE_LENGTH = 20;
    static final int MAX_GENERATIONS = 1000;
    
    // Mutation rate: Probability that a bit will flip (e.g. 0.015 = 1.5%)
    // Usually 1/GENE_LENGTH is a good starting point.
    static final double MUTATION_RATE = 0.05; 
    static final boolean ELITISM = true;

    // --- Data Structures ---
    static String[] candidate = new String[POPULATION_SIZE];
    static double[] fit = new double[POPULATION_SIZE];
    static Random random = new Random();

    public static void main(String[] args) {
        generateRandomPop();

        int generation = 0;
        boolean satisfied = false;

        System.out.println("Starting evolution...");

        while (!satisfied && generation < MAX_GENERATIONS) {
            generation++;

            // 1. Evaluate & Find Best
            double maxFitness = -1.0;
            int bestIndex = -1;

            for (int i = 0; i < POPULATION_SIZE; i++) {
                fit[i] = evaluate(candidate[i]);
                if (fit[i] > maxFitness) {
                    maxFitness = fit[i];
                    bestIndex = i;
                }
            }

            // Print fewer lines to avoid clutter (every 10 gens or if solution found)
            if (generation % 10 == 0 || maxFitness == 1.0) {
                System.out.printf("Gen %d: Best Fitness = %.2f | Best: %s%n", 
                                  generation, maxFitness, candidate[bestIndex]);
            }

            if (maxFitness == 1.0) {
                System.out.println("Solution found in generation " + generation);
                satisfied = true;
                break;
            }

            // 2. Generate Next Generation
            generateNextGeneration(bestIndex);
        }
    }

    static void generateRandomPop() {
        for (int i = 0; i < POPULATION_SIZE; i++) {
            StringBuilder sb = new StringBuilder(GENE_LENGTH);
            for (int j = 0; j < GENE_LENGTH; j++) {
                sb.append(random.nextBoolean() ? "1" : "0");
            }
            candidate[i] = sb.toString();
        }
    }

    static double evaluate(String individual) {
        int ones = 0;
        for (char c : individual.toCharArray()) {
            if (c == '1') ones++;
        }
        return (double) ones / GENE_LENGTH;
    }

    static void generateNextGeneration(int bestIndex) {
        String[] nextCandidate = new String[POPULATION_SIZE];
        int startIndex = 0;

        // --- ELITISM IMPLEMENTATION ---
        // If enabled, copy the best parent directly to the next generation
        if (ELITISM && bestIndex != -1) {
            nextCandidate[0] = candidate[bestIndex];
            startIndex = 1; // Start crossover loop from index 1
        }

        // --- CROSSOVER LOOP ---
        for (int i = startIndex; i < POPULATION_SIZE; i++) {
            // Pick parents
            String parentA = candidate[pickParent()];
            String parentB = candidate[pickParent()];
            
            // Create ONE child per iteration (simplified for odd/even handling)
            // Note: In standard GA, we usually make 2 children, but handling
            // the array index with Elitism (offset by 1) is messier. 
            // This is a valid variation: Pick 2 parents -> Make 1 child.
            String child = singleCrossOneChild(parentA, parentB);
            
            // --- MUTATION IMPLEMENTATION ---
            child = mutate(child);
            
            nextCandidate[i] = child;
        }

        // Update population
        System.arraycopy(nextCandidate, 0, candidate, 0, POPULATION_SIZE);
    }

    static int pickParent() {
        double totalFitness = 0;
        for (double f : fit) totalFitness += f;
        
        double x = random.nextDouble() * totalFitness;
        double sum = 0;
        for (int i = 0; i < POPULATION_SIZE; i++) {
            sum += fit[i];
            if (sum >= x) return i;
        }
        return POPULATION_SIZE - 1;
    }

    // Helper: Returns just one child (randomly picks left or right side of split)
    static String singleCrossOneChild(String a, String b) {
        int x = 1 + random.nextInt(GENE_LENGTH - 1);
        // Randomly decide if we take (Head A + Tail B) or (Head B + Tail A)
        if (random.nextBoolean()) {
            return a.substring(0, x) + b.substring(x);
        } else {
            return b.substring(0, x) + a.substring(x);
        }
    }

    // --- NEW MUTATION FUNCTION ---
    static String mutate(String individual) {
        char[] genes = individual.toCharArray();
        boolean mutated = false;
        
        for (int i = 0; i < genes.length; i++) {
            // For every gene, small chance to flip
            if (random.nextDouble() < MUTATION_RATE) {
                if (genes[i] == '1') genes[i] = '0';
                else genes[i] = '1';
                mutated = true;
            }
        }
        return new String(genes);
    }
}