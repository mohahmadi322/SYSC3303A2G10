import java.util.LinkedList;
import java.util.Queue;

public class Drone implements Runnable{
    private Scheduler scheduler;
    private FireIncidentEvent currentEvent;
    private static int MAXLOAD = 15;
    private static int STARTING_ZONE_X = 0;
    private static int STARTING_ZONE_Y = 0;
    private  double DRONE_SPEED = 26.4;
    private  double TAKE_OFF_TIME = 6;
    private  double LANDING_TIME = 4;
    private  double TIME_TO_OPEN_NOZZLE = 0.5;

    private volatile boolean running = true;
    public enum Status{
        FLIGHT,
        APPROACHING,
        DROPPING_AGENT,
        IDLE
    }
    private Status status;
    public Drone (Scheduler s){
        scheduler = s;
        status = Status.IDLE;
    }
    public int waterRequired(FireIncidentEvent e){
        return switch(e.getSeverity()){
            case Low -> 10;
            case Moderate -> 20;
            case High -> 30;
        };

    }
    public synchronized void event(FireIncidentEvent e){
        this.currentEvent = e;
        notifyAll();
    }

    public double calculateTime(Zone zone){
        int endX = zone.getEndX();
        int endY = zone.getEndY();
        int length = endX - STARTING_ZONE_X;
        int width =  endY - STARTING_ZONE_Y;
        double hypotneus = Math.sqrt(Math.pow(length, 2) + Math.pow(width, 2));
        double time = DRONE_SPEED / hypotneus;
        return time;

    }
    public void travelToZone(){
        currentEvent.changeStatus(FireIncidentEvent.Status.DRONE_REQUESTED);
        System.out.println("Drone is traveling to zone" + currentEvent.getZone().toString());
        try {
            Thread.sleep((int)(TAKE_OFF_TIME * 1000));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        status = Status.FLIGHT;
        System.out.println("Drone status: " + status.toString());
        try {
            Thread.sleep((int)(calculateTime(currentEvent.getZone()) * 1000));
            status = Status.APPROACHING;
            System.out.println("Drone status: " + status.toString());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        try {
            Thread.sleep((int)(LANDING_TIME * 1000));
            System.out.println("Drone has landed");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

    }
    public synchronized FireIncidentEvent dropWater(){
        status = Status.DROPPING_AGENT;
        int waterNeeded = waterRequired(currentEvent);

        double timeLoad = ((MAXLOAD / 10.0) + TIME_TO_OPEN_NOZZLE);
        try {
            Thread.sleep((int)(timeLoad * 1000));
            System.out.println("Drone: Dropped " + waterNeeded + "L at zone " + currentEvent.getZone());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        try {
            Thread.sleep((int)(calculateTime(currentEvent.getZone()) * 1000));
            System.out.println("Drone is heading back to origin");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        status = Status.IDLE;
        FireIncidentEvent event = currentEvent;
        currentEvent = null;
        scheduler.registerDrone(this);
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
    public synchronized void stop(){
        running = false;
        notifyAll();
    }
    @java.lang.Override
    public void run() {
        scheduler.registerDrone(this);
        while(running){
            waitForEvent();
            if(!running)return;
            FireIncidentEvent e = currentEvent;
            travelToZone();
            dropWater();
            scheduler.firePutOut(e);
        }
    }
}
