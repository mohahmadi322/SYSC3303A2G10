import java.util.LinkedList;
import java.util.Queue;

public class Drone implements Runnable{
    private Scheduler scheduler;
    private FireIncidentEvent currentEvent;
    private static int MAXLOAD = 15;
    private static int STARTING_ZONE_X = 0;
    private static int STARTING_ZONE_Y = 0;

    private static double DRONE_SPEED = 26.4;

    private static double TAKE_OFF_TIME = 26.4;
    private static double LANDING_TIME = 26.4;


    private static double TIME_TO_OPEN_NOZZLE = 0.5;
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

    public double travelToZone(Zone zone){
        int startX = zone.getStartX();
        int endX = zone.getEndX();
        int startY = zone.getStartY();
        int endY = zone.getEndY();

        int length = startX - endX;
        int width = startY - endY;

        double hypotneus = Math.sqrt(Math.pow(length, 2) + Math.pow(width, 2));


        return hypotneus;
    }


    public synchronized FireIncidentEvent dropWater(){
        currentEvent.changeStatus(FireIncidentEvent.Status.DRONE_REQUESTED);
        status = Status.DROPPING_AGENT;
        int waterNeeded = waterRequired(currentEvent);

        double timeLoad = ((MAXLOAD / 10.0) + TIME_TO_OPEN_NOZZLE);
        try {
            Thread.sleep((int)(timeLoad * 1000));
            System.out.println("Drone: Dropped " + waterNeeded + "L at zone " + currentEvent.getZone());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        status = Status.IDLE;
        FireIncidentEvent event = currentEvent;
        currentEvent.getZone().fireExtinguished();
        currentEvent = null;
        return event;
    }
    public synchronized void waitForEvent(){
        while(currentEvent == null){
            try{
                System.out.println("Drone has no mission");
                wait();
            }catch (InterruptedException exception) {throw new RuntimeException();}
        }

    }
    @java.lang.Override
    public void run() {
        scheduler.registerDrone(this);
        while(true){
            waitForEvent();
            FireIncidentEvent e = currentEvent;
            dropWater();
            scheduler.registerDrone(this);
            scheduler.firePutOut(e);
        }
    }
}
