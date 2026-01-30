
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
public class SystemTest{
    @Test
    public void testFireSubsystemFileRead() throws InterruptedException {

        Scheduler scheduler = new Scheduler();

        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(scheduler);

        fireIncidentSubsystem.zones.put(3, new Zone(3, 700, 0, 1400, 1500));

        FireIncidentEvent e = fireIncidentSubsystem.parseIncidentFiles("Event_File.csv").get(0);
        System.out.println(e.getZone().toString());


        assertEquals(3, e.getZone().getId());
        assertEquals(FireIncidentEvent.Status.FIRE_DETECTED, e.getStatus());
        assertEquals(FireIncidentEvent.Severity.Low, e.getSeverity());

    }
    @Test
    public void testCommunication() throws InterruptedException {
        Scheduler scheduler = new Scheduler();
        Drone drone = new Drone(scheduler);
        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(scheduler);
        fireIncidentSubsystem.zones.put(3, new Zone(3, 700, 0, 1400, 1500));

        FireIncidentEvent e = fireIncidentSubsystem.parseIncidentFiles("Event_File.csv").get(0);

        assertEquals(FireIncidentEvent.Status.FIRE_DETECTED, e.getStatus());
        scheduler.newIncident(e);
        assertTrue(e.getZone().isFireActive());
        scheduler.registerDrone(drone);
        scheduler.handleEvent();
        drone.dropWater();
        assertEquals(FireIncidentEvent.Status.DRONE_REQUESTED, e.getStatus());
        assertEquals(e.getZone().isFireActive(), false);


    }

}
