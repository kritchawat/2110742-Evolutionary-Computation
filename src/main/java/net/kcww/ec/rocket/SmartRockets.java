package net.kcww.ec.rocket;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Smart Rockets - Evolution Strategy Visualization
 */
public class SmartRockets extends Application {

    // --- Configuration Constants ---
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;
    private static final int LIFESPAN = 250;
    private static final int POP_SIZE = 50;
    private static final double MUTATION_RATE = 0.02;
    private static final double MAX_FORCE = 0.3;

    private static final Vector2D TARGET = new Vector2D(WIDTH / 2.0, 50);
    private static final double TARGET_R = 16;

    // Obstacle: x, y, w, h
    private static final double OBS_X = 200;
    private static final double OBS_Y = 200;
    private static final double OBS_W = 200;
    private static final double OBS_H = 20;

    // --- State ---
    private Population population;
    private int generation = 1;
    private int age = 0;
    private boolean isRunning = false;
    private int speedMultiplier = 1;

    // UI Elements
    private Label statsLabel;
    private Canvas canvas;

    @Override
    public void start(Stage primaryStage) {
        // Initialize Logic
        population = new Population(POP_SIZE);

        // --- UI Setup ---
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e293b;"); // Slate-800 equivalent

        // Canvas
        canvas = new Canvas(WIDTH, HEIGHT);
        BorderPane canvasPane = new BorderPane(canvas);
        canvasPane.setStyle("-fx-border-color: #334155; -fx-border-width: 4; -fx-background-color: #0f172a;");
        canvasPane.setMaxSize(WIDTH + 8, HEIGHT + 8);
        root.setCenter(canvasPane);

        // Top Stats Bar
        statsLabel = new Label("Gen: 1 | Step: 0 | Success: 0");
        statsLabel.setTextFill(Color.web("#e2e8f0"));
        statsLabel.setFont(Font.font("Monospaced", 16));
        HBox topBar = new HBox(statsLabel);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER);
        root.setTop(topBar);

