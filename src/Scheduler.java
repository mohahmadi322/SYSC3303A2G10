import java.awt.*;
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
    private Queue<Drone> availableDrones;//Queue of available drones.
    private ArrayList<Drone> allDrones;//Array of all the drones.
    FireIncidentSubsystem fireIncidentSubsystem;//The fire incident subsystem.
    private volatile boolean running = true;//Flag to check system is running.
    private GUI gui;

    public enum Status{//The statuses of the drone.
        FIRE_DETECTED,
        DRONE_REQUESTED,
    }

    private Scheduler.Status status;// The current status of the scheduler.

    /**
     * Constructor methods for Scheduler class. Initialized the queues and array.
     * @param gui an instance of the GUI class.
     */
    public Scheduler(GUI gui){
        this.gui = gui;
        incidentQueue = new LinkedList<>();
        availableDrones = new LinkedList<>();
        allDrones = new ArrayList<>();
        fireIncidentSubsystem = new FireIncidentSubsystem(this, gui);
    }
    /**
     * Register a drone.
     * @param drone The drone that to be registered.
     */
    public synchronized void registerDrone(Drone drone){
        availableDrones.add(drone);
        allDrones.add(drone);
    }
    /**
     * Register a new fire from the Fire Incident subsystem.
     * @param fireIncidentEvent The fire that is happening.
     */
    public synchronized void newIncident  (FireIncidentEvent fireIncidentEvent){
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
        status = Status.FIRE_DETECTED;
        gui.log("Scheduler has received new fire event:\n" + fireIncidentEvent);
        // Display the fire on the zone map by placing a red severity square (H/M/L).
        // updateOrReplaceSquare ensures the zone shows ONLY the current fire state
        gui.updateOrReplaceSquare(fireIncidentEvent.getZone().getId(),
                GUI.severityLetter(fireIncidentEvent.getSeverity()), Color.RED);
        notifyAll();
    }

    /**
     * Handles an event by assigning it to a drone to handle.
     */
    public synchronized void handleEvent(){
        //If program is not running, and  either one of the drone or incident queue is empty, then thread waits.
        while((incidentQueue.isEmpty() || availableDrones.isEmpty()) && running){
            try{
                wait();
            }catch (InterruptedException e) {throw new RuntimeException();}
        }
        if(!running)return;
        FireIncidentEvent e = incidentQueue.poll();//Get head of queue.
        Drone drone = availableDrones.poll();

        drone.event(e);
        status = Status.DRONE_REQUESTED;
    }

    /**
     * Fire has been put out. Notify GUI and FireIncidentSubsystem. Drone is added back to available drone queue.
     * @param e The fire that has been extinguished.
     * @param drone The drone that is to be readded to the drone queue.
     */
    public synchronized void firePutOut(FireIncidentEvent e, Drone drone){
        // Extract the zone ID where the fire was extinguished
        int zoneId = e.getZone().getId();
        fireIncidentSubsystem.firePutout(e.getZone());
        gui.updateOrReplaceSquare(zoneId, GUI.severityLetter(e.getSeverity()), null);

        //ADD a green square to show the fire is extinguished
        gui.updateOrReplaceSquare(zoneId, " ", Color.GREEN);
        availableDrones.add(drone);
        notifyAll();
    }

    /**
     * While program is running handle events. Once program is no longer running call the drones to stop.
     */
    @Override
    public void run() {
        while(running) {
            handleEvent();
            if(incidentQueue.isEmpty()){running = false;}
        }
        for (Drone d : allDrones) {
            d.stop();   // sets running=false + notifyAll
        }
    }
}
