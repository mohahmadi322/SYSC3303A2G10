import java.util.*;

public class Scheduler implements Runnable{
    private Queue<FireIncidentEvent> incidentQueue;
    private Queue<Drone> availableDrones;

    private ArrayList<Drone> allDrones;
    FireIncidentSubsystem fireIncidentSubsystem;
    private volatile boolean running = true;
    public Scheduler(){
        incidentQueue = new LinkedList<>();
        availableDrones = new LinkedList<>();
        allDrones = new ArrayList<>();
        fireIncidentSubsystem = new FireIncidentSubsystem(this);
    }
    public synchronized void registerDrone(Drone drone){
        availableDrones.add(drone);
        allDrones.add(drone);
    }

    public synchronized void newIncident  (FireIncidentEvent fireIncidentEvent){
        if(fireIncidentEvent == null){
            try{
                System.out.println("No new fires at the moment");
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        incidentQueue.add(fireIncidentEvent);
        System.out.println("Scheduler has receivedd new fire event:\n" + fireIncidentEvent.toString() );
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
    public synchronized void firePutOut(FireIncidentEvent e){
        fireIncidentSubsystem.firePutout(e.getZone());
        notifyAll();
    }
    @Override
    public void run() {
        while(running){
            handleEvent();
            if(incidentQueue.isEmpty()){running = false;}
        }
        for (Drone d : allDrones) {
            d.stop();   // sets running=false + notifyAll
        }
        System.out.println("Scheduler thread is exiting");
        return;
    }
}
