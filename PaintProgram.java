import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** A small, self-contained desktop paint program. */
public class PaintProgram extends JFrame {
    private final DrawingCanvas canvas = new DrawingCanvas();
    private final JButton colorButton = new JButton("Color");
    private final JSlider sizeSlider = new JSlider(1, 50, 6);
    private final JLabel sizeLabel = new JLabel("6 px");

    public PaintProgram() {
        super("Java Paint");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(createToolbar(), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        setMinimumSize(new Dimension(820, 620));
        setSize(1050, 760);
        setLocationByPlatform(true);
    }

    private JComponent createToolbar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(new EmptyBorder(7, 8, 7, 8));

        colorButton.setBackground(canvas.getBrushColor());
        colorButton.setOpaque(true);
        colorButton.addActionListener(e -> chooseColor());

        JButton eraser = new JButton("Eraser");
        eraser.addActionListener(e -> canvas.setEraser(!canvas.isEraser()));

        sizeSlider.setPreferredSize(new Dimension(130, 28));
        sizeSlider.addChangeListener(this::changeBrushSize);

        JButton undo = new JButton("Undo");
        undo.addActionListener(e -> canvas.undo());
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> canvas.clear());
        JButton open = new JButton("Open");
        open.addActionListener(e -> openImage());
        JButton save = new JButton("Save PNG");
        save.addActionListener(e -> saveImage());

        bar.add(colorButton);
        bar.addSeparator();
        bar.add(new JLabel("Brush: "));
        bar.add(sizeSlider);
        bar.add(sizeLabel);
        bar.addSeparator();
        bar.add(eraser);
        bar.add(undo);
        bar.add(clear);
        bar.addSeparator();
        bar.add(open);
        bar.add(save);
        return bar;
    }

    private void chooseColor() {
        Color picked = JColorChooser.showDialog(this, "Choose brush color", canvas.getBrushColor());
        if (picked != null) {
            canvas.setBrushColor(picked);
            colorButton.setBackground(picked);
        }
    }

    private void changeBrushSize(ChangeEvent event) {
        int size = sizeSlider.getValue();
        canvas.setBrushSize(size);
        sizeLabel.setText(size + " px");
    }

    private void saveImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save drawing as PNG");
        chooser.setSelectedFile(new File("drawing.png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) file = new File(file.getPath() + ".png");
        try {
            ImageIO.write(canvas.getImage(), "png", file);
        } catch (IOException ex) {
            showError("Could not save the image.", ex);
        }
    }

    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage image = ImageIO.read(chooser.getSelectedFile());
            if (image == null) throw new IOException("Unsupported image format");
            canvas.setImage(image);
        } catch (IOException ex) {
            showError("Could not open that image.", ex);
        }
    }

    private void showError(String message, Exception error) {
        JOptionPane.showMessageDialog(this, message + "\n" + error.getMessage(), "Java Paint", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PaintProgram().setVisible(true));
    }

    private static final class DrawingCanvas extends JPanel {
        private BufferedImage image;
        private final List<BufferedImage> history = new ArrayList<>();
        private Color brushColor = Color.BLACK;
        private int brushSize = 6;
        private boolean eraser;
        private Point previous;

        DrawingCanvas() {
            setBackground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            MouseAdapter painter = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    ensureImage();
                    saveHistory();
                    previous = e.getPoint();
                    drawLine(previous, previous);
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (previous == null) return;
                    Point current = e.getPoint();
                    drawLine(previous, current);
                    previous = current;
                }
                @Override public void mouseReleased(MouseEvent e) { previous = null; }
            };
            addMouseListener(painter);
            addMouseMotionListener(painter);
        }

        Color getBrushColor() { return brushColor; }
        void setBrushColor(Color color) { brushColor = color; eraser = false; }
        void setBrushSize(int size) { brushSize = size; }
        boolean isEraser() { return eraser; }
        void setEraser(boolean enabled) { eraser = enabled; }
        BufferedImage getImage() { ensureImage(); return image; }

        void clear() {
            ensureImage();
            saveHistory();
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.dispose();
            repaint();
        }

        void undo() {
            if (!history.isEmpty()) {
                image = history.remove(history.size() - 1);
                repaint();
            }
        }

        void setImage(BufferedImage source) {
            saveHistory();
            image = new BufferedImage(Math.max(getWidth(), source.getWidth()), Math.max(getHeight(), source.getHeight()), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(source, 0, 0, null);
            g.dispose();
            repaint();
        }

        private void drawLine(Point from, Point to) {
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(eraser ? Color.WHITE : brushColor);
            g.drawLine(from.x, from.y, to.x, to.y);
            g.dispose();
            repaint();
        }

        private void ensureImage() {
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            if (image == null || width > image.getWidth() || height > image.getHeight()) {
                BufferedImage previousImage = image;
                image = new BufferedImage(Math.max(width, previousImage == null ? 1 : previousImage.getWidth()),
                        Math.max(height, previousImage == null ? 1 : previousImage.getHeight()), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, image.getWidth(), image.getHeight());
                if (previousImage != null) g.drawImage(previousImage, 0, 0, null);
                g.dispose();
            }
        }

        private void saveHistory() {
            if (image == null) return;
            BufferedImage snapshot = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            snapshot.getGraphics().drawImage(image, 0, 0, null);
            history.add(snapshot);
            if (history.size() > 20) history.remove(0);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ensureImage();
            g.drawImage(image, 0, 0, null);
        }
    }
}
