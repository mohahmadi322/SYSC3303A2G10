import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.time.LocalTime;
import java.util.*;
/**
 *The scheduler class. The channel for communication between the drone and the fire incident subsystem.
 *
 * Takes in incidents from the fire incident subsystem and assigns it to a drone.
 *
 * Implements Runnable interface.
 *
 *  Assign fires to drones realistically:
 *   1. Prefer drones with enough water
 *   2. Among them, choose the closest
 *   3. If tied, choose the one with the most water
 *   4. If none have enough water, choose the closest anyway
 *   5. Apply path-based reassignment rule (Iteration 3 hint)
 *  Communicate with drones via UDP
 *  Update GUI
 *
 * @author Mohammad Ahmadi
 * @author Zeina Mouhtadi
 * @date 2026-01-31
 */
public abstract class Scheduler implements Runnable{

    //Queue of all fires waiting to be handled
    private Queue<FireIncidentEvent> fireQueue;

    // Stores all drones known to the Scheduler.
    // Key = drone ID, Value = DroneInfo (Scheduler-side representation)
    private Map<Integer, DroneInfo> drones;

    // Active fires by zone (used for extinguished logic)
    private Map<Integer, FireIncidentEvent> activeEventsByZone;

    private volatile boolean running = true;//Flag to check system is running.
    private GUI gui;

    protected abstract void initSockets();

    public enum Status{//The statuses of the drone.
        IDLE,
    }

    private Scheduler.Status status;// The current status of the scheduler.

    private DatagramSocket receiveSocket;//Socket for receiving packets.
    private DatagramSocket sendSocket;//Sockets for sending packets.

    /**
     * Internal representation of a drone.
     * Tracks:
     *  - ID
     *  - UDP port
     *  - current state
     *  - current zone
     *  - water remaining
     */


    public class DroneInfo {
        public int id; // Drone ID (unique)
        public int port; // UDP port for sending assignments
        int targetZone;     // Zone the drone is currently heading to
        public DroneState state;
        public int currentZone; // Last known zone of the drone
        public int agentCapacity = 15;
        public int agentRemaining = 15;

        // For handling drone faults
        public boolean isFaulty = false;
        public boolean isHardFault = false;
        public long dispatchTime = 0; // for timeout

        public DroneInfo(int id, int port) {
            this.id = id;
            this.port = port;
            this.state = DroneState.IDLE;
            this.currentZone = -1;
            this.targetZone = -1;
        }

        public enum DroneState {
            IDLE,
            OUTBOUND,
            APPROACHING,
            DROPPING,
            RETURNING
        }
    }

