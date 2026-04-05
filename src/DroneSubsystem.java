import java.io.IOException;
import java.net.*;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The drone subsystem. Takes an event assigned to it by the scheduler and travels to the zone to put the fire out.
 * Signals back to the scheduler when the fire is put out.
 *
 * Implements the runnable interface.
 *
 * @author Mohammad Ahmadi 101267874
 * @author Zeina Mouhtadi
 * @date 2026-01-31
 */

public class DroneSubsystem implements Runnable{
    private FireIncidentEvent currentEvent; // The current event assigned to the drone.

    private static int MAXLOAD = 15; // The max load drone can hold. Value is from iteration 0.
    private static int STARTING_ZONE_X = 0; //X coordinate of the starting point of the drone.
    private static int STARTING_ZONE_Y = 0;//Y coordinate of the starting point of the drone.
    private  static double DRONE_SPEED = 26.4;// Speed of the drone in km/h. Value is from iteration 0.
    private  static double TAKE_OFF_TIME = 6;// Take off time of the drone. Value is from iteration 0.
    private static double LANDING_TIME = 4;//Landing time of the drone. Value is from iteration 0.
    private static double TIME_TO_OPEN_NOZZLE = 0.5;//Time it takes the drone to open the nozzle. Value is from iteration 0.

    private double fuelLevel = 100.0; // Drone fuel levels


    private volatile boolean running = true;//To check if drone is running or not.

    // Add fault flags
    private boolean isStuck = false;
    private boolean nozzleBroken = false;
    public enum Status{//The statuses of the drone.
        FLIGHT,
        APPROACHING,
        DROPPING_AGENT,
        RETURNING,
        IDLE,
        FAULTED
    }

    public enum Event {
        ASSIGNED_FIRE,
        ARRIVING_AT_ZONE,
        LANDED,
        DROP_COMPLETE,

    }
    private int currentLoad;
    private Status status;// The current status of the drone.

    // 1. Create a static AtomicInteger shared across all instances of this class
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    // 2. Create a final instance variable to hold the unique ID for the specific object
    private final int droneId;
    private DatagramPacket sendPacket, receivePacket;
    private DatagramSocket sendReceiveSocket;
    private FireIncidentEvent.FaultType injectedFault =
            FireIncidentEvent.FaultType.NONE;

    private boolean packetLossActive = false;
    private boolean packetLostOnce = false;