        // Bottom Controls
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(15));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: #0f172a;");

        Button btnStart = new Button("Start / Pause");
        styleButton(btnStart, "#10b981"); // Emerald
        btnStart.setOnAction(e -> isRunning = !isRunning);

        Button btnReset = new Button("Reset");
        styleButton(btnReset, "#64748b"); // Slate
        btnReset.setOnAction(e -> reset());

        Label speedLabel = new Label("Speed:");
        speedLabel.setTextFill(Color.WHITE);

        Slider speedSlider = new Slider(1, 20, 1);
        speedSlider.setBlockIncrement(1);
        speedSlider.valueProperty().addListener((obs, oldV, newV) -> speedMultiplier = newV.intValue());

        controls.getChildren().addAll(btnStart, btnReset, speedLabel, speedSlider);
        root.setBottom(controls);

        // --- Animation Loop ---
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isRunning) {
                    for (int i = 0; i < speedMultiplier; i++) {
                        updateSimulation();
                    }
                }
                draw();
                updateStats();
            }
        };
        timer.start();

        // Initial Draw
        draw();

        Scene scene = new Scene(root, WIDTH + 40, HEIGHT + 150);
        primaryStage.setTitle("Evolution Strategy - JavaFX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void styleButton(Button btn, String colorHex) {
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 5;");
    }

    private void reset() {
        population = new Population(POP_SIZE);
        generation = 1;
        age = 0;
        isRunning = false;
        draw();
        updateStats();
    }

    private void updateSimulation() {
        population.run(age);
        age++;

        // End of Generation check
        if (age >= LIFESPAN || population.allCrashedOrDone()) {
            population.evaluate();
            population.selection();
            generation++;
            age = 0;
        }
    }

    private void updateStats() {
        int success = population.getSuccessCount();
        statsLabel.setText(String.format("Gen: %d | Step: %d/%d | Success: %d | MaxFit: %.4f",
                generation, age, LIFESPAN, success, population.maxFitness));
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Clear
        gc.setFill(Color.web("#1e293b"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw Target
        gc.setFill(Color.web("#10b981"));
        gc.fillOval(TARGET.x - TARGET_R, TARGET.y - TARGET_R, TARGET_R * 2, TARGET_R * 2);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeOval(TARGET.x - TARGET_R, TARGET.y - TARGET_R, TARGET_R * 2, TARGET_R * 2);

        // Draw Obstacle
        gc.setFill(Color.web("#ef4444"));
        gc.fillRect(OBS_X, OBS_Y, OBS_W, OBS_H);

        // Draw Rockets
        for (Rocket r : population.rockets) {
            r.show(gc);
        }
    }

    // ==========================================
    // INNER CLASSES FOR LOGIC
    // ==========================================

    /**
     * Simple 2D Vector Class
     */
    static class Vector2D {
        double x, y;

        Vector2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void add(Vector2D v) {
            this.x += v.x;
            this.y += v.y;
        }

        static Vector2D add(Vector2D v1, Vector2D v2) {
            return new Vector2D(v1.x + v2.x, v1.y + v2.y);
        }

        static Vector2D sub(Vector2D v1, Vector2D v2) {
            return new Vector2D(v1.x - v2.x, v1.y - v2.y);
        }

        static Vector2D mult(Vector2D v, double n) {
            return new Vector2D(v.x * n, v.y * n);
        }

        double mag() {
            return Math.sqrt(x * x + y * y);
        }

        void normalize() {
            double m = mag();
            if (m != 0) {
                x /= m;
                y /= m;
            }
        }

        void limit(double max) {
            if (mag() > max) {
                normalize();
                x *= max;
                y *= max;
            }
        }

        static double dist(Vector2D v1, Vector2D v2) {
            return Math.sqrt(Math.pow(v2.x - v1.x, 2) + Math.pow(v2.y - v1.y, 2));
        }

        static Vector2D random2D() {
            Random r = new Random();
            double angle = r.nextDouble() * 2 * Math.PI;
            return new Vector2D(Math.cos(angle), Math.sin(angle));
        }
    }

    /**
     * DNA: Sequence of vectors
     */
    static class DNA {
        Vector2D[] genes;

        // Constructor for random DNA
        DNA() {
            genes = new Vector2D[LIFESPAN];
            for (int i = 0; i < LIFESPAN; i++) {
                Vector2D v = Vector2D.random2D();
                // Random magnitude
                v = Vector2D.mult(v, Math.random() * MAX_FORCE);
                genes[i] = v;
            }
        }

        // Constructor for child DNA
        DNA(Vector2D[] newGenes) {
            this.genes = newGenes;
        }

        DNA crossover(DNA partner) {
            Vector2D[] newGenes = new Vector2D[LIFESPAN];
            int mid = new Random().nextInt(LIFESPAN);

            for (int i = 0; i < LIFESPAN; i++) {
                if (i > mid) newGenes[i] = this.genes[i];
                else newGenes[i] = partner.genes[i];
            }
            return new DNA(newGenes);
        }

        void mutation() {
            Random r = new Random();
            for (int i = 0; i < LIFESPAN; i++) {
                if (r.nextDouble() < MUTATION_RATE) {
                    Vector2D v = Vector2D.random2D();
                    genes[i] = Vector2D.mult(v, r.nextDouble() * MAX_FORCE);
                }
            }
        }
    }

    /**
     * Rocket: The agent
     */
    static class Rocket {
        Vector2D pos;
        Vector2D vel;
        Vector2D acc;
        DNA dna;
        double fitness = 0;
        boolean completed = false;
        boolean crashed = false;
        Color color;

        Rocket() {
            this(new DNA());
        }

        Rocket(DNA dna) {
            this.pos = new Vector2D(WIDTH / 2.0, HEIGHT - 30);
            this.vel = new Vector2D(0, 0);
            this.acc = new Vector2D(0, 0);
            this.dna = dna;
            // Random bluish color
            double hue = 180 + Math.random() * 60;
            this.color = Color.hsb(hue, 0.7, 0.8);
        }

        void applyForce(Vector2D force) {
            this.acc.add(force);
        }

        void update(int age) {
            if (completed || crashed) return;

            // distance to target
            double d = Vector2D.dist(this.pos, TARGET);
            if (d < TARGET_R) {
                completed = true;
                pos = new Vector2D(TARGET.x, TARGET.y);
            }

            // Obstacle Hit
            if (pos.x > OBS_X && pos.x < OBS_X + OBS_W &&
                    pos.y > OBS_Y && pos.y < OBS_Y + OBS_H) {
                crashed = true;
            }

            // Wall Hit
            if (pos.x < 0 || pos.x > WIDTH || pos.y < 0 || pos.y > HEIGHT) {
                crashed = true;
            }

            if (!completed && !crashed) {
                applyForce(dna.genes[age]);
                vel.add(acc);
                pos.add(vel);
                acc = new Vector2D(0, 0); // Reset acc
            }
        }

        void calculateFitness() {
            double d = Vector2D.dist(this.pos, TARGET);
            // Map 0-600 dist to 1-0 fitness (inverse)
            this.fitness = 1 / (d + 1);

            if (completed) this.fitness *= 10;
            if (crashed) this.fitness /= 10;
        }

        void show(GraphicsContext gc) {
            gc.save();
            gc.translate(pos.x, pos.y);

            double angle = Math.atan2(vel.y, vel.x);
            gc.rotate(Math.toDegrees(angle)); // JavaFX rotate uses degrees

            if (completed) gc.setFill(Color.web("#facc15")); // Gold
            else if (crashed) gc.setFill(Color.web("#475569")); // Grey
            else gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.6));

            // Draw Triangle
            gc.beginPath();
            gc.moveTo(10, 0);
            gc.lineTo(-6, -5);
            gc.lineTo(-6, 5);
            gc.closePath();
            gc.fill();

            gc.restore();
        }
    }

    /**
     * Population: Manages the swarm
     */
    static class Population {
        Rocket[] rockets;
        List<Rocket> matingPool;
        double maxFitness = 0;

        Population(int size) {
            rockets = new Rocket[size];
            for (int i = 0; i < size; i++) {
                rockets[i] = new Rocket();
            }
            matingPool = new ArrayList<>();
        }

        void run(int age) {
            for (Rocket r : rockets) {
                r.update(age);
            }
        }

        boolean allCrashedOrDone() {
            for (Rocket r : rockets) {
                if (!r.crashed && !r.completed) return false;
            }
            return true;
        }

        int getSuccessCount() {
            int count = 0;
            for (Rocket r : rockets) if (r.completed) count++;
            return count;
        }

        void evaluate() {
            maxFitness = 0;
            for (Rocket r : rockets) {
                r.calculateFitness();
                if (r.fitness > maxFitness) maxFitness = r.fitness;
            }

            // Normalize
            for (Rocket r : rockets) {
                r.fitness /= maxFitness;
            }

            // Create Mating Pool
            matingPool.clear();
            for (Rocket r : rockets) {
                int n = (int) (r.fitness * 100);
                for (int j = 0; j < n; j++) {
                    matingPool.add(r);
                }
            }
        }

        void selection() {
            Random r = new Random();
            Rocket[] newRockets = new Rocket[rockets.length];

            for (int i = 0; i < rockets.length; i++) {
                // If pool is empty (all died horribly), pick random parents from prev gen
                Rocket parentA = matingPool.isEmpty() ? rockets[r.nextInt(rockets.length)]
                        : matingPool.get(r.nextInt(matingPool.size()));
                Rocket parentB = matingPool.isEmpty() ? rockets[r.nextInt(rockets.length)]
                        : matingPool.get(r.nextInt(matingPool.size()));

                DNA childDNA = parentA.dna.crossover(parentB.dna);
                childDNA.mutation();
                newRockets[i] = new Rocket(childDNA);
            }
            rockets = newRockets;
        }
    }
}