    /**
     * Constructor methods for Scheduler class. Initialized the queues and array.
     * @param gui an instance of the GUI class.
     */
    public Scheduler(GUI gui){
        this.gui = gui;
        fireQueue = new LinkedList<>();
        drones = new HashMap<>();
        activeEventsByZone = new HashMap<>();

        status = Status.IDLE;

        try {
            sendSocket = new DatagramSocket();
            receiveSocket = new DatagramSocket(5000);//Initialize to port 5000.
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
    }
    public Map<Integer, DroneInfo> getDrones(){
        return drones;
    }

    public Queue<FireIncidentEvent> getFireQueue(){
        return fireQueue;
    }

    /**
     * Contains the main logic for communication with other programs.
     * @throws UnknownHostException
     */
    public void sendAndReceive() throws IOException {
        byte[] data = new byte[200];

        DatagramPacket packet = new DatagramPacket(data, data.length);

        receiveSocket.receive(packet);

        String msg = new String(packet.getData(),0,packet.getLength());

        String[] parts = msg.split("\\|");

        switch (parts[0]){
            /**
             * FIRE|time|zone|status|severity
             * Add fire to queue and display red severity square.
             */
            case "FIRE":

                System.out.println("Scheduler received: " + msg);

                LocalTime time = LocalTime.parse(parts[1]);
                int zoneID = Integer.parseInt(parts[2]);

                Zone zone = Zone.findZoneById(zoneID);

                FireIncidentEvent event = new FireIncidentEvent(
                        time,
                        zone,
                        FireIncidentEvent.Status.valueOf(parts[3]),
                        FireIncidentEvent.Severity.valueOf(parts[4]),
                        FireIncidentEvent.FaultType.valueOf(parts[5])
                );

                activeEventsByZone.put(zoneID, event);
                fireQueue.add(event);

                SwingUtilities.invokeLater(() ->
                        gui.updateOrReplaceSquare(
                        zoneID,
                        GUI.severityLetter(event.getSeverity()),
                        Color.RED
                ));

                assignDrone();
                break;
            /**
             * EXTINGUISHED|zone|droneID
             * Remove fire, update drone water, show green square.
             */
            case "EXTINGUISHED":
                System.out.println("Scheduler received: " + msg);

                zoneID = Integer.parseInt(parts[1]);
                int droneID2 = Integer.parseInt(parts[2]);

                FireIncidentEvent done = activeEventsByZone.remove(zoneID);
                DroneInfo drone = drones.get(droneID2);

                if (done != null) {
                    int used = waterRequired(done);
                    drone.agentRemaining -= used;
                    if (drone.agentRemaining < 0) drone.agentRemaining = 0;

                    Zone z = Zone.findZoneById(zoneID);
                    z.fireExtinguished();

                    SwingUtilities.invokeLater(() -> {
                        gui.updateOrReplaceSquare(zoneID, " ", null);   // FULL CLEAR
                        gui.updateOrReplaceSquare(zoneID, " ", Color.GREEN); // Show extinguished
                    });
                }
                drone.currentZone = -1;
                drone.targetZone = -1;

                assignDrone();
                break;

            case "DRONE_OUTBOUND":
                updateDrone(parts, DroneInfo.DroneState.OUTBOUND, Color.YELLOW);
                break;

            case "DRONE_APPROACHING":
                updateDrone(parts, DroneInfo.DroneState.APPROACHING, Color.ORANGE);
                break;

            case "DRONE_DROPPING":
                updateDrone(parts, DroneInfo.DroneState.DROPPING, new Color(0, 128, 0));
                break;

            case "DRONE_RETURNING":
                updateDrone(parts, DroneInfo.DroneState.RETURNING,new Color(0,128,0));
                break;

            case "DRONE_CLEAR":
                int droneId = Integer.parseInt(parts[1]);
                int zoneId = Integer.parseInt(parts[2]);
                SwingUtilities.invokeLater(() ->
                        gui.updateOrReplaceSquare(zoneId, " ", null));
                break;

            case "DRONE_READY":

                droneId = Integer.parseInt(parts[1]);

                drones.putIfAbsent(droneId, new DroneInfo(droneId, 6000 + droneId));

                DroneInfo d = drones.get(droneId);
                d.state = DroneInfo.DroneState.IDLE;
                d.agentRemaining = d.agentCapacity;

                SwingUtilities.invokeLater(() -> gui.log("Drone " + droneId + " is ready"));

                assignDrone();
                break;
        }
    }

    // Updates the GUI to show the drone's current state.
    private void updateDrone(String[] parts, DroneInfo.DroneState state, Color color) {
        int droneId = Integer.parseInt(parts[1]);
        int zoneId = Integer.parseInt(parts[2]);

        DroneInfo info = drones.get(droneId);
        info.state = state;
        info.currentZone = zoneId;

        // Update GUI to show drone at this zone
        SwingUtilities.invokeLater(() ->
                gui.updateOrReplaceSquare(zoneId, "D(" + droneId + ")", color));
    }

    /**
     * Assigns the best available drone to the next fire in queue.
     */
    public synchronized void assignDrone() throws UnknownHostException {
        if (fireQueue.isEmpty()) return;

        FireIncidentEvent fire = fireQueue.peek();
        int required = waterRequired(fire);

        DroneInfo best = null;
        int bestScore = Integer.MAX_VALUE;

        for (DroneInfo d : drones.values()) {
            if (d.state != DroneInfo.DroneState.IDLE) continue;
            if(d.isFaulty) continue; // Prevent faulty drones from being re-assigned

            int score = computeScore(d, fire);

            if (score < bestScore) {
                bestScore = score;
                best = d;
            }
        }

        // Check if best is null before using
        if (best == null) {
            System.out.println("No available drones for zone " + fire.getZone().getId() + ". Fire remains in queue.");
            return; // skip assignment for now
        }

        // Start the timer when a Drone is assigned
        best.dispatchTime = System.currentTimeMillis();

        fireQueue.remove(fire);

        if (fire.getFaultType() == FireIncidentEvent.FaultType.NOZZLE_FAIL) {
            best.isFaulty = true;
            best.isHardFault = true;
            System.out.println("HARD FAULT: Drone " + best.id + " nozzle failure");

            return; // don't assign this drone
        }

        // set dispatch time
        best.dispatchTime = System.currentTimeMillis();

        sendAssign(best, fire);

        best.state = DroneInfo.DroneState.OUTBOUND;
        best.currentZone = fire.getZone().getId();
        best.targetZone = fire.getZone().getId();
    }

    public DatagramSocket getSendSocket(){
        return sendSocket;
    }

    public DatagramSocket getReceiveSocket(){
        return receiveSocket;
    }
/**
 * SEND ASSIGN MESSAGE
 */
        private void sendAssign(DroneInfo d, FireIncidentEvent fire) throws UnknownHostException {
        //build assign message
        String message = "ASSIGN|" +
                fire.getTime() + "|" +
                fire.getZone().getId() + "|" +
                fire.getStatus() + "|" +
                fire.getSeverity();

        byte[] data = message.getBytes();

        //send it to the drone's port
        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                InetAddress.getLocalHost(),
                d.port
        );

        try{
            sendSocket.send(packet);
        }catch(SocketException exception){} catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        SwingUtilities.invokeLater(() ->
                gui.log("Assigned drone " + d.id + " to zone " + fire.getZone().getId())
        );

        System.out.println("Sent: " + message);

        notifyAll();

    }



