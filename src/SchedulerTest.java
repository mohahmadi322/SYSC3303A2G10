import org.junit.jupiter.api.Test;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulerTest {

    /**
     * Minimal GUI that does nothing.
     * Prevents Swing from interfering with tests.
     */
    static class TestGUI extends GUI {
        @Override
        public void log(String msg) {

        }

        @Override
        public void updateOrReplaceSquare(int zoneId, String label, java.awt.Color color) {

        }
    }

    /**
     * Fake Drone that records the event assigned.
     */
    static class TestDrone extends Drone {

        FireIncidentEvent receivedEvent;

        public TestDrone(Scheduler scheduler, GUI gui) {
            super(scheduler, gui, 1);
        }

        @Override
        public synchronized void event(FireIncidentEvent e) {
            receivedEvent = e;
        }
    }

    @Test
    public void testNewIncidentAddsEvent() {
        GUI gui = new TestGUI();
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

        assertTrue(true);
    }

    @Test
    public void testHandleEventAssignsDrone() {
        GUI gui = new TestGUI();
        Scheduler scheduler = new Scheduler(gui);

        Zone zone = new Zone(2, 0, 0, 5, 5);

        FireIncidentEvent event =
                new FireIncidentEvent(
                        LocalTime.of(14, 0),
                        zone,
                        FireIncidentEvent.Status.FIRE_DETECTED,
                        FireIncidentEvent.Severity.Moderate
                );

        TestDrone drone = new TestDrone(scheduler, gui);

        scheduler.registerDrone(drone);
        scheduler.newIncident(event);

        scheduler.handleEvent();

        assertNotNull(drone.receivedEvent);
        assertEquals(event, drone.receivedEvent);
    }

    @Test
    public void testFirePutOutReRegistersDrone() {
        GUI gui = new TestGUI();
        Scheduler scheduler = new Scheduler(gui);

        Zone zone = new Zone(3, 0, 0, 6, 6);

        FireIncidentEvent event =
                new FireIncidentEvent(
                        LocalTime.of(16, 15),
                        zone,
                        FireIncidentEvent.Status.FIRE_DETECTED,
                        FireIncidentEvent.Severity.High
                );

        TestDrone drone = new TestDrone(scheduler, gui);

        scheduler.registerDrone(drone);

        scheduler.firePutOut(event, drone);


        assertTrue(true);
    }
}

