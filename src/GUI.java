import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI class for visualizing firefighting drone swarm simulation.
 */
public class GUI extends JFrame {

    private JTextArea logArea;

    // zone id -> cell panel
    private final HashMap<Integer, JPanel> zoneCells = new HashMap<>();

    // zone id -> squares currently shown in that zone
    private final HashMap<Integer, List<JLabel>> zoneSquares = new HashMap<>();

    // drone status panel
    private JPanel droneStatusPanel;
    private final Map<Integer, JLabel> droneStatusLabels = new HashMap<>();

    // overlay animation
    private final Map<Integer, JLabel> droneOverlayLabels = new HashMap<>();
    private final Map<Integer, Timer> droneTimers = new HashMap<>();

    // drones parked in base
    private final Map<Integer, JLabel> baseDroneLabels = new HashMap<>();

    private JLayeredPane layeredPane;
    private JPanel mapPanel;
    private JPanel droneOverlayPanel;
    private JPanel baseStationPanel;

    public GUI() {
        setTitle("Firefighting Drone Swarm");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLayout(new BorderLayout(10, 10));

        // left: drone status
        droneStatusPanel = new JPanel();
        droneStatusPanel.setLayout(new BoxLayout(droneStatusPanel, BoxLayout.Y_AXIS));
        droneStatusPanel.setBorder(BorderFactory.createTitledBorder("Drone Status"));
        droneStatusPanel.setBackground(Color.WHITE);
        droneStatusPanel.setPreferredSize(new Dimension(220, 1100));
        add(new JScrollPane(droneStatusPanel), BorderLayout.WEST);

        // center: map + base + overlay
        JPanel centerWrapper = new JPanel(new BorderLayout(10, 10));

        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(620, 450));

        mapPanel = createMapPanel();
        mapPanel.setBounds(0, 0, 450, 450);

        droneOverlayPanel = new JPanel(null);
        droneOverlayPanel.setOpaque(false);
        droneOverlayPanel.setBounds(0, 0, 620, 450);

        layeredPane.add(mapPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(droneOverlayPanel, JLayeredPane.PALETTE_LAYER);

        centerWrapper.add(layeredPane, BorderLayout.CENTER);
        centerWrapper.add(createBasePanel(), BorderLayout.EAST);

        add(centerWrapper, BorderLayout.CENTER);

        // right: legend
        add(createLegendPanel(), BorderLayout.EAST);

        // bottom: logs
        add(createLogPanel(), BorderLayout.SOUTH);

        setVisible(true);
    }

    private JLabel createSquare(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(color);
        label.setForeground(Color.WHITE);
        label.setPreferredSize(new Dimension(34, 34));
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        return label;
    }

