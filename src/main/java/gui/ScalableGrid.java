package gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;

public class ScalableGrid extends JPanel implements MouseWheelListener, KeyListener, ActionListener, MouseListener, MouseMotionListener, ComponentListener {
    private static final int CELL_SIZE = 50;  // Default cell size
    private final boolean[][] matrix;  // The matrix to be displayed (boolean values)
    private double scale = 1.0;  // Scaling factor for zooming
    private double targetScale = 1.0;  // Target scale for smooth zooming
    private double offsetX = 0, offsetY = 0;  // Offset for moving the grid
    private double targetOffsetX = 0, targetOffsetY = 0;  // Target offset for smooth movement

    // Variables to store initial positions for panning
    private int lastMouseX, lastMouseY;
    private boolean panning = false;
    private boolean drawPanning = false;

    public ScalableGrid(boolean[][] matrix) {
        this.matrix = matrix;
        addMouseWheelListener(this);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);
        setFocusable(true);  // To capture key events

        // Setup a timer for smooth movement (60 FPS)
        // Timer for smooth movement
        Timer movementTimer = new Timer(16, this);
        movementTimer.start();

        // Enable double buffering to reduce flickering and lag
        setDoubleBuffered(true);
    }

    private int[] getCellCoordinates(int mouseX, int mouseY) {
        int realX = (int) ((mouseX - offsetX) / scale);
        int realY = (int) ((mouseY - offsetY) / scale);

        // Determine which cell was clicked
        int col = realX / CELL_SIZE;
        int row = realY / CELL_SIZE;

        // Return an array with row and col, or null if out of bounds
        if (row >= 0 && row < matrix.length && col >= 0 && col < matrix[0].length) {
            return new int[]{row, col};
        }
        return null; // return null if the cell is out of bounds
    }

    // Toggle the cell's value
    private void toggleCell(int mouseX, int mouseY) {
        int[] coordinates = getCellCoordinates(mouseX, mouseY);
        if (coordinates != null) {
            int row = coordinates[0];
            int col = coordinates[1];
            matrix[row][col] = !matrix[row][col];
        }
    }

    // Set the cell's value to a specific boolean
    private void setCell(int mouseX, int mouseY, boolean cellValue) {
        int[] coordinates = getCellCoordinates(mouseX, mouseY);
        if (coordinates != null) {
            int row = coordinates[0];
            int col = coordinates[1];
            matrix[row][col] = cellValue;
        }
    }

    // Method to reset position and scale to default
    private void resetPosition() {
        targetOffsetX = 0;
        targetOffsetY = 0;
        targetScale = 1.0;
        calculateInitialFit();  // Recalculate initial fit
    }

    private void calculateInitialFit() {
        // Calculate the optimal scale to fit the grid within the panel
        double panelWidth = getWidth();
        double panelHeight = getHeight();
        double gridWidth = matrix[0].length * CELL_SIZE;
        double gridHeight = matrix.length * CELL_SIZE;

        // Calculate scale factors for both dimensions
        double scaleX = panelWidth / gridWidth;
        double scaleY = panelHeight / gridHeight;

        targetScale = Math.min(scaleX, scaleY);
    }

    // Paint the grid
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smooth rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply scaling transformation
        AffineTransform transform = new AffineTransform();
        transform.translate(offsetX, offsetY);  // Move the grid based on offset
        transform.scale(scale, scale);  // Apply scaling
        g2d.setTransform(transform);

        // Calculate visible grid boundaries (optimization to avoid rendering off-screen cells)
        int startCol = Math.max(0, (int) (-offsetX / scale / CELL_SIZE));
        int startRow = Math.max(0, (int) (-offsetY / scale / CELL_SIZE));
        int endCol = Math.min(matrix[0].length, (int) ((getWidth() - offsetX) / scale / CELL_SIZE) + 1);
        int endRow = Math.min(matrix.length, (int) ((getHeight() - offsetY) / scale / CELL_SIZE) + 1);

        // Draw only the visible part of the grid
        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                // Calculate cell position
                int x = col * CELL_SIZE;
                int y = row * CELL_SIZE;

                // Draw cell background based on boolean value (black for true, white for false)
                if (matrix[row][col]) {
                    g2d.setColor(Color.BLACK);  // True => Black
                } else {
                    g2d.setColor(Color.WHITE);  // False => White
                }
                g2d.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                // Draw cell border
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    // ActionPerformed is called every time the timer ticks (60 times per second)
    @Override
    public void actionPerformed(ActionEvent e) {
        // Smoothly interpolate towards the target offset and scale
        offsetX +=  ((targetOffsetX - offsetX) * 0.1);
        offsetY +=  ((targetOffsetY - offsetY) * 0.1);
        scale += (targetScale - scale) * 0.1;

        repaint();
    }

    // Mouse wheel event for zooming in and out
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getPreciseWheelRotation() < 0) {
            targetScale += 0.1;  // Zoom in
        } else {
            targetScale -= 0.1;  // Zoom out
        }
    }

    // Mouse pressed event
    @Override
    public void mousePressed(MouseEvent e) {
        switch (e.getButton()) {
            case MouseEvent.BUTTON1: {
                toggleCell(e.getX(), e.getY());
                break;
            }
            case MouseEvent.BUTTON2: {
                drawPanning = true;
                break;
            }
            case MouseEvent.BUTTON3: {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                panning = true;
                break;
            }
        }
    }

    // Mouse released event
    @Override
    public void mouseReleased(MouseEvent e) {
        switch (e.getButton()) {
            case MouseEvent.BUTTON2: {
                drawPanning = false;
                break;
            }
            case MouseEvent.BUTTON3: {
                panning = false;
                break;
            }
        }
    }

    // Mouse dragged event for panning
    @Override
    public void mouseDragged(MouseEvent e) {
        if (panning) {
            // Calculate the movement delta and update the target offset
            int deltaX = e.getX() - lastMouseX;
            int deltaY = e.getY() - lastMouseY;
            targetOffsetX += deltaX;
            targetOffsetY += deltaY;

            // Update last known mouse position
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        } else if (drawPanning) {
            setCell(e.getX(), e.getY(),true);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    // Mouse moved event (not used but required for MouseMotionListener)
    @Override
    public void mouseMoved(MouseEvent e) {}

    // Unused but required for MouseListener
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    // Handle arrow keys and home button
    @Override
    public void keyPressed(KeyEvent e) {
        int moveAmount = 20;  // Amount of movement in pixels

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:{
                targetOffsetX -= moveAmount;
                break;
            }
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:{
                targetOffsetX += moveAmount;
                break;
            }
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:{
                targetOffsetY -= moveAmount;
                break;
            }
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:{
                targetOffsetY += moveAmount;
                break;
            }
            case KeyEvent.VK_HOME: {
                resetPosition();
                break;
            }
            case KeyEvent.VK_R: {
                Arrays.stream(matrix).forEach(x -> Arrays.fill(x, false));
                break;
            }
            case KeyEvent.VK_F: {
                Random random = new Random();
                Arrays.stream(matrix).forEach(row -> {
                    for (int i = 0; i < row.length; i++) {
                        row[i] = random.nextBoolean();  // fill with random boolean values
                    }
                });
                break;
            }
        }
    }

    // Unused but required for KeyListener
    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void componentResized(ComponentEvent e) {
        resetPosition();
    }

    //Unused ComponentListener
    @Override
    public void componentMoved(ComponentEvent e) {}

    @Override
    public void componentShown(ComponentEvent e) {}

    @Override
    public void componentHidden(ComponentEvent e) {}

    public static void main(String[] args) {
        // Example matrix (all cells initially set to false, meaning white)
        boolean[][] matrix = new boolean[20][20];

        JFrame frame = new JFrame("Scalable and Moveable Clickable Grid");
        ScalableGrid grid = new ScalableGrid(matrix);
        frame.add(grid);
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
