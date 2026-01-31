//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        GUI gui = new GUI();
        Scheduler scheduler = new Scheduler(gui);

        Thread schedulerThread = new Thread(scheduler);
        Thread fireIncident = new Thread(new FireIncidentSubsystem(scheduler, gui));

        fireIncident.start();
        schedulerThread.start();

        // Create multiple drones
        for (int i = 0; i < 3; i++) {
            Thread droneThread = new Thread(new Drone(scheduler, gui));
            droneThread.start();
        }
    }
}