    /**
     * Constructor for drone. Sets the status of the drone to idle.
     *
     */
    public DroneSubsystem() throws UnknownHostException {
        status = Status.IDLE;
        droneId = idCounter.incrementAndGet();
        currentLoad = MAXLOAD;
        try {
            sendReceiveSocket = new DatagramSocket(6000 + droneId);
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
        notifyDroneReady();
    }
    /**
     * Main gameplay loop.
     * Receives packets from host, prompts the user to make a move for the next command to be sent as a DatagramPacket to the host.
     * If the user types "QUIT" the program will close the socket and exit.
     */
    public void sendAndReceive() throws IOException {
        byte[] data = new byte[200];

        receivePacket = new DatagramPacket(data, data.length);

        sendReceiveSocket.receive(receivePacket );


        String msg = new String(receivePacket .getData(),0,receivePacket .getLength());

        String[] parts = msg.split("\\|");
        System.out.println("[Drone " + droneId + "] Received: " + msg);


        // Parse for CSV drone faults
        if(parts[0].equals("DRONE_STUCK")) isStuck = true;
        if(parts[0].equals("NOZZLE_FAIL")) nozzleBroken = true;

        if(parts[0].equals("ASSIGN")){

            LocalTime time = LocalTime.parse(parts[1]);
            int zoneID = Integer.parseInt(parts[2]);

            Zone zone = Zone.findZoneById(zoneID);


            FireIncidentEvent.FaultType fault = (parts.length > 5)
                    ? FireIncidentEvent.FaultType.valueOf(parts[5])
                    : FireIncidentEvent.FaultType.NONE;
            // Inject fault based on event
            if (fault == FireIncidentEvent.FaultType.STUCK) {
                isStuck = true;
            }
            if (fault == FireIncidentEvent.FaultType.NOZZLE_FAIL ||
                    fault == FireIncidentEvent.FaultType.NOZZLE_JAMMED) {
                nozzleBroken = true;
            }
            if (fault == FireIncidentEvent.FaultType.PACKET_LOSS) {
                packetLossActive = true;
            }

            FireIncidentEvent e = new FireIncidentEvent(
                    time,
                    zone,
                    FireIncidentEvent.Status.valueOf(parts[3]),
                    FireIncidentEvent.Severity.valueOf(parts[4]),
                    fault
            );

            event(e);  // existing state machine

        }
        if(parts[0].equals("DONE")){

            stop();

        }
    }

    /**
     * Handle events in the drone state machine.
     * @param e The event to be handled.
     * @throws UnknownHostException
     */

    private synchronized void handleEvent(Event e) throws UnknownHostException {
        switch(status){
            case IDLE:
                if (e == Event.ASSIGNED_FIRE){
                    transitionState(Status.FLIGHT);
                    travelToZone();

                }
                break;
            case FLIGHT:
                if (e == Event.ARRIVING_AT_ZONE){
                    transitionState(Status.APPROACHING);
                    approachingZone();
                }
                break;
            case APPROACHING:
                if(e == Event.LANDED){
                    transitionState(Status.DROPPING_AGENT);
                    dropWater();
                }
                break;
            case RETURNING:
                if(e == Event.LANDED){
                    transitionState(Status.DROPPING_AGENT);
                    dropWater();
                }
                break;
            case DROPPING_AGENT:
                if(e == Event.DROP_COMPLETE){
                    firePutOut();
                    if (currentLoad == 0) {
                        // Tank empty → must return to origin
                        System.out.println("Drone " + droneId + " has 0 water. Returning to origin to refill.");
                        transitionState(Status.RETURNING);
                        returnToOrigin();
                    } else {
                        // Still has water → drone is available
                        transitionState(Status.IDLE);
                        notifyDroneReady();
                    }

                }

                break;
            default:
                break;
        }
    }

    /**
     * Simulates drone heading back to point of origin.
     * @throws UnknownHostException
     */
    private void returnToOrigin() throws UnknownHostException {
        System.out.println("Drone " + droneId + " is heading back to origin");

        try {
            Thread.sleep(3000); // simulate travel time
        } catch (InterruptedException ignored) {}

        System.out.println("Drone " + droneId + " reached origin. Refilling...");
        currentLoad = MAXLOAD;           // refill tank

        fuelLevel = 100.0; // Refill the drone fuel 'at base'
        transitionState(Status.IDLE);    // now available
        notifyDroneReady();              // notify Scheduler
    }

    /**
     * Send schedular a message stating that the fire has been extinguished.
     * @throws UnknownHostException
     */
    private void firePutOut() throws UnknownHostException {
        String msg = "EXTINGUISHED|" + currentEvent.getZone().getId() + "|"+ droneId;
        byte[] data = msg.getBytes();
        sendPacket = new DatagramPacket(data,
                data.length, InetAddress.getLocalHost(), 5000);
        try{
            sendReceiveSocket.send(sendPacket);
            System.out.println(msg + "From drone");
        }catch(Exception e){}
    }

    /**
     * Returns the amount of water the fire needed to put out the fire
     * based on the severity of the event.
     * @param e Fire that needs to be put out.
     * @return The amount of water needed.
     */
    public int waterRequired(FireIncidentEvent e){
        return switch(e.getSeverity()){
            case Low -> 10;
            case Moderate -> 20;
            case High -> 30;
        };

    }

    /**
     * The method called by the scheduler to assign an event to the drone.
     * @param e The fire drone needs to put out.
     */
    public synchronized void event(FireIncidentEvent e) throws UnknownHostException {
        this.currentEvent = e;
        handleEvent(Event.ASSIGNED_FIRE);
        notifyAll();
    }

    /**
     * Calculate the time it takes to get the zone based on the location of the zone.
     * Calculates based on speed of the drone.
     *
     * Calculates the distance by creating a rectangle from starting zone to the end of the fire zone.
     * @param zone The zone fire is located.
     * @return The time it takes to get there.
     */
    public double calculateTime(Zone zone){
        int endX = zone.getEndX();
        int endY = zone.getEndY();
        int length = endX - STARTING_ZONE_X;
        int width =  endY - STARTING_ZONE_Y;
        double hypotneus = Math.sqrt(Math.pow(length, 2) + Math.pow(width, 2));
        double time = DRONE_SPEED / hypotneus;
        return time;

    }

    /**
     * Transitions state to the next one based on what is occurring.
     * @param s next state
     */
    private void transitionState(DroneSubsystem.Status s){
        if(status != s){
            this.status = s;
        }
    }

    /**
     * Simulate the drone travelling to the zone. Change the status of the drone during the simulation
     * accordingly.
     */
    public void travelToZone() throws UnknownHostException {
        int zoneId = currentEvent.getZone().getId();
        System.out.println("[Drone " + droneId + "] Travelling to zone " + zoneId);

        // Subtract from fuel level for drone when it travels
        fuelLevel -= 5;
        if (fuelLevel < 0) fuelLevel = 0; // Handle below 0 fuel level
        updateScheduler("DRONE_OUTBOUND|" + droneId + "|" + zoneId + "|" + fuelLevel);

        try {
            Thread.sleep((int) (TAKE_OFF_TIME * 1000));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        if (isStuck) {
            isStuck = false;
            System.out.println("[Drone " + droneId + "] STUCK mid-flight — reporting fault.");
            updateSchedulerCritical("DRONE_STUCK|" + droneId + "|" + currentEvent.getZone().getId());
            transitionState(Status.FAULTED);
            transitionState(Status.IDLE);
            return;
        }
        handleEvent(Event.ARRIVING_AT_ZONE);
    }

    /**
     * Simulate the drone approaching zone with fire.
     * @return Time it took to get to zone.
     * @throws UnknownHostException
     */
    public double approachingZone() throws UnknownHostException {
        int zoneId = currentEvent.getZone().getId();
        try {
            Thread.sleep((int)(calculateTime(currentEvent.getZone()) * 1000));

            // Decrease from drone fuel level when approaching zone
            fuelLevel -= 10;
            if (fuelLevel < 0) fuelLevel = 0;
            // Update fuel below
            updateScheduler("DRONE_APPROACHING|" + droneId + "|" + zoneId + "|" + fuelLevel);

            System.out.println("DroneSubsystem status: " + status.toString());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        try {
            Thread.sleep((int)(LANDING_TIME * 1000));
            System.out.println("DroneSubsystem has landed");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        double travelTime = calculateTime(currentEvent.getZone())  + TAKE_OFF_TIME + LANDING_TIME;
        System.out.println("Took drone " + travelTime + " to get to zone.");
        handleEvent(Event.LANDED);
        return travelTime;
    }

    /**
     * Simulate the drone dropping water on the fire and signal the scheduler that the drone is ready again for action.
     * @return The fire incident event after the fire has been put out.
     */
    public synchronized FireIncidentEvent dropWater() throws UnknownHostException {

        int zoneId = currentEvent.getZone().getId();
        //Simulate broken nozzle and notify scheduler
        if (nozzleBroken) {
            nozzleBroken = false;
            System.out.println("[Drone " + droneId + "] HARD FAULT — nozzle broken.");
            updateSchedulerCritical("NOZZLE_FAIL|" + droneId + "|" + currentEvent.getZone().getId()); // ADDED zone            transitionState(Status.FAULTED);
            transitionState(Status.FAULTED);
            return currentEvent;
        }

        int waterNeeded = waterRequired(currentEvent);
        if (waterNeeded > currentLoad) waterNeeded = currentLoad;

        double timeLoad = (currentLoad / 10.0) + TIME_TO_OPEN_NOZZLE;
        currentLoad -= waterNeeded;
        if (currentLoad < 0) currentLoad = 0;

        try {
            Thread.sleep((int) (timeLoad * 1000));
            updateScheduler("DRONE_DROPPING|" + droneId + "|" + zoneId + "|" + fuelLevel);
            System.out.println("[Drone " + droneId + "] Dropped " + waterNeeded + "L at zone " + zoneId
                    + ". Remaining: " + currentLoad + "L");
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        updateScheduler("DRONE_CLEAR|" + droneId + "|" + zoneId + "|" + fuelLevel);
        FireIncidentEvent event = currentEvent;
        handleEvent(Event.DROP_COMPLETE);
        return event;
    }



    /**
     * Stop the program from running.
     */
    public synchronized void stop(){
        running = false;
        notifyAll();
    }

    /**
     * Sends packet to scheduler with messages of current drone status.
     * @param msg
     */
    private void updateScheduler(String msg) {
        try {
            // only for packet loss events, simulate drone retrying sending packet.
            if (packetLossActive && !packetLostOnce) {
                packetLostOnce = true;
                System.out.println("[Drone " + droneId + "] PACKET LOSS simulated (dropping first message)");
                updateSchedulerCritical("PACKET_LOST|" + droneId + "|" + currentEvent.getZone().getId());
                new Thread(() -> {
                    try {
                        Thread.sleep(3000); // retry delay
                        System.out.println("[Drone " + droneId + "] RETRYING message...");

                        updateSchedulerCritical(msg);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                return;
            }

            updateSchedulerCritical(msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Critical messages, never dropped (faults, extinguished, ready).
     */
    private void updateSchedulerCritical(String msg) {
        try {
            byte[] data = msg.getBytes();
            sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 5000);
            sendReceiveSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Register the drone and put out assigned fires until the scheduler is out of events.
     */
    @java.lang.Override
    public void run() {
        while(running){
            if(!running)return;
            try {
                sendAndReceive();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Notify scheduler that drone is ready.
     * @throws UnknownHostException
     */
    private void notifyDroneReady() throws UnknownHostException {
        String msg = "DRONE_READY|"+ droneId;
        byte[] data = msg.getBytes();
        sendPacket = new DatagramPacket(data, data.length,InetAddress.getLocalHost(), 5000);
        try{
            sendReceiveSocket.send(sendPacket);
        }catch (Exception e){}


    }
    public static void main(String[] args) throws UnknownHostException {
        Zone.parseZones("Zone_File.csv");

        for (int i=0; i<5; i++) {
            DroneSubsystem drone = new DroneSubsystem();
            Thread droneThread = new Thread(drone);
            droneThread.start();
        }
    }
}