    private int extractDroneId(String label) {
        if (label == null) return -1;
        if (!label.startsWith("D(") || !label.endsWith(")")) return -1;
        try {
            return Integer.parseInt(label.substring(2, label.length() - 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void removeDroneFromAllZones(int droneId) {
        String droneLabel = "D(" + droneId + ")";
        for (int z = 1; z <= 5; z++) {
            List<JLabel> squares = zoneSquares.get(z);
            if (squares != null) {
                squares.removeIf(sq -> droneLabel.equals(sq.getText()));
                updateZone(z);
            }
        }
    }

    /**
     * Adds/replaces/clears squares in a zone.
     * CLEAR_ALL   -> clears everything in the zone
     * CLEAR_DRONE -> clears drone squares only
     * CLEAR_FIRE  -> clears H/M/L/E
     * CLEAR_FAULT -> clears F(...), S(...), P(...)
     */
    public synchronized void updateOrReplaceSquare(int zoneId, String label, Color color) {
        if (zoneId <= 0) return;

        List<JLabel> squares = zoneSquares.get(zoneId);
        if (squares == null) return;

        if (color == null && label.equals("CLEAR_ALL")) {
            squares.clear();
            updateZone(zoneId);
            return;
        }

        if (color == null && label.equals("CLEAR_DRONE")) {
            squares.removeIf(sq -> sq.getText().startsWith("D("));
            updateZone(zoneId);
            return;
        }

        if (color == null && label.equals("CLEAR_FIRE")) {
            squares.removeIf(sq ->
                    sq.getText().equals("H") ||
                            sq.getText().equals("M") ||
                            sq.getText().equals("L") ||
                            sq.getText().equals("E"));
            updateZone(zoneId);
            return;
        }

        if (color == null && label.equals("CLEAR_FAULT")) {
            squares.removeIf(sq ->
                    sq.getText().startsWith("F(") ||
                            sq.getText().startsWith("S(") ||
                            sq.getText().startsWith("P("));
            updateZone(zoneId);
            return;
        }

        if (label.startsWith("D(")) {
            int droneId = extractDroneId(label);
            if (droneId != -1) {
                removeDroneFromAllZones(droneId);
            }
            squares.removeIf(sq -> sq.getText().startsWith("D("));
        } else {
            squares.removeIf(sq -> sq.getText().equals(label));
        }

        squares.add(createSquare(label, color));
        updateZone(zoneId);
    }

    private void updateZone(int zoneId) {
        SwingUtilities.invokeLater(() -> {
            JPanel cell = zoneCells.get(zoneId);
            if (cell == null) return;

            JPanel squarePanel = (JPanel) cell.getClientProperty("squarePanel");
            if (squarePanel == null) return;

            squarePanel.removeAll();
            for (JLabel sq : zoneSquares.get(zoneId)) {
                squarePanel.add(sq);
            }
            squarePanel.revalidate();
            squarePanel.repaint();
        });
    }

    private JPanel createMapPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Zone Map"));
        panel.setBackground(Color.WHITE);

        for (int i = 1; i <= 5; i++) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            cell.setBackground(Color.WHITE);

            JLabel zoneLabel = new JLabel("Z(" + i + ")", SwingConstants.CENTER);
            zoneLabel.setFont(new Font("Arial", Font.BOLD, 16));
            cell.add(zoneLabel, BorderLayout.NORTH);

            JPanel squarePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
            squarePanel.setOpaque(false);
            cell.add(squarePanel, BorderLayout.CENTER);

            cell.putClientProperty("squarePanel", squarePanel);

            zoneCells.put(i, cell);
            zoneSquares.put(i, new ArrayList<>());
            panel.add(cell);
        }


        return panel;
    }

    private JPanel createBasePanel() {
        baseStationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        baseStationPanel.setPreferredSize(new Dimension(160, 160));
        baseStationPanel.setBackground(new Color(220, 220, 220));
        baseStationPanel.setBorder(BorderFactory.createTitledBorder("Base Station"));

        JLabel baseLabel = new JLabel("BASE");
        baseLabel.setFont(new Font("Arial", Font.BOLD, 16));
        baseStationPanel.add(baseLabel);

        return baseStationPanel;
    }

    private JPanel createLegendPanel() {
        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setBorder(BorderFactory.createTitledBorder("Legend"));

        legend.add(createLegendItem(Color.RED, "Active fire (H/M/L)"));
        legend.add(createLegendItem(Color.GREEN, "Fire extinguished (E)"));
        legend.add(createLegendItem(Color.GRAY, "Drone in base / idle"));
        legend.add(createLegendItem(Color.YELLOW, "Drone outbound"));
        legend.add(createLegendItem(Color.ORANGE, "Drone approaching"));
        legend.add(createLegendItem(new Color(0, 128, 0), "Drone dropping agent"));
        legend.add(createLegendItem(new Color(128, 0, 128), "Drone returning"));
        legend.add(createLegendItem(Color.PINK, "Packet loss"));

        legend.add(new JSeparator());

        legend.add(createLegendItem(Color.BLUE, "Stuck fault"));
        legend.add(createLegendItem(Color.MAGENTA, "Hard fault"));

        return legend;
    }

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

    private JScrollPane createLogPanel() {
        logArea = new JTextArea(8, 80);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("System Logs"));
        return scrollPane;
    }

    public static String severityLetter(FireIncidentEvent.Severity severity) {
        return switch (severity) {
            case High -> "H";
            case Moderate -> "M";
            case Low -> "L";
        };
    }

    public void updateDronePosition(int droneId, int x, int y, Color color) {
        JLabel label = droneOverlayLabels.get(droneId);

        if (label == null) {
            label = new JLabel("D" + droneId, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setForeground(Color.WHITE);
            label.setBackground(color);
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            label.setSize(24, 24);
            droneOverlayLabels.put(droneId, label);
            droneOverlayPanel.add(label);
        }

        label.setBackground(color);
        label.setBounds(x, y, 24, 24);
        droneOverlayPanel.revalidate();
        droneOverlayPanel.repaint();
    }

    private void updateBaseDrone(int droneId, Color color) {
        if (baseStationPanel == null) return;

        JLabel label = baseDroneLabels.get(droneId);
        if (label == null) {
            label = new JLabel("D(" + droneId + ")", SwingConstants.CENTER);
            label.setOpaque(true);
            label.setForeground(Color.WHITE);
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            label.setPreferredSize(new Dimension(34, 34));
            baseDroneLabels.put(droneId, label);
            baseStationPanel.add(label);
        }

        label.setBackground(color);
        baseStationPanel.revalidate();
        baseStationPanel.repaint();
    }

    private void removeBaseDrone(int droneId) {
        JLabel label = baseDroneLabels.remove(droneId);
        if (label != null) {
            baseStationPanel.remove(label);
            baseStationPanel.revalidate();
            baseStationPanel.repaint();
        }
    }

    public void clearDroneFromBase(int droneId) {
        SwingUtilities.invokeLater(() -> removeBaseDrone(droneId));
    }

    public Point zoneToPixel(int zoneId) {
        if (zoneId == 0) {
            Point p = SwingUtilities.convertPoint(
                    baseStationPanel.getParent(),
                    baseStationPanel.getLocation(),
                    layeredPane
            );
            return new Point(
                    p.x + baseStationPanel.getWidth() / 2 - 12,
                    p.y + baseStationPanel.getHeight() / 2 - 12
            );
        }

        JPanel cell = zoneCells.get(zoneId);
        Point p = SwingUtilities.convertPoint(cell.getParent(), cell.getLocation(), layeredPane);
        return new Point(
                p.x + cell.getWidth() / 2 - 12,
                p.y + cell.getHeight() / 2 - 12
        );
    }

    public void animateDroneMove(int droneId, int fromZone, int toZone, Color color, boolean leaveStableSquare) {
        Point start = zoneToPixel(fromZone);
        Point end = zoneToPixel(toZone);

        Timer oldTimer = droneTimers.get(droneId);
        if (oldTimer != null && oldTimer.isRunning()) {
            oldTimer.stop();
        }

        removeDroneOverlay(droneId);
        removeBaseDrone(droneId);

        int steps = 20;
        int delay = 40;

        Timer timer = new Timer(delay, null);
        droneTimers.put(droneId, timer);

        final int[] step = {0};

        timer.addActionListener(e -> {
            double t = (double) step[0] / steps;
            int x = (int) (start.x + t * (end.x - start.x));
            int y = (int) (start.y + t * (end.y - start.y));

            updateDronePosition(droneId, x, y, color);

            step[0]++;
            if (step[0] > steps) {
                timer.stop();
                removeDroneOverlay(droneId);

                if (toZone == 0) {
                    updateBaseDrone(droneId, color);
                } else if (leaveStableSquare) {
                    updateOrReplaceSquare(toZone, "D(" + droneId + ")", color);
                }
            }
        });

        timer.start();
    }

    public void updateDroneStatus(int id,
                                  int zoneId,
                                  int agentRemaining,
                                  String state,
                                  boolean isFaulty,
                                  boolean isHardFault,
                                  int fuelPercent) {
        SwingUtilities.invokeLater(() -> {
            String zoneText = (zoneId <= 0) ? "BASE" : ("Zone " + zoneId);

            Color color = new Color(225, 240, 225);

            String text = String.format(
                    "<html><b>Drone %d</b><br>" +
                            "Location: %s<br>" +
                            "Fuel: %d%%<br>" +
                            "Agent: %dL<br>" +
                            "State: %s<br>" +
                            "Fault: %s</html>",
                    id, zoneText, fuelPercent, agentRemaining, state,
                    isHardFault ? "HARD FAULT" : (isFaulty ? "FAULT" : "OK")
            );

            JLabel label = droneStatusLabels.get(id);
            if (label == null) {
                label = new JLabel(text);
                label.setOpaque(true);
                label.setBackground(color);
                label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                label.setPreferredSize(new Dimension(200, 100));
                label.setMaximumSize(new Dimension(200, 100));
                droneStatusLabels.put(id, label);
                droneStatusPanel.add(label);
            } else {
                label.setText(text);
                label.setBackground(color);
            }

            droneStatusPanel.revalidate();
            droneStatusPanel.repaint();
        });
    }

    private void removeDroneOverlay(int droneId) {
        JLabel label = droneOverlayLabels.remove(droneId);
        if (label != null) {
            droneOverlayPanel.remove(label);
            droneOverlayPanel.repaint();
        }

        Timer timer = droneTimers.remove(droneId);
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }

    public void showDroneInBase(int droneId, Color color) {
        SwingUtilities.invokeLater(() -> updateBaseDrone(droneId, color));
    }

    public void clearDroneOverlay(int droneId) {
        SwingUtilities.invokeLater(() -> removeDroneOverlay(droneId));
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }
}