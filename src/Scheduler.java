import java.awt.*;
import java.util.*;

public class Scheduler implements Runnable{
    private Queue<FireIncidentEvent> incidentQueue;
    private Queue<Drone> availableDrones;
    private GUI gui;

    private ArrayList<Drone> allDrones;
    FireIncidentSubsystem fireIncidentSubsystem;
    private volatile boolean running = true;
    public Scheduler(GUI gui){
        this.gui = gui;
        incidentQueue = new LinkedList<>();
        availableDrones = new LinkedList<>();
        allDrones = new ArrayList<>();
        fireIncidentSubsystem = new FireIncidentSubsystem(this, gui);
    }
    public synchronized void registerDrone(Drone drone){
        availableDrones.add(drone);
        allDrones.add(drone);
    }

    public synchronized void newIncident  (FireIncidentEvent fireIncidentEvent){
        if(fireIncidentEvent == null){
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
        notifyAll();
    }
    public synchronized void handleEvent(){
        while((incidentQueue.isEmpty() || availableDrones.isEmpty()) && running){
            try{
                wait();
            }catch (InterruptedException e) {throw new RuntimeException();}
        }
        if(!running)return;
        FireIncidentEvent e = incidentQueue.poll();
        Drone drone = availableDrones.poll();
        drone.event(e);
    }
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
    @Override
    public void run() {
        while(running) {
            handleEvent();
        }
        for (Drone d : allDrones) {
            d.stop();   // sets running=false + notifyAll
        }
    }
}
