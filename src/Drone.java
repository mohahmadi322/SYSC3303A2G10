import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

/**
 * The drone subsystem. Takes an event assigned to it by the scheduler and travels to the zone to put the fire out.
 * Signals back to the scheduler when the fire is put out.
 *
 * Implements the runnable interface.
 *
 * @author Mohammad Ahmadi 101267874
 * @date 2026-01-31
 */

public class Drone implements Runnable{
    private Scheduler scheduler; //Instane of scheduler to be used for communication with fire incident subsystem.
    private FireIncidentEvent currentEvent; // The current event assigned to the drone.

    private GUI gui;
    private int droneId;

    private static int MAXLOAD = 15; // The max load drone can hold. Value is from iteration 0.
    private static int STARTING_ZONE_X = 0; //X coordinate of the starting point of the drone.
    private static int STARTING_ZONE_Y = 0;//Y coordinate of the starting point of the drone.
    private  double DRONE_SPEED = 26.4;// Speed of the drone in km/h. Value is from iteration 0.
    private  double TAKE_OFF_TIME = 6;// Take off time of the drone. Value is from iteration 0.
    private  double LANDING_TIME = 4;//Landing time of the drone. Value is from iteration 0.
    private  double TIME_TO_OPEN_NOZZLE = 0.5;//Time it takes the drone to open the nozzle. Value is from iteration 0.
    private volatile boolean running = true;//To check if drone is running or not.

    public enum Status{//The statuses of the drone.
        FLIGHT,
        APPROACHING,
        DROPPING_AGENT,
        IDLE
    }
    private int currentLoad;
    private Status status;// The current status of the drone.

    /**
     * Constructor for drone. Sets the status of the drone to idle.
     * @param s Scheduler the drone will use for communication and to be assigned tasks.
     */
    public Drone (Scheduler s, GUI gui, int id){
        scheduler = s;
        status = Status.IDLE;
        this.gui = gui;
        droneId = id;
        currentLoad = MAXLOAD;
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
    public synchronized void event(FireIncidentEvent e){
        this.currentEvent = e;
        transitionState(Status.FLIGHT);

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
    private void transitionState(Drone.Status s){
        if(status != s){
            gui.log("Scheduler state: " + status + "->" + s);
            this.status = s;
        }
    }

    /**
     * Simulate the drone travelling to the zone. Change the status of the drone during the simulation
     * accordingly.
     */
    public double travelToZone(){
        int zoneId = currentEvent.getZone().getId();
        double travelTime;
        String msg = "Drone " + droneId + " is traveling to zone " + currentEvent.getZone();
        System.out.println(msg);
        gui.log(msg);

        try {
            Thread.sleep((int)(TAKE_OFF_TIME * 1000));
            gui.updateOrReplaceSquare(zoneId, "D(" + droneId + ")", Color.YELLOW);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Drone status: " + status.toString());
        gui.log("Drone status: " + status.toString());

        transitionState(Status.APPROACHING);

        try {
            Thread.sleep((int)(calculateTime(currentEvent.getZone()) * 1000));
            gui.log("Drone " + droneId + " approaching zone " + zoneId);
            System.out.println("Drone status: " + status.toString());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        try {
            Thread.sleep((int)(LANDING_TIME * 1000));
            System.out.println("Drone has landed");
            gui.log("Drone landed");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        scheduler.droneArrived(this);
        travelTime = calculateTime(currentEvent.getZone())  + TAKE_OFF_TIME + LANDING_TIME;
        System.out.println("Took drone " + travelTime + " to get to zone.");
        gui.log("Took drone " + travelTime + " to get to zone.");
        return travelTime;

    }

    /**
     * Simulate the drone dropping water on the fire and signal the scheduler that the drone is ready again for action.
     * @return The fire incident event after the fire has been put out.
     */
    public synchronized FireIncidentEvent dropWater(){
        transitionState(Status.DROPPING_AGENT);

        int waterNeeded = waterRequired(currentEvent);
        int zoneId = currentEvent.getZone().getId();

        /**
         * This uses the formula that was used in iteration 0.
         * The 10 comes from the previous iteration where it was the estimated duration of flow.
         * This calculates the time it would take for the water to fall on the fire.
         */
        double timeLoad = ((currentLoad / 10.0) + TIME_TO_OPEN_NOZZLE);
        currentLoad = currentLoad - waterNeeded;
        try {
            Thread.sleep((int)(timeLoad * 1000));

            String msg = "Drone " + droneId + " dropped " + waterNeeded + "L at zone " + currentEvent.getZone();
            System.out.println(msg);
            gui.log(msg);
            gui.updateOrReplaceSquare(zoneId, "D(" + droneId + ")", new Color(0,128,0));
            msg = "Drone has " + currentLoad + " water left in the tank.";
            gui.log(msg);
            System.out.println(msg);
            Thread.sleep(500);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }


        try {
            // Drone returning (purple)
            gui.updateOrReplaceSquare(zoneId, "D(" + droneId + ")", new Color(128,0,128));
            Thread.sleep(500);
            Thread.sleep((int)(calculateTime(currentEvent.getZone()) * 1000));
            System.out.println("Drone is heading back to origin");
            gui.log("Drone heading back to origin");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        gui.updateOrReplaceSquare(zoneId, "D(" + droneId + ")", null);
        currentEvent.getZone().fireExtinguished();//Change the status of the zone.
        transitionState(Status.IDLE);
        FireIncidentEvent event = currentEvent;
        currentEvent = null;//Empty this event.
        scheduler.registerDrone(this); //Reregister this drone for action.
        scheduler.droneDone(this);
        return event;
    }
    public synchronized void waitForEvent(){
        while (currentEvent == null && running) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Stop the program from running.
     */
    public synchronized void stop(){
        running = false;
        notifyAll();
    }

    /**
     * Register the drone and put out assigned fires until the scheduler is out of events.
     */
    @java.lang.Override
    public void run() {
        scheduler.registerDrone(this);
        while(running){
            waitForEvent();
            if(!running)return;
            FireIncidentEvent e = currentEvent;
            travelToZone();
            dropWater();
            scheduler.firePutOut(e, this);
        }
    }
}

