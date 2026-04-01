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

    private static final Analyzer analyzer = new Analyzer();
    private static final Logger logger = new Logger(500);

    //Queue of all fires waiting to be handled
    private static Queue<FireIncidentEvent> fireQueue;

    // stores all drones known to the scheduler
    private Map<Integer, DroneInfo> drones;

    // Active fires by zone
    private static Map<Integer, FireIncidentEvent> activeEventsByZone;

    private volatile boolean running = true;//Flag to check system is running.
    private GUI gui;


    public enum Status{//The statuses of the scheduler.
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
                System.out.println("[Scheduler] Received: " + msg);

                LocalTime time   = LocalTime.parse(parts[1]);
                int       zoneID = Integer.parseInt(parts[2]);
                Zone      zone   = Zone.findZoneById(zoneID);

                FireIncidentEvent event = new FireIncidentEvent(
                        time, zone,
                        FireIncidentEvent.Status.valueOf(parts[3]),
                        FireIncidentEvent.Severity.valueOf(parts[4]),
                        FireIncidentEvent.FaultType.valueOf(parts[5])
                );

                activeEventsByZone.put(zoneID, event);
                fireQueue.add(event);

                SwingUtilities.invokeLater(() ->
                        gui.updateOrReplaceSquare(zoneID,
                                GUI.severityLetter(event.getSeverity()), Color.RED));

                logger.fireReceived(zoneID);
                assignDrone();
                break;
            /**
             * EXTINGUISHED|zone|droneID
             * Remove fire, update drone water, show green square.
             */
            case "EXTINGUISHED":
                System.out.println("[Scheduler] Received: " + msg);

                int z   = Integer.parseInt(parts[1]);
                int droneID2 = Integer.parseInt(parts[2]);
                DroneInfo drone    = drones.get(droneID2);

                FireIncidentEvent done = activeEventsByZone.remove(z  );
                if (done != null && drone != null) {
                    int used = waterRequired(done);
                    drone.agentRemaining = Math.max(0, drone.agentRemaining - used);
                    Zone.findZoneById(z  ).fireExtinguished();
                    SwingUtilities.invokeLater(() -> {
                        gui.updateOrReplaceSquare(z, "CLEAR_ALL", null);
                    });
                }
                if (drone != null) {
                    drone.currentZone = -1;
                    drone.targetZone  = -1;
                }
                if (activeEventsByZone.isEmpty() && fireQueue.isEmpty()) {
                    logger.shutdown();
                    new Analyzer().run();
                }
                assignDrone();
                logger.fireExtinguished(droneID2, z);
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
                updateDrone(parts, DroneInfo.DroneState.RETURNING,   new Color(128, 0, 128));
                break;
            case "DRONE_CLEAR":
                int droneId = Integer.parseInt(parts[1]);
                int zoneId = Integer.parseInt(parts[2]);

                DroneInfo info = drones.get(droneId);

                if (info != null && info.isFaulty) break;

                SwingUtilities.invokeLater(() ->
                        gui.updateOrReplaceSquare(zoneId, "CLEAR_DRONE", null));
                break;

            case "DRONE_READY":

                droneId = Integer.parseInt(parts[1]);
                logger.droneIdle(droneId);

                drones.putIfAbsent(droneId, new DroneInfo(droneId, 6000 + droneId));

                DroneInfo d = drones.get(droneId);
                if (d.isFaulty) {
                    SwingUtilities.invokeLater(() ->
                            gui.log("Drone " + droneId + " is faulty and cannot be reused")
                    );
                    break;
                }
                d.state          = DroneInfo.DroneState.IDLE;
                d.agentRemaining = d.agentCapacity;
                SwingUtilities.invokeLater(() ->
                        gui.log("[" + LocalTime.now() + "] Drone " + droneId + " is ready."));

                assignDrone();
                break;
            case "DRONE_STUCK":
                int       stuckId    = Integer.parseInt(parts[1]);
                DroneInfo stuckDrone = drones.get(stuckId);
                if (stuckDrone == null) break;

                stuckDrone.isFaulty = true;
                stuckDrone.state    = DroneInfo.DroneState.IDLE;
                // save stuckZone before clearing it
                int stuckZone = Integer.parseInt(parts[2]);

                System.out.println("[Scheduler] Drone " + stuckId + " STUCK at zone " + stuckZone);
                int finalStuckZone1 = stuckZone;

                gui.updateOrReplaceSquare(finalStuckZone1, "CLEAR_DRONE", null);
                gui.updateOrReplaceSquare(finalStuckZone1, "S(" + stuckId + ")", Color.BLUE);

                gui.log("[" + LocalTime.now() + "] Drone " + stuckId
                        + " STUCK (soft fault) — fire re-queued.");


                // Re-queue the fire so another drone handles it
                if (stuckDrone.targetZone != -1) {
                    FireIncidentEvent e = activeEventsByZone.get(stuckDrone.targetZone);
                    if (e != null) {
                        FireIncidentEvent old = activeEventsByZone.get(stuckDrone.targetZone);
                        if (old != null) {
                            FireIncidentEvent retry = new FireIncidentEvent(
                                    old.getTime(),
                                    old.getZone(),
                                    old.getStatus(),
                                    old.getSeverity(),
                                    FireIncidentEvent.FaultType.NONE
                            );
                            fireQueue.add(retry);
                        }
                    }
                }
                stuckDrone.targetZone = -1;
                assignDrone();
                break;
            case "NOZZLE_FAIL":
                int failedId = Integer.parseInt(parts[1]);
                DroneInfo failed   = drones.get(failedId);
                if (failed == null) break;

                System.out.println("[Scheduler] Drone " + failedId + " HARD FAULT (nozzle).");

                // save targetZone before clearing it
                int failZone = Integer.parseInt(parts[2]);
                failed.isFaulty    = true;
                failed.isHardFault = true;
                failed.state       = DroneInfo.DroneState.IDLE;


                gui.updateOrReplaceSquare(failZone, "CLEAR_DRONE", null);

                gui.updateOrReplaceSquare(failZone, "F(" + failedId + ")", Color.MAGENTA);

                gui.log("[" + LocalTime.now() + "] Drone " + failedId
                        + " HARD FAULT (nozzle) — taken offline, fire re-queued.");


                // Re-queue the fire
                if (failZone > 0) {
                    FireIncidentEvent old = activeEventsByZone.get(failZone);
                    if (old != null) {
                        FireIncidentEvent retry = new FireIncidentEvent(
                                old.getTime(),
                                old.getZone(),
                                old.getStatus(),
                                old.getSeverity(),
                                FireIncidentEvent.FaultType.NONE
                        );
                        fireQueue.add(retry);
                    }
                }
                failed.targetZone = -1;
                assignDrone();
                break;


            case "PACKET_LOST":
                int pDroneId = Integer.parseInt(parts[1]);
                int pZone = Integer.parseInt(parts[2]);

                System.out.println("[Scheduler] Packet loss detected from Drone " + pDroneId);

                SwingUtilities.invokeLater(() -> {
                    gui.updateOrReplaceSquare(pZone, "P(" + pDroneId + ")", Color.PINK);
                    gui.log("[" + LocalTime.now() + "] Packet loss from Drone " + pDroneId + " — retrying...");
                });

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
        if (state == DroneInfo.DroneState.APPROACHING) {
            logger.droneArrived(droneId, zoneId);


        }
        logger.droneBusy(droneId);
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
        fireQueue.remove(fire);



        // set dispatch time
        best.dispatchTime = System.currentTimeMillis();
        best.currentZone = fire.getZone().getId();
        best.targetZone  = fire.getZone().getId();
        sendAssign(best, fire);
        gui.log("[" + LocalTime.now() + "] Assigned drone " + best.id + " to zone " + fire.getZone().getId());
        best.state = DroneInfo.DroneState.OUTBOUND;
        best.currentZone = fire.getZone().getId();
        best.targetZone = fire.getZone().getId();
        logger.droneBusy(best.id);
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
                fire.getSeverity()
                + "|" + fire.getFaultType();

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
        logger.droneAssigned(d.id, fire.getZone().getId());
        logger.droneBusy(d.id);
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
            if (d.state != DroneInfo.DroneState.IDLE && !d.isFaulty && d.dispatchTime > 0) {
                long elapsed = now - d.dispatchTime;
                if (elapsed > 20000) {
                    int zone = d.targetZone != -1 ? d.targetZone : d.currentZone;
                    System.out.println("[Scheduler] Timeout: Drone " + d.id + " at zone " + zone);

                    d.isFaulty = true;
                    d.state    = DroneInfo.DroneState.IDLE;
                    SwingUtilities.invokeLater(() -> {
                        if (zone > 0) {
                            gui.updateOrReplaceSquare(zone, "CLEAR_DRONE", null);
                            gui.updateOrReplaceSquare(zone, "S(" + d.id + ")", Color.BLUE);
                        }
                        gui.log("[" + LocalTime.now() + "] Drone " + d.id
                                + " timed out (STUCK) — fire re-queued.");
                    });

                    if (zone > 0) {
                        FireIncidentEvent e = activeEventsByZone.get(zone);
                        FireIncidentEvent retry = new FireIncidentEvent(
                                e.getTime(),
                                e.getZone(),
                                e.getStatus(),
                                e.getSeverity(),
                                FireIncidentEvent.FaultType.NONE
                        );
                        fireQueue.add(retry);
                    }
                    d.targetZone = -1;

                    try { assignDrone(); } catch (UnknownHostException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    // Allow tests to inspect fire queue
    protected Queue<FireIncidentEvent> getFireQueue() {
        return fireQueue;
    }

    // Allow tests to inspect drones
    protected Map<Integer, DroneInfo> getDrones() {
        return drones;
    }

    protected Map<Integer, FireIncidentEvent> getActiveEvents() {
        return activeEventsByZone;
    }

    protected DatagramSocket getSendSocket() { return sendSocket; }
    protected DatagramSocket getReceiveSocket() { return receiveSocket; }

    /**
     * While program is running handle events. Once program is no longer running call the drones to stop.
     */
    @Override
    public void run() {
        Zone.parseZones("Zone_File.csv");
        while (running) {
            checkForTimeouts(); // Checking for timeouts so we can detect stuck drones

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

        };
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.start();
        if (activeEventsByZone.isEmpty() && fireQueue.isEmpty()) {
            logger.shutdown();
            new Analyzer().run();
        }
    }
}