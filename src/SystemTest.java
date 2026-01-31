
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class SystemTest{
    @Test
    public void testFireSubsystemFileRead() throws InterruptedException {
        GUI gui = new GUI();
        Scheduler scheduler = new Scheduler(gui);

        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(scheduler, gui);

        fireIncidentSubsystem.zones.put(9, new Zone(9, 700, 0, 1400, 1500));
        fireIncidentSubsystem.zones.put(1, new Zone(1, 0, 0, 700, 1500));
        fireIncidentSubsystem.zones.put(5, new Zone(5, 700, 0, 1400, 1500));

        FireIncidentEvent e = fireIncidentSubsystem.parseIncidentFiles("Event_File.csv").get(0);
        System.out.println(e.getZone().toString());


        assertEquals(9, e.getZone().getId());
        assertEquals(FireIncidentEvent.Status.FIRE_DETECTED, e.getStatus());
        assertEquals(FireIncidentEvent.Severity.High, e.getSeverity());

    }
    @Test
    public void testCommunication() throws InterruptedException {
        GUI gui = new GUI();
        Scheduler scheduler = new Scheduler(gui);

        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(scheduler, gui);
        fireIncidentSubsystem.zones.put(9, new Zone(9, 700, 0, 1400, 1500));
        fireIncidentSubsystem.zones.put(1, new Zone(1, 0, 0, 700, 1500));
        fireIncidentSubsystem.zones.put(5, new Zone(5, 700, 0, 1400, 1500));

        Drone drone = new Drone(scheduler, gui);
        scheduler.registerDrone(drone);

        FireIncidentEvent e = fireIncidentSubsystem.parseIncidentFiles("Event_File.csv").get(0);

        assertEquals(FireIncidentEvent.Status.FIRE_DETECTED, e.getStatus());
        scheduler.newIncident(e);
        assertTrue(e.getZone().isFireActive());
        scheduler.handleEvent();
        drone.dropWater();
        assertEquals(FireIncidentEvent.Status.DRONE_REQUESTED, e.getStatus());
        assertEquals(e.getZone().isFireActive(), false);


    }

}
