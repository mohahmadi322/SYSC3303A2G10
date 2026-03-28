/*
import java.net.DatagramSocket;
import java.time.LocalTime;
import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulerTest {

    Scheduler scheduler;

    @BeforeEach
    void setup() throws Exception {
        GUI gui = new GUI();

        // Anonymous subclass prevents socket creation
        scheduler = new Scheduler(gui) {
            DatagramSocket sendSocket;
            DatagramSocket receiveSocket;

            {
                // Override any sockets with dummy sockets
                // Use ephemeral ports (0) to avoid conflicts
                sendSocket = new DatagramSocket(0);
                receiveSocket = new DatagramSocket(0);
            }

            @Override
            public void run() {
                // do nothing for tests
            }

            @Override
            protected void initSockets() {
                // skip real networking in tests
            }
        };

        // Add test drones
        Scheduler.DroneInfo d1 = scheduler.new DroneInfo(1, 6001);
        Scheduler.DroneInfo d2 = scheduler.new DroneInfo(2, 6002);
        scheduler.getDrones().put(1, d1);
        scheduler.getDrones().put(2, d2);



    }

    @Test
    void queuedFireAssignedAfterDroneReturns() throws UnknownHostException {
        FireIncidentEvent f1 = new FireIncidentEvent(
                LocalTime.of(2, 0),
                new Zone(1, 0, 0, 5, 5),
                FireIncidentEvent.Status.FIRE_DETECTED,
                FireIncidentEvent.Severity.Low
        );

        FireIncidentEvent f2 = new FireIncidentEvent(
                LocalTime.of(5, 0),
                new Zone(2, 0, 0, 5, 5),
                FireIncidentEvent.Status.FIRE_DETECTED,
                FireIncidentEvent.Severity.Low
        );

        scheduler.getFireQueue().add(f1);
        scheduler.getFireQueue().add(f2);

        // Pick a drone and simulate it finishing first fire
        Scheduler.DroneInfo d = scheduler.getDrones().get(1);
        d.state = Scheduler.DroneInfo.DroneState.IDLE;

        // Assign first fire
        scheduler.assignDrone();
        d.state = Scheduler.DroneInfo.DroneState.IDLE; // simulate drone return

        // Assign second fire
        scheduler.assignDrone();

        assertEquals(Scheduler.DroneInfo.DroneState.OUTBOUND, d.state);

        // f2 should now be removed from queue since it’s assigned
        assertFalse(scheduler.getFireQueue().contains(f2), "f2 should have been assigned");
    }

*
     @Test
     void droneWithoutWaterSkipped() throws UnknownHostException {
     scheduler.getDrones().get(1).agentRemaining = 0;
     scheduler.getDrones().get(2).agentRemaining = 10;

     scheduler.getFireQueue().add(new FireIncidentEvent(
     LocalTime.of(2, 0),
     new Zone(1, 0, 0, 5, 5),
     FireIncidentEvent.Status.FIRE_DETECTED,
     FireIncidentEvent.Severity.Low
     ));

     scheduler.assignDrone();

     assertEquals(Scheduler.DroneInfo.DroneState.OUTBOUND, scheduler.getDrones().get(2).state);
     }




  */
/**
         * Fake DroneSubsystem that records the event assigned.
         *//*




        static class TestDrone extends DroneSubsystem {

            FireIncidentEvent receivedEvent;

            public TestDrone(Scheduler scheduler) {
                droneInfo = scheduler.new DroneInfo(id, 6001);
                scheduler.getDrones().put(id, droneInfo);
            }

            @Override
            public synchronized void event(FireIncidentEvent e) {
                receivedEvent = e;
            }
        }

        @Test
        public void testNewIncidentAddsEvent() throws UnknownHostException {
            GUI gui = new SchedulerTest.TestGUI();
            Scheduler scheduler = new Scheduler(gui);

            Zone zone = new Zone(1, 0, 0, 10, 10);

            FireIncidentEvent event =
                    new FireIncidentEvent(
                            LocalTime.of(12, 30),
                            zone,
                            FireIncidentEvent.Status.FIRE_DETECTED,
                            FireIncidentEvent.Severity.Low
                    );

            scheduler.newIncident(event);

            assertTrue(scheduler.getStatus() == Scheduler.Status.FIRE_DETECTED);
        }

        @Test
        public void testHandleEventAssignsDrone() throws UnknownHostException {
            GUI gui = new SchedulerTest.TestGUI();
            Scheduler scheduler = new Scheduler(gui);

            Zone zone = new Zone(2, 0, 0, 5, 5);

            FireIncidentEvent event =
                    new FireIncidentEvent(
                            LocalTime.of(14, 0),
                            zone,
                            FireIncidentEvent.Status.FIRE_DETECTED,
                            FireIncidentEvent.Severity.Moderate
                    );

            SchedulerTest.TestDrone drone = new SchedulerTest.TestDrone(scheduler, gui);

            scheduler.registerDrone(drone.getID());
            scheduler.newIncident(event);

            scheduler.handleEvent(Scheduler.Event.NEW_FIRE);

            assertNotNull(drone.receivedEvent);
            assertEquals(event, drone.receivedEvent);
        }

        @Test
        public void testFirePutOutReRegistersDrone() {
            GUI gui = new SchedulerTest.TestGUI();
            Scheduler scheduler = new Scheduler(gui);

            Zone zone = new Zone(3, 0, 0, 6, 6);

            FireIncidentEvent event =
                    new FireIncidentEvent(
                            LocalTime.of(16, 15),
                            zone,
                            FireIncidentEvent.Status.FIRE_DETECTED,
                            FireIncidentEvent.Severity.High
                    );

            SchedulerTest.TestDrone drone = new SchedulerTest.TestDrone(scheduler, gui);

            scheduler.registerDrone(drone.getID());

            try {
                scheduler.firePutOut(event, drone.getID());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }


            assertTrue(true);
    }


}
*/
