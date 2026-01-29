//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
            Scheduler scheduler = new Scheduler();
            Thread schedulerThread = new Thread(scheduler);
            Thread drone = new Thread(new Drone(scheduler));
            Thread fireIncident = new Thread(new FireIncident(scheduler));

            fireIncident.start();
            schedulerThread.start();
            drone.start();



    }
}