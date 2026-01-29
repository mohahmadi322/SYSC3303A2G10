import java.util.*;

public class Scheduler implements Runnable{
    private Queue<FireIncidentEvent> incidentQueue = new LinkedList<>();
    private Queue<Drone> availableDrones = new LinkedList<>();


    public synchronized void registerDrone(Drone drone){
        availableDrones.add(drone);
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
        System.out.println("New fire " + fireIncidentEvent.toString() );
        notifyAll();
    }

    public synchronized void handleEvent(){
        while(incidentQueue.isEmpty() || availableDrones.isEmpty()){
            try{
                wait();
            }catch (InterruptedException e) {throw new RuntimeException();}
        }
        FireIncidentEvent e = incidentQueue.poll();
        Drone drone = availableDrones.poll();

        drone.event(e);
    }

    public synchronized void firePutOut(FireIncidentEvent e){
        System.out.println("Fire is put out " + e.toString());
        notifyAll();
    }
    @Override
    public void run() {
        while(true){
            handleEvent();
            if(incidentQueue.isEmpty())return;
        }
    }
}
