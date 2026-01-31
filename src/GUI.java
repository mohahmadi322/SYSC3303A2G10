import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * GUI class for visualizing firefighting drone swarm simulation.
 * Displays zone map, drone/fire status, legend, and system logs.
 */
public class GUI extends JFrame {

    private JTextArea logArea; // Text area for system logs
    private HashMap<Integer, JPanel> zoneCells = new HashMap<>(); // Maps zone ID to its panel
    private HashMap<Integer, List<JLabel>> zoneSquares = new HashMap<>(); // Tracks squares per zone

    public GUI() {
        setTitle("Firefighting Drone Swarm");
        setSize(1500, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(createControlPanel(), BorderLayout.NORTH);
        add(createMapPanel(), BorderLayout.CENTER);
        add(createLegendPanel(), BorderLayout.EAST);
        add(createLogPanel(), BorderLayout.SOUTH);

        // Initialize square lists for each zone
        for (int i = 1; i <= 9; i++) {
            zoneSquares.put(i, new ArrayList<>());
        }
        setVisible(true);
    }

    // Creates a labeled square with specified color
    private JLabel createSquare(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(color);
        label.setForeground(Color.WHITE);
        label.setPreferredSize(new Dimension(40, 40));
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        return label;
    }

    // Replaces or removes a square in a zone
    public void updateOrReplaceSquare(int zoneId, String label, Color color) {
        List<JLabel> squares = zoneSquares.get(zoneId);
        if (squares == null) return;

        // Remove any square with matching label
        squares.removeIf(sq -> sq.getText().equals(label));

        // Add new square if color is provided
        if (color != null) {
            squares.add(createSquare(label, color));
        }

        updateZone(zoneId);
    }

    // Refreshes the visual panel for a zone
    private void updateZone(int zoneId) {
        SwingUtilities.invokeLater(() -> {
            JPanel cell = zoneCells.get(zoneId);
            JPanel squarePanel = (JPanel) cell.getClientProperty("squarePanel");

            squarePanel.removeAll();
            for (JLabel sq : zoneSquares.get(zoneId)) {
                squarePanel.add(sq);
            }

            squarePanel.revalidate();
            squarePanel.repaint();
        });
    }

    // Control panel with simulation buttons
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JButton("Load Incident File"));
        panel.add(new JButton("Start Simulation"));
        return panel;
    }

    // Creates the zone map grid with labeled cells
    private JPanel createMapPanel() {
        JPanel mapPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        mapPanel.setBorder(BorderFactory.createTitledBorder("Zone Map"));

        for (int i = 1; i <= 9; i++) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            cell.setBackground(Color.WHITE);

            JLabel zoneLabel = new JLabel("Z(" + i + ")", SwingConstants.CENTER);
            zoneLabel.setFont(new Font("Arial", Font.BOLD, 16));
            cell.add(zoneLabel, BorderLayout.NORTH);

            JPanel squarePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
            squarePanel.setOpaque(false);
            cell.add(squarePanel, BorderLayout.CENTER);

            cell.putClientProperty("squarePanel", squarePanel);

            mapPanel.add(cell);
            zoneCells.put(i, cell);
        }

        return mapPanel;
    }

    // Legend panel explaining square colors
    private JPanel createLegendPanel() {
        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setBorder(BorderFactory.createTitledBorder("Legend"));

        legend.add(createLegendItem(Color.RED, "Active fire (H/M/L)"));
        legend.add(createLegendItem(Color.GREEN, "Extinguished fire"));
        legend.add(createLegendItem(Color.YELLOW, "Drone outbound"));
        legend.add(createLegendItem(new Color(0,128,0), "Drone extinguishing"));
        legend.add(createLegendItem(new Color(128,0,128), "Drone returning"));

        return legend;
    }

    // Helper for creating a legend item
    private JPanel createLegendItem(Color color, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel colorBox = new JLabel("     ");
        colorBox.setOpaque(true);
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(20, 20));
        JLabel label = new JLabel(text);
        item.add(colorBox);
        item.add(label);
        return item;
    }

    // Log panel for system messages
    private JScrollPane createLogPanel() {
        logArea = new JTextArea(8, 80);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("System Logs"));
        return scrollPane;
    }

    //Converts fire severity enum to display letter
    public static String severityLetter(FireIncidentEvent.Severity severity) {
        return switch (severity) {
            case High -> "H";
            case Moderate -> "M";
            case Low -> "L";
        };
    }

    // Appends a message to the log panel
    public void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }
}
