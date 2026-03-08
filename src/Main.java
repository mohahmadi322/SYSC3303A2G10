//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

/**
 * The main class, entry point for program. Runs the simulation.
 */
public class Main {
    public static void main(String[] args) {
        GUI gui = new GUI();
        Scheduler scheduler = new Scheduler(gui);

        Thread schedulerThread = new Thread(scheduler);
        Thread fireIncident = new Thread(new FireIncidentSubsystem(scheduler, gui));
        Thread droneThread = new Thread(new Drone(scheduler, gui, 1));
        droneThread.start();
        fireIncident.start();
        schedulerThread.start();

    }
}