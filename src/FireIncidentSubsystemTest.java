import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSubsystemTest {

    private FireIncidentSubsystem subsystem;
    private Scheduler fakeScheduler;

    @BeforeEach
    void setup() {

        fakeScheduler = new Scheduler(new GUI()) {
            @Override
            public synchronized void newIncident(FireIncidentEvent fireIncidentEvent) {
                // Do nothing for test
            }
        };
        subsystem = new FireIncidentSubsystem(fakeScheduler, new GUI());

        // Prepare zones map
        HashMap<Integer, Zone> zones = new HashMap<>();
        zones.put(1, new Zone(1, 0,0,10,10));
        zones.put(2, new Zone(2, 11,0,20,10));
        subsystem.zones = zones;
    }

    // Helper to get the 'time' field from FireIncidentEvent via reflection
    private LocalTime getTimeField(FireIncidentEvent event) {
        try {
            Field timeField = event.getClass().getDeclaredField("time"); // matches actual field
            timeField.setAccessible(true);
            return (LocalTime) timeField.get(event);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not access time field via reflection: " + e.getMessage());
            return null;
        }
    }

    @Test
    void testReadIncidentEvent() {
        String[] csvRow = {"12:30:00", "1", "FIRE_DETECTED", "Low"};

        FireIncidentEvent event = subsystem.readIncidentEvent(csvRow);

        assertEquals(subsystem.zones.get(1), event.getZone());
        assertEquals(FireIncidentEvent.Status.FIRE_DETECTED, event.getStatus());
        assertEquals(FireIncidentEvent.Severity.Low, event.getSeverity());
        assertEquals(LocalTime.of(12,30,0), getTimeField(event));
    }

    @Test
    void testMultipleIncidentEvents() {
        String[] row1 = {"08:15:30", "1", "FIRE_DETECTED", "High"};
        String[] row2 = {"09:45:00", "2", "FIRE_DETECTED", "Moderate"};

        FireIncidentEvent e1 = subsystem.readIncidentEvent(row1);
        FireIncidentEvent e2 = subsystem.readIncidentEvent(row2);

        ArrayList<FireIncidentEvent> events = new ArrayList<>();
        events.add(e1);
        events.add(e2);

        assertEquals(LocalTime.of(8,15,30), getTimeField(events.get(0)));
        assertEquals(FireIncidentEvent.Severity.High, events.get(0).getSeverity());

        assertEquals(LocalTime.of(9,45,0), getTimeField(events.get(1)));
        assertEquals(FireIncidentEvent.Severity.Moderate, events.get(1).getSeverity());
    }

    @Test
    void testFirePutoutUpdatesZone() {
        Zone testZone = new Zone(1,0,0,10,10);
        assertFalse(testZone.isFireActive());
        testZone.activeFire();
        assertTrue(testZone.isFireActive());

        subsystem.firePutout(testZone);
        assertFalse(testZone.isFireActive());
    }
}




