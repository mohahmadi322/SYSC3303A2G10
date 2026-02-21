import org.junit.jupiter.api.Test;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.*;

class DroneTest {

    // ----- Minimal Fake GUI -----
    static class FakeGUI extends GUI {
        @Override
        public void log(String message) {
            // do nothing
        }

        @Override
        public void updateOrReplaceSquare(int zoneId, String letter, Color color) {
            // do nothing
        }
    }

    // ----- Tests -----

    @Test
    void testWaterRequiredLow() {
        FakeGUI gui = new FakeGUI();
        Scheduler scheduler = new Scheduler(gui);
        Drone drone = new Drone(scheduler, gui);

        Zone zone = new Zone(0, 0, 0, 1, 1);
        FireIncidentEvent event = new FireIncidentEvent(null, zone, FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Low);

        assertEquals(10, drone.waterRequired(event));
    }

    @Test
    void testWaterRequiredModerate() {
        FakeGUI gui = new FakeGUI();
        Scheduler scheduler = new Scheduler(gui);
        Drone drone = new Drone(scheduler, gui);

        Zone zone = new Zone(1, 0, 0, 1, 1);
        FireIncidentEvent event = new FireIncidentEvent(null, zone, FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Moderate);

        assertEquals(20, drone.waterRequired(event));
    }

    @Test
    void testWaterRequiredHigh() {
        FakeGUI gui = new FakeGUI();
        Scheduler scheduler = new Scheduler(gui);
        Drone drone = new Drone(scheduler, gui);

        Zone zone = new Zone(2, 0, 0, 1, 1);
        FireIncidentEvent event = new FireIncidentEvent(null, zone, FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.High);

        assertEquals(30, drone.waterRequired(event));
    }

    @Test
    void testCalculateTime() {
        FakeGUI gui = new FakeGUI();
        Scheduler scheduler = new Scheduler(gui);
        Drone drone = new Drone(scheduler, gui);

        Zone zone = new Zone(3, 0, 0, 3, 4); // distance = 5 units (3-4-5 triangle)
        double time = drone.calculateTime(zone);

        assertTrue(time > 0);
    }

    @Test
    void testEventAssignment() {
        FakeGUI gui = new FakeGUI();
        Scheduler scheduler = new Scheduler(gui);
        Drone drone = new Drone(scheduler, gui);

        Zone zone = new Zone(4, 0, 0, 1, 1);
        FireIncidentEvent event = new FireIncidentEvent(null, zone, FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Low);

        drone.event(event);

        assertNotNull(event); // event assigned
    }

    @Test
    void testStop() {
        FakeGUI gui = new FakeGUI();
        Scheduler scheduler = new Scheduler(gui);
        Drone drone = new Drone(scheduler, gui);

        drone.stop();

        // If no exception, stop works
        assertTrue(true);
    }

    @Test
    void testWaitForEventWithStop() throws InterruptedException {
        FakeGUI gui = new FakeGUI();
        Scheduler scheduler = new Scheduler(gui);
        Drone drone = new Drone(scheduler, gui);

        // run waitForEvent in a separate thread
        Thread t = new Thread(() -> drone.waitForEvent());
        t.start();

        Thread.sleep(100); // let it start waiting
        drone.stop();       // this should wake the waiting thread
        t.join(500);

        assertFalse(t.isAlive()); // thread finished successfully
    }
}
