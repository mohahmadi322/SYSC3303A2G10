
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.net.UnknownHostException;
import java.time.LocalTime;

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
    void testWaterRequiredLow() throws UnknownHostException {

        DroneSubsystem drone = new DroneSubsystem();

        Zone zone = new Zone(0,0,0,1,1);

        FireIncidentEvent event =
                new FireIncidentEvent(
                        LocalTime.of(10,0),
                        zone,
                        FireIncidentEvent.Status.FIRE_DETECTED,
                        FireIncidentEvent.Severity.Low
                );

        assertEquals(10, drone.waterRequired(event));
    }



    @Test
    void testWaterRequiredModerate() throws UnknownHostException {

        DroneSubsystem drone = new DroneSubsystem();

        Zone zone = new Zone(1, 0, 0, 1, 1);

        FireIncidentEvent event =
                new FireIncidentEvent(
                        LocalTime.of(10,0),
                        zone,
                        FireIncidentEvent.Status.FIRE_DETECTED,
                        FireIncidentEvent.Severity.Moderate
                );

        assertEquals(20, drone.waterRequired(event));
    }


    @Test
    void testWaterRequiredHigh() throws UnknownHostException {

        DroneSubsystem drone = new DroneSubsystem();

        Zone zone = new Zone(2, 0, 0, 1, 1);

        FireIncidentEvent event =
                new FireIncidentEvent(
                        LocalTime.of(10,0),
                        zone,
                        FireIncidentEvent.Status.FIRE_DETECTED,
                        FireIncidentEvent.Severity.High
                );

        assertEquals(30, drone.waterRequired(event));
    }


    @Test
    void testCalculateTime() throws UnknownHostException {

        DroneSubsystem drone = new DroneSubsystem();

        Zone zone = new Zone(3, 0, 0, 3, 4); // distance = 5

        double time = drone.calculateTime(zone);

        assertTrue(time > 0);
    }


    @Test
    void testEventAssignment() throws UnknownHostException {

        DroneSubsystem drone = new DroneSubsystem();

        Zone zone = new Zone(4,0,0,1,1);

        FireIncidentEvent event =
                new FireIncidentEvent(
                        LocalTime.of(10,0),
                        zone,
                        FireIncidentEvent.Status.FIRE_DETECTED,
                        FireIncidentEvent.Severity.Low
                );

        drone.event(event);

        assertNotNull(event);
    }


    @Test
    void testStop() throws UnknownHostException {
        FakeGUI gui = new FakeGUI();
        Scheduler scheduler = new Scheduler(gui) {
            @Override
            protected void initSockets() {

            }
        };
        //DroneSubsystem drone = new DroneSubsystem( 1);
        //Scheduler.DroneInfo drone = scheduler.new DroneInfo(1, 6001);
        DroneSubsystem drone = new DroneSubsystem();

        drone.stop();

        // If no exception, stop works
        assertTrue(true);
    }


}

