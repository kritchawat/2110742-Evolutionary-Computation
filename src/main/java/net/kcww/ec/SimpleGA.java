package net.kcww.ec;

import java.util.Random;

public class SimpleGA {

    // --- Configuration ---
    static final int POPULATION_SIZE = 10; // Must be an even number for the step-2 loop
    static final int GENE_LENGTH = 20;     // Length of the binary string
    static final int MAX_GENERATIONS = 1000;

    // --- Data Structures ---
    // Population: an array of individuals (Binary Strings)
    static String[] candidate = new String[POPULATION_SIZE];
    
    // Fitness: keeps fit[.] corresponding to candidate[.]
    static double[] fit = new double[POPULATION_SIZE];

    static Random random = new Random();

    public static void main(String[] args) {
        // 1. Generate random string to be first generation population
        generateRandomPop();

        int generation = 0;
        boolean satisfied = false;

        // Loop: Repeat step 2 to 4 until satisfy
        while (!satisfied && generation < MAX_GENERATIONS) {
            generation++;

            // 2. Evaluate each candidate
            // apply objective function to candidate[i] -> fit[i]
            double maxFitness = 0;
            int bestIndex = 0;
            
            for (int i = 0; i < POPULATION_SIZE; i++) {
                fit[i] = evaluate(candidate[i]);
                
                // Track best for display and termination
                if (fit[i] > maxFitness) {
                    maxFitness = fit[i];
                    bestIndex = i;
                }
            }

            // Print status
            System.out.printf("Gen %d: Best Fitness = %.2f | Best Candidate: %s%n", 
                              generation, maxFitness, candidate[bestIndex]);

            // Termination Check: If we found a perfect string (all 1s)
            if (maxFitness == 1.0) {
                satisfied = true;
                System.out.println("Solution found!");
                break;
            }

            // 3 & 4. Select good parents and Generate next generation
            generateNextGeneration();
        }
    }

    // --- Step 1: Generate Random Population ---
    static void generateRandomPop() {
        for (int i = 0; i < POPULATION_SIZE; i++) {
            StringBuilder sb = new StringBuilder(GENE_LENGTH);
            for (int j = 0; j < GENE_LENGTH; j++) {
                // Randomly append 0 or 1
                sb.append(random.nextBoolean() ? "1" : "0");
            }
            candidate[i] = sb.toString();
        }
    }

    // --- Step 2: Evaluation (Objective Function) ---
    // Problem: "OneMax" - maximize the number of 1s in the string.
    // Returns 0.0 to 1.0
    static double evaluate(String individual) {
        int ones = 0;
        for (char c : individual.toCharArray()) {
            if (c == '1') {
                ones++;
            }
        }
        return (double) ones / GENE_LENGTH;
    }

    // --- Step 3 & 4: Generate Next Generation ---
    static void generateNextGeneration() {
        // This is a second population, used to hold the next generation
        String[] nextCandidate = new String[POPULATION_SIZE];

        // for i = 1 to population size, step 2
        // (Using 0-based index: 0 to size-1, step 2)
        for (int i = 0; i < POPULATION_SIZE; i += 2) {
            
            // 3. Select good parents
            int parentAIndex = pickParent();
            int parentBIndex = pickParent();
            
            String parentA = candidate[parentAIndex];
            String parentB = candidate[parentBIndex];

            // 4. Use single point crossover
            // We generate two children at once as per requirements
            String[] children = singleCross(parentA, parentB);
            
            nextCandidate[i] = children[0];
            
            // Check bounds just in case POP_SIZE is odd (though we set it to 10)
            if (i + 1 < POPULATION_SIZE) {
                nextCandidate[i + 1] = children[1];
            }
        }

        // Copy the next generation back to the old population
        for (int i = 0; i < POPULATION_SIZE; i++) {
            candidate[i] = nextCandidate[i];
        }
    }

    // --- Selection: Fitness Proportional (Roulette Wheel) ---
    static int pickParent() {
        // First, calculate sum of all fitness to determine the range
        double totalFitness = 0;
        for (double f : fit) {
            totalFitness += f;
        }

        // x = random 0..sum of all fitness
        double x = random.nextDouble() * totalFitness;

        // "accumulative sum of fitness" logic
        double sum = 0;
        for (int i = 0; i < POPULATION_SIZE; i++) {
            sum += fit[i];
            // if sum exceeds the random marker, this is the chosen individual
            if (sum >= x) {
                return i;
            }
        }
        // Fallback (should theoretically not be reached due to rounding errors)
        return POPULATION_SIZE - 1;
    }

    // --- Crossover: Single Point ---
    static String[] singleCross(String a, String b) {
        // x = random 1 to (length of candidate - 1)
        // This ensures at least one character is swapped from both sides
        int x = 1 + random.nextInt(GENE_LENGTH - 1);

        // firstchild = copy candidate[a] 1..x concat to copy candidate[b] x+1..end
        String child1 = a.substring(0, x) + b.substring(x);

        // secondchild = copy candidate[b] 1..x concat to copy candidate[a] x+1..end
        String child2 = b.substring(0, x) + a.substring(x);

        return new String[] { child1, child2 };
    }
}