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
 * @author Mohammad Ahmadi
 * @date 2026-01-31
 */
public class Scheduler implements Runnable{
    private Queue<FireIncidentEvent> incidentQueue;//Queue of fires that need to be put out.
    private Queue<Integer> availableDrones;//Queue of available drones.
    private ArrayList<Integer> allDrones;//Array of all the drones.
    private volatile boolean running = true;//Flag to check system is running.
    private GUI gui;

    private FireIncidentEvent currEvent;

    public enum Status{//The statuses of the drone.
        IDLE,
        FIRE_DETECTED,
        DRONE_REQUESTED,
        WAIT,
    }

    public enum Event{
        NEW_FIRE,
        DRONE_AVAILABLE,
        DRONE_DISPATCHED,
        FIRE_EXTINGUISHED,
        STOP
    }

    private Scheduler.Status status;// The current status of the scheduler.

    private DatagramSocket receiveSocket;//Socket for receiving packets.
    private DatagramSocket sendSocket;//Sockets for sending packets.

    /**
     * Constructor methods for Scheduler class. Initialized the queues and array.
     * @param gui an instance of the GUI class.
     */
    public Scheduler(GUI gui){
        this.gui = gui;
        incidentQueue = new LinkedList<>();
        availableDrones = new LinkedList<>();
        allDrones = new ArrayList<>();
        status = Status.IDLE;
        availableDrones.add(1);
        allDrones.add(1);

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
            case "FIRE":
                System.out.println("Scheduler received: " + msg);

                LocalTime time = LocalTime.parse(parts[1]);
                int zoneID = Integer.parseInt(parts[2]);

                Zone zone = Zone.findZoneById(zoneID);

                currEvent = new FireIncidentEvent(
                        time,
                        zone,
                        FireIncidentEvent.Status.valueOf(parts[3]),
                        FireIncidentEvent.Severity.valueOf(parts[4])
                );
                gui.updateOrReplaceSquare(
                        zoneID,
                        GUI.severityLetter(currEvent.getSeverity()),
                        Color.RED
                );

                gui.log("Fire detected at zone " + zoneID);

                newIncident(currEvent);

                break;
            case "EXTINGUISHED":
                System.out.println("Scheduler received: " + msg);

                zoneID = Integer.parseInt(parts[1]);
                int droneID = Integer.parseInt(parts[2]);

                zone = Zone.findZoneById(zoneID);

                firePutOut(currEvent,zone, droneID);
                break;
            case "DRONE_OUTBOUND":

                int droneId = Integer.parseInt(parts[1]);
                int zoneId = Integer.parseInt(parts[2]);

                gui.updateOrReplaceSquare(zoneId, "D(" + droneId + ")", Color.YELLOW);
                break;
            case "DRONE_APPROACHING":
                 droneId = Integer.parseInt(parts[1]);
                 zoneId = Integer.parseInt(parts[2]);

                gui.log("DroneSubsystem " + droneId + " approaching zone " + zoneId);
                break;
            case "DRONE_DROPPING":
                droneId = Integer.parseInt(parts[1]);
                zoneId = Integer.parseInt(parts[2]);

                gui.updateOrReplaceSquare(zoneId, "D(" + droneId + ")", new Color(0,128,0));
                break;
            case "DRONE_RETURNING":
                droneId = Integer.parseInt(parts[1]);
                zoneId = Integer.parseInt(parts[2]);

                gui.updateOrReplaceSquare(zoneId, "D(" + droneId + ")", new Color(0,128,0));
                break;
            case "DRONE_CLEAR":

                droneId = Integer.parseInt(parts[1]);
                zoneId = Integer.parseInt(parts[2]);
                gui.updateOrReplaceSquare(zoneId, "D(" + droneId + ")", null);
                break;
            case "DRONE_READY":

                droneId = Integer.parseInt(parts[1]);

                if(!allDrones.contains(droneId)){
                    allDrones.add(droneId);
                }

                availableDrones.add(droneId);

                gui.log("Drone " + droneId + " is ready");

                handleEvent(Event.DRONE_AVAILABLE);
                break;
        }


    }


    /**
     * Register a new fire from the Fire Incident subsystem.
     * @param fireIncidentEvent The fire that is happening.
     */
    public synchronized void newIncident  (FireIncidentEvent fireIncidentEvent) throws UnknownHostException {
        if(fireIncidentEvent == null){//This ensures that a null event is not added.
            try{
                System.out.println("No new fires at the moment");
                gui.log("No new fires at the moment");
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        fireIncidentEvent.getZone().activeFire();
        incidentQueue.add(fireIncidentEvent);
        System.out.println("Scheduler has received new fire event:\n" + fireIncidentEvent.toString() );

        gui.log("Scheduler has received new fire event:\n" + fireIncidentEvent);
        // Display the fire on the zone map by placing a red severity square (H/M/L).
        // updateOrReplaceSquare ensures the zone shows ONLY the current fire state
        gui.updateOrReplaceSquare(fireIncidentEvent.getZone().getId(),
                GUI.severityLetter(fireIncidentEvent.getSeverity()), Color.RED);
        handleEvent(Event.NEW_FIRE);

        notifyAll();
    }

    /**
     * Handles an event by assigning it to a drone to handle.
     */
    public synchronized void assignDrone() throws UnknownHostException {
        //If program is not running, and  either one of the drone or incident queue is empty, then thread waits.
        while((incidentQueue.isEmpty() || availableDrones.isEmpty()) && running){
            try{
                wait();
            }catch (InterruptedException e) {throw new RuntimeException();}
        }
        if(!running)return;
        FireIncidentEvent e = incidentQueue.poll();//Get head of queue.
        int drone = availableDrones.poll();

        String message = "ASSIGN|" +
                e.getTime() + "|" +
                e.getZone().getId() + "|" +
                e.getStatus() + "|" +
                e.getSeverity();

        byte[] data = message.getBytes();

        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                InetAddress.getLocalHost(),
                6000 + drone
        );

        try{
            sendSocket.send(packet);
        }catch(SocketException exception){} catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        System.out.println("Sent: " + message);

        notifyAll();

    }

    /**
     * Fire has been put out. Notify GUI and FireIncidentSubsystem. DroneSubsystem is added back to available drone queue.
     *  @param e the fire that has been extinguished.
     *  @param zone The zone with the fire that has been extinguished.
     * @param drone The drone that is to be readded to the drone queue.
     */
    public synchronized void firePutOut(FireIncidentEvent e, Zone zone, int drone) throws UnknownHostException {
        gui.updateOrReplaceSquare(zone.getId(), GUI.severityLetter(e.getSeverity()), null);
        //ADD a green square to show the fire is extinguished
        gui.updateOrReplaceSquare(zone.getId(), " ", Color.GREEN);
        zone.fireExtinguished();

        availableDrones.add(drone);
        gui.updateOrReplaceSquare(zone.getId(), "D(" + drone + ")", null);
        handleEvent(Event.FIRE_EXTINGUISHED);
        gui.updateOrReplaceSquare(zone.getId(), GUI.severityLetter(e.getSeverity()), null);

        notifyAll();

    }

    /**
     * Returns the current status of the scheduler.
     * @return Status
     */
    public Status getStatus(){return status;}

    /**
     * Handles the events of the state machine and changes the state of the scheduler accordingly.
     * @param e The event.
     * @throws UnknownHostException
     */
    private synchronized void handleEvent (Event e) throws UnknownHostException {

        switch(status){
            case IDLE:
                if(e == Event.NEW_FIRE){
                    transitionState(Status.FIRE_DETECTED);
                    handleEvent(Event.DRONE_AVAILABLE);
                }
                break;

            case FIRE_DETECTED:
                if(e == Event.DRONE_AVAILABLE){
                    if(!availableDrones.isEmpty()){
                        assignDrone();
                        transitionState(Status.DRONE_REQUESTED);
                    }
                }
                break;

            case DRONE_REQUESTED:
                transitionState(Status.WAIT);
                break;

            case WAIT:
                if(e == Event.FIRE_EXTINGUISHED){
                    currEvent = null;

                    transitionState(Status.IDLE);

                }
                break;

        }


    }

    /**
     * Transitions between the states of the scheduler.
     * @param s The current status
     */
    private void transitionState(Status s){
        if(status != s){
            gui.log("Scheduler state: " + status + "->" + s);
            this.status = s;
        }
    }

    /**
     * While program is running handle events. Once program is no longer running call the drones to stop.
     */
    @Override
    public void run() {
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
        Scheduler scheduler = new Scheduler(gui);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.start();

    }
}