    /**
     * SCORING:
     *   Prefer drones with enough water
     *   Prefer the closest drone
     *   Prefer drone with more water
     *   Apply path-based reassignment rule
     *   Scoring function for drone selection.
     *   Lower score = better drone.
     */
    private int computeScore(DroneInfo d, FireIncidentEvent f) {

        int fireZone = f.getZone().getId();
        int required = waterRequired(f);

        int droneZone = (d.currentZone == -1 ? 1 : d.currentZone);

        int score = distanceBetweenZones(droneZone, fireZone);

        // Prefer drones with enough water
        if (d.agentRemaining >= required) score -= 50;

        // Prefer drones with more water
        score -= d.agentRemaining;

            // Severity priority: High > Moderate > Low
            int severityWeight = switch (f.getSeverity()) {
                case High -> -200;
                case Moderate -> -100;
                case Low -> 0;
            };
            score += severityWeight;

        return score;
    }

    /***
     *  Computes the distance between two zones in the 4x4 grid.
     *  Used to estimate how far a drone is from a fire.
     * @param a
     * @param b
     * @return
     */
    private int distanceBetweenZones(int a, int b) {
        int ar = (a - 1) / 4, ac = (a - 1) % 4;
        int br = (b - 1) / 4, bc = (b - 1) % 4;
        return Math.abs(ar - br) + Math.abs(ac - bc);
    }

    private int waterRequired(FireIncidentEvent e) {
        return switch (e.getSeverity()) {
            case Low -> 10;
            case Moderate -> 20;
            case High -> 30;
        };
    }

    /**
     * Check for timeouts for detecting stuck drones
     */
    private void checkForTimeouts() {
        long now = System.currentTimeMillis();
        for (DroneInfo d : drones.values()) {
            if(d.state != DroneInfo.DroneState.IDLE && !d.isFaulty) {
                long elapsed = now - d.dispatchTime;
                if(elapsed > 10000) { // 10 sec timeout
                    System.out.println("FAULT: Drone " + d.id + " timed out");
                    d.isFaulty = true;
                    d.state = DroneInfo.DroneState.IDLE;

                    // reassign its fire
                    if(d.targetZone != -1) {
                        FireIncidentEvent e = activeEventsByZone.get(d.targetZone);
                        if(e != null) fireQueue.add(e);
                    }
                }
            }
        }
    }


    /**
     * While program is running handle events. Once program is no longer running call the drones to stop.
     */
    @Override
    public void run() {
        checkForTimeouts(); // Checking for timeouts so we can detect stuck drones
        Zone.parseZones("Zone_File.csv");
        while (running) {
                try {
                    sendAndReceive();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

        }
    }

    /**
     * Main method for Scheduler program.
     * @param args
     */

    public static void main(String[] args) {
        GUI gui = new GUI();
        Scheduler scheduler = new Scheduler(gui) {
            @Override
            protected void initSockets() {

            }
        };
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.start();

    }
}
