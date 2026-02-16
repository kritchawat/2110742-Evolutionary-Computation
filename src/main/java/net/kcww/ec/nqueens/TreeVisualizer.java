package net.kcww.ec.nqueens;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

import java.util.*;

/**
 * A Zoomable, Pannable pane that draws the N-Queens search tree using Scene Graph Nodes.
 * Uses a recursive layout algorithm to prevent subtree overlaps.
 * Refactored to use Shapes instead of Canvas to avoid GPU texture limits on large trees.
 */
public class TreeVisualizer extends ScrollPane {
    private final Group contentGroup;
    private final Scale scaleTransform;

    // Tree Model
    private final Map<String, TreeNode> nodeMap = new HashMap<>();
    private TreeNode root;

    // Layout Constants
    private static final double NODE_RADIUS = 10;
    private static final double LEVEL_HEIGHT = 60;
    private static final double MIN_NODE_SPACING = 30; // Minimum horizontal space between leaves

    // Internal class to hold tree structure for layout
    private class TreeNode {
        String id;
        String parentId;
        int colIndex; // The move made (column index)
        List<TreeNode> children = new ArrayList<>();
        double x, y;
        double width; // Calculated subtree width
        boolean isBacktracked = false;

        TreeNode(String id, String parentId, int colIndex) {
            this.id = id;
            this.parentId = parentId;
            this.colIndex = colIndex;
        }
    }

    public TreeVisualizer() {
        this.contentGroup = new Group();
        this.setContent(contentGroup);

        this.scaleTransform = new Scale(1, 1, 0, 0);
        contentGroup.getTransforms().add(scaleTransform);

        this.setPannable(true);
        this.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        this.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        // Zoom Logic
        this.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double delta = 1.2;
                double scale = event.getDeltaY() > 0 ? delta : 1 / delta;
                scaleTransform.setX(scaleTransform.getX() * scale);
                scaleTransform.setY(scaleTransform.getY() * scale);
                event.consume();
            }
        });

        reset();
    }

    public void reset() {
        nodeMap.clear();
        root = new TreeNode("root", null, -1);
        nodeMap.put("root", root);

        performLayoutAndDraw();
    }

    public void updateTree(int row, int col, boolean placing, String nodeId, String parentId) {
        // Run on JavaFX thread to ensure safety if called directly
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateTree(row, col, placing, nodeId, parentId));
            return;
        }

        TreeNode targetNode = null;

        if (!placing) {
            // Visualize backtracking: Mark node as backtracked
            TreeNode node = nodeMap.get(nodeId);
            if (node != null) {
                node.isBacktracked = true;
                performLayoutAndDraw();
                targetNode = node;
            }
        } else {
            // Check if node already exists to prevent duplicate additions
            if (nodeMap.containsKey(nodeId)) {
                return;
            }

            TreeNode parent = nodeMap.get(parentId);
            // Fallback for root children if parentId refers to generic "root" but ID differs
            if (parent == null && "root".equals(parentId)) parent = root;

            if (parent != null) {
                TreeNode newNode = new TreeNode(nodeId, parentId, col);
                nodeMap.put(nodeId, newNode);

                parent.children.add(newNode);
                // Sort children by column index to maintain visual order (left to right)
                parent.children.sort(Comparator.comparingInt(n -> n.colIndex));

                performLayoutAndDraw();
                targetNode = newNode;
            }
        }

        // Auto-scroll to the latest activity
        if (targetNode != null) {
            scrollToNode(targetNode);
        }
    }

    private void scrollToNode(TreeNode node) {
        Platform.runLater(() -> {
            double nodeX = node.x;
            double nodeY = node.y;

            double scaleX = scaleTransform.getX();
            double scaleY = scaleTransform.getY();

            Bounds viewportBounds = getViewportBounds();
            double viewportWidth = viewportBounds.getWidth();
            double viewportHeight = viewportBounds.getHeight();

            Bounds contentBounds = contentGroup.getBoundsInParent();
            double contentWidth = contentBounds.getWidth();
            double contentHeight = contentBounds.getHeight();

            double hRange = contentWidth - viewportWidth;
            double vRange = contentHeight - viewportHeight;

            if (hRange > 0) {
                double hVal = ((nodeX * scaleX) - (viewportWidth / 2)) / hRange;
                setHvalue(Math.max(0, Math.min(getHmax(), hVal)));
            }

            if (vRange > 0) {
                double vVal = ((nodeY * scaleY) - (viewportHeight / 2)) / vRange;
                setVvalue(Math.max(0, Math.min(getVmax(), vVal)));
            }
        });
    }

    private void performLayoutAndDraw() {
        // 1. Calculate Layout (Subtree Widths)
        calculateSubtreeWidth(root);

        // 2. Assign X, Y positions (Centering the tree)
        assignPositions(root, 0, 30);

        // 3. Draw (Rebuild Scene Graph)
        contentGroup.getChildren().clear();

        // Use lists to layer elements: Lines at back, Circles in middle, Text on top
        List<Node> lines = new ArrayList<>();
        List<Node> circles = new ArrayList<>();
        List<Node> labels = new ArrayList<>();

        collectNodesRecursive(root, lines, circles, labels);

        // Add all to group in correct layer order
        contentGroup.getChildren().addAll(lines);
        contentGroup.getChildren().addAll(circles);
        contentGroup.getChildren().addAll(labels);
    }

    // Post-order traversal: Width of a node is the sum of its children's widths
    private void calculateSubtreeWidth(TreeNode node) {
        if (node.children.isEmpty()) {
            node.width = MIN_NODE_SPACING;
        } else {
            double childrenWidth = 0;
            for (TreeNode child : node.children) {
                calculateSubtreeWidth(child);
                childrenWidth += child.width;
            }
            node.width = childrenWidth;
        }
    }

    // Pre-order traversal: Assign actual coordinates based on widths
    private void assignPositions(TreeNode node, double leftX, double y) {
        // Position node in the center of its allocated width
        node.x = leftX + node.width / 2;
        node.y = y;

        double currentLeft = leftX;
        for (TreeNode child : node.children) {
            assignPositions(child, currentLeft, y + LEVEL_HEIGHT);
            currentLeft += child.width;
        }
    }

    private void collectNodesRecursive(TreeNode node, List<Node> lines, List<Node> circles, List<Node> labels) {
        // Create connections to children
        for (TreeNode child : node.children) {
            Line line = new Line(node.x, node.y + NODE_RADIUS, child.x, child.y - NODE_RADIUS);
            line.setStroke(Color.GRAY);
            line.setStrokeWidth(1.0);
            lines.add(line);

            collectNodesRecursive(child, lines, circles, labels);
        }

        // Create the node circle
        Color fill;
        if (node.id.equals("root")) {
            fill = Color.BLACK;
        } else if (node.isBacktracked) {
            fill = Color.LIGHTGRAY;
        } else {
            fill = Color.web("#4CAF50"); // Green
        }

        Circle circle = new Circle(node.x, node.y, NODE_RADIUS);
        circle.setFill(fill);

        // Correctly add only to circles list
        circles.add(circle);

        // Create column index text
        if (!node.id.equals("root")) {
            Text text = new Text(String.valueOf(node.colIndex));
            text.setX(node.x - 3);
            text.setY(node.y + 4);
            text.setFill(node.isBacktracked ? Color.DARKGRAY : Color.WHITE);
            text.setFont(Font.font("System", 10)); // Ensure readable font size
            labels.add(text);
        }
    }
}