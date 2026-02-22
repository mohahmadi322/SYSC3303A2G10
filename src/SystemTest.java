
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Test class to test the system and the communication between the subsystems.
 * @author Mohammad Ahmadi 101267874
 * @date 2026-01-31
 */
public class SystemTest{
    /**
     * Tests FireIncident object created by FireIncidentSubsystem after reading the csv files.
     * @throws InterruptedException
     */
    @Test
    public void testFireSubsystemFileRead() throws InterruptedException {
        GUI gui = new GUI();//GUI
        Scheduler scheduler = new Scheduler(gui);//Scheduler

        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(scheduler, gui);

        //Create a zone from the CSV file.
        fireIncidentSubsystem.zones = fireIncidentSubsystem.parseZones("Zone_File.csv");
        //Read the FireIncdient Event from CSV file and create an object
        FireIncidentEvent e = fireIncidentSubsystem.parseIncidentFiles("Event_File.csv").get(0);
        //Assert that the zones have the same ID.
        assertEquals(9, e.getZone().getId());
        //Assert that the status is the same as its suppose to be in the file.
        assertEquals(FireIncidentEvent.Status.FIRE_DETECTED, e.getStatus());
        //Assert that the severity is the same as its suppose to be in the file.
        assertEquals(FireIncidentEvent.Severity.Low, e.getSeverity());

    }
    /**
     * Test the communication between the FireIncidentSubsystem and Drone subsystems through the scheduler.
     * @throws InterruptedException
     */
    @Test
    public void testCommunication() throws InterruptedException {
        GUI gui = new GUI();
        Scheduler scheduler = new Scheduler(gui);

        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(scheduler, gui);

        Drone drone = new Drone(scheduler, gui, 1);
        scheduler.registerDrone(drone);


        //Create a zone from the CSV file.
        fireIncidentSubsystem.zones = fireIncidentSubsystem.parseZones("Zone_File.csv");
        //Read the FireIncdient Event from CSV file and create an object
        FireIncidentEvent e = fireIncidentSubsystem.parseIncidentFiles("Event_File.csv").get(0);
        //Check that status of event is correct.
        assertEquals(FireIncidentEvent.Status.FIRE_DETECTED, e.getStatus());
        //Incident Subsystem sends event to scheduler.
        scheduler.newIncident(e);
        //Check that zone status has been updated.
        assertTrue(e.getZone().isFireActive());
        //Scheduler handles event by assigning it to a drone.
        scheduler.handleEvent();
        drone.dropWater();
        //Check that status of event is updated
        //
        // .
        assertEquals(FireIncidentEvent.Status.FIRE_DETECTED, e.getStatus());
        assertEquals(e.getZone().isFireActive(), false);


    }